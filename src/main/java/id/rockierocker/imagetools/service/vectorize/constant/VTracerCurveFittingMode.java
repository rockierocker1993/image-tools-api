package id.rockierocker.imagetools.service.vectorize.constant;

import lombok.Getter;

@Getter
public enum VTracerCurveFittingMode {
    PIXEL("PIXEL","pixel"),
    POLYGON("POLYGON","polygon"),
    SPLINE("SPLINE","spline"),;
    private VTracerCurveFittingMode(String mode, String command) {
        this.mode = mode;
        this.command = command;
    }
    private final String mode;
    private final String command;

    public static VTracerCurveFittingMode fromString(String mode) {
        for (VTracerCurveFittingMode fittingMode : VTracerCurveFittingMode.values()) {
            if (fittingMode.getMode().equalsIgnoreCase(mode)) {
                return fittingMode;
            }
        }
        return null;
    }
}
