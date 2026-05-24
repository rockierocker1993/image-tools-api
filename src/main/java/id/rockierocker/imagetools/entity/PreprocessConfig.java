package id.rockierocker.imagetools.entity;

import id.rockierocker.imagetools.entity.converter.JsonListStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

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

    @Convert(converter = JsonListStringConverter.class)
    @Column(name = "steps", columnDefinition = "jsonb")
    private List<String> steps;

    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

}
