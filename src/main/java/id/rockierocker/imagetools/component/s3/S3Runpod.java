package id.rockierocker.imagetools.component.s3;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Service
public class S3Runpod extends S3Abstract {

    public S3Runpod(
            @Qualifier("runpodS3Client") S3Client s3Client,
            @Qualifier("runpodS3Presigner") S3Presigner s3Presigner) {
        super(s3Client, s3Presigner);
    }

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.endpoint-url}")
    private String endpointUrl;

    @Value("${aws.s3.presigned-url-expiration-minutes:60}")
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
    protected int getPresignedUrlExpirationMinutes() {
        return presignedUrlExpirationMinutes;
    }

    @Override
    protected String getRegion() {
        return region;
    }
}
