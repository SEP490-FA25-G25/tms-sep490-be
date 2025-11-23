package org.fyp.tmssep490be.dtos.teacher;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileDTO {
    // Personal Information
    private Long teacherId;
    private String teacherCode;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String gender;
    private LocalDate dateOfBirth;
    private String facebookUrl;
    private String status;
    private OffsetDateTime lastLoginAt;
    private String branchName;
    private Long branchId;

    // Teaching Statistics
    private Long totalClasses;
    private Long activeClasses;
    private Long completedClasses;
    private LocalDate firstClassDate;

    // All Classes (clickable for navigation)
    private List<TeacherClassInfoDTO> classes;
}

