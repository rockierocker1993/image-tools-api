package id.rockierocker.imagetools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.rockierocker.imagetools.constant.PreprocessEnum;
import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.exception.InternalServerErrorException;
import id.rockierocker.imagetools.preprocess.Preprocess;
import id.rockierocker.imagetools.preprocess.model.PreprocessConfig;
import id.rockierocker.imagetools.repository.PreprocessConfigRepository;
import id.rockierocker.imagetools.util.ImageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class PreprocessService {

    private final PreprocessConfigRepository preprocessConfigRepository;
    private final ObjectMapper objectMapper;

    public File preprocess(String preprocessConfigCode, File inputFile) {
        var preprocessConfig = preprocessConfigRepository.findFirstByConfigCode(preprocessConfigCode).orElse(null);
        if (Objects.nonNull(preprocessConfig)) {
            log.info("Preprocessing input file before VTrace vectorization.");
            try {
                List<String> preprocessSteps = preprocessConfig.getSteps();
                PreprocessConfig config = objectMapper.convertValue(preprocessConfig, PreprocessConfig.class);
                BufferedImage bufferedImage = ImageUtil.toBufferedImage(inputFile, new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD_TO_BUFFERED_IMAGE));
                for (String step : preprocessSteps) {
                    PreprocessEnum preprocessEnum = PreprocessEnum.fromString(step);
                    BufferedImage newBufferedImage = Preprocess.process(preprocessEnum, bufferedImage, config, new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD));
                    if (newBufferedImage == null) {
                        log.warn("Preprocess step '{}' returned null, stopping further preprocessing.", step);
                        break;
                    }
                    byte[] imageBytes = ImageUtil.toBytes(newBufferedImage);
                    Files.write(inputFile.toPath(), imageBytes);
                    bufferedImage = newBufferedImage;
                }
            } catch (Exception e) {
                log.error("Error during preprocessing: " + e.getMessage(), e);
            }
        }
        return inputFile;
    }
}
