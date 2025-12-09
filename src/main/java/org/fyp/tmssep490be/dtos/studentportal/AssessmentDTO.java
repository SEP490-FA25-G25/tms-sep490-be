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
public class AssessmentDTO {
    private Long id;
    private String name;
    private String kind;
    private Integer durationMinutes;
    private String description;
    private OffsetDateTime scheduledDate;
    private BigDecimal maxScore;
}
