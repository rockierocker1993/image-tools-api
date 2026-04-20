package id.rockierocker.imagetools.service;

import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.dto.removebackground.RemoveDto;
import id.rockierocker.imagetools.exception.BadRequestException;
import id.rockierocker.imagetools.exception.InternalServerErrorException;
import id.rockierocker.imagetools.util.CommonUtil;
import id.rockierocker.imagetools.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class RemoveBackgroundService {

    private final OutputDirectoryManagerService outputDirectoryManagerService;
    private final PreprocessService preprocessService;

    private final RestTemplate restTemplate;
    @Value("${remove-bg.url}")
    private String url;
    @Value("${image.allowed.extensions:png,jpg,jpeg}")
    private List<String> allowedExtensions = List.of("png", "jpg", "jpeg");


    public ResponseEntity<byte[]> removeBackground(MultipartFile multipartFile, RemoveDto removeDto) {
        try {
            log.info("Removing background for file: {}", multipartFile.getOriginalFilename());
            String originalFilename = StringUtils.cleanPath(multipartFile.getOriginalFilename() == null ? "" : multipartFile.getOriginalFilename());
            String ext = CommonUtil.getExtensionLower(originalFilename);
            if (!ext.isEmpty() && !allowedExtensions.contains(ext)) {
                log.info("Unsupported file extension for SVG conversion: {}", ext);
                throw new BadRequestException(ResponseCode.EXTENSION_NOT_SUPPORTED);
            }
            byte[] inputBytes = CommonUtil.getBytes(multipartFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
            File inputFile = outputDirectoryManagerService.createTempFile("upload-" + originalFilename + "-", "." + ext,
                    inputBytes, new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE));

            if (!"png".equalsIgnoreCase(ext)) {
                // do convert to png first
                log.info("Converting input image to PNG format before VTrace vectorization.");
                BufferedImage bufferedImage = ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
                byte[] pngBytes = ImageUtil.toBytesPng(bufferedImage);
                ext = "png";
                Path pngPath = outputDirectoryManagerService.createTempFile("converted-png-" + originalFilename + "-", "." + ext, pngBytes).toPath();
                // Update inputFile and ext for further processing
                inputFile = pngPath.toFile();
                log.info("Input image converted to PNG successfully.");
            }
            if(StringUtils.hasText(removeDto.getPreprocessStepCode()))
                inputFile = preprocessService.preprocess(removeDto.getPreprocessStepCode(), inputFile);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(inputFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Sending request to Remove Background API at: {}", url);
            Resource response = restTemplate.postForObject(url, requestEntity, Resource.class);
            log.info("Received response from Remove Background API, file size: {} bytes, filename {}", response.contentLength(), response.getFilename());
            byte[]rembgBytes = response.getContentAsByteArray();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"result.png\""
                    )
                    .contentLength(rembgBytes.length)
                    .body(rembgBytes);
        }catch (Exception e){
            log.error("Error while removing background: ", e);
            throw new InternalServerErrorException(ResponseCode.UKNOWN_ERROR);
        }
    }
}
