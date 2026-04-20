package id.rockierocker.imagetools.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages output directory for processed files with optional automatic cleanup.
 */
@Slf4j
@Service
public class OutputDirectoryManagerService {

    private final Path outputDirectoryPath;
    private final boolean isTemporary;
    private final ScheduledExecutorService cleanupScheduler;

    private final int maxAgeHours;

    public OutputDirectoryManagerService(
            @Value("${output.directory:}") String outputDirectory,
            @Value("${temp.file.max.age.hours:24}") int maxAgeHours,
            @Value("${temp.file.auto.cleanup.enabled:true}") boolean autoCleanupEnabled,
            @Value("${temp.file.cleanup.interval.minutes:60}") int cleanupIntervalMinutes
    ) {
        this.maxAgeHours = maxAgeHours;

        if (outputDirectory != null && !outputDirectory.isBlank()) {
            this.outputDirectoryPath = Paths.get(outputDirectory.trim());
            this.isTemporary = false;
            log.info("Using configured output directory: {}", outputDirectoryPath);
        } else {
            String tempDir = System.getProperty("java.io.tmpdir");
            this.outputDirectoryPath = Paths.get(tempDir, "image-processing-output");
            this.isTemporary = true;
            log.info("Using temporary output directory: {}", outputDirectoryPath);
        }

        try {
            Files.createDirectories(outputDirectoryPath);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create output directory: " + outputDirectoryPath, e);
        }

        if (isTemporary && autoCleanupEnabled) {
            cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "TempFileCleanup");
                thread.setDaemon(true);
                return thread;
            });

            cleanupScheduler.scheduleAtFixedRate(
                    this::cleanupOldFiles,
                    cleanupIntervalMinutes,
                    cleanupIntervalMinutes,
                    TimeUnit.MINUTES
            );

            log.info("Automatic cleanup scheduled every {} minutes", cleanupIntervalMinutes);

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } else {
            cleanupScheduler = null;
        }
    }

    public Path getOutputDirectoryPath() {
        return outputDirectoryPath;
    }

    public boolean isTemporary() {
        return isTemporary;
    }

    public File createOutputFile(String fileName) {
        Path filePath = outputDirectoryPath.resolve(fileName);
        return filePath.toFile();
    }

    public File createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(outputDirectoryPath, prefix, suffix).toFile();
    }

    public File createTempFile(String prefix, String suffix, RuntimeException e) {
        try {
            return Files.createTempFile(outputDirectoryPath, prefix, suffix).toFile();
        } catch (IOException ex) {
            log.error("Error creating temporary file: " + ex.getMessage(), ex);
            throw e;
        }
    }

    public File createTempFile(String prefix, String suffix, byte[] fileByte) {
        try {
            File file = Files.createTempFile(outputDirectoryPath, prefix, suffix).toFile();
            return Files.write(file.toPath(), fileByte).toFile();
        } catch (IOException ex) {
            log.error("Error creating temporary file: " + ex.getMessage(), ex);
            log.warn("returning null instead");
            return null;
        }
    }

    public File createTempFile(String prefix, String suffix, byte[] fileByte, RuntimeException e) {
        try {
            File file = Files.createTempFile(outputDirectoryPath, prefix, suffix).toFile();
            return Files.write(file.toPath(), fileByte).toFile();
        } catch (IOException ex) {
            log.error("Error creating temporary file: " + ex.getMessage(), ex);
            throw e;
        }
    }

    public void cleanupOldFiles() {
        if (!isTemporary) return;

        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(maxAgeHours);

        try (var paths = Files.walk(outputDirectoryPath)) {
            paths
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            Instant creationTime = attrs.creationTime().toInstant();

                            if (creationTime.toEpochMilli() < cutoffTime) {
                                Files.deleteIfExists(path);
                                log.info("Deleted old file: {}", path.getFileName());
                            }
                        } catch (IOException e) {
                            log.warn("Failed to delete file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Error during cleanup", e);
        }
    }

    public void shutdown() {
        if (cleanupScheduler != null) {
            try {
                cleanupScheduler.shutdown();
                if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    cleanupScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cleanupScheduler.shutdownNow();
            }
        }
    }
}
