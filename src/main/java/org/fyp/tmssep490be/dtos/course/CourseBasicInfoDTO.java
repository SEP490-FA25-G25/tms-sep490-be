package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseBasicInfoDTO {
    private Long subjectId;
    private Long levelId;
    private String name;
    private String code;
    private String description;
    private String prerequisites;
    private Integer durationHours;
    private Integer durationWeeks;
    private String scoreScale;
    private String targetAudience;
    private String teachingMethods;
    private java.time.LocalDate effectiveDate;
    private Integer sessionPerWeek;
    private java.math.BigDecimal hoursPerSession;
}
