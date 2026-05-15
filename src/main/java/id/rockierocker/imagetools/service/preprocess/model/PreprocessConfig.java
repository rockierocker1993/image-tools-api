package id.rockierocker.imagetools.service.preprocess.model;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PreprocessConfig {
    private Integer kColors;
    private Float contrast;
    private Integer iterations;
    private List<List<Float>> sharpenKernel;
}
