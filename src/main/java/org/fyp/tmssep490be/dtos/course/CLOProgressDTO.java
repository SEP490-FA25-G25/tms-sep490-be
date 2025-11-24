package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CLOProgressDTO {
    private Long cloId;
    private String cloCode;
    private String description;
    private Double achievementRate;
    private Boolean isAchieved;
    private Integer totalAssessments;
    private Integer completedAssessments;
    private Double averageScore;
}