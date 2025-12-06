package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssessmentInput {

    @NotNull(message = "Skill is required")
    private Skill skill;

    @NotNull(message = "Level ID is required")
    private Long levelId;

    private String score; // Ielts 35/40, Toeic 500/650

    private String note;

    private Long assessedByUserId;
}
