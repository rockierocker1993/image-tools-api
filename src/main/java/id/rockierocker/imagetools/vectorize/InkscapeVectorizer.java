package id.rockierocker.imagetools.vectorize;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class InkscapeVectorizer extends AbstractVectorizer implements Vectorizer {

    private final String inkscapeCmd;

    public InkscapeVectorizer(String inkscapeCmd) {
        this.inkscapeCmd = inkscapeCmd;
    }

    @Override
    public String getName() {
        return "Inkscape";
    }

    @Override
    public byte[] vectorize(Path input, List<String> additionalCommand, RuntimeException runtimeException) {
        try {
            return vectorize(input, additionalCommand);
        } catch (Exception e) {
            log.error("Inkscape vectorization failed: {}", e.getMessage(), e);
            throw runtimeException;
        }
    }

    @Override
    public byte[] vectorize(Path inputImage, List<String> additionalCommand) throws Exception {
        log.info("Starting Inkscape vectorization...");
        Path output = getOutputPath();
        List<String> command = List.of(
                inkscapeCmd,
                inputImage.toAbsolutePath().toString(),
                "--batch-process",
                //"--actions=TraceBitmap;ObjectToPath;ExportPlainSVG",
                "--actions=SelectAll;TraceBitmap;Delete;ExportPlainSVG",
                "--export-filename=" + output.toAbsolutePath()
        );
        ProcessBuilder pb = new ProcessBuilder(command);
        exec(pb, "Inkscape");
        return readAndDelete(output);
    }
}

