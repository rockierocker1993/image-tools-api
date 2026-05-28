package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Command: Cartoonize (OpenCV).
 *
 * Algoritma klasik cartoon effect:
 *  1. Convert ke grayscale → median blur → adaptive threshold → edge mask
 *  2. Pada gambar warna: bilateral filter berulang kali untuk meratakan warna
 *     namun mempertahankan tepi (efek lukisan / cell-shading).
 *  3. Gabungkan edge mask dengan hasil bilateral via bitwise_and per channel
 *     → garis tepi hitam di atas warna yang sudah di-smooth.
 *
 * Konfigurasi (di Config):
 *  - cartoonizeBlurSize             (default 7, harus ganjil)
 *  - cartoonizeEdgeBlockSize        (default 9, harus ganjil & >1)
 *  - cartoonizeEdgeC                (default 2)
 *  - cartoonizeBilateralIterations  (default 7)
 *  - cartoonizeBilateralD           (default 9)
 *  - cartoonizeSigmaColor           (default 75.0)
 *  - cartoonizeSigmaSpace           (default 75.0)
 */
@Slf4j
@Component
public class CartoonizeComand implements PreprocessCommand {

    static {
        ImageUtil.loadOpenCv();
    }

    @Override
    public String getStepName() {
        return "CARTOONIZE";
    }

    @Override
    public void validate(Config config) {
        Integer blur = config.getCartoonizeBlurSize();
        if (blur != null && (blur < 1 || blur % 2 == 0)) {
            throw new IllegalArgumentException("cartoonizeBlurSize harus bilangan ganjil >= 1");
        }
        Integer block = config.getCartoonizeEdgeBlockSize();
        if (block != null && (block <= 1 || block % 2 == 0)) {
            throw new IllegalArgumentException("cartoonizeEdgeBlockSize harus bilangan ganjil > 1");
        }
        Integer it = config.getCartoonizeBilateralIterations();
        if (it != null && it < 1) {
            throw new IllegalArgumentException("cartoonizeBilateralIterations harus >= 1");
        }
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        int blurSize       = config.getCartoonizeBlurSize() != null ? config.getCartoonizeBlurSize() : 7;
        int edgeBlockSize  = config.getCartoonizeEdgeBlockSize() != null ? config.getCartoonizeEdgeBlockSize() : 9;
        int edgeC          = config.getCartoonizeEdgeC() != null ? config.getCartoonizeEdgeC() : 2;
        int bilateralIter  = config.getCartoonizeBilateralIterations() != null ? config.getCartoonizeBilateralIterations() : 7;
        int bilateralD     = config.getCartoonizeBilateralD() != null ? config.getCartoonizeBilateralD() : 9;
        double sigmaColor  = config.getCartoonizeSigmaColor() != null ? config.getCartoonizeSigmaColor() : 75.0;
        double sigmaSpace  = config.getCartoonizeSigmaSpace() != null ? config.getCartoonizeSigmaSpace() : 75.0;

        log.info("[{}] Cartoonizing: blur={}, edgeBlock={}, edgeC={}, bilateralIter={}, d={}, sigmaColor={}, sigmaSpace={}",
                getStepName(), blurSize, edgeBlockSize, edgeC, bilateralIter, bilateralD, sigmaColor, sigmaSpace);

        boolean hasAlpha = image.getColorModel().hasAlpha();

        // Simpan alpha original untuk dikembalikan di akhir (OpenCV diproses di BGR saja).
        Mat alphaMat = hasAlpha ? extractAlpha(image) : null;

        Mat src = bufferedImageToMatBgr(image);

        // 1. Edge mask
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Mat grayBlur = new Mat();
        Imgproc.medianBlur(gray, grayBlur, blurSize);
        Mat edges = new Mat();
        Imgproc.adaptiveThreshold(
                grayBlur, edges,
                255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY,
                edgeBlockSize,
                edgeC);
        gray.release();
        grayBlur.release();

        // 2. Color smoothing via bilateral filter (iteratif).
        Mat color = src;
        for (int i = 0; i < bilateralIter; i++) {
            Mat smoothed = new Mat();
            Imgproc.bilateralFilter(color, smoothed, bilateralD, sigmaColor, sigmaSpace);
            if (color != src) color.release();
            color = smoothed;
        }

        // 3. Gabungkan: warna AND edges (broadcast mask 1ch ke 3ch via cvtColor).
        Mat edges3 = new Mat();
        Imgproc.cvtColor(edges, edges3, Imgproc.COLOR_GRAY2BGR);
        Mat cartoon = new Mat();
        Core.bitwise_and(color, edges3, cartoon);
        edges.release();
        edges3.release();
        if (color != src) color.release();
        src.release();

        BufferedImage result = matBgrToBufferedImage(cartoon, hasAlpha);
        cartoon.release();

        if (hasAlpha) {
            restoreAlpha(result, alphaMat);
            alphaMat.release();
        }

        log.debug("[{}] Done. Output size={}x{}", getStepName(), result.getWidth(), result.getHeight());
        return result;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BufferedImage ⇄ Mat helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Convert BufferedImage (any type) ke Mat BGR 3-channel 8-bit. */
    private Mat bufferedImageToMatBgr(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage bgr = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        java.awt.Graphics2D g = bgr.createGraphics();
        // Latar putih untuk pixel transparan (alpha akan di-restore terpisah).
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.drawImage(image, 0, 0, null);
        g.dispose();

        byte[] data = ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(h, w, CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    /** Convert Mat BGR 8UC3 ke BufferedImage (TYPE_3BYTE_BGR, atau TYPE_INT_ARGB jika withAlpha). */
    private BufferedImage matBgrToBufferedImage(Mat mat, boolean withAlpha) {
        int w = mat.cols();
        int h = mat.rows();
        byte[] data = new byte[w * h * (int) mat.elemSize()];
        mat.get(0, 0, data);

        BufferedImage bgr = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        System.arraycopy(data, 0,
                ((DataBufferByte) bgr.getRaster().getDataBuffer()).getData(),
                0, data.length);

        if (!withAlpha) return bgr;

        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = argb.createGraphics();
        g.drawImage(bgr, 0, 0, null);
        g.dispose();
        return argb;
    }

    /** Ambil channel alpha sebagai Mat 8UC1. */
    private Mat extractAlpha(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
        byte[] alpha = new byte[w * h];
        for (int i = 0; i < argb.length; i++) {
            alpha[i] = (byte) ((argb[i] >> 24) & 0xff);
        }
        Mat mat = new Mat(h, w, CvType.CV_8UC1);
        mat.put(0, 0, alpha);
        return mat;
    }

    /** Tulis kembali alpha ke BufferedImage TYPE_INT_ARGB. */
    private void restoreAlpha(BufferedImage argbImage, Mat alphaMat) {
        int w = argbImage.getWidth();
        int h = argbImage.getHeight();
        byte[] alpha = new byte[w * h];
        alphaMat.get(0, 0, alpha);
        int[] argb = argbImage.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < argb.length; i++) {
            argb[i] = (argb[i] & 0x00ffffff) | ((alpha[i] & 0xff) << 24);
        }
        argbImage.setRGB(0, 0, w, h, argb, 0, w);
    }
}
