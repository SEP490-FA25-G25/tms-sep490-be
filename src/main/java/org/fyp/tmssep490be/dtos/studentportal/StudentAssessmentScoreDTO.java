package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssessmentScoreDTO {
    private Long assessmentId;
    private Long studentId;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String feedback;
    private String gradedBy;
    private OffsetDateTime gradedAt;
    private boolean isSubmitted;
    private boolean isGraded;
    private BigDecimal scorePercentage;
    private OffsetDateTime createdAt;
}
