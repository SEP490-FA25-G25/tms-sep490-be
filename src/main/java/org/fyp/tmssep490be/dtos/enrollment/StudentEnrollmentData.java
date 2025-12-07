package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentData {
    private String fullName;
    private String email;
    private String phone;
    private String facebookUrl;
    private String address;
    private Gender gender;
    private LocalDate dob;

    private StudentResolutionStatus status;  // FOUND/CREATE/DUPLICATE/ERROR
    private Long resolvedStudentId;  // Nếu FOUND
    private String errorMessage;  // Nếu ERROR



}
