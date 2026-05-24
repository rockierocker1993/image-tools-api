package id.rockierocker.imagetools.service.preprocess.registry;

import id.rockierocker.imagetools.service.preprocess.command.PreprocessCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registry semua PreprocessCommand yang terdaftar sebagai Spring Bean.
 * Auto-discover semua implementasi PreprocessCommand via constructor injection.
 *
 * Cara menambah command baru:
 * 1. Buat class implements PreprocessCommand
 * 2. Annotate dengan @Component
 * 3. Implement getStepName() dengan nama unik
 * → Registry akan otomatis menemukannya tanpa perubahan kode lain.
 */
@Slf4j
@Component
public class PreprocessCommandRegistry {

    private final Map<String, PreprocessCommand> commandMap;

    public PreprocessCommandRegistry(List<PreprocessCommand> commands) {
        this.commandMap = commands.stream()
                .collect(Collectors.toMap(
                        cmd -> cmd.getStepName().toUpperCase(),
                        cmd -> cmd
                ));
        log.info("PreprocessCommandRegistry initialized with {} commands: {}",
                commandMap.size(), commandMap.keySet());
    }

    /**
     * Ambil command berdasarkan nama step.
     *
     * @param stepName nama step (case-insensitive)
     * @return PreprocessCommand yang sesuai
     * @throws IllegalArgumentException jika step tidak ditemukan
     */
    public PreprocessCommand getCommand(String stepName) {
        PreprocessCommand command = commandMap.get(stepName.toUpperCase());
        if (command == null) {
            throw new IllegalArgumentException(
                    String.format("Unknown preprocess step: '%s'. Available steps: %s",
                            stepName, commandMap.keySet()));
        }
        return command;
    }

    public boolean hasCommand(String stepName) {
        return commandMap.containsKey(stepName.toUpperCase());
    }
}

