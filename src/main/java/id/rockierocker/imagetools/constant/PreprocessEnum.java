package id.rockierocker.imagetools.constant;

import id.rockierocker.imagetools.preprocess.AdjustContrast;
import id.rockierocker.imagetools.preprocess.KMeansQuantization;
import id.rockierocker.imagetools.preprocess.RemoveOutline;
import id.rockierocker.imagetools.preprocess.Sharpen;

public enum PreprocessEnum {
    K_MEANS_QUANTIZATION(KMeansQuantization.class),
    ADJUST_CONTRAST(AdjustContrast.class),
    SHARPEN(Sharpen.class),
    REMOVE_OUTLINE(RemoveOutline.class);

    PreprocessEnum(Class<?> PreprocessClass) {
        this.PreprocessClass = PreprocessClass;
    }

    public final Class<?> PreprocessClass;
    public static PreprocessEnum fromString(String name) {
        for (PreprocessEnum p : PreprocessEnum.values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Unknown preprocess: " + name);
    }
}
