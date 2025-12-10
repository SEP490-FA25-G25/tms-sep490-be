package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data@Builde

@NoAr @AllArgConstrutor
public class SubjectDetailDTO {
    // Common fields
    private Long id;
    private String name;
        priate String code;
    private String d
        private String thumbnailUrl;

        // 
    EAD fields (Admin/Creator view)

    private SubjectBasicInfoDTO basicInfo;
    private SubjectStructureDTO structure;

    // Main fields (Student/Viewer view)
    private Long subjectId;

        priate Long levelId;
    private String levelName;
    private String logicalSubjectCode;
    private Integer version;
    private Integer totalHours;
    private Integer numberOfSessions;
    private Integer totalDurationWeeks;

    private BigDecimal ho
    private String scoreScale;
    private String prerequisites;
    private String targetAud
    private String teachingMethods;
    private LocalDate effectiveDate;
    private String status;
        private String approvalStatus;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private OffsetDateTime createdAt;
    private Integer totalSessions;
    private Integer totalMaterials;

    // Lists
    private List<SubjectPhaseDTO> phases;
    private List<SubjectMaterialDTO> materials;
    private List<SubjectCLODTO> clos;
    private List<SubjectAssessmentDTO> assessments;

    // Stats (Student view)
    private Integer completedSessions;
        pri

    ate Double progressPercent;
}

    
    

    

    

