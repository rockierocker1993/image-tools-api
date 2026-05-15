package id.rockierocker.imagetools.service.preprocess;


import id.rockierocker.imagetools.service.preprocess.model.PreprocessConfig;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
public class RemoveOutline implements ImagePreprocess {

    @Override
    public BufferedImage process(BufferedImage inputImage, PreprocessConfig preprocessConfig) {
        log.info("Removing outline: {}", preprocessConfig.getSharpenKernel());
        return removeContour(inputImage, 2);
    }

    public BufferedImage  removeContour(BufferedImage img, int radius) {
        int w = img.getWidth();
        int h = img.getHeight();

        BufferedImage copy = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        for (int y = radius; y < h - radius; y++) {
            for (int x = radius; x < w - radius; x++) {

                int a = (copy.getRGB(x, y) >> 24) & 0xFF;
                if (a == 0) continue;

                boolean touchTransparent = false;

                for (int dy = -radius; dy <= radius && !touchTransparent; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int na = (copy.getRGB(x + dx, y + dy) >> 24) & 0xFF;
                        if (na == 0) {
                            touchTransparent = true;
                            break;
                        }
                    }
                }

                if (touchTransparent) {
                    img.setRGB(x, y, 0x00000000);
                }
            }
        }
        return copy;
    }



}
