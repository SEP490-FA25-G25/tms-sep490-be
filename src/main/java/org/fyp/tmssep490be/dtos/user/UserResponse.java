package org.fyp.tmssep490be.dtos.user;
import lombok.*;
import java.time.LocalDate;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String facebookUrl;
    private LocalDate dob;
    private Gender gender;
    private String address;
    private String avatarUrl;
    private UserStatus status;
    private Set<String> roles;
    private Set<String> branches;
}

