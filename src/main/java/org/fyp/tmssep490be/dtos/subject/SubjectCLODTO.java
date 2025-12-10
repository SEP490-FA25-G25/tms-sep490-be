package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectCLODTO {
    private Long id;
    private String code;
    private String description;

    // HEAD fields
    private List<String> mappedPLOs;

    // Main fields
    private String competencyLevel;
    private List<String> assessmentMethods;
    private List<SubjectPLODTO> relatedPLOs;
    private Boolean isAchieved;
    private Double achievementRate;
}
