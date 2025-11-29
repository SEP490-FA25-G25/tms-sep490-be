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
public class CourseSessionDTO {
    private Long id;
    private String topic;

    // HEAD fields
    private String studentTask;
    private List<String> mappedCLOs; // List of CLO codes

    // Main fields
    private Integer sequenceNo;
    private String description;
    private String objectives;
    private String skill;
    private List<CourseMaterialDTO> materials;
    private Integer totalMaterials;
    private Boolean isCompleted;
}
