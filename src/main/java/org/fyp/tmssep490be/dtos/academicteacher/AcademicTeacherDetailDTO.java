package org.fyp.tmssep490be.dtos.academicteacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// DTO chi tiết giáo viên với đầy đủ thông tin skills
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTeacherDetailDTO {
    private Long teacherId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeCode;
    private String avatarUrl;
    private String status;
    private String address;
    private String facebookUrl;
    private LocalDate dob;
    private String gender;
    private LocalDate hireDate;
    private String contractType;
    private String note;
    
    // Thông tin branch
    private Long branchId;
    private String branchName;
    
    // Chi tiết skills
    private List<TeacherSkillDTO> skills;
}

