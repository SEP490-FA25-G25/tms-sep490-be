package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2-100 characters")
    private String fullName;

    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Phone must be 10-11 digits starting with 0")
    private String phone;

    private String facebookUrl;

    private String address;

    private String avatarUrl;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @Valid
    private List<SkillAssessmentInput> skillAssessments;

    // Note: Password is always auto-generated as "12345678" by the system
    // Students can change it after first login
}
