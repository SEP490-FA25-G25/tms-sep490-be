package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileDTO {
    // Personal Information
    private Long studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String gender;
    private LocalDate dateOfBirth;
    private String status;
    private OffsetDateTime lastLoginAt;
    private String branchName;
    private Long branchId;

    // Academic Statistics
    private Long totalEnrollments;
    private Long activeEnrollments;
    private Long completedEnrollments;
    private LocalDate firstEnrollmentDate;
    private BigDecimal attendanceRate;
    private BigDecimal averageScore;
    private Long totalSessions;
    private Long totalAbsences;

    // All Enrollments (clickable for navigation) - includes ENROLLED, COMPLETED, DROPPED, WITHDRAWN
    private List<StudentActiveClassDTO> enrollments;
}