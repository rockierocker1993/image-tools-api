package id.rockierocker.imagetools.dto;

import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class VectorizeResponseDto {
    private String jobId;
    private Long processingTime;
    private String inputStorageMode;
    private String outputStorageMode;
    private String outputUrl;
    private String outputVolume;
    private String format;
    private String modelSize;
    private Integer numCandidates;
    private Integer pathCount;
    private List<Integer> originalSize;
    private List<Integer> targetSize;
    private Integer svgBytes;
    private String svg;
}
