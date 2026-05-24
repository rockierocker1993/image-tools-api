package id.rockierocker.imagetools.service.preprocess.pipeline;

import id.rockierocker.imagetools.service.preprocess.command.PreprocessCommand;
import id.rockierocker.imagetools.service.preprocess.model.Config;
import id.rockierocker.imagetools.service.preprocess.registry.PreprocessCommandRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Command Pattern — Invoker.
 *
 * Menjalankan pipeline preprocessing secara berurutan.
 * Setiap step:
 *   1. Lookup command dari registry
 *   2. Validate config
 *   3. Execute command
 *   4. Hasil menjadi input step berikutnya
 *
 * Jika satu step menghasilkan null, pipeline berhenti (logged as warning).
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PreprocessPipeline {

    private final PreprocessCommandRegistry registry;

    /**
     * Eksekusi pipeline preprocessing.
     *
     * @param steps  list nama step berurutan (contoh: ["K_MEANS_QUANTIZATION", "SHARPEN"])
     * @param image  image awal
     * @param config konfigurasi untuk semua step
     * @return image hasil setelah semua step dijalankan
     */
    public BufferedImage execute(List<String> steps, BufferedImage image, Config config) {
        log.info("Starting preprocess pipeline: steps={}", steps);
        BufferedImage current = image;

        for (int i = 0; i < steps.size(); i++) {
            String stepName = steps.get(i);
            log.info("Pipeline step [{}/{}]: {}", i + 1, steps.size(), stepName);

            PreprocessCommand command = registry.getCommand(stepName);
            command.validate(config);

            BufferedImage result = command.execute(current, config);

            if (result == null) {
                log.warn("Pipeline step '{}' returned null — stopping pipeline.", stepName);
                break;
            }

            current = result;
        }

        log.info("Preprocess pipeline completed. Final image size={}x{}", current.getWidth(), current.getHeight());
        return current;
    }
}

