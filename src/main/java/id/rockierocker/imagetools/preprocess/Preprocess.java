package id.rockierocker.imagetools.preprocess;

import id.rockierocker.imagetools.preprocess.model.PreprocessConfig;
import id.rockierocker.imagetools.constant.PreprocessEnum;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;

@Slf4j
public class Preprocess {
    public static BufferedImage process(PreprocessEnum preprocessEnum, BufferedImage inputImage, PreprocessConfig preprocessConfig, RuntimeException runtimeException) {
        try {
            log.info("Image Preprocess using {}", preprocessEnum.name());
            ImagePreprocess imagePreprocess = (ImagePreprocess) preprocessEnum.PreprocessClass.getDeclaredConstructor().newInstance();
            return imagePreprocess.process(inputImage, preprocessConfig);
        } catch (Exception e) {
            throw runtimeException;
        }
    }
}
