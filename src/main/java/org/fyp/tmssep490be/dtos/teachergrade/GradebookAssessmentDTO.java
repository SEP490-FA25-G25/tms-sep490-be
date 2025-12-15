package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class GradebookAssessmentDTO {
    private Long id;
    private String name;
    private String kind;
    private Double maxScore;
    private Integer durationMinutes;
    private OffsetDateTime scheduledDate;
}

