package id.rockierocker.imagetools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "preprocess_config")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreprocessConfig extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_code", length = 50)
    private String configCode;

    @Column(name = "process", length = 20)
    private String process;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", columnDefinition = "jsonb")
    private List<String> steps;

    @Column(name = "k_colors")
    private Integer kColors;

    @Column(name = "contrast")
    private Float contrast;

    @Column(name = "iterations")
    private Integer iterations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sharpen_kernel", columnDefinition = "jsonb")
    private List<List<Float>> sharpenKernel;

}
