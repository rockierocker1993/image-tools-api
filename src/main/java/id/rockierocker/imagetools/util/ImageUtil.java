package id.rockierocker.imagetools.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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


    /**
     * Convert a BufferedImage to a PNG byte array with high quality settings.
     *
     * @param bufferedImage The BufferedImage to be converted.
     * @param runtimeException The exception to throw if an error occurs.
     * @return A byte array representing the image data.
     */
    public static byte[] toBytesPng(BufferedImage bufferedImage, RuntimeException runtimeException) {
        try {
            return toBytesPng(bufferedImage);
        } catch (Exception e){
            log.error("Error converting BufferedImage to PNG byte array {}", e.getMessage(), e);
            throw runtimeException;
        }
    }

    /**
     * Get the hexadecimal RGBA color of a pixel at (x, y) in the image.
     *
     * @param image The BufferedImage to sample.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @return A string representing the color in hexadecimal RGBA format.
     */
    public static String getHexRGBA(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);

        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        return String.format("#%02X%02X%02X%02X", r, g, b, a);
    }

    /**
     * Get the hexadecimal RGB color of a pixel at (x, y) in the image.
     *
     * @param image The BufferedImage to sample.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @return A string representing the color in hexadecimal RGB format.
     */
    public static String getHexFromPixel(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return String.format("#%02X%02X%02X", r, g, b);
    }

    /**
     * Convert an integer RGB value to a hexadecimal color string.
     *
     * @param rgb The integer RGB value.
     * @return A string representing the color in hexadecimal RGB format.
     */
    public static String getHexFast(int rgb) {
        char[] hex = new char[7];
        hex[0] = '#';

        int[] v = {
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF
        };

        final char[] table = "0123456789ABCDEF".toCharArray();

        for (int i = 0; i < 3; i++) {
            hex[i * 2 + 1] = table[v[i] >>> 4];
            hex[i * 2 + 2] = table[v[i] & 0x0F];
        }

        return new String(hex);
    }

    /**
     * Scan the corners of an image and count the occurrence of each hexadecimal color.
     *
     * @param img        The BufferedImage to scan.
     * @param sampleSize The size of the square area to sample at each corner.
     * @return A map with hexadecimal color strings as keys and their occurrence counts as values.
     */
    public static Map<String, Integer> scanCornerHex(BufferedImage img, int sampleSize) {
        Map<String, Integer> counter = new HashMap<>();

        int w = img.getWidth();
        int h = img.getHeight();

        int[][] points = {
                {0, 0}, {w - 1, 0}, {0, h - 1}, {w - 1, h - 1}
        };

        for (int[] p : points) {
            for (int dx = 0; dx < sampleSize; dx++) {
                for (int dy = 0; dy < sampleSize; dy++) {
                    int x = Math.min(p[0] + dx, w - 1);
                    int y = Math.min(p[1] + dy, h - 1);

                    String hex = getHexFast(img.getRGB(x, y));
                    counter.merge(hex, 1, Integer::sum);
                }
            }
        }
        return counter;
    }

    /**
     * Determine if a given hexadecimal color represents a white background.
     *
     * @param hex The hexadecimal color string (e.g., "#FFFFFF").
     * @return True if the color is considered white, false otherwise.
     */
    public static boolean isBackgroundWhite(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);

        return r > 240 && g > 240 && b > 240;
    }

    /**
     * Convert a hexadecimal color string to an RGB integer array.
     *
     * @param hex The hexadecimal color string (e.g., "#RRGGBB").
     * @return An array of integers representing the RGB values.
     */
    public static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);

        return new int[]{r, g, b};
    }

    /**
     * Convert a hexadecimal color string to an RGBA integer array.
     *
     * @param hex The hexadecimal color string (e.g., "#RRGGBBAA").
     * @return An array of integers representing the RGBA values.
     */
    public static int[] hexToRgba(String hex) {
        hex = hex.replace("#", "");

        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        int a = Integer.parseInt(hex.substring(6, 8), 16);

        return new int[]{r, g, b, a};
    }

    /**
     * Convert a hexadecimal color string to an integer array representing RGB or RGBA values.
     *
     * @param hex The hexadecimal color string (e.g., "#RRGGBB" or "#RRGGBBAA").
     * @return An array of integers representing the RGB or RGBA values.
     */
    public static int[] hexToColor(String hex) {
        hex = hex.replace("#", "");

        if (hex.length() == 6) {
            return new int[]{
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16)
            };
        }

        if (hex.length() == 8) {
            return new int[]{
                    Integer.parseInt(hex.substring(0, 2), 16),
                    Integer.parseInt(hex.substring(2, 4), 16),
                    Integer.parseInt(hex.substring(4, 6), 16),
                    Integer.parseInt(hex.substring(6, 8), 16)
            };
        }

        throw new IllegalArgumentException("Invalid hex color: " + hex);
    }

    public static BufferedImage removeColor(String hexColor, BufferedImage image) {
        int[] targetRgb = hexToRgb(hexColor);

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                if (r == targetRgb[0] && g == targetRgb[1] && b == targetRgb[2]) {
                    outputImage.setRGB(x, y, 0x00000000); // Set pixel to transparent
                } else {
                    outputImage.setRGB(x, y, rgb | 0xFF000000); // Preserve original pixel with full opacity
                }
            }
        }

        return outputImage;
    }

    public static boolean hasTransparency(BufferedImage img) {
        if (!img.getColorModel().hasAlpha()) return false;

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (img.getRGB(x, y) >> 24) & 0xff;
                if (a < 250) return true;
            }
        }
        return false;
    }

    public static double edgeSharpnessScore(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        double total = 0;
        int count = 0;

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int c = img.getRGB(x, y) & 0xff;
                int r = img.getRGB(x + 1, y) & 0xff;
                int b = img.getRGB(x, y + 1) & 0xff;

                total += Math.abs(c - r) + Math.abs(c - b);
                count++;
            }
        }
        return total / count;
    }

    public static double borderColorVariance(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        List<Integer> colors = new ArrayList<>();

        for (int x = 0; x < w; x++) {
            colors.add(img.getRGB(x, 0));
            colors.add(img.getRGB(x, h - 1));
        }
        for (int y = 0; y < h; y++) {
            colors.add(img.getRGB(0, y));
            colors.add(img.getRGB(w - 1, y));
        }

        double avg = colors.stream().mapToInt(c -> c & 0xff).average().orElse(0);
        double var = 0;

        for (int c : colors) {
            double v = (c & 0xff) - avg;
            var += v * v;
        }
        return var / colors.size();
    }

    public static String detectBackgroundStatus(BufferedImage img) {
        boolean transparent = hasTransparency(img);
        double edge = edgeSharpnessScore(img);
        double borderVar = borderColorVariance(img);

        if (transparent && edge > 10) {
            return "BACKGROUND_REMOVED";
        }

        if (!transparent && borderVar < 10 && edge > 10) {
            return "SOLID_BACKGROUND_NOT_REMOVED";
        }

        if (edge < 5) {
            return "BLUR_BACKGROUND";
        }

        return "DIRTY_OR_PARTIAL_BACKGROUND";
    }

    public static BufferedImage toBufferedImage(File file, RuntimeException runtimeException) {
        try {
            return ImageIO.read(file);
        } catch (Exception e) {
            throw runtimeException;
        }
    }

    public static File toTempFile(BufferedImage bufferedImage, String format, RuntimeException runtimeException) {
        try {
            File tempFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), null);
            ImageIO.write(bufferedImage, format, tempFile);
            return tempFile;
        } catch (IOException e) {
            throw runtimeException;
        }
    }

    public static File converToFng(File jpgFile, RuntimeException runtimeException) {
        try {
            BufferedImage img = ImageIO.read(jpgFile);
            File pngFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), null);
            ImageIO.write(img, "png", pngFile);
            return pngFile;
        } catch (IOException e) {
            throw runtimeException;
        }
    }


}
