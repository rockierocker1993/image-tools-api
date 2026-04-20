package id.rockierocker.imagetools.vectorize.constant;

import lombok.Getter;
/**
 * Enum for hierarchical modes.
 * Only applies to color mode, ColorMode.COLOR
 */
@Getter
public enum VTracerHierarchical {
    STACKED("STACKED", "stacked"),
    CUTOUT("CUTOUT","cutout");
    private VTracerHierarchical(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;

    public static VTracerHierarchical fromString(String mode) {
        for (VTracerHierarchical hierarchical : VTracerHierarchical.values()) {
            if (hierarchical.getMode().equalsIgnoreCase(mode)) {
                return hierarchical;
            }
        }
        return null;
    }
}
