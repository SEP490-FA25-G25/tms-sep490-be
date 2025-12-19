package org.fyp.tmssep490be.dtos.publicapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for displaying course information on landing page
 * Contains only publicly visible information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicSubjectDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String thumbnailUrl;

    // Level info
    private String levelName;
    private String levelDescription;

    // Curriculum info
    private String curriculumCode;
    private String curriculumName;

    // Course structure
    private Integer totalHours;
    private Integer numberOfSessions;
    private BigDecimal hoursPerSession;

    // Phases for syllabus display
    private List<PublicSubjectPhaseDTO> phases;

    // CLOs for learning outcomes display
    private List<PublicSubjectCLODTO> clos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicSubjectPhaseDTO {
        private Long id;
        private Integer phaseNumber;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicSubjectCLODTO {
        private String code;
        private String description;
    }
}
