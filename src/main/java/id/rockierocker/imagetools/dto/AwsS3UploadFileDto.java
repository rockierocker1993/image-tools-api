package id.rockierocker.imagetools.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AwsS3UploadFileDto {
    private String folder;
    private String fullPath;
    private String uuid;
    private String extension;
    private String publicUrl;
    private String imageName;
}
