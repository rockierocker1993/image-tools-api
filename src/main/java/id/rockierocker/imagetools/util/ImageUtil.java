package id.rockierocker.imagetools.util;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

@Slf4j
public class ImageUtil {

    /**
     * Resize a BufferedImage to the given width and height with high quality.
     * Preserves transparency if present in the original image.
     *
     * @param img The original image to be resized.
     * @param w   The target width.
     * @param h   The target height.
     * @return A new BufferedImage that is the resized version of the original image.
     */
    public static BufferedImage resize(BufferedImage img, int w, int h) {
        // Preserve image type, especially for transparency
        int imageType = img.getType();
        if (imageType == BufferedImage.TYPE_CUSTOM) {
            // Use appropriate type based on alpha channel
            imageType = img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        }

        BufferedImage out = new BufferedImage(w, h, imageType);
        Graphics2D g = out.createGraphics();

        // Use high-quality rendering hints
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Convert a BufferedImage to a byte array.
     *
     * @param bufferedImage The BufferedImage to be converted.
     * @return A byte array representing the image data.
     * @throws IOException If an error occurs during writing.
     */
    public static byte[] toBytes(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // If the image has an alpha channel, write as PNG to preserve transparency.
        String format = bufferedImage.getColorModel().hasAlpha() ? "png" : "jpg";
        ImageIO.write(bufferedImage, format, baos);
        return baos.toByteArray();
    }

    /**
     * Convert a BufferedImage to a PNG byte array with high quality settings.
     *
     * @param bufferedImage The BufferedImage to be converted.
     * @return A byte array representing the image data.
     * @throws IOException If an error occurs during writing.
     */
    public static byte[] toBytesPng(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Use ImageWriter for better control over PNG quality
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            // Fallback to basic write
            ImageIO.write(bufferedImage, "png", baos);
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            ImageWriteParam param = writer.getDefaultWriteParam();
            // PNG doesn't use compression mode in the same way as JPEG
            // But we can ensure the best quality output

            writer.write(null, new javax.imageio.IIOImage(bufferedImage, null, null), param);
            writer.dispose();
        }

        return baos.toByteArray();
    }
    
    public static BufferedImage toBufferedImage(File file, RuntimeException runtimeException) {
        // 1) Coba pakai ImageIO dulu (cepat, no native).
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                return img;
            }
            log.warn("ImageIO.read returned null for {}, fallback to OpenCV", file.getAbsolutePath());
        } catch (Exception e) {
            log.warn("ImageIO.read failed for {} ({}), fallback to OpenCV",
                    file.getAbsolutePath(), e.getMessage());
        }

        // 2) Fallback: decode via OpenCV (handle 16-bit PNG, CMYK, iCCP warning, dll).
        try {
            byte[] pngBytes = toPngBytesViaOpenCv(file);
            BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(pngBytes));
            if (img == null) {
                throw new RuntimeException("Decoded PNG bytes still unreadable by ImageIO");
            }
            return img;
        } catch (Exception e) {
            log.error("Failed to read image (ImageIO + OpenCV) for {}: {}",
                    file.getAbsolutePath(), e.getMessage(), e);
            throw runtimeException;
        }
    }

    /**
     * Convert file gambar (JPG/JPEG/PNG/BMP/dll) ke byte array PNG menggunakan OpenCV.
     * Lebih reliable dibanding ImageIO karena bisa handle CMYK JPEG, progressive JPEG, EXIF orientation, dll.
     *
     * @param inputFile file gambar sumber
     * @return byte array dalam format PNG
     */
    public static byte[] toPngBytesViaOpenCv(File inputFile) {
        loadOpenCv();
        Mat mat = Imgcodecs.imread(inputFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
        if (mat.empty()) {
            // Fallback: coba baca via ImageIO lalu re-write
            try {
                BufferedImage img = ImageIO.read(inputFile);
                if (img == null) {
                    throw new RuntimeException("Cannot decode image: " + inputFile.getAbsolutePath());
                }
                // Convert ke TYPE_INT_ARGB agar konsisten
                BufferedImage normalized = new BufferedImage(
                        img.getWidth(), img.getHeight(),
                        img.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g = normalized.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(normalized, "png", baos);
                return baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read image file (both OpenCV and ImageIO): "
                        + inputFile.getAbsolutePath(), e);
            }
        }

        MatOfByte outputBuffer = new MatOfByte();
        MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_PNG_COMPRESSION, 3);
        boolean success = Imgcodecs.imencode(".png", mat, outputBuffer, params);
        mat.release();

        if (!success) {
            throw new RuntimeException("Failed to encode image to PNG bytes: " + inputFile.getAbsolutePath());
        }
        log.info("Converted {} to PNG ({} bytes) via OpenCV", inputFile.getAbsolutePath(), outputBuffer.toArray().length);
        return outputBuffer.toArray();
    }

    // -------------------------------------------------------------------------
    // WebP Conversion (using OpenCV)
    // -------------------------------------------------------------------------

    /**
     * Konversi file gambar (PNG/JPG/JPEG) ke file WebP.
     * File output disimpan di direktori yang sama dengan nama yang sama + ekstensi .webp
     *
     * @param inputFile file gambar sumber (png/jpg/jpeg)
     * @return file WebP hasil konversi
     */
    public static File toWebpFile(File inputFile) {
        return toWebpFile(inputFile, 80);
    }

    /**
     * Konversi file gambar (PNG/JPG/JPEG) ke file WebP dengan kualitas tertentu.
     *
     * @param inputFile file gambar sumber (png/jpg/jpeg)
     * @param quality   kualitas WebP 0-100 (80 = recommended, 100 = lossless-like)
     * @return file WebP hasil konversi
     */
    public static File toWebpFile(File inputFile, int quality) {
        loadOpenCv();
        String inputPath = inputFile.getAbsolutePath();
        String outputPath = inputPath.replaceAll("(?i)\\.(png|jpe?g)$", "") + ".webp";

        Mat mat = Imgcodecs.imread(inputPath, Imgcodecs.IMREAD_UNCHANGED);
        if (mat.empty()) {
            throw new RuntimeException("Failed to read image file: " + inputPath);
        }

        MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_WEBP_QUALITY, quality);
        boolean success = Imgcodecs.imwrite(outputPath, mat, params);
        mat.release();

        if (!success) {
            throw new RuntimeException("Failed to write WebP file: " + outputPath);
        }

        log.info("Converted {} to WebP: {}", inputPath, outputPath);
        return new File(outputPath);
    }

    /**
     * Konversi file gambar (PNG/JPG/JPEG) ke byte array WebP.
     *
     * @param inputFile file gambar sumber
     * @return byte array dalam format WebP
     */
    public static byte[] toWebpBytes(File inputFile) {
        return toWebpBytes(inputFile, 80);
    }

    /**
     * Konversi file gambar (PNG/JPG/JPEG) ke byte array WebP dengan kualitas tertentu.
     *
     * @param inputFile file gambar sumber
     * @param quality   kualitas WebP 0-100
     * @return byte array dalam format WebP
     */
    public static byte[] toWebpBytes(File inputFile, int quality) {
        loadOpenCv();
        Mat mat = Imgcodecs.imread(inputFile.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
        if (mat.empty()) {
            throw new RuntimeException("Failed to read image file: " + inputFile.getAbsolutePath());
        }

        MatOfByte outputBuffer = new MatOfByte();
        MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_WEBP_QUALITY, quality);
        boolean success = Imgcodecs.imencode(".webp", mat, outputBuffer, params);
        mat.release();

        if (!success) {
            throw new RuntimeException("Failed to encode image to WebP bytes");
        }

        return outputBuffer.toArray();
    }

    /**
     * Konversi BufferedImage ke byte array WebP.
     *
     * @param bufferedImage gambar sumber
     * @param quality       kualitas WebP 0-100
     * @return byte array dalam format WebP
     * @throws IOException jika gagal convert ke bytes sementara
     */
    public static byte[] toWebpBytes(BufferedImage bufferedImage, int quality) throws IOException {
        // Simpan ke temp file dulu lalu konversi
        File tempFile = File.createTempFile("img-convert-", ".png");
        try {
            ImageIO.write(bufferedImage, "png", tempFile);
            return toWebpBytes(tempFile, quality);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Load OpenCV native library (lazy, hanya load sekali).
     */
    private static volatile boolean opencvLoaded = false;

    public static void loadOpenCv() {
        if (!opencvLoaded) {
            synchronized (ImageUtil.class) {
                if (!opencvLoaded) {
                    nu.pattern.OpenCV.loadLocally();
                    opencvLoaded = true;
                    log.info("OpenCV native library loaded successfully");
                }
            }
        }
    }

}
