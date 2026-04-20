package id.rockierocker.imagetools.vectorize;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * VTracer vectorizer implementation.
 */
@Component
@Slf4j
public class VTracerVectorizer extends AbstractVectorizer implements Vectorizer {
    private final String vtracerCmd = "vtracer"; // Assuming vtracer is in PATH
    @Override
    public String getName() {
        return "VTracer";
    }

    @Override
    public byte[] vectorize(Path input, List<String> additionalCommand, RuntimeException runtimeException) {
        try {
            return vectorize(input, additionalCommand);
        } catch (Exception e) {
            log.error("VTracer vectorization failed: {}", e.getMessage(), e);
            throw runtimeException;
        }
    }

    @Override
    public byte[] vectorize(Path input, List<String> additionalCommand) throws Exception {
        log.info("Starting VTracer vectorization...");
        Path output = getOutputPath();
        ProcessBuilder pb = new ProcessBuilder(
                vtracerCmd,
                "--input", input.toAbsolutePath().toString(),
                "--output", output.toString()
        );
        if (additionalCommand != null && !additionalCommand.isEmpty()) {
            pb.command().addAll(additionalCommand);
        }
        exec(pb, "vtracer");
        return readAndDelete(output);
    }
}

