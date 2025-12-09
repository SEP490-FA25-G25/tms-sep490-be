package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementCandidateDTO {
    private Long teacherId;
    private String fullName;
    private String displayName;
    private String email;
    private String phone;
    private String level;
    private Double matchScore;
    private String availability;
    private String availabilityStatus;
    private String specialization;
    private String note;
    private List<String> tags;
    private String skillSummary;
    private String skillsDescription;
    private List<ReplacementCandidateSkillDTO> skills;
}
