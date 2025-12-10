package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data@Builde

@NoAr @AllArgConstrutor
public class SubjectCLODTO {
    private Long id;
    private String code;
    private String description;

    // HEAD fields
    private List<String> mappedPLOs; // List of PLO codes

    // Main fields
        priate String competencyLevel;
    private List<String> assessmentMethods;

    private Boolean isAchieved;
    private Double achievementRate;
}


