package id.rockierocker.imagetools.util;

import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.dto.BaseResponse;

public class ResponseUtil {

    public static <T> BaseResponse<T> buildSuccessResponse(
            ResponseCode responseCode,
            T data
    ) {
        if (responseCode == null) {
            throw new IllegalArgumentException("responseCode must not be null");
        }

        return BaseResponse.<T>builder()
                .status(responseCode.isStatus())
                .responseCode(responseCode.getCode())
                .messageId(responseCode.getMessageId())
                .messageEn(responseCode.getMessageEn())
                .titleId(responseCode.getTitleId())
                .titleEn(responseCode.getTitleEn())
                .data(data)
                .build();
    }
}

