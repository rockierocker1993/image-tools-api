package id.rockierocker.imagetools.preprocess;

import id.rockierocker.imagetools.preprocess.model.PreprocessConfig;

import java.awt.image.BufferedImage;

public interface ImagePreprocess {

    // =========================
    // CONFIG
    // =========================
//    static final int K_COLORSs = 5;        // recommended 4–6
//    static final float CONTRASTs = 1.2f;   // 1.1 – 1.3
//    static final int ITERATIONSs = 10;
//    static final float[][] SHARPEN_KERNELs = {
//            { 0, -1,  0 },
//            { -1,  5, -1 },
//            { 0, -1,  0 }
//    };
    BufferedImage process(BufferedImage inputImage, PreprocessConfig preprocessConfig);

}
