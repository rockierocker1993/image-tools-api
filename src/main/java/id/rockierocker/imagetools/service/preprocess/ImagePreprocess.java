package id.rockierocker.imagetools.service.preprocess;

import id.rockierocker.imagetools.service.preprocess.model.PreprocessConfig;

import java.awt.image.BufferedImage;

public interface ImagePreprocess {


    BufferedImage process(BufferedImage inputImage, PreprocessConfig preprocessConfig);

}
