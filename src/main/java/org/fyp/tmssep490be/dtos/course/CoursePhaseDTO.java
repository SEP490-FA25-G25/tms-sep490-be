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
public class CoursePhaseDTO {
    private Long id;
    private String name;
    private Integer phaseNumber;
    private String description;
    private Integer sequenceNo;

    private List<CourseSessionDTO> sessions;

    // Main fields
    private List<CourseMaterialDTO> materials;
    private Integer totalSessions;
    private Integer totalMaterials;
}
