package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDetailDTO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String thumbnailUrl;
    private Long subjectId; // Curriculum ID
    private String subjectName; // Curriculum Name
    private Long levelId;
    private String levelName;

    // Basic Info DTO wrapper
    private SubjectBasicInfoDTO basicInfo;

    // Structure DTO wrapper
    private SubjectStructureDTO structure;

    private String status;
    private String approvalStatus;
    private String rejectionReason;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private Integer totalHours;
    private Integer numberOfSessions;
    private BigDecimal hoursPerSession;
    private String scoreScale;
    private String prerequisites;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;

    private List<SubjectCLODTO> clos;
    private List<SubjectPhaseDTO> phases;
    private List<SubjectAssessmentDTO> assessments;
    private List<SubjectMaterialDTO> materials;
}
