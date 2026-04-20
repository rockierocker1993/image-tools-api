package id.rockierocker.imagetools.vectorize.constant;

import lombok.Getter;

@Getter
public enum VTracerColorMode {
    BW("BW", "bw"),
    COLOR("COLOR","color");
    private VTracerColorMode(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;

    public static VTracerColorMode fromString(String mode) {
        for (VTracerColorMode colorMode : VTracerColorMode.values()) {
            if (colorMode.getMode().equalsIgnoreCase(mode)) {
                return colorMode;
            }
        }
        return null;
    }
}
