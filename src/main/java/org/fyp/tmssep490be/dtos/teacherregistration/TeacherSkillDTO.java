package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO thông tin kỹ năng giáo viên
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSkillDTO {
    private String skill;
    private String specialization;
    private String language;
    private Double level;
}
