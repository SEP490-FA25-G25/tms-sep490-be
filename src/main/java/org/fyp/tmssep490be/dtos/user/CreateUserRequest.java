package org.fyp.tmssep490be.dtos.user;

import lombok.*;
import java.time.LocalDate;
import java.util.Set;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import jakarta.validation.constraints.*;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    // Phone - chỉ cho phép số, 10-11 ký tự
    @Pattern(regexp = "^0[0-9]{9}$", message = "Phone must be 10 digits starting with 0")
    private String phone;

    private String facebookUrl;

    // Date of birth - phải là ngày trong quá khứ
    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotNull(message = "Gender is required")
    private Gender gender = Gender.MALE;

    private String address;

    private String avatarUrl;

    @NotNull(message = "Status is required")
    private UserStatus status = UserStatus.ACTIVE;

    @NotNull(message = "At least one role is required")
    @Size(min = 1, message = "At least one role is required")
    private Set<Long> roleIds;

    private Set<Long> branchIds;
}