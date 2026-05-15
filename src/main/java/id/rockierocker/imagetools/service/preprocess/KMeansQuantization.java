package id.rockierocker.imagetools.service.preprocess;


import id.rockierocker.imagetools.service.preprocess.model.Color;
import id.rockierocker.imagetools.service.preprocess.model.PreprocessConfig;
import id.rockierocker.imagetools.service.preprocess.util.PreprocessUtil;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Arrays;

@Slf4j
public class KMeansQuantization implements ImagePreprocess{

    @Override
    public BufferedImage process(BufferedImage inputImage, PreprocessConfig preprocessConfig) {
        log.info("Applying K-Means Quantization with k={} and iterations={}",
                preprocessConfig.getKColors(),
                preprocessConfig.getIterations());

        return kMeansQuantization(inputImage, preprocessConfig.getKColors(), preprocessConfig.getIterations());
    }

    public static BufferedImage kMeansQuantization(BufferedImage img, int k, int iterations) {
        int w = img.getWidth();
        int h = img.getHeight();
        int total = w * h;

        // Read pixels in bulk
        int[] src = img.getRGB(0, 0, w, h, null, 0, w);
        boolean hasAlpha = img.getColorModel().hasAlpha();

        // Build list of pixels to cluster: skip fully transparent pixels so we don't "fill" holes
        List<Color> pixels = new ArrayList<>(total);
        int[] mapping = new int[total]; // maps position -> index in pixels list, -1 if skipped
        Arrays.fill(mapping, -1);

        final int TRANSPARENT_THRESHOLD = 8; // alpha <= threshold will be considered transparent and skipped
        int idxMap = 0;
        for (int i = 0; i < total; i++) {
            int rgb = src[i];
            int a = (rgb >> 24) & 0xff;
            if (hasAlpha && a <= TRANSPARENT_THRESHOLD) {
                // skip transparent pixel
                continue;
            }
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;
            pixels.add(new Color(r, g, b));
            mapping[i] = idxMap++;
        }

        // If no non-transparent pixels, return a copy preserving alpha
        if (pixels.isEmpty()) {
            BufferedImage outEmpty = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            outEmpty.setRGB(0, 0, w, h, src, 0, w);
            return outEmpty;
        }

        // Adjust k if there are fewer pixels than k
        if (k <= 0) k = 1;
        if (k > pixels.size()) k = pixels.size();

        Random rand = new Random();
        List<Color> centroids = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            centroids.add(pixels.get(rand.nextInt(pixels.size())));
        }

        // Main k-means loop
        for (int iter = 0; iter < iterations; iter++) {
            // Prepare clusters: store sum r,g,b and count to avoid storing lists of points
            float[] sumR = new float[k];
            float[] sumG = new float[k];
            float[] sumB = new float[k];
            int[] counts = new int[k];

            // Assign pixels to nearest centroid
            for (Color p : pixels) {
                int ci = PreprocessUtil.nearestCentroid(p, centroids);
                sumR[ci] += p.r;
                sumG[ci] += p.g;
                sumB[ci] += p.b;
                counts[ci]++;
            }

            // Recompute centroids; if cluster empty, reinitialize to random pixel
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

            // Early exit if centroids didn't change (converged)
            if (!changed) break;
        }

        // Build output image preserving alpha/transparency: transparent pixels remain transparent
        BufferedImage out = new BufferedImage(w, h, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int pos = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++, pos++) {
                int mapIdx = mapping[pos];
                if (mapIdx == -1) {
                    // Preserve original alpha (transparent or nearly transparent)
                    if (hasAlpha) {
                        out.setRGB(x, y, 0); // fully transparent
                    } else {
                        // no alpha: use original pixel
                        out.setRGB(x, y, src[pos]);
                    }
                    continue;
                }
                Color p = pixels.get(mapIdx);
                Color c = centroids.get(PreprocessUtil.nearestCentroid(p, centroids));
                int r = PreprocessUtil.clamp(Math.round(c.r));
                int g = PreprocessUtil.clamp(Math.round(c.g));
                int b = PreprocessUtil.clamp(Math.round(c.b));
                if (hasAlpha) {
                    int a = (src[pos] >> 24) & 0xff;
                    int rgb = (a << 24) | (r << 16) | (g << 8) | b;
                    out.setRGB(x, y, rgb);
                } else {
                    int rgb = (r << 16) | (g << 8) | b;
                    out.setRGB(x, y, rgb);
                }
            }
        }

        return out;
    }
}
