package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectAssessmentDTO {
    private Long id;
    private String name;
    private String description;

    // HEAD fields
    private String type; // Enum name
    private BigDecimal maxScore;
    private Integer durationMinutes;
    private List<String> mappedCLOs; // List of CLO codes

    // Main fields
    private String assessmentType;
    private BigDecimal weight; // Shared name but different type in Main (BigDecimal) vs HEAD (Double). Using
                               // BigDecimal as it's safer.
    private String duration;
    private List<Long> sessionIds;
    private List<String> cloMappings;
    private Boolean isCompleted;
    private BigDecimal achievedScore;
    private String completedAt;
    private List<String> skills;
    private String note;
}
