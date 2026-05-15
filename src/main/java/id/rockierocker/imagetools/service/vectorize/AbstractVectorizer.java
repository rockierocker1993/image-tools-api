package id.rockierocker.imagetools.service.vectorize;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
abstract class AbstractVectorizer {

    protected void exec(ProcessBuilder pb, String name) throws Exception {
        log.info("Executing {} command: {}", name, String.join(" ", pb.command()));

        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        log.info("{} output: \n{}", name, output.toString());
        if (p.waitFor() != 0) {
            throw new RuntimeException(name + " failed");
        }
    }

    protected Path getOutputPath() throws IOException {
        File tempFile = File.createTempFile("vector", ".svg");
        return tempFile.toPath();
    }

    protected byte[] readAndDelete(Path file) throws IOException {
        byte[] data = Files.readAllBytes(file);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete temp file: {}", file, e);
        }
        return data;
    }

}
