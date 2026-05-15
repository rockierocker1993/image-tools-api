package id.rockierocker.imagetools.service.preprocess;


import id.rockierocker.imagetools.service.preprocess.model.PreprocessConfig;
import id.rockierocker.imagetools.service.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
public class Sharpen implements ImagePreprocess {

    @Override
    public BufferedImage process(BufferedImage inputImage, PreprocessConfig preprocessConfig) {
        log.info("Adjusting sharpen with kernel: {}", preprocessConfig.getSharpenKernel());
        return sharpen(inputImage, preprocessConfig.getSharpenKernel());
    }

    // =========================
    // SHARPEN
    // =========================
    static BufferedImage sharpen(BufferedImage img, List<List<Float>> sharpenKernel) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean hasAlpha = img.getColorModel().hasAlpha();
        // choose ARGB if source has alpha so we can preserve transparency
        BufferedImage out = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        // Read source pixels in bulk for faster access
        int[] src = img.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        // For performance, copy border pixels directly (don't apply kernel) to avoid out-of-bounds handling
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pos = y * w + x;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    // copy original pixel (preserve alpha if present)
                    dst[pos] = src[pos];
                    continue;
                }

                // apply kernel per channel using existing utility (keeps existing kernel semantics)
                int r = PreprocessUtil.applyKernel(img, x, y, 16, sharpenKernel);
                int g = PreprocessUtil.applyKernel(img, x, y, 8, sharpenKernel);
                int b = PreprocessUtil.applyKernel(img, x, y, 0, sharpenKernel);

                r = PreprocessUtil.clamp(r);
                g = PreprocessUtil.clamp(g);
                b = PreprocessUtil.clamp(b);

                int a = hasAlpha ? ((src[pos] >> 24) & 0xff) : 0xff;
                dst[pos] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // Write all pixels back in one call
        out.setRGB(0, 0, w, h, dst, 0, w);
        return out;
    }

}
