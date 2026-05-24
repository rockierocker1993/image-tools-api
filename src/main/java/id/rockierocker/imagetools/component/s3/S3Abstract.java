package id.rockierocker.imagetools.component.s3;

import id.rockierocker.imagetools.dto.AwsS3UploadFileDto;
import id.rockierocker.imagetools.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
abstract class S3Abstract {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    protected abstract String getBucketName();
    protected abstract String getEndpointUrl();
    protected abstract int getPresignedUrlExpirationMinutes();
    protected abstract String getRegion();

    /**
     * Upload file dari MultipartFile ke S3.
     *
     * @param multipartFile file yang akan diupload
     * @param folder        folder tujuan di S3 (opsional, bisa kosong atau null)
     * @return public URL atau S3 object key dari file yang diupload
     */
    public AwsS3UploadFileDto uploadFile(MultipartFile multipartFile, String folder) {
        String originalFilename = multipartFile.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        String key = buildKey(folder, uuid, extension);

        log.info("Uploading file to S3: bucket={}, key={}", getBucketName(), key);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .contentType(multipartFile.getContentType())
                    .contentLength(multipartFile.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
            log.info("File uploaded successfully to S3: key={}", key);
            return AwsS3UploadFileDto.builder()
                    .publicUrl(getPublicUrl(key))
                    .fullPath(key)
                    .uuid(uuid)
                    .extension(extension)
                    .folder(folder)
                    .imageName(uuid + (extension.isEmpty() ? "" : "." + extension))
                    .build();
        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Upload file dari byte array ke S3.
     *
     * @param fileBytes     konten file dalam bentuk byte array
     * @param fileName      nama file asli (digunakan untuk ekstensi)
     * @param contentType   MIME type dari file
     * @param folder        folder tujuan di S3 (opsional)
     * @return S3 object key dari file yang diupload
     */
    public String uploadFile(byte[] fileBytes, String fileName, String contentType, String folder) {
        String extension = getExtension(fileName);
        String key = buildKey(folder, UUID.randomUUID().toString(), extension);

        log.info("Uploading byte[] to S3: bucket={}, key={}", getBucketName(), key);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(getBucketName())
                .key(key)
                .contentType(contentType)
                .contentLength((long) fileBytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
        log.info("Byte[] uploaded successfully to S3: key={}", key);
        return key;
    }

    /**
     * Upload file dari File ke S3.
     *
     * @param file          file yang akan diupload
     * @param contentType   MIME type dari file
     * @param folder        folder tujuan di S3 (opsional)
     * @return S3 object key dari file yang diupload
     */
    public AwsS3UploadFileDto uploadFile(File file, String contentType, String folder) {
        String originalFilename = file.getName();
        String extension = getExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        String key = buildKey(folder, uuid, extension);

        log.info("Uploading file to S3: bucket={}, key={}", getBucketName(), key);

        try (InputStream inputStream = java.nio.file.Files.newInputStream(file.toPath())) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.length())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, file.length()));
            log.info("File uploaded successfully to S3: key={}", key);
            return AwsS3UploadFileDto.builder()
                    .publicUrl(getPublicUrl(key))
                    .fullPath(key)
                    .uuid(uuid)
                    .extension(extension)
                    .folder(folder)
                    .imageName(uuid + (extension.isEmpty() ? "" : "." + extension))
                    .build();
        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Hapus file dari S3 berdasarkan key.
     *
     * @param key S3 object key
     */
    public void deleteFile(String key) {
        log.info("Deleting file from S3: bucket={}, key={}", getBucketName(), key);
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(getBucketName())
                .key(key)
                .build();
        s3Client.deleteObject(deleteRequest);
        log.info("File deleted from S3: key={}", key);
    }

    /**
     * Generate presigned URL sementara untuk akses file di S3.
     *
     * @param key S3 object key
     * @return presigned URL yang valid sesuai konfigurasi expiration
     */
    public String generatePresignedUrl(String key) {
        log.info("Generating presigned URL for key={}, expiration={}min", key, getPresignedUrlExpirationMinutes());

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(getBucketName())
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(getPresignedUrlExpirationMinutes()))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();
        log.info("Presigned URL generated: {}", url);
        return url;
    }

    /**
     * Generate public URL untuk file di S3 (hanya berlaku jika bucket/object bersifat publik).
     *
     * @param key S3 object key
     * @return public URL
     */
    public String getPublicUrl(String key) {
        String baseUrl = getEndpointUrl().endsWith("/") ? getEndpointUrl() : getEndpointUrl() + "/";
        return String.format("%s%s/%s", baseUrl, getBucketName(), key);
    }

    /**
     * Cek apakah sebuah object/key ada di S3.
     *
     * @param key S3 object key
     * @return true jika ada, false jika tidak
     */
    public boolean fileExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Download file dari S3 dan kembalikan sebagai byte array.
     *
     * @param key S3 object key
     * @return konten file dalam bentuk byte array
     */
    public byte[] downloadFileAsBytes(String key) {
        log.info("Downloading file from S3 as bytes: bucket={}, key={}", getBucketName(), key);
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();
            byte[] bytes = s3Client.getObjectAsBytes(getRequest).asByteArray();
            log.info("File downloaded successfully from S3: key={}, size={} bytes", key, bytes.length);
            return bytes;
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: key={}", key);
            throw new RuntimeException("File not found in S3: " + key, e);
        } catch (Exception e) {
            log.error("Failed to download file from S3: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Download file dari S3 dan simpan ke direktori lokal.
     * Nama file akan diambil dari bagian akhir key (setelah '/').
     *
     * @param key       S3 object key
     * @param targetDir direktori tujuan penyimpanan
     * @return File yang sudah tersimpan
     */
    public File downloadFileToDir(String key, Path targetDir) {
        String filename = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        Path targetPath = targetDir.resolve(filename);
        return downloadFileToPath(key, targetPath);
    }

    /**
     * Download file dari S3 dan simpan ke path tertentu.
     *
     * @param key        S3 object key
     * @param targetPath path lengkap tujuan penyimpanan file
     * @return File yang sudah tersimpan
     */
    public File downloadFileToPath(String key, Path targetPath) {
        log.info("Downloading file from S3 to path: bucket={}, key={}, target={}", getBucketName(), key, targetPath);
        try {
            Files.createDirectories(targetPath.getParent());

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getRequest)) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("File downloaded successfully from S3 to: {}", targetPath);
            return targetPath.toFile();
        } catch (NoSuchKeyException e) {
            log.error("File not found in S3: key={}", key);
            throw new RuntimeException("File not found in S3: " + key, e);
        } catch (IOException e) {
            log.error("Failed to save downloaded file: path={}, error={}", targetPath, e.getMessage(), e);
            throw new RuntimeException("Failed to save downloaded file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to download file from S3: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Failed to download file from S3: " + e.getMessage(), e);
        }
    }

    /**
     * Download file dari S3 ke temporary file.
     * Caller bertanggung jawab menghapus temp file setelah selesai digunakan.
     *
     * @param key    S3 object key
     * @param prefix prefix nama temp file
     * @param suffix suffix/ekstensi temp file (contoh: ".png")
     * @return temporary File hasil download
     */
    public File downloadFileToTemp(String key, String prefix, String suffix) {
        log.info("Downloading file from S3 to temp: bucket={}, key={}", getBucketName(), key);
        try {
            Path tempPath = Files.createTempFile(prefix, suffix);
            return downloadFileToPath(key, tempPath);
        } catch (IOException e) {
            log.error("Failed to create temp file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create temp file: " + e.getMessage(), e);
        }
    }

    // ==================== Private Helper Methods ====================

    private String buildKey(String folder, String uniqueName, String extension) {
        String filename = uniqueName + (extension.isEmpty() ? "" : "." + extension);
        if (folder == null || folder.isBlank()) {
            return filename;
        }
        String normalizedFolder = folder.endsWith("/") ? folder : folder + "/";
        return normalizedFolder + filename;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
