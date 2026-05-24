package id.rockierocker.imagetools.service.preprocess.command;

import id.rockierocker.imagetools.service.preprocess.model.Config;

import java.awt.image.BufferedImage;

/**
 * Command Pattern — Command Interface.
 *
 * Setiap preprocessing step mengimplementasikan interface ini.
 * Pipeline akan memanggil execute() secara berurutan.
 */
public interface PreprocessCommand {

    /**
     * Nama unik step ini — dipakai sebagai key di registry.
     * Harus sama dengan nama yang disimpan di database PreprocessConfig.steps.
     */
    String getStepName();

    /**
     * Eksekusi preprocessing pada image.
     *
     * @param image  BufferedImage input
     * @param config konfigurasi preprocessing
     * @return BufferedImage hasil processing
     */
    BufferedImage execute(BufferedImage image, Config config);

    /**
     * Validasi config sebelum execute().
     * Override untuk validasi spesifik per command.
     * Lempar IllegalArgumentException jika config tidak valid.
     */
    default void validate(Config config) {
        // default: no validation
    }
}

