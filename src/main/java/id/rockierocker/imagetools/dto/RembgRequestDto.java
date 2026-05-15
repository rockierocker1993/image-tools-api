package id.rockierocker.imagetools.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class RembgRequestDto {
    private Boolean webhookEnabled = false;
    private String image;
    private String model = "birefnet-general";
    private String outputFormat = "png";
    private Integer outputQuality = 100;
}
