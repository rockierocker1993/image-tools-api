package id.rockierocker.imagetools.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class SvgVectorCreateRegionDto {
    private byte[] svgRegion;
    private List<SvgVectorColorRegion> regions;
}
