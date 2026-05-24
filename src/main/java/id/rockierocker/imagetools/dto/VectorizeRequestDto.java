package id.rockierocker.imagetools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class VectorizeRequestDto {
    private Boolean webhookEnabled = false;
    private String image;
    private String modelSize = "4B";
    private Float temperature = 0.3f;
    private Float topP = 0.9f;
    private Integer topK = 50;
    private Float repetitionPenalty = 1.05f;
    private Integer maxLength = 1024;
    private Boolean replaceBackground = true;
    private Boolean returnSvg = true;
    private Integer numCandidates = 3;
}
