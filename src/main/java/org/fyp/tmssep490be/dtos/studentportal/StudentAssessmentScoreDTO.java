package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for student assessment scores in student portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAssessmentScoreDTO {
    private Long assessmentId;
    private Long studentId;
    private BigDecimal score;
    private String feedback;
    private String gradedBy;
    private OffsetDateTime gradedAt;
    private OffsetDateTime createdAt;
    private BigDecimal maxScore;
    private Boolean isSubmitted;
    private Boolean isGraded;
    private BigDecimal scorePercentage;
}