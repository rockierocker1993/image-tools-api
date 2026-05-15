package id.rockierocker.imagetools.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDto {
    private String originalUrl;
    private String originalFormat;
    private String webpUrl;
    private Integer width;
    private Integer height;
}
