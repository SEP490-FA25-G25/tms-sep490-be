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
public class CourseAssessmentDTO {
    private String name;
    private String type;
    private Double weight;
    private Integer durationMinutes;
    private List<String> mappedCLOs; // List of CLO codes
}
