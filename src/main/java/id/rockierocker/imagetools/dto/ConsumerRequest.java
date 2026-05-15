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
public class ConsumerRequest<T> {
    private String requestId;
    private Boolean callRunpodSync;
    private T data;
}
