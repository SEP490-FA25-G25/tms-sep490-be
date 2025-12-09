package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectProgressDTO {
    private Long subjectId;
    private Long studentId;
    private Integer totalSessions;
    private Integer completedSessions;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
    private Double progressPercentage;
    private Double attendanceRate;
    private List<CLOProgressDTO> cloProgress;
    private List<AssessmentProgressDTO> assessmentProgress;
    private String currentPhase;
    private String nextSession;
    private Long estimatedCompletionDate;
}