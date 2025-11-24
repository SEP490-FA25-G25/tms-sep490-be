package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCLODTO {
    private Long id;
    private String code;
    private String description;

    // HEAD fields
    private List<String> mappedPLOs; // List of PLO codes

    // Main fields
    private String competencyLevel;
    private List<String> assessmentMethods;
    private List<CoursePLODTO> relatedPLOs;
    private Boolean isAchieved;
    private Double achievementRate;
}
