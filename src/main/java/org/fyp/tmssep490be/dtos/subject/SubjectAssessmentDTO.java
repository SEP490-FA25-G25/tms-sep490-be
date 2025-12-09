package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectAssessmentDTO {
    private Long id;
    private String name;
    private String type; // AssessmentKind enum value
    private BigDecimal weight;
    private BigDecimal maxScore;
    private Integer durationMinutes;
    private String description;
    private String note;
    private List<String> skills;
    private List<String> mappedCLOs;
}