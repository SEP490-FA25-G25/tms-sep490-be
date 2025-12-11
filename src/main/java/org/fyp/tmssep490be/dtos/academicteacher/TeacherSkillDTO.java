package org.fyp.tmssep490be.dtos.academicteacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

// DTO cho skill đơn lẻ của giáo viên
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSkillDTO {
    private Skill skill;
    private String specialization; // Ví dụ: IELTS, TOEIC, TOEFL
    private String language; // Ví dụ: English, Vietnamese
    private Short level; // Mức độ từ 1-10
}

