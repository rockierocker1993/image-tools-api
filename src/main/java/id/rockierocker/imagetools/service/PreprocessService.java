package id.rockierocker.imagetools.service;

import id.rockierocker.imagetools.constant.ResponseCode;
import id.rockierocker.imagetools.exception.InternalServerErrorException;
import id.rockierocker.imagetools.repository.PreprocessConfigRepository;
import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.service.preprocess.pipeline.PreprocessPipeline;
import id.rockierocker.imagetools.util.ImageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@Service
public class PreprocessService {

    private final PreprocessConfigRepository preprocessConfigRepository;
    private final PreprocessPipeline preprocessPipeline;
    private final ObjectMapper objectMapper;

    /**
     * Jalankan preprocessing pipeline pada file gambar.
     * Config steps diambil dari database berdasarkan configCode.
     *
     * @param preprocessConfigCode kode config di database
     * @param inputFile            file gambar input
     * @return file gambar hasil preprocessing (file yang sama, isi diperbarui)
     */
    public File preprocess(String preprocessConfigCode, File inputFile) {

        try {
            var preprocessConfigEntity = preprocessConfigRepository
                    .findFirstByConfigCode(preprocessConfigCode)
                    .orElse(null);

            if (Objects.isNull(preprocessConfigEntity)) {
                log.warn("PreprocessConfig not found for code='{}', skipping preprocessing.", preprocessConfigCode);
                return inputFile;
            }

            log.info("Running preprocess pipeline for code='{}', steps={}",
                    preprocessConfigCode, preprocessConfigEntity.getSteps());
            Config config = objectMapper.readValue(preprocessConfigEntity.getConfig(), Config.class);
            BufferedImage inputImage = ImageUtil.toBufferedImage(inputFile,
                    new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD_TO_BUFFERED_IMAGE));

            BufferedImage outputImage = preprocessPipeline.execute(
                    preprocessConfigEntity.getSteps(), inputImage, config);

            byte[] outputBytes = ImageUtil.toBytes(outputImage);
            Files.write(inputFile.toPath(), outputBytes);

            log.info("Preprocessing completed for code='{}'. Output written to: {}",
                    preprocessConfigCode, inputFile.getAbsolutePath());

        } catch (IllegalArgumentException e) {
            log.error("Invalid preprocess config for code='{0}': {}", preprocessConfigCode, e);
            throw new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD);
        } catch (Exception e) {
            log.error("Preprocessing failed for code='{}': {}", preprocessConfigCode, e.getMessage(), e);
            throw new InternalServerErrorException(ResponseCode.PREPROCESS_FAIELD);
        }

        return inputFile;
    }
}
