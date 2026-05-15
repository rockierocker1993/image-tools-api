package id.rockierocker.imagetools.component.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Service
public class S3LocalMinio extends S3Abstract {

    public S3LocalMinio(
            @Qualifier("minioS3Client") S3Client s3Client,
            @Qualifier("minioS3Presigner") S3Presigner s3Presigner) {
        super(s3Client, s3Presigner);
    }

    @Value("${minio.s3.bucket-name}")
    private String bucketName;

    @Value("${minio.s3.region}")
    private String region;

    @Value("${minio.s3.endpoint-url}")
    private String endpointUrl;

    @Value("${minio.s3.presigned-url-expiration-minutes:60}")
    private int presignedUrlExpirationMinutes;

    @Override
    protected String getBucketName() {
        return bucketName;
    }

    @Override
    protected String getEndpointUrl() {
        return endpointUrl;
    }

    @Override
    public int getPresignedUrlExpirationMinutes() {
        return presignedUrlExpirationMinutes;
    }

    @Override
    protected String getRegion() {
        return region;
    }
}
