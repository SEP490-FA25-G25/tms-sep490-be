package org.fyp.tmssep490be.dtos.user;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * DTO cho thông tin profile của user hiện tại
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileDTO {
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
