package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectDetailDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String thumbnailUrl;
    private Long curriculumId;
    private String curriculumName;
    private Long levelId;
    private String levelName;
    private String logicalSubjectCode;
    private Integer version;
    private SubjectBasicInfoDTO basicInfo;
    private String status;
    private String approvalStatus;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private OffsetDateTime createdAt;
    private Integer totalHours;
    private Integer numberOfSessions;
    private BigDecimal hoursPerSession;
    private String scoreScale;
    private String prerequisites;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private List<SubjectCLODTO> clos;
    private SubjectStructureDTO structure;
    private List<SubjectPhaseDTO> phases;
    private List<SubjectAssessmentDTO> assessments;
    private List<SubjectMaterialDTO> materials;
}