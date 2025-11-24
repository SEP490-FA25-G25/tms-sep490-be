package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for assessment information in student portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentDTO {
    private Long id;
    private Long classId;
    private Long courseAssessmentId;
    private String name;
    private String description;
    private String kind;
    private BigDecimal maxScore;
    private Integer durationMinutes;
    private OffsetDateTime scheduledDate;
    private OffsetDateTime actualDate;
    private String teacherName;
}