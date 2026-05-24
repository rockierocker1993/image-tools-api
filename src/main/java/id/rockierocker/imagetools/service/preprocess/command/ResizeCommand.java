package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Command: Resize
 * Mengubah ukuran gambar agar tingginya tidak melebihi config.maxHeight,
 * dengan mempertahankan aspek rasio. Jika maxHeight null atau gambar sudah
 * lebih kecil, gambar dikembalikan tanpa perubahan.
 */
@Slf4j
@Component
public class ResizeCommand implements PreprocessCommand {

    @Override
    public String getStepName() {
        return "RESIZE";
    }

    @Override
    public void validate(Config config) {
        if (config.getMaxHeightOrWidth() != null && config.getMaxHeightOrWidth() <= 0) {
            throw new IllegalArgumentException("Resize requires 'maxHeight' > 0 in config");
        }
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        int maxHeightOrWidth = config.getMaxHeightOrWidth() != null ? config.getMaxHeightOrWidth() : 0;

        int w = image.getWidth();
        int h = image.getHeight();

        if (maxHeightOrWidth <= 0 || h <= maxHeightOrWidth) {
            log.debug("[{}] No resize needed. size={}x{}, maxHeightOrWidth={}", getStepName(), w, h, maxHeightOrWidth);
            return image;
        }
        int newH = h;
        int newW = w;
        if (newH > newW){
            newH = maxHeightOrWidth;
            newW = (int) Math.round((double) w * newH / h);
        } else {
            newW = maxHeightOrWidth;
            newH = (int) Math.round((double) h * newW / w);
        }

        log.info("[{}] Resizing {}x{} → {}x{} (maxHeight={})", getStepName(), w, h, newW, newH, maxHeightOrWidth);

        boolean hasAlpha = image.getColorModel().hasAlpha();
        BufferedImage output = new BufferedImage(newW, newH,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = output.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(image, 0, 0, newW, newH, null);
        g2d.dispose();

        log.debug("[{}] Done. Output size={}x{}", getStepName(), newW, newH);
        return output;
    }

}

