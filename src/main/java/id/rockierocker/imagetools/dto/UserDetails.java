package id.rockierocker.imagetools.dto;

import id.rockierocker.imagetools.constant.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UserDetails {
    private String userId;
    private String email;
    private String fullName;
    private UserType userType;
}
