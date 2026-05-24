package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.service.preprocess.model.Config.QualityConfig;
import id.rockierocker.imagetools.service.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Command: Adjust Quality
 * Menerapkan serangkaian penyesuaian kualitas gambar berdasarkan QualityConfig:
 *  - Denoising (Gaussian blur / Median filter)
 *  - Brightness
 *  - Gamma correction
 *  - Normalize exposure (histogram stretching)
 *  - Saturation
 *  - White balance (gray world)
 *  - Histogram equalization
 *  - CLAHE (Contrast Limited Adaptive Histogram Equalization)
 *  - Unsharp mask
 *  - Fill transparent background
 *  - Preset shortcuts
 */
@Slf4j
@Component
public class AdjustQualityCommand implements PreprocessCommand {

    @Override
    public String getStepName() {
        return "ADJUST_QUALITY";
    }

    @Override
    public void validate(Config config) {
        if (config.getQuality() == null) {
            throw new IllegalArgumentException("AdjustQuality requires 'quality' config object");
        }
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        QualityConfig q = config.getQuality();
        log.info("[{}] Starting quality adjustment", getStepName());

        // Apply preset if provided (overrides individual settings)
        if (q.getPreset() != null) {
            q = applyPreset(q.getPreset());
        }

        BufferedImage current = image;

        // 1. Fill transparent background
        if (q.getFillTransparentWith() != null) {
            current = fillTransparent(current, q.getFillTransparentWith());
        }

        // 2. Median filter (salt-and-pepper noise removal)
        if (Boolean.TRUE.equals(q.getMedianFilterEnabled())) {
            current = applyMedianFilter(current);
        }

        // 3. Gaussian denoise
        if (q.getDenoiseSigma() != null && q.getDenoiseSigma() > 0.0) {
            current = gaussianBlur(current, q.getDenoiseSigma());
        }

        // 4. Normalize exposure (histogram stretching)
        if (Boolean.TRUE.equals(q.getNormalizeExposureEnabled())) {
            current = normalizeExposure(current);
        }

        // 5. White balance (gray world)
        if (Boolean.TRUE.equals(q.getWhiteBalanceEnabled())) {
            current = applyWhiteBalance(current);
        }

        // 6. Gamma correction
        if (q.getGamma() != null && Double.compare(q.getGamma(), 1.0) != 0) {
            current = applyGamma(current, q.getGamma());
        }

        // 7. Brightness adjustment
        if (q.getBrightness() != null && Float.compare(q.getBrightness(), 0f) != 0) {
            current = applyBrightness(current, q.getBrightness());
        }

        // 8. Saturation
        if (q.getSaturation() != null && Float.compare(q.getSaturation(), 1.0f) != 0) {
            current = applySaturation(current, q.getSaturation());
        }

        // 9. Histogram equalization
        if (Boolean.TRUE.equals(q.getHistogramEqualizationEnabled())) {
            current = histogramEqualization(current);
        }

        // 10. CLAHE
        if (Boolean.TRUE.equals(q.getClaheEnabled())) {
            double clipLimit = q.getClaheClipLimit() != null ? q.getClaheClipLimit() : 2.0;
            int tileSize = q.getClaheTileSize() != null ? q.getClaheTileSize() : 8;
            current = applyClahe(current, clipLimit, tileSize);
        }

        // 11. Unsharp mask
        if (q.getUnsharpMaskAmount() != null && q.getUnsharpMaskAmount() > 0f) {
            int radius = q.getUnsharpMaskRadius() != null ? q.getUnsharpMaskRadius() : 1;
            current = applyUnsharpMask(current, radius, q.getUnsharpMaskAmount());
        }

        log.info("[{}] Done. Output size={}x{}", getStepName(), current.getWidth(), current.getHeight());
        return current;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Preset
    // ──────────────────────────────────────────────────────────────────────────

    private QualityConfig applyPreset(String preset) {
        log.info("[{}] Applying preset={}", getStepName(), preset);
        QualityConfig q = new QualityConfig();
        switch (preset.toUpperCase()) {
            case "REMOVE_BG":
                q.setNormalizeExposureEnabled(true);
                q.setWhiteBalanceEnabled(true);
                q.setMedianFilterEnabled(true);
                q.setClaheEnabled(true);
                q.setClaheClipLimit(2.0);
                q.setClaheTileSize(8);
                break;
            case "UPSCALE":
                q.setDenoiseSigma(0.5);
                q.setGamma(0.9);
                q.setBrightness(0f);
                break;
            case "VECTORIZE":
                q.setSaturation(1.3f);
                q.setHistogramEqualizationEnabled(true);
                q.setUnsharpMaskRadius(1);
                q.setUnsharpMaskAmount(0.5f);
                break;
            case "BALANCED":
                q.setDenoiseSigma(0.5);
                q.setNormalizeExposureEnabled(true);
                q.setSaturation(1.1f);
                q.setUnsharpMaskRadius(1);
                q.setUnsharpMaskAmount(0.3f);
                break;
            default:
                log.warn("[{}] Unknown preset '{}', ignoring.", getStepName(), preset);
        }
        return q;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fill Transparent
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage fillTransparent(BufferedImage image, String colorName) {
        log.debug("[{}] Filling transparent with={}", getStepName(), colorName);
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = output.createGraphics();

        Color bg;
        switch (colorName.toUpperCase()) {
            case "BLACK": bg = Color.BLACK; break;
            case "GRAY":  bg = Color.GRAY;  break;
            default:      bg = Color.WHITE; break;
        }
        g2d.setColor(bg);
        g2d.fillRect(0, 0, w, h);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Median Filter (3x3)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyMedianFilter(BufferedImage image) {
        log.debug("[{}] Applying median filter", getStepName());
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int type = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage output = new BufferedImage(w, h, type);
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pos = y * w + x;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) {
                    dst[pos] = src[pos];
                    continue;
                }
                int[] rs = new int[9], gs = new int[9], bs = new int[9];
                int idx = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int rgb = src[(y + dy) * w + (x + dx)];
                        rs[idx] = (rgb >> 16) & 0xff;
                        gs[idx] = (rgb >>  8) & 0xff;
                        bs[idx] =  rgb        & 0xff;
                        idx++;
                    }
                }
                java.util.Arrays.sort(rs);
                java.util.Arrays.sort(gs);
                java.util.Arrays.sort(bs);
                int a = hasAlpha ? ((src[pos] >> 24) & 0xff) : 0xff;
                dst[pos] = (a << 24) | (rs[4] << 16) | (gs[4] << 8) | bs[4];
            }
        }
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gaussian Blur (denoising)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage gaussianBlur(BufferedImage image, double sigma) {
        log.debug("[{}] Gaussian blur sigma={}", getStepName(), sigma);
        int radius = (int) Math.ceil(sigma * 3);
        int size = 2 * radius + 1;
        float[] kernel = new float[size];
        float sum = 0;
        for (int i = 0; i < size; i++) {
            float x = i - radius;
            kernel[i] = (float) Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        for (int i = 0; i < size; i++) kernel[i] /= sum;

        return applyGaussianSeparable(image, kernel, radius);
    }

    private BufferedImage applyGaussianSeparable(BufferedImage image, float[] kernel, int radius) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int type = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);

        // Horizontal pass
        int[] tmp = new int[src.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                for (int k = -radius; k <= radius; k++) {
                    int sx = Math.min(Math.max(x + k, 0), w - 1);
                    int rgb = src[y * w + sx];
                    float kv = kernel[k + radius];
                    r += ((rgb >> 16) & 0xff) * kv;
                    g += ((rgb >>  8) & 0xff) * kv;
                    b += ( rgb        & 0xff) * kv;
                    a += ((rgb >> 24) & 0xff) * kv;
                }
                int av = hasAlpha ? PreprocessUtil.clamp(Math.round(a)) : ((src[y * w + x] >> 24) & 0xff);
                tmp[y * w + x] = (av << 24) | (PreprocessUtil.clamp(Math.round(r)) << 16)
                        | (PreprocessUtil.clamp(Math.round(g)) << 8) | PreprocessUtil.clamp(Math.round(b));
            }
        }

        // Vertical pass
        int[] dst = new int[src.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                for (int k = -radius; k <= radius; k++) {
                    int sy = Math.min(Math.max(y + k, 0), h - 1);
                    int rgb = tmp[sy * w + x];
                    float kv = kernel[k + radius];
                    r += ((rgb >> 16) & 0xff) * kv;
                    g += ((rgb >>  8) & 0xff) * kv;
                    b += ( rgb        & 0xff) * kv;
                    a += ((rgb >> 24) & 0xff) * kv;
                }
                int av = hasAlpha ? PreprocessUtil.clamp(Math.round(a)) : ((src[y * w + x] >> 24) & 0xff);
                dst[y * w + x] = (av << 24) | (PreprocessUtil.clamp(Math.round(r)) << 16)
                        | (PreprocessUtil.clamp(Math.round(g)) << 8) | PreprocessUtil.clamp(Math.round(b));
            }
        }

        BufferedImage output = new BufferedImage(w, h, type);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Normalize Exposure (per-channel histogram stretching)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage normalizeExposure(BufferedImage image) {
        log.debug("[{}] Normalizing exposure", getStepName());
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);

        int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;
        for (int rgb : src) {
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >>  8) & 0xff;
            int b =  rgb        & 0xff;
            if (r < minR) minR = r; if (r > maxR) maxR = r;
            if (g < minG) minG = g; if (g > maxG) maxG = g;
            if (b < minB) minB = b; if (b > maxB) maxB = b;
        }

        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            int rgb = src[i];
            int a =  (rgb >> 24) & 0xff;
            int r = stretch((rgb >> 16) & 0xff, minR, maxR);
            int g = stretch((rgb >>  8) & 0xff, minG, maxG);
            int b = stretch( rgb        & 0xff, minB, maxB);
            dst[i] = hasAlpha ? (a << 24) | (r << 16) | (g << 8) | b : (0xff << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    private int stretch(int v, int min, int max) {
        if (max == min) return v;
        return PreprocessUtil.clamp((v - min) * 255 / (max - min));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // White Balance (Gray World)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyWhiteBalance(BufferedImage image) {
        log.debug("[{}] Applying white balance (gray world)", getStepName());
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);

        long sumR = 0, sumG = 0, sumB = 0, count = 0;
        for (int rgb : src) {
            if (hasAlpha && ((rgb >> 24) & 0xff) < 8) continue;
            sumR += (rgb >> 16) & 0xff;
            sumG += (rgb >>  8) & 0xff;
            sumB +=  rgb        & 0xff;
            count++;
        }
        if (count == 0) return image;

        double avgR = (double) sumR / count;
        double avgG = (double) sumG / count;
        double avgB = (double) sumB / count;
        double avg  = (avgR + avgG + avgB) / 3.0;

        double scaleR = avg / avgR;
        double scaleG = avg / avgG;
        double scaleB = avg / avgB;

        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            int rgb = src[i];
            int a = (rgb >> 24) & 0xff;
            int r = PreprocessUtil.clamp((int) Math.round(((rgb >> 16) & 0xff) * scaleR));
            int g = PreprocessUtil.clamp((int) Math.round(((rgb >>  8) & 0xff) * scaleG));
            int b = PreprocessUtil.clamp((int) Math.round(( rgb        & 0xff) * scaleB));
            dst[i] = hasAlpha ? (a << 24) | (r << 16) | (g << 8) | b : (0xff << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gamma Correction
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyGamma(BufferedImage image, double gamma) {
        log.debug("[{}] Applying gamma={}", getStepName(), gamma);
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = PreprocessUtil.clamp((int) Math.round(Math.pow(i / 255.0, gamma) * 255));
        }
        return applyPerChannelLut(image, lut);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Brightness Adjustment
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyBrightness(BufferedImage image, float brightness) {
        log.debug("[{}] Applying brightness={}", getStepName(), brightness);
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = PreprocessUtil.clamp(Math.round(i + brightness));
        }
        return applyPerChannelLut(image, lut);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Saturation
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applySaturation(BufferedImage image, float saturation) {
        log.debug("[{}] Applying saturation={}", getStepName(), saturation);
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        for (int i = 0; i < src.length; i++) {
            int rgb = src[i];
            int a = (rgb >> 24) & 0xff;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >>  8) & 0xff;
            int b =  rgb        & 0xff;

            // Convert to HSL-like: luminance
            float gray = 0.2126f * r + 0.7152f * g + 0.0722f * b;
            int nr = PreprocessUtil.clamp(Math.round(gray + saturation * (r - gray)));
            int ng = PreprocessUtil.clamp(Math.round(gray + saturation * (g - gray)));
            int nb = PreprocessUtil.clamp(Math.round(gray + saturation * (b - gray)));
            dst[i] = hasAlpha ? (a << 24) | (nr << 16) | (ng << 8) | nb
                    : (0xff << 24) | (nr << 16) | (ng << 8) | nb;
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Histogram Equalization (luminance channel)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage histogramEqualization(BufferedImage image) {
        log.debug("[{}] Applying histogram equalization", getStepName());
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);

        // Build luminance histogram
        int[] hist = new int[256];
        for (int rgb : src) {
            if (hasAlpha && ((rgb >> 24) & 0xff) < 8) continue;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >>  8) & 0xff;
            int b =  rgb        & 0xff;
            int lum = (int) Math.round(0.2126 * r + 0.7152 * g + 0.0722 * b);
            hist[lum]++;
        }

        // Build CDF → LUT
        int total = w * h;
        int[] cdf = new int[256];
        cdf[0] = hist[0];
        for (int i = 1; i < 256; i++) cdf[i] = cdf[i - 1] + hist[i];
        int cdfMin = 0;
        for (int i = 0; i < 256; i++) { if (cdf[i] > 0) { cdfMin = cdf[i]; break; } }

        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            lut[i] = PreprocessUtil.clamp((int) Math.round(
                    (double)(cdf[i] - cdfMin) / (total - cdfMin) * 255));
        }

        int[] dst = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            int rgb = src[i];
            int a = (rgb >> 24) & 0xff;
            if (hasAlpha && a < 8) { dst[i] = rgb; continue; }
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >>  8) & 0xff;
            int b =  rgb        & 0xff;
            int lum = (int) Math.round(0.2126 * r + 0.7152 * g + 0.0722 * b);
            float scale = lum == 0 ? 1 : (float) lut[lum] / lum;
            int nr = PreprocessUtil.clamp(Math.round(r * scale));
            int ng = PreprocessUtil.clamp(Math.round(g * scale));
            int nb = PreprocessUtil.clamp(Math.round(b * scale));
            dst[i] = hasAlpha ? (a << 24) | (nr << 16) | (ng << 8) | nb
                    : (0xff << 24) | (nr << 16) | (ng << 8) | nb;
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CLAHE (Contrast Limited Adaptive Histogram Equalization)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyClahe(BufferedImage image, double clipLimit, int tileSize) {
        log.debug("[{}] Applying CLAHE clipLimit={}, tileSize={}", getStepName(), clipLimit, tileSize);
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        int tilesX = (int) Math.ceil((double) w / tileSize);
        int tilesY = (int) Math.ceil((double) h / tileSize);

        // Build per-tile LUT
        int[][][] tileLuts = new int[tilesY][tilesX][256];
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                int x0 = tx * tileSize, x1 = Math.min(x0 + tileSize, w);
                int y0 = ty * tileSize, y1 = Math.min(y0 + tileSize, h);
                int[] hist = new int[256];
                int count = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int rgb = src[y * w + x];
                        if (hasAlpha && ((rgb >> 24) & 0xff) < 8) continue;
                        int lum = luminance(rgb);
                        hist[lum]++;
                        count++;
                    }
                }
                // Clip and redistribute
                int clip = (int) Math.round(clipLimit * count / 256);
                int excess = 0;
                for (int i = 0; i < 256; i++) {
                    if (hist[i] > clip) { excess += hist[i] - clip; hist[i] = clip; }
                }
                int add = excess / 256;
                for (int i = 0; i < 256; i++) hist[i] += add;

                // Build CDF → LUT
                int[] cdf = new int[256];
                cdf[0] = hist[0];
                for (int i = 1; i < 256; i++) cdf[i] = cdf[i - 1] + hist[i];
                int cdfMin = 0;
                for (int i = 0; i < 256; i++) { if (cdf[i] > 0) { cdfMin = cdf[i]; break; } }
                int total = count == 0 ? 1 : count;
                for (int i = 0; i < 256; i++) {
                    tileLuts[ty][tx][i] = PreprocessUtil.clamp(
                            (int) Math.round((double)(cdf[i] - cdfMin) / (total - cdfMin) * 255));
                }
            }
        }

        // Apply with bilinear interpolation between tiles
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src[y * w + x];
                int a = (rgb >> 24) & 0xff;
                if (hasAlpha && a < 8) { dst[y * w + x] = rgb; continue; }

                float tileX = (float) x / tileSize - 0.5f;
                float tileY = (float) y / tileSize - 0.5f;
                int tx0 = Math.max(0, (int) tileX);
                int ty0 = Math.max(0, (int) tileY);
                int tx1 = Math.min(tilesX - 1, tx0 + 1);
                int ty1 = Math.min(tilesY - 1, ty0 + 1);
                float fx = tileX - tx0;
                float fy = tileY - ty0;

                int lum = luminance(rgb);
                float v00 = tileLuts[ty0][tx0][lum];
                float v10 = tileLuts[ty0][tx1][lum];
                float v01 = tileLuts[ty1][tx0][lum];
                float v11 = tileLuts[ty1][tx1][lum];
                float newLum = (v00 * (1 - fx) * (1 - fy))
                             + (v10 * fx * (1 - fy))
                             + (v01 * (1 - fx) * fy)
                             + (v11 * fx * fy);

                float scale = lum == 0 ? 1 : newLum / lum;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >>  8) & 0xff;
                int b =  rgb        & 0xff;
                int nr = PreprocessUtil.clamp(Math.round(r * scale));
                int ng = PreprocessUtil.clamp(Math.round(g * scale));
                int nb = PreprocessUtil.clamp(Math.round(b * scale));
                dst[y * w + x] = hasAlpha ? (a << 24) | (nr << 16) | (ng << 8) | nb
                        : (0xff << 24) | (nr << 16) | (ng << 8) | nb;
            }
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    private int luminance(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >>  8) & 0xff;
        int b =  rgb        & 0xff;
        return (int) Math.round(0.2126 * r + 0.7152 * g + 0.0722 * b);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Unsharp Mask
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyUnsharpMask(BufferedImage image, int radius, float amount) {
        log.debug("[{}] Applying unsharp mask radius={}, amount={}", getStepName(), radius, amount);
        double sigma = radius;
        BufferedImage blurred = gaussianBlur(image, sigma);

        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src     = image.getRGB(0, 0, w, h, null, 0, w);
        int[] blurred2 = blurred.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        for (int i = 0; i < src.length; i++) {
            int orig = src[i];
            int blur = blurred2[i];
            int a = (orig >> 24) & 0xff;
            int r = PreprocessUtil.clamp(Math.round(((orig >> 16) & 0xff) + amount * (((orig >> 16) & 0xff) - ((blur >> 16) & 0xff))));
            int g = PreprocessUtil.clamp(Math.round(((orig >>  8) & 0xff) + amount * (((orig >>  8) & 0xff) - ((blur >>  8) & 0xff))));
            int b = PreprocessUtil.clamp(Math.round(( orig        & 0xff) + amount * (( orig        & 0xff) - ( blur        & 0xff))));
            dst[i] = hasAlpha ? (a << 24) | (r << 16) | (g << 8) | b
                    : (0xff << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utility: Apply per-channel LUT (R, G, B)
    // ──────────────────────────────────────────────────────────────────────────

    private BufferedImage applyPerChannelLut(BufferedImage image, int[] lut) {
        int w = image.getWidth();
        int h = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];

        for (int i = 0; i < src.length; i++) {
            int rgb = src[i];
            int a = (rgb >> 24) & 0xff;
            int r = lut[(rgb >> 16) & 0xff];
            int g = lut[(rgb >>  8) & 0xff];
            int b = lut[ rgb        & 0xff];
            dst[i] = hasAlpha ? (a << 24) | (r << 16) | (g << 8) | b
                    : (0xff << 24) | (r << 16) | (g << 8) | b;
        }

        BufferedImage output = new BufferedImage(w, h,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        output.setRGB(0, 0, w, h, dst, 0, w);
        return output;
    }
}

