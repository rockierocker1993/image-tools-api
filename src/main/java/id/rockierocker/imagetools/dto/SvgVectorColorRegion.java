package id.rockierocker.imagetools.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.awt.geom.Rectangle2D;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SvgVectorColorRegion {
    private String id;
    private String color;
    private List<String> elementIds;
    @JsonIgnore
    private Rectangle2D bounds;
    private List<String> neighbors;
}
