package id.rockierocker.imagetools.exception;

import id.rockierocker.imagetools.constant.ResponseCode;
import lombok.Getter;

@Getter
public class InternalServerErrorException extends RuntimeException {

    private final ResponseCode responseCode;

    public InternalServerErrorException(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }
}
