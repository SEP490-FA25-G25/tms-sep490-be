package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

// DTO chi tiết đăng ký (cho AA xem)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDetailDTO {
    private Long registrationId;
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;
    private String employeeCode;
    private String contractType;
    private String note;
    private OffsetDateTime registeredAt;
    private String status;
    
    // Thông tin bổ sung để AA quyết định
    private int currentClassCount; // Số lớp đang dạy
    private List<TeacherSkillDTO> skills;
}
