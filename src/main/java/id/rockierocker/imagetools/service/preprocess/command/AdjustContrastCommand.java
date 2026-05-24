package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.service.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;

/**
 * Command: Adjust Contrast
 * Mengatur kontras gambar berdasarkan config.contrast (factor).
 * factor > 1.0 = tingkatkan kontras, factor < 1.0 = turunkan kontras.
 */
@Slf4j
@Component
public class AdjustContrastCommand implements PreprocessCommand {

    @Override
    public String getStepName() {
        return "ADJUST_CONTRAST";
    }

    @Override
    public void validate(Config config) {
        if (config.getContrast() == null) {
            throw new IllegalArgumentException("AdjustContrast requires 'contrast' value in config");
        }
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        float factor = config.getContrast();
        log.info("[{}] Adjusting contrast with factor={}", getStepName(), factor);

        // Fast path: no change needed
        if (Float.compare(factor, 1.0f) == 0) {
            log.debug("[{}] Contrast factor is 1.0, skipping.", getStepName());
            return image;
        }

        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();

        BufferedImage output = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);

        for (int i = 0; i < pixels.length; i++) {
            int rgb = pixels[i];
            int a  = (rgb >> 24) & 0xff;
            int r0 = (rgb >> 16) & 0xff;
            int g0 = (rgb >>  8) & 0xff;
            int b0 =  rgb        & 0xff;

            int r = PreprocessUtil.clamp((int) Math.round((r0 - 128) * factor + 128));
            int g = PreprocessUtil.clamp((int) Math.round((g0 - 128) * factor + 128));
            int b = PreprocessUtil.clamp((int) Math.round((b0 - 128) * factor + 128));

            pixels[i] = hasAlpha
                    ? (a << 24) | (r << 16) | (g << 8) | b
                    : (r << 16) | (g << 8) | b;
        }

        output.setRGB(0, 0, w, h, pixels, 0, w);
        log.debug("[{}] Done. Output size={}x{}", getStepName(), w, h);
        return output;
    }
}

