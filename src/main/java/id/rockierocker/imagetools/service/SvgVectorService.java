package id.rockierocker.imagetools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.rockierocker.imagetools.constant.*;
import id.rockierocker.imagetools.dto.svgvector.TraceDto;
import id.rockierocker.imagetools.exception.BadRequestException;
import id.rockierocker.imagetools.exception.InternalServerErrorException;
import id.rockierocker.imagetools.model.VtraceConfig;
import id.rockierocker.imagetools.preprocess.Preprocess;
import id.rockierocker.imagetools.constant.PreprocessEnum;
import id.rockierocker.imagetools.preprocess.model.PreprocessConfig;
import id.rockierocker.imagetools.repository.PreprocessConfigRepository;
import id.rockierocker.imagetools.repository.VtraceConfigRepository;
import id.rockierocker.imagetools.util.CommonUtil;
import id.rockierocker.imagetools.util.ImageUtil;
import id.rockierocker.imagetools.vectorize.VTracerVectorizer;
import id.rockierocker.imagetools.vectorize.Vectorizer;
import id.rockierocker.imagetools.vectorize.constant.VTracerColorMode;
import id.rockierocker.imagetools.vectorize.constant.VTracerCurveFittingMode;
import id.rockierocker.imagetools.vectorize.constant.VTracerHierarchical;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class SvgVectorService {

    private final OutputDirectoryManagerService outputDirectoryManagerService;
    private final PreprocessService preprocess;

    @Value("${image.allowed.extensions:png,jpg,jpeg}")
    private List<String> allowedExtensions = List.of("png", "jpg", "jpeg");

    private final VTracerVectorizer vectorizerVtrace;
    private final VtraceConfigRepository vtraceConfigRepository;


    /* VTRACE SVG CONVERSION
     *  see the doc for more info: https://github.com/visioncortex/vtracer?tab=readme-ov-file
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<byte[]> trace(MultipartFile file, TraceDto traceDto) {
        log.info("Starting SVG conversion using {}", vectorizerVtrace.getName());
        try {
            byte[] svgBytes = doProcessingVTrace(file, traceDto);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"result.svg\""
                    )
                    .contentLength(svgBytes.length)
                    .body(svgBytes);
        } catch (BadRequestException | InternalServerErrorException e) {
            throw e;
        } catch (IOException e) {
            log.error("IO error during VTrace SVG conversion", e);
            throw new InternalServerErrorException(ResponseCode.VECTORIZE_FAILED);
        } catch (Exception e) {
            log.error("unexpected error during VTrace SVG conversion", e);
            throw new InternalServerErrorException(ResponseCode.VECTORIZE_FAILED);
        }

    }

    public byte[] doProcessingVTrace(MultipartFile file, TraceDto traceDto) throws IOException {
        log.info("Starting processing for VTrace vectorization.");
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = CommonUtil.getExtensionLower(originalFilename);
        if (!ext.isEmpty() && !allowedExtensions.contains(ext)) {
            log.info("Unsupported file extension for SVG conversion: {}", ext);
            throw new BadRequestException(ResponseCode.EXTENSION_NOT_SUPPORTED);
        }
        byte[] inputBytes = CommonUtil.getBytes(file, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
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

        // Preprocess image if requested
        if(StringUtils.hasText(traceDto.getPreprocessStepCode()))
            inputFile = preprocess.preprocess(traceDto.getPreprocessStepCode(), inputFile);

        // Vectorize
        List<String> additionalCommand = buildAdditionalCommandList(traceDto.getTraceConfigCode());
        return doVectorization(vectorizerVtrace, inputFile, additionalCommand);
    }

    // Helper: convert additional command map to flat list of args
    private List<String> buildAdditionalCommandList(String vtraceConfigCode) {
        Map<String, Object> additionalCommandMap = buildAdditionalCommandMap(vtraceConfigCode);
        List<String> additionalCommand = new ArrayList<>();
        additionalCommandMap.forEach((k, v) -> {
            if (Objects.nonNull(v)) {
                additionalCommand.add(k);
                additionalCommand.add(String.valueOf(v).trim());
            }
        });
        return additionalCommand;
    }

    private Map<String, Object> buildAdditionalCommandMap(String vtraceConfigCode) {
        log.info("Building additional command map for VTrace vectorization. by config code: {}", vtraceConfigCode);
        VtraceConfig vtraceConfig = vtraceConfigRepository.findFirstByConfigCode(vtraceConfigCode)
                .orElseThrow(() -> new BadRequestException(ResponseCode.VTRACE_CONFIG_NOT_FOUND));

        VTracerColorMode colorMode = VTracerColorMode.fromString(vtraceConfig.getColorMode());
        if (colorMode == null)
            colorMode = VTracerColorMode.COLOR;

        VTracerHierarchical hierarchical = VTracerHierarchical.fromString(vtraceConfig.getHierarchical());
        if (hierarchical == null)
            hierarchical = VTracerHierarchical.STACKED;

        VTracerCurveFittingMode curveFittingMode = VTracerCurveFittingMode.fromString(vtraceConfig.getCurveFittingMode());
        if (curveFittingMode == null)
            curveFittingMode = VTracerCurveFittingMode.SPLINE;

        Map<String, Object> additionalCommandMap = new HashMap<>();
        additionalCommandMap.put("--colormode", colorMode.getCommand());
        if (colorMode == VTracerColorMode.COLOR)
            additionalCommandMap.put("--hierarchical", hierarchical.getCommand());

        additionalCommandMap.put("--filter_speckle", vtraceConfig.getFilterSpeckle());
        additionalCommandMap.put("--color_precision", vtraceConfig.getColorPrecision());
        additionalCommandMap.put("--gradient_step", vtraceConfig.getGradientStep());
        additionalCommandMap.put("--mode", curveFittingMode.getCommand());
        if (curveFittingMode == VTracerCurveFittingMode.SPLINE) {
            additionalCommandMap.put("--segment_length", vtraceConfig.getSegmentLength());
            additionalCommandMap.put("--splice_threshold", vtraceConfig.getSpliceThreshold());
            additionalCommandMap.put("--corner_threshold", vtraceConfig.getCornerThreshold());
        }
        return additionalCommandMap;
    }





    private byte[] doVectorization(Vectorizer vectorizer, File inputFile, List<String> additionalCommand) {
        log.info("Performing vectorization using " + vectorizer.getName());
        try {
            byte[] outputBytes = vectorizer.vectorize(inputFile.toPath(), additionalCommand);
            log.info("Vectorization completed ");
            return outputBytes;
        } catch (Exception e) {
            log.error("Error during vectorization: " + e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.VECTORIZE_FAILED);
        }
    }

}
