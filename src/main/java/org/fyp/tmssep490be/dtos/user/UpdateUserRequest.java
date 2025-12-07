package org.fyp.tmssep490be.dtos.user;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UpdateUserRequest {
    private String fullName;
    private String phone;
    private String facebookUrl;
    private LocalDate dob;
    private Gender gender;
    private String address;
    private String avatarUrl;
    private UserStatus status;
    private Set<Long> roleIds;
    private Set<Long> branchIds;
}
