package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectBasicInfoDTO {
    private Long curriculumId;
    private Long levelId;
    private String name;
    private String code;
    private String description;
    private String prerequisites;
    private Integer durationHours;
    private String scoreScale;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private Integer numberOfSessions;
    private BigDecimal hoursPerSession;
    private String thumbnailUrl;
}