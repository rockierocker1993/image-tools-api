package id.rockierocker.imagetools.exception;

import id.rockierocker.imagetools.constant.ResponseCode;
import lombok.Getter;

@Getter
public class UnauthorizedException extends RuntimeException {

    private final ResponseCode responseCode;

    public UnauthorizedException(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }
}
