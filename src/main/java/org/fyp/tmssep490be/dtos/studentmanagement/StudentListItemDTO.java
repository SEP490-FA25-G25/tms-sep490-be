package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentListItemDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String gender;        // MALE, FEMALE, OTHER
    private String status;        // ACTIVE, INACTIVE, SUSPENDED
    private String branchName;
    private Long branchId;
    private Long activeEnrollments;
    private LocalDate lastEnrollmentDate;
    private Boolean canEnroll;
}
