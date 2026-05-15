package id.rockierocker.imagetools.util;

import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
public class ValidatorUtil {

    @Value("${allowed-image-extensions:png,jpg,jpeg}")
    private String allowedExtensionsRaw;

    private static List<String> allowedExtensions;

    @PostConstruct
    private void initStaticEnv() {
        allowedExtensions = Arrays.asList(allowedExtensionsRaw.split(","));
    }

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

    public static void validateAllowedImageExt(String ext) {
        if (!ext.isEmpty() && !allowedExtensions.contains(ext)) {
            throw new BadRequestException(ResponseCode.EXTENSION_NOT_SUPPORTED);
        }
    }
}
