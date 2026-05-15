package id.rockierocker.imagetools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "image")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @Column(name = "image_id", length = 100, unique = true)
    private String imageId;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "image_key", length = 255)
    private String imageKey;

    @Column(name = "image_provider", length = 20)
    private String imageProvider;

    @Column(name = "webp_image_name", length = 255)
    private String webpImageName;

    @Column(name = "webp_image_key", length = 255)
    private String webpImageKey;

    @Column(name = "webp_provider", length = 20)
    private String webpProvider;

    @Column(name = "format", length = 10)
    private String format;

    @Column(name = "width", length = 5)
    private Integer width;

    @Column(name = "height", length = 5)
    private Integer height;

    @Column(name = "category", length = 20)
    private String category;

}
