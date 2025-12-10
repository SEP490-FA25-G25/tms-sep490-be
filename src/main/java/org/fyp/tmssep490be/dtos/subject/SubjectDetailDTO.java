package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDetailDTO {
    // Common fields
    private Long id;
    private String name;
    private String code;
    private String description;
    private String thumbnailUrl;

    // HEAD fields (Admin/Creator view)
    private SubjectBasicInfoDTO basicInfo;
    private SubjectStructureDTO structure;

    // Main fields (Student/Viewer view)
    private Long subjectId;
    private String subjectName 
    private Long levelId; 
    private String levelName;
    private String logicalSubjectCode;
    private Integer version; 
    private Integer totalHours;
    private Integer numberOfSessions; // Total number of sessions from subject
    private Integer totalDurationWeeks; // Computed from sum of phase durations

    private BigDecimal hoursPerSession;
    private String scoreScale;
    private String prerequisites;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private String status;
    private String approvalStatus;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private OffsetDateTime createdAt;
    private Integer totalSessions; // Actual count from phases
    private Integer totalMaterials;

    // Lists (Merged)
    private List<SubjectPhaseDTO> phases;
    private List<SubjectMaterialDTO> materials;
    private List<SubjectCLODTO> clos;
    private List<SubjectAssessmentDTO> assessments;
}
