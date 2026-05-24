package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Command: Remove Outline
 * Menghapus tepi/kontur transparan dari gambar (erosi alpha mask).
 * config.removeOutlineRadius = radius erosi (default: 2).
 */
@Slf4j
@Component
public class RemoveOutlineCommand implements PreprocessCommand {

    private static final int DEFAULT_RADIUS = 2;

    @Override
    public String getStepName() {
        return "REMOVE_OUTLINE";
    }

    @Override
    public BufferedImage execute(BufferedImage image, Config config) {
        int radius = config.getRemoveOutlineRadius() != null ? config.getRemoveOutlineRadius() : DEFAULT_RADIUS;
        log.info("[{}] Removing outline with radius={}", getStepName(), radius);

        int w = image.getWidth();
        int h = image.getHeight();

        // Snapshot original untuk deteksi pixel transparan (hindari artefak saat modifikasi)
        BufferedImage snapshot = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = snapshot.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gOut = output.createGraphics();
        gOut.drawImage(image, 0, 0, null);
        gOut.dispose();

        for (int y = radius; y < h - radius; y++) {
            for (int x = radius; x < w - radius; x++) {
                int a = (snapshot.getRGB(x, y) >> 24) & 0xFF;
                if (a == 0) continue;

                if (touchesTransparentNeighbor(snapshot, x, y, radius)) {
                    output.setRGB(x, y, 0x00000000); // set transparent
                }
            }
        }

        log.debug("[{}] Done. Output size={}x{}", getStepName(), w, h);
        return output;
    }

    private boolean touchesTransparentNeighbor(BufferedImage img, int x, int y, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (((img.getRGB(x + dx, y + dy) >> 24) & 0xFF) == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}

