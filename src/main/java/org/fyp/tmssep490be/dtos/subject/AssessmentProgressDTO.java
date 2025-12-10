package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AssessmentProgressDTO {
    private Long assessmentId;
    private String name;
    private String assessmentType;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private BigDecimal achievedScore;
    private Boolean isCompleted;
    private String completedAt;
    private Double percentageScore;
}