package id.rockierocker.imagetools.preprocess.util;

import id.rockierocker.imagetools.preprocess.model.Color;

import java.awt.image.BufferedImage;
import java.util.List;

public class PreprocessUtil {
    public static int nearestCentroid(Color p, List<Color> centroids) {
        float min = Float.MAX_VALUE;
        int idx = 0;
        for (int i = 0; i < centroids.size(); i++) {
            float d = distance(p, centroids.get(i));
            if (d < min) {
                min = d;
                idx = i;
            }
        }
        return idx;
    }

    public static float distance(Color a, Color b) {
        float dr = a.r - b.r;
        float dg = a.g - b.g;
        float db = a.b - b.b;
        return dr * dr + dg * dg + db * db;
    }

    public static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static int applyKernel(BufferedImage img, int x, int y, int shift, List<List<Float>> sharpenKernel) {
        float sum = 0;
        for (int ky = -1; ky <= 1; ky++) {
            for (int kx = -1; kx <= 1; kx++) {
                int rgb = img.getRGB(x + kx, y + ky);
                int v = (rgb >> shift) & 0xff;
                sum += v * sharpenKernel.get(ky + 1).get(kx + 1);
            }
        }
        return clamp((int) sum);
    }

}
