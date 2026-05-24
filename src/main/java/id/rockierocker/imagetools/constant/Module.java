package id.rockierocker.imagetools.constant;

public enum Module {
    REMBG("REMBG"),
    UPSCALE("UPSCALE"),
    VECTOR("VECTOR"),
    ;

    public static Module findByName(String name) {
        for (Module jobType : Module.values()) {
            if (jobType.name.equalsIgnoreCase(name)) {
                return jobType;
            }
        }
        throw new IllegalArgumentException("No Module with name: " + name);
    }
    Module(String name) {
        this.name = name;

    }
    public final String name;
}
