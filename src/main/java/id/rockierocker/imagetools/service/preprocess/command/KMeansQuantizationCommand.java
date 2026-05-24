package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Color;
import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.service.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Command: K-Means Color Quantization
 * Mereduksi jumlah warna pada gambar menggunakan algoritma K-Means clustering.
 * config.kColors = jumlah warna (k), config.iterations = max iterasi.
 */
@Slf4j
@Component
public class KMeansQuantizationCommand implements PreprocessCommand {

    private static final int TRANSPARENT_THRESHOLD = 8;

    @Override
    public String getStepName() {
        return "K_MEANS_QUANTIZATION";
    }

    @Override
    public void validate(Config config) {
        if (config.getKColors() == null || config.getKColors() <= 0) {
            throw new IllegalArgumentException("KMeansQuantization requires 'kColors' > 0 in config");
        }
        if (config.getIterations() == null || config.getIterations() <= 0) {
            throw new IllegalArgumentException("KMeansQuantization requires 'iterations' > 0 in config");
        }
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        int k = config.getKColors();
        int iterations = config.getIterations();
        log.info("[{}] k={}, iterations={}", getStepName(), k, iterations);

        int w = image.getWidth();
        int h = image.getHeight();
        int total = w * h;

        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        boolean hasAlpha = image.getColorModel().hasAlpha();

        // Collect non-transparent pixels
        List<Color> pixels = new ArrayList<>(total);
        int[] mapping = new int[total];
        Arrays.fill(mapping, -1);

        int idxMap = 0;
        for (int i = 0; i < total; i++) {
            int rgb = src[i];
            int a   = (rgb >> 24) & 0xff;
            if (hasAlpha && a <= TRANSPARENT_THRESHOLD) continue;
            pixels.add(new Color((rgb >> 16) & 0xff, (rgb >> 8) & 0xff, rgb & 0xff));
            mapping[i] = idxMap++;
        }

        if (pixels.isEmpty()) {
            log.warn("[{}] No non-transparent pixels found, returning original.", getStepName());
            BufferedImage empty = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            empty.setRGB(0, 0, w, h, src, 0, w);
            return empty;
        }

        // Clamp k to available pixels
        k = Math.min(k, pixels.size());
        List<Color> centroids = initCentroids(pixels, k);
        centroids = runKMeans(pixels, centroids, k, iterations);

        return buildOutputImage(src, pixels, mapping, centroids, w, h, hasAlpha);
    }

    private List<Color> initCentroids(List<Color> pixels, int k) {
        Random rand = new Random();
        List<Color> centroids = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            centroids.add(pixels.get(rand.nextInt(pixels.size())));
        }
        return centroids;
    }

    private List<Color> runKMeans(List<Color> pixels, List<Color> centroids, int k, int iterations) {
        Random rand = new Random();
        for (int iter = 0; iter < iterations; iter++) {
            float[] sumR = new float[k], sumG = new float[k], sumB = new float[k];
            int[] counts = new int[k];

            for (Color p : pixels) {
                int ci = PreprocessUtil.nearestCentroid(p, centroids);
                sumR[ci] += p.r;
                sumG[ci] += p.g;
                sumB[ci] += p.b;
                counts[ci]++;
            }

            boolean changed = false;
            for (int i = 0; i < k; i++) {
                if (counts[i] == 0) {
                    centroids.set(i, pixels.get(rand.nextInt(pixels.size())));
                    changed = true;
                    continue;
                }
                float nr = sumR[i] / counts[i];
                float ng = sumG[i] / counts[i];
                float nb = sumB[i] / counts[i];
                Color old = centroids.get(i);
                if (old.r != nr || old.g != ng || old.b != nb) changed = true;
                centroids.set(i, new Color(nr, ng, nb));
            }

            if (!changed) {
                log.debug("[{}] Converged at iteration {}", getStepName(), iter + 1);
                break;
            }
        }
        return centroids;
    }

    private BufferedImage buildOutputImage(int[] src, List<Color> pixels, int[] mapping,
                                           List<Color> centroids, int w, int h, boolean hasAlpha) {
        BufferedImage output = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int pos = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++, pos++) {
                if (mapping[pos] == -1) {
                    output.setRGB(x, y, hasAlpha ? 0 : src[pos]);
                    continue;
                }
                Color p = pixels.get(mapping[pos]);
                Color c = centroids.get(PreprocessUtil.nearestCentroid(p, centroids));
                int r = PreprocessUtil.clamp(Math.round(c.r));
                int g = PreprocessUtil.clamp(Math.round(c.g));
                int b = PreprocessUtil.clamp(Math.round(c.b));
                int rgb = hasAlpha
                        ? (((src[pos] >> 24) & 0xff) << 24) | (r << 16) | (g << 8) | b
                        : (r << 16) | (g << 8) | b;
                output.setRGB(x, y, rgb);
            }
        }
        return output;
    }
}

