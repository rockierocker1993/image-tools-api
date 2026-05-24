package id.rockierocker.imagetools.service.tools.job;

import id.rockierocker.imagetools.component.RedisPublisher;
import id.rockierocker.imagetools.component.WebSocketPublisher;
import id.rockierocker.imagetools.component.s3.S3LocalMinio;
import id.rockierocker.imagetools.component.s3.S3Runpod;
import id.rockierocker.imagetools.constant.Module;
import id.rockierocker.imagetools.dto.*;
import id.rockierocker.imagetools.repository.ImageRepository;
import id.rockierocker.imagetools.repository.UserActivityRepository;
import id.rockierocker.imagetools.service.OutputDirectoryManagerService;
import id.rockierocker.imagetools.service.PreprocessService;
import id.rockierocker.imagetools.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UpscalerJobService extends AbstractJob<UpscalerRequestDto, UpscalerResponseDto> {

    @Value("${redis.channel.job-upscaler-request}")
    private String jobChannel;
    @Value("${aws.s3.upload-temp.upscaler:upload-temp-upscaler}")
    private String uploadTemp;
    @Value("${minio.path.webp.upscaler}")
    private String minioWebpPath;
    @Value("${minio.path.result.upscaler}")
    private String minioResultPath;
    @Value("${upscaler.preprocess-config-code}")
    private String preprocessCode;

    public UpscalerJobService(S3Runpod awsS3Runpod, S3LocalMinio awsS3LocalMinio, UserService userService, OutputDirectoryManagerService outputDirectoryManagerService, PreprocessService preprocessService, RedisPublisher redisPublisher, WebSocketPublisher webSocketPublisher, UserActivityRepository userActivityRepository, ImageRepository imageRepository, RedisTemplate<String, String> redisTemplate) {
        super(awsS3Runpod, awsS3LocalMinio, userService, outputDirectoryManagerService, preprocessService, redisPublisher, webSocketPublisher, userActivityRepository, imageRepository, redisTemplate);
    }

    @Override
    protected String getJobChannel() {
        return jobChannel;
    }

    @Override
    protected String getUploadTemp() {
        return uploadTemp;
    }

    @Override
    protected String getPathWebp() {
        return minioWebpPath;
    }

    @Override
    protected String getPathResult() {
        return minioResultPath;
    }

    @Override
    protected UpscalerRequestDto buildConsumerRequestData(String image, JobRequestDto jobRequestDto) {
        return UpscalerRequestDto.builder()
                .webhookEnabled(true)
                .scale(Optional.ofNullable(jobRequestDto)
                        .filter(item -> Objects.nonNull(item.getScale()))
                        .map(JobRequestDto::getScale).orElse(2))
                .image(image)
                .outputFormat("png")
                .outputQuality(100)
                .build();
    }

    @Override
    protected ConsumeJobData extractConsumeJobData(UpscalerResponseDto upscalerResponseDto) {
        return ConsumeJobData.builder()
                .outputPath(upscalerResponseDto.getOutputVolume())
                .ext(upscalerResponseDto.getFormat())
                .width(Optional.ofNullable(upscalerResponseDto.getOutputSize()).filter(size -> !size.isEmpty()).map(size -> size.get(0)).orElse(null))
                .height(Optional.ofNullable(upscalerResponseDto.getOutputSize()).filter(size -> size.size() > 1).map(size -> size.get(1)).orElse(null))
                .build();
    }

    @Override
    protected Module getModule() {
        return Module.UPSCALE;
    }

    @Override
    protected String getPreprocessCode() {
        return preprocessCode;
    }
}
