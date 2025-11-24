package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for replacement candidate teachers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplacementCandidateDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private Integer skillPriority; // Higher = better match
    private Integer availabilityPriority; // Higher = more available
    private Boolean hasConflict; // true if teacher has conflict at session time
    private java.util.List<SkillDetail> skills; // list of skills with levels

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillDetail {
        private String skill; // Enum name, e.g. "SPEAKING"
        private Short level;  // Nullable level (0-? depending on data)
    }
}


