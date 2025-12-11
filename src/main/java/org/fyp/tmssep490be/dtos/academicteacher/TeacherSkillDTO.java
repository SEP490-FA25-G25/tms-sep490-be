package org.fyp.tmssep490be.dtos.academicteacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSkillDTO {
    private Skill skill;
    private String specialization;
    private String language;
    private Short level;
}

