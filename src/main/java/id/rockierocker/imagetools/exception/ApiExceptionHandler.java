package id.rockierocker.imagetools.exception;


import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.dto.BaseResponse;
import id.rockierocker.imagetools.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {
        ResponseCode responseCode = ex.getResponseCode();
        return ResponseEntity.badRequest().body(ResponseUtil.buildSuccessResponse(responseCode, null));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse> handleUnathorizedException(
            UnauthorizedException ex,
            HttpServletRequest request) {
        ResponseCode responseCode = ex.getResponseCode();
        return new ResponseEntity<>(ResponseUtil.buildSuccessResponse(responseCode, null), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<BaseResponse> handleInternalException(
            InternalServerErrorException ex,
            HttpServletRequest request) {

        ResponseCode responseCode = ex.getResponseCode();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.buildSuccessResponse(responseCode, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGeneralException(
            Exception ex,
            HttpServletRequest request) {
        ResponseCode responseCode = ResponseCode.UKNOWN_ERROR;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseUtil.buildSuccessResponse(responseCode, null));
    }
}
