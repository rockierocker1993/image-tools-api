package id.rockierocker.imagetools.service.job;

import id.rockierocker.imagetools.component.RedisPublisher;
import id.rockierocker.imagetools.component.WebSocketPublisher;
import id.rockierocker.imagetools.component.s3.S3LocalMinio;
import id.rockierocker.imagetools.component.s3.S3Runpod;
import id.rockierocker.imagetools.constant.Module;
import id.rockierocker.imagetools.dto.ConsumeJobData;
import id.rockierocker.imagetools.dto.RembgRequestDto;
import id.rockierocker.imagetools.dto.RembgResponseDto;
import id.rockierocker.imagetools.repository.ImageRepository;
import id.rockierocker.imagetools.repository.UserActivityRepository;
import id.rockierocker.imagetools.service.OutputDirectoryManagerService;
import id.rockierocker.imagetools.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RembgJobService extends AbstractJob<RembgRequestDto, RembgResponseDto> {

    @Value("${redis.channel.job-rembg-request}")
    private String jobRembgChannel;
    @Value("${aws.s3.upload-temp.rembg:upload-temp-rembg}")
    private String uploadTemp;
    @Value("${minio.path.webp.rembg}")
    private String minioWebpRembgPath;
    @Value("${minio.path.result.rembg}")
    private String minioResultRembgPath;
    @Value("${remove-bg.model:birefnet-general}")
    private String rembgModel;

    public RembgJobService(S3Runpod awsS3Runpod, S3LocalMinio awsS3LocalMinio, UserService userService, OutputDirectoryManagerService outputDirectoryManagerService, RedisPublisher redisPublisher, WebSocketPublisher webSocketPublisher, UserActivityRepository userActivityRepository, ImageRepository imageRepository, RedisTemplate<String, String> redisTemplate) {
        super(awsS3Runpod, awsS3LocalMinio, userService, outputDirectoryManagerService, redisPublisher, webSocketPublisher, userActivityRepository, imageRepository, redisTemplate);
    }

    @Override
    protected String getJobChannel() {
        return jobRembgChannel;
    }

    @Override
    protected String getUploadTemp() {
        return uploadTemp;
    }

    @Override
    protected String getPathWebp() {
        return minioWebpRembgPath;
    }

    @Override
    protected String getPathResult() {
        return minioResultRembgPath;
    }

    @Override
    protected RembgRequestDto buildConsumerRequestData(String image) {
        return RembgRequestDto.builder()
                .webhookEnabled(true)
                .model(rembgModel)
                .image(image)
                .outputFormat("png")
                .outputQuality(100)
                .build();
    }

    @Override
    protected ConsumeJobData extractConsumeJobData(RembgResponseDto rembgResponseDto) {
        return ConsumeJobData.builder()
                .outputPath(rembgResponseDto.getOutputVolume())
                .ext(rembgResponseDto.getFormat())
                .width(Optional.ofNullable(rembgResponseDto.getOutputSize()).filter(size -> !size.isEmpty()).map(size -> size.get(0)).orElse(null))
                .height(Optional.ofNullable(rembgResponseDto.getOutputSize()).filter(size -> size.size() > 1).map(size -> size.get(1)).orElse(null))
                .build();
    }

    @Override
    protected Module getModule() {
        return Module.REMBG;
    }
}
