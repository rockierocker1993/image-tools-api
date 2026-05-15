package id.rockierocker.imagetools.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class AwsS3Config {

    // -------------------------------------------------------------------------
    // RunPod S3 — credentials dari aws.s3.*
    // -------------------------------------------------------------------------

    @Value("${aws.s3.access-key}")
    private String runpodAccessKey;

    @Value("${aws.s3.secret-key}")
    private String runpodSecretKey;

    @Value("${aws.s3.region}")
    private String runpodRegion;

    @Value("${aws.s3.endpoint-url}")
    private String runpodEndpointUrl;

    @Bean("runpodS3Client")
    @Primary
    public S3Client runpodS3Client() {
        return buildS3Client(runpodAccessKey, runpodSecretKey, runpodRegion, runpodEndpointUrl);
    }

    @Bean("runpodS3Presigner")
    @Primary
    public S3Presigner runpodS3Presigner() {
        return buildS3Presigner(runpodAccessKey, runpodSecretKey, runpodRegion, runpodEndpointUrl);
    }

    // -------------------------------------------------------------------------
    // MinIO Local S3 — credentials dari minio.s3.*
    // -------------------------------------------------------------------------

    @Value("${minio.s3.access-key}")
    private String minioAccessKey;

    @Value("${minio.s3.secret-key}")
    private String minioSecretKey;

    @Value("${minio.s3.region:us-east-1}")
    private String minioRegion;

    @Value("${minio.s3.endpoint-url}")
    private String minioEndpointUrl;

    @Bean("minioS3Client")
    public S3Client minioS3Client() {
        return buildS3Client(minioAccessKey, minioSecretKey, minioRegion, minioEndpointUrl);
    }

    @Bean("minioS3Presigner")
    public S3Presigner minioS3Presigner() {
        return buildS3Presigner(minioAccessKey, minioSecretKey, minioRegion, minioEndpointUrl);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private S3Client buildS3Client(String accessKey, String secretKey, String region, String endpointUrl) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpointUrl))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private S3Presigner buildS3Presigner(String accessKey, String secretKey, String region, String endpointUrl) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Presigner.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpointUrl))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
