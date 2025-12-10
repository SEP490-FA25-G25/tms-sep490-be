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
public class SubjectSessionDTO {
    private Long id;
    private String topic;

    // HEAD fields
    private String studentTask;
    private List<String> mappedCLOs; // List of CLO codes

    // Main fields
    private Integer sequenceNo;
    private String description;
    private String objectives;
    private List<String> skills;
    private List<SubjectMaterialDTO> materials;
    private Integer totalMaterials;
    private Boolean isCompleted;
}
