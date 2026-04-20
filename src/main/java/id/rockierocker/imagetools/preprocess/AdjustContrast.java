package id.rockierocker.imagetools.preprocess;


import id.rockierocker.imagetools.preprocess.model.PreprocessConfig;
import id.rockierocker.imagetools.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;

@Slf4j
public class AdjustContrast implements ImagePreprocess{

    @Override
    public BufferedImage process(BufferedImage inputImage, PreprocessConfig preprocessConfig) {
        log.info("Adjusting contrast with factor: {}", preprocessConfig.getContrast());
        return adjustContrast(inputImage, preprocessConfig.getContrast());
    }


    // =========================
    // CONTRAST
    // =========================
    static BufferedImage adjustContrast(BufferedImage img, float factor) {
        int w = img.getWidth();
        int h = img.getHeight();

        // Fast path: no change required
        if (Float.compare(factor, 1.0f) == 0) {
            return img;
        }

        // Preserve alpha channel if present in the source image
        boolean hasAlpha = img.getColorModel().hasAlpha();
        BufferedImage out = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        // Read all pixels into an int array (faster than getRGB per pixel)
        int[] pixels = img.getRGB(0, 0, w, h, null, 0, w);

        // Process pixels in a tight loop
        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];

            int a = (rgb >> 24) & 0xff;
            int r0 = (rgb >> 16) & 0xff;
            int g0 = (rgb >> 8) & 0xff;
            int b0 = rgb & 0xff;

            // apply contrast formula and clamp
            int r = PreprocessUtil.clamp((int) Math.round((r0 - 128) * factor + 128));
            int g = PreprocessUtil.clamp((int) Math.round((g0 - 128) * factor + 128));
            int b = PreprocessUtil.clamp((int) Math.round((b0 - 128) * factor + 128));

            if (hasAlpha) {
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            } else {
                pixels[i] = (r << 16) | (g << 8) | b;
            }
        }

        // Write modified pixels back to the output image in one call
        out.setRGB(0, 0, w, h, pixels, 0, w);
        return out;
    }
}
