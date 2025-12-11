package org.fyp.tmssep490be.dtos.availability;

import lombok.*;

// DTO cho skill của giáo viên kèm thông tin level
// Dùng để hiển thị mức độ thành thạo skill trong UI chọn giáo viên
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSkillDetailDTO {
    
    // Tên skill (ví dụ: LISTENING, READING, SPEAKING, WRITING, GENERAL)
    private String skill;
    
    // Specialization (ví dụ: IELTS, TOEIC, TOEFL)
    private String specialization;
    
    // Mức độ thành thạo (thang điểm 1-10)
    private Integer level;
}

