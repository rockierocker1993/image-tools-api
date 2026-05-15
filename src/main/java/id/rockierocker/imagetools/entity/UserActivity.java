package id.rockierocker.imagetools.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "user_activity")
@SQLRestriction("deleted is null")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "userId", length = 100)
    private String userId;

    @Column(name = "requestId", length = 255)
    private String requestId;

    @Column(name = "module", length = 255)
    private String module;

    @Column(name = "activity", length = 255)
    private String activity;

    @Column(name = "user_type", length = 20)
    private String userType;

}
