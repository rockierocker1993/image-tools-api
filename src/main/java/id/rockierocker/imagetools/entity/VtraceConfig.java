package id.rockierocker.imagetools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "vtrace_config")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VtraceConfig extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_code", length = 50)
    private String configCode;

    @Column(name = "color_mode", length = 5)
    private String colorMode;

    @Column(name = "hierarchical", length = 10)
    private String hierarchical;

    @Column(name = "filter_speckle")
    private Integer filterSpeckle;

    @Column(name = "color_precision")
    private Integer colorPrecision;

    @Column(name = "gradient_step")
    private Integer gradientStep;

    @Column(name = "corner_threshold")
    private Integer cornerThreshold;

    @Column(name = "segment_length")
    private Double segmentLength;

    @Column(name = "splice_threshold")
    private Integer spliceThreshold;

    @Column(name = "curve_fitting_mode", length = 10)
    private String curveFittingMode;
}
