package id.rockierocker.imagetools.util;

import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.exception.BadRequestException;
import org.springframework.validation.BindingResult;

import java.util.Objects;

public class ValidatorUtil {
    public static void throwIfHasError(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String firstErrorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            ResponseCode responseCode = ResponseCode.findByCode(firstErrorMessage);
            if (Objects.nonNull(responseCode)) {
                throw new BadRequestException(responseCode);
            } else {
                responseCode = ResponseCode.BAD_REQUEST;
                responseCode.setMessageEn(firstErrorMessage);
                responseCode.setMessageId(firstErrorMessage);
                throw new BadRequestException(responseCode);
            }
        }
    }
}
