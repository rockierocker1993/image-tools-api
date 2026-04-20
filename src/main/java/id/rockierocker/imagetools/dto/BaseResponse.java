package id.rockierocker.imagetools.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@Builder
public class BaseResponse<T> {
    private String responseCode;
    private boolean status;
    private String messageId;
    private String messageEn;
    private String titleId;
    private String titleEn;
    private T data;
}
