package id.rockierocker.imagetools.service;

import id.rockierocker.imagetools.component.s3.S3LocalMinio;
import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.dto.BaseResponse;
import id.rockierocker.imagetools.dto.ImageDto;
import id.rockierocker.imagetools.dto.UserDetails;
import id.rockierocker.imagetools.entity.Image;
import id.rockierocker.imagetools.exception.BadRequestException;
import id.rockierocker.imagetools.repository.ImageRepository;
import id.rockierocker.imagetools.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


@Slf4j
@RequiredArgsConstructor
@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final S3LocalMinio awsS3LocalMinio;
    private final UserService userService;

    public ResponseEntity<BaseResponse<ImageDto>> getImageById(String imageId) {
        log.info("getImageById imageId={}", imageId);
        UserDetails userDetails = userService.getCurrentUser();
        Image image = imageRepository.findByImageIdAndUserId(imageId, userDetails.getUserId())
                .orElseThrow(() -> new BadRequestException(ResponseCode.IMAGE_NOT_FOUND));
        return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, ImageDto.builder()
                .originalUrl(awsS3LocalMinio.getPublicUrl(image.getImageKey()))
                .originalFormat(image.getFormat())
                .webpUrl(awsS3LocalMinio.getPublicUrl(image.getWebpImageKey()))
                .width(image.getWidth())
                .height(image.getHeight())
                .build()));
    }

    public ResponseEntity<BaseResponse<ImageDto>> getImageTimeLimitById(String imageId) {
        log.info("getImageTimeLimitById imageId={}, expired = {} minutes", imageId, awsS3LocalMinio.getPresignedUrlExpirationMinutes());
        UserDetails userDetails = userService.getCurrentUser();
        Image image = imageRepository.findByImageIdAndUserId(imageId, userDetails.getUserId())
                .orElseThrow(() -> new BadRequestException(ResponseCode.IMAGE_NOT_FOUND));
        return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS, ImageDto.builder()
                .originalUrl(awsS3LocalMinio.generatePresignedUrl(image.getImageKey()))
                .originalFormat(image.getFormat())
                .webpUrl(awsS3LocalMinio.generatePresignedUrl(image.getWebpImageKey()))
                .width(image.getWidth())
                .height(image.getHeight())
                .build()));
    }
}
