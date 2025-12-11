package org.fyp.tmssep490be.dtos.academicteacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO cho danh sách giáo viên trong quản lý Academic Teacher
// Hiển thị thông tin cơ bản của giáo viên kèm tóm tắt về skills
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicTeacherListItemDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeCode;
    private String avatarUrl;
    private String status;
    
    // Tóm tắt về skills
    private Boolean hasSkills; // Có skills hay không
    private Integer totalSkills; // Tổng số skills
    private List<String> specializations; // Danh sách các specialization (ví dụ: ["IELTS", "TOEIC"])
}

