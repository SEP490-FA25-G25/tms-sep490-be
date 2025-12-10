package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class GradebookStudentScoreDTO {
    private Long assessmentId;
    private Double score;
    private Double scorePercentage;
    private Double maxScore;
    private String feedback;
    private String gradedBy;
    private OffsetDateTime gradedAt;
}

