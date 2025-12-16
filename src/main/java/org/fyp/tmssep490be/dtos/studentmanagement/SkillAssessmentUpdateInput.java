package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssessmentUpdateInput {

 
    private Long id;

    @NotNull(message = "Skill is required")
    private Skill skill;

    @NotNull(message = "Level ID is required")
    private Long levelId;

    private String score;

    private LocalDate assessmentDate;

    private String assessmentType;

    private String note;

    private Long assessedByUserId;
}
