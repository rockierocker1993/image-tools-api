package id.rockierocker.imagetools.service.job;

import id.rockierocker.imagetools.component.RedisPublisher;
import id.rockierocker.imagetools.component.WebSocketPublisher;
import id.rockierocker.imagetools.component.s3.S3LocalMinio;
import id.rockierocker.imagetools.component.s3.S3Runpod;
import id.rockierocker.imagetools.constant.*;
import id.rockierocker.imagetools.constant.Module;
import id.rockierocker.imagetools.dto.*;
import id.rockierocker.imagetools.dto.websocket.JobNotifyDto;
import id.rockierocker.imagetools.entity.Image;
import id.rockierocker.imagetools.entity.UserActivity;
import id.rockierocker.imagetools.repository.ImageRepository;
import id.rockierocker.imagetools.repository.UserActivityRepository;
import id.rockierocker.imagetools.service.OutputDirectoryManagerService;
import id.rockierocker.imagetools.service.UserService;
import id.rockierocker.imagetools.util.CommonUtil;
import id.rockierocker.imagetools.util.ImageUtil;
import id.rockierocker.imagetools.util.ResponseUtil;
import id.rockierocker.imagetools.util.ValidatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
abstract class AbstractJob<T, R> {

    private final S3Runpod awsS3Runpod;
    private final S3LocalMinio awsS3LocalMinio;

    private final UserService userService;
    private final OutputDirectoryManagerService outputDirectoryManagerService;

    private final RedisPublisher redisPublisher;
    private final WebSocketPublisher webSocketPublisher;

    private final UserActivityRepository userActivityRepository;
    private final ImageRepository imageRepository;

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${runpod.base-volume}")
    protected String runpodBaseVolume;


    @Value("${enableWarmupEndpoint:false}")
    protected boolean enableWarmupEndpoint;


    protected abstract String getJobChannel();

    protected abstract String getUploadTemp();

    protected abstract String getPathWebp();

    protected abstract String getPathResult();

    protected abstract T buildConsumerRequestData(String image);

    protected abstract ConsumeJobData extractConsumeJobData(R consumerRequest);

    protected abstract Module getModule();

    public ResponseEntity<BaseResponse<JobResponseDto>> crateJob(MultipartFile multipartFile, String token) {
        log.info("Creating job {} for file: {}", getModule().name, multipartFile.getOriginalFilename());

        UserDetails userDetails = userService.getCurrentUser();
        String originalFilename = StringUtils.cleanPath(multipartFile.getOriginalFilename() == null ? "" : multipartFile.getOriginalFilename());
        String ext = CommonUtil.getExtensionLower(originalFilename);
        ValidatorUtil.validateAllowedImageExt(ext);
        AwsS3UploadFileDto awsS3UploadFileDto = awsS3Runpod.uploadFile(multipartFile, getUploadTemp());
        String requestId = awsS3UploadFileDto.getUuid();
        String imageName = awsS3UploadFileDto.getImageName();
        redisPublisher.publish(getJobChannel(), requestId, ConsumerRequest.<T>builder()
                .callRunpodSync(true)
                .requestId(requestId)
                .data(buildConsumerRequestData(imageName)).build(), 60L
        );

        userActivityRepository.save(UserActivity.builder()
                .userId(userDetails.getUserId())
                .requestId(requestId)
                .module(getModule().name)
                .activity(Activity.CREATE_JOB.name())
                .userType(userDetails.getUserType().name())
                .build());
        log.info("Job {} careted successfully with request id: {}", getModule().name, requestId);
        return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS,
                JobResponseDto.builder().imageId(requestId).build())
        );
    }

    public void consumeJobResult(ConsumerRequest<R> consumerRequest) {
        log.info("Consuming job {} result for requestId={}", getModule().name, consumerRequest.getRequestId());
        String requestId = consumerRequest.getRequestId();
        ConsumeJobData consumeJobData = extractConsumeJobData(consumerRequest.getData());
        String ext = consumeJobData.getExt();
        String outputVolume = consumeJobData.getOutputPath().replaceAll(runpodBaseVolume, "");
        Integer width = consumeJobData.getWidth();
        Integer height = consumeJobData.getHeight();
        String webpUrl = "";
        boolean isSuccess;
        try {
            byte[] outputFile = awsS3Runpod.downloadFileAsBytes(outputVolume);
            File webpFile = outputDirectoryManagerService.createTempFile("converted-webp-" + requestId + "-", ".webp", outputFile);
            byte[] webpFileByte = ImageUtil.toWebpBytes(webpFile);
            String webpImageName = "webp-" + requestId + ".webp";
            String webpImageKey = awsS3LocalMinio.uploadFile(webpFileByte, webpImageName, "image/webp", getPathWebp());
            webpUrl = awsS3LocalMinio.generatePresignedUrl(webpImageKey);
            String resultImageName = "result-" + requestId + "-" + UUID.randomUUID().toString() + "." + ext;
            String imageKey = awsS3LocalMinio.uploadFile(webpFileByte, resultImageName, "image/" + ext, getPathResult());

            Image image = Image.builder()
                    .imageId(requestId)
                    .userId(userService.getCurrentUser().getUserId())
                    .imageKey(imageKey)
                    .imageName(resultImageName)
                    .imageProvider(StorageProvider.MINIO.name())
                    .webpImageKey(webpImageKey)
                    .webpImageName(webpImageName)
                    .webpProvider(StorageProvider.MINIO.name())
                    .format(ext)
                    .width(width)
                    .height(height)
                    .category(getModule().name())
                    .build();
            imageRepository.save(image);
            isSuccess = true;
        } catch (Exception e) {
            isSuccess = false;
        }
        UserActivity userActivity = userActivityRepository.findFirstByRequestId(requestId).orElse(null);
        if (Objects.nonNull(userActivity)) {
            webSocketPublisher.sendJobResult(userActivity.getUserId(), JobNotifyDto.builder()
                    .requestId(requestId)
                    .status(isSuccess)
                    .webpUrl(webpUrl)
                    .module(getModule())
                    .build());
        }
        awsS3Runpod.deleteFile(outputVolume);

        log.info("Image {} result saved successfully for jobId={}", getModule().name, requestId);
    }

    public ResponseEntity<BaseResponse<String>> warmingUp() {
        log.info("Warming up job {} by sending test message to channel {}", getModule().name, getJobChannel());
        if (!enableWarmupEndpoint) {
            return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, "Warmup endpoint is disabled"));
        }

        String warmingUpKey = "warming-up";
        String alreadyWarmingUp = redisTemplate.opsForValue().get(String.format("%s:%s", getModule().name, warmingUpKey));
        if ("true".equals(alreadyWarmingUp)) {
            log.info("Skipping warming up for job {} because it's already warming up", getModule().name);
            return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, "Already warming up"));
        }
        String runningCountStr = redisTemplate.opsForValue().get(String.format("%s:RUNNING", getModule().name));
        int runningCount = StringUtils.hasText(runningCountStr) ? Integer.parseInt(runningCountStr) : 0;
        if (runningCount > 0) {
            log.info("Skipping warming up for job {} because there are already {} running instances", getModule().name, runningCount);
            return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, "Already running"));
        }
        log.info("No running instances found for job {}, proceeding with warming up", getModule().name);
        redisPublisher.publish(getJobChannel(), warmingUpKey, ConsumerRequest.<T>builder()
                .callRunpodSync(true)
                .requestId(warmingUpKey)
                .data(buildConsumerRequestData(warmingUpKey)).build(), 30L
        );
        redisTemplate.opsForValue().set(String.format("%s:%s", getModule().name, warmingUpKey), "true", Duration.ofSeconds(60 * 10));
        return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, "Warming up started"));
    }

}
