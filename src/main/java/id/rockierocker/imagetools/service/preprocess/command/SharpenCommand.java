package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.service.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Command: Sharpen
 * Menerapkan convolution kernel untuk mempertajam gambar.
 * config.sharpenKernel = 3x3 kernel matrix (List<List<Float>>).
 */
@Slf4j
@Component
public class SharpenCommand implements PreprocessCommand {

    @Override
    public String getStepName() {
        return "SHARPEN";
    }

    @Override
    public void validate(Config config) {
        if (config.getSharpenKernel() == null || config.getSharpenKernel().size() != 3) {
            throw new IllegalArgumentException("Sharpen requires 'sharpenKernel' as 3x3 matrix in config");
        }
        for (List<Float> row : config.getSharpenKernel()) {
            if (row == null || row.size() != 3) {
                throw new IllegalArgumentException("Each row of sharpenKernel must have exactly 3 elements");
            }
        }
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        List<List<Float>> kernel = config.getSharpenKernel();
        log.info("[{}] Applying sharpen kernel={}", getStepName(), kernel);

        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();

        BufferedImage output = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pos = y * w + x;

                // Border pixels: preserve original (avoid out-of-bounds kernel)
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    dst[pos] = src[pos];
                    continue;
                }

                int r = PreprocessUtil.clamp(PreprocessUtil.applyKernel(image, x, y, 16, kernel));
                int g = PreprocessUtil.clamp(PreprocessUtil.applyKernel(image, x, y,  8, kernel));
                int b = PreprocessUtil.clamp(PreprocessUtil.applyKernel(image, x, y,  0, kernel));
                int a = hasAlpha ? ((src[pos] >> 24) & 0xff) : 0xff;

                dst[pos] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        output.setRGB(0, 0, w, h, dst, 0, w);
        log.debug("[{}] Done. Output size={}x{}", getStepName(), w, h);
        return output;
    }
}

