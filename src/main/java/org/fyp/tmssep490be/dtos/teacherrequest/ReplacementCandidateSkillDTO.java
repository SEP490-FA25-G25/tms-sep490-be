package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementCandidateSkillDTO {
    private Long id;
    private String name;
    private String skillName;
    private String level;
    private String skillLevel;
    private String proficiency;
    private String description;
}
