package id.rockierocker.imagetools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ConsumeJobData {
    private String outputPath;
    private String ext;
    private Integer width;
    private Integer height;
}
