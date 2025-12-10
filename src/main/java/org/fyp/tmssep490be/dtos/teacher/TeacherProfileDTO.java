package org.fyp.tmssep490be.dtos.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// DTO cho thông tin profile của giáo viên
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherProfileDTO {
    // Thông tin cá nhân
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

    // Thống kê giảng dạy
    private Long totalClasses;
    private Long activeClasses;
    private Long completedClasses;
    private LocalDate firstClassDate;

    // Tất cả các lớp học (có thể click để điều hướng)
    private List<TeacherClassInfoDTO> classes;
}

