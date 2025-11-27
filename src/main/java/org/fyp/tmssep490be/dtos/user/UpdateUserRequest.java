package org.fyp.tmssep490be.dtos.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * Update user account request DTO (admin only)
 * Note: Email and password cannot be changed via this endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    private String fullName;

    private String phone;

    private String facebookUrl;

    private LocalDate dob;

    private Gender gender;

    private String address;

    private UserStatus status;

    private Set<Long> roleIds;

    private Set<Long> branchIds;
}

