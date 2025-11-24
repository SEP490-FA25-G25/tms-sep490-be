package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressDTO {
    private Long courseId;
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