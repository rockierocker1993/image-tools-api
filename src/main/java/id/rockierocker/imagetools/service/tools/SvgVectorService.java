package id.rockierocker.imagetools.service.tools;

import id.rockierocker.imagetools.component.s3.S3LocalMinio;
import id.rockierocker.imagetools.constant.*;
import id.rockierocker.imagetools.constant.Module;
import id.rockierocker.imagetools.dto.*;
import id.rockierocker.imagetools.entity.Image;
import id.rockierocker.imagetools.entity.UserActivity;
import id.rockierocker.imagetools.exception.BadRequestException;
import id.rockierocker.imagetools.exception.InternalServerErrorException;
import id.rockierocker.imagetools.entity.VtraceConfig;
import id.rockierocker.imagetools.repository.ImageRepository;
import id.rockierocker.imagetools.repository.UserActivityRepository;
import id.rockierocker.imagetools.repository.VtraceConfigRepository;
import id.rockierocker.imagetools.service.OutputDirectoryManagerService;
import id.rockierocker.imagetools.service.PreprocessService;
import id.rockierocker.imagetools.service.UserService;
import id.rockierocker.imagetools.util.CommonUtil;
import id.rockierocker.imagetools.util.ImageUtil;
import id.rockierocker.imagetools.service.vectorize.VTracerVectorizer;
import id.rockierocker.imagetools.service.vectorize.Vectorizer;
import id.rockierocker.imagetools.service.vectorize.constant.VTracerColorMode;
import id.rockierocker.imagetools.service.vectorize.constant.VTracerCurveFittingMode;
import id.rockierocker.imagetools.service.vectorize.constant.VTracerHierarchical;
import id.rockierocker.imagetools.util.ResponseUtil;
import id.rockierocker.imagetools.util.ValidatorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
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
    private final ImageRepository imageRepository;
    private final UserActivityRepository userActivityRepository;
    private final UserService userService;
    private final S3LocalMinio s3LocalMinio;

    private final VTracerVectorizer vectorizerVtrace;
    private final VtraceConfigRepository vtraceConfigRepository;
    @Value("${vector.preprocess-config-code:P_VECTOR001}")
    private String preprocessCode;
    @Value("${vector.config-code:VECTOR001}")
    private String configCode;
    @Value("${minio.path.result.vectorize}")
    private String minioVectorSvgPath;

    /* VTRACE SVG CONVERSION
     *  see the doc for more info: https://github.com/visioncortex/vtracer?tab=readme-ov-file
     * */
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<BaseResponse<SvgVectorResponseDto>> trace(MultipartFile file) {
        log.info("Starting SVG conversion using {}", vectorizerVtrace.getName());
        try {
            byte[] svgBytes = doProcessingVTrace(file, SvgVectorConfigDto.builder()
                    .preprocessConfigCode(preprocessCode)
                    .vectorConfigCode(configCode)
                    .build());
            String requestId = UUID.randomUUID().toString();
            String imageName = "svg-" + requestId + ".svg";
            String imageKey = s3LocalMinio.uploadFile(svgBytes, imageName, "image/svg", minioVectorSvgPath);

            UserDetails userDetails = userService.getCurrentUser();
            userActivityRepository.save(UserActivity.builder()
                    .userId(requestId)
                    .requestId(UUID.randomUUID().toString())
                    .module(Module.VECTOR.name)
                    .activity(Activity.GENERATE_VECTOR.name())
                    .userType(userDetails.getUserType().name())
                    .build());

            Image image = Image.builder()
                    .imageId(requestId)
                    .userId(userDetails.getUserId())
                    .imageKey(imageKey)
                    .imageName(imageName)
                    .imageProvider(StorageProvider.MINIO.name())
                    .webpProvider(StorageProvider.MINIO.name())
                    .format("svg")
                    .category(Module.VECTOR.name)
                    .build();
            imageRepository.save(image);
            return ResponseEntity.ok().body(ResponseUtil.buildSuccessResponse(ResponseCode.SUCCESS,
                    SvgVectorResponseDto.builder()
                            .imageId(requestId)
                            .svg(new String(svgBytes))
                            .regions(analyze(svgBytes))
                            .build())
            );
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

    public byte[] doProcessingVTrace(MultipartFile file, SvgVectorConfigDto svgVectorConfigDto) throws IOException {
        log.info("Starting processing for VTrace vectorization.");
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = CommonUtil.getExtensionLower(originalFilename);
        ValidatorUtil.validateAllowedImageExt(ext);
        byte[] inputBytes = CommonUtil.getBytes(file, new InternalServerErrorException(ResponseCode.FAILED_READ_FILE));
        File inputFile = outputDirectoryManagerService.createTempFile("upload-" + originalFilename + "-", "." + ext,
                inputBytes, new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE));

        if (StringUtils.hasText(svgVectorConfigDto.getPreprocessConfigCode()))
            inputFile = preprocess.preprocess(svgVectorConfigDto.getPreprocessConfigCode(), inputFile);

        log.info("Final input for vectorize: path={}, size={} bytes, exists={}",
                inputFile.getAbsolutePath(), inputFile.length(), inputFile.exists());

        // Vectorize
        List<String> additionalCommand = buildAdditionalCommandList(svgVectorConfigDto.getVectorConfigCode());
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

    public List<SvgVectorColorRegion> analyze(byte[] svgByte) throws Exception {
        File inputFile = outputDirectoryManagerService.createTempFile("anlyze-region"+UUID.randomUUID().toString(), ".svg",
                svgByte, new InternalServerErrorException(ResponseCode.FAILED_CREATE_TEMP_FILE));
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
        SVGDocument document = factory.createSVGDocument(inputFile.toURI().toString());

        NodeList all = document.getElementsByTagName("*");
        Map<String, List<Element>> groupedByColor = new HashMap<>();

        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (!(node instanceof Element element)) {
                continue;
            }
            String fill = element.getAttribute("fill");
            if (fill == null || fill.isBlank()) {
                continue;
            }
            String normalized = normalizeColor(fill);
            if (normalized == null) {
                continue;
            }
            groupedByColor
                    .computeIfAbsent(normalized, k -> new ArrayList<>())
                    .add(element);
        }

        List<SvgVectorColorRegion> result = new ArrayList<>();
        int idx = 0;

        for (Map.Entry<String, List<Element>> entry : groupedByColor.entrySet()) {
            List<String> ids = new ArrayList<>();
            Rectangle2D unionBounds = null;

            for (Element el : entry.getValue()) {
                String id = el.getAttribute("id");
                if (id == null || id.isBlank()) {
                    id = UUID.randomUUID().toString();
                    el.setAttribute("id", id);
                }
                ids.add(id);

                Rectangle2D bounds = extractBounds(el);
                if (bounds != null) {
                    if (unionBounds == null) {
                        // Copy agar perubahan add() berikutnya tidak memodifikasi bounds asli.
                        unionBounds = new Rectangle2D.Double(
                                bounds.getX(), bounds.getY(),
                                bounds.getWidth(), bounds.getHeight());
                    } else {
                        unionBounds.add(bounds);
                    }
                }
            }

            SvgVectorColorRegion region = SvgVectorColorRegion.builder()
                    .id("region-" + idx++)
                    .color(entry.getKey())
                    .elementIds(ids)
                    .bounds(unionBounds)
                    .build();
            result.add(region);
        }

        buildNeighbors(result);
        return result;
    }

    private String normalizeColor(String color) {
        try {
            // Hanya support #RRGGBB / #RGB. Skip nilai "none", "url(#...)", warna nama, dll.
            String trimmed = color.trim();
            if (!trimmed.startsWith("#")) {
                return null;
            }
            Color c = Color.decode(trimmed);
            int r = (c.getRed()   / 16) * 16;
            int g = (c.getGreen() / 16) * 16;
            int b = (c.getBlue()  / 16) * 16;
            return String.format("#%02X%02X%02X", r, g, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Rectangle2D extractBounds(Element element) {
        try {
            String d = element.getAttribute("d");
            if (d == null || d.isBlank()) {
                return null;
            }
            AWTPathProducer producer = new AWTPathProducer();
            PathParser parser = new PathParser();
            parser.setPathHandler(producer);
            parser.parse(d);
            Shape shape = producer.getShape();
            return shape.getBounds2D();
        } catch (Exception e) {
            log.debug("Failed to extract bounds: {}", e.getMessage());
            return null;
        }
    }

    private void buildNeighbors(List<SvgVectorColorRegion> regions) {
        for (SvgVectorColorRegion a : regions) {
            List<String> neighbors = new ArrayList<>();
            if (a.getBounds() == null) {
                a.setNeighbors(neighbors);
                continue;
            }
            Rectangle2D expanded = new Rectangle2D.Double(
                    a.getBounds().getX() - 5,
                    a.getBounds().getY() - 5,
                    a.getBounds().getWidth() + 10,
                    a.getBounds().getHeight() + 10
            );
            for (SvgVectorColorRegion b : regions) {
                if (a == b) continue;
                if (b.getBounds() == null) continue;
                if (expanded.intersects(b.getBounds())) {
                    neighbors.add(b.getId());
                }
            }

            a.setNeighbors(neighbors);
        }
    }
    
}
