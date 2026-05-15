package id.rockierocker.imagetools.service.vectorize;

import java.nio.file.Path;
import java.util.List;

public interface Vectorizer {
    byte[] vectorize(
            Path inputImage,
            List<String> additionalCommand,
            RuntimeException runtimeException
    );

    byte[] vectorize(
            Path inputImage,
            List<String> additionalCommand
    ) throws Exception;

    String getName();
}
