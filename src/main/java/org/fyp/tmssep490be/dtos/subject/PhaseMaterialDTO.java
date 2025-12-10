package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PhaseMaterialDTO {
    private Long id;
    private Integer phaseNumber;
    private String name;
    private List<SubjectMaterialDTO> materials;
    private List<SessionMaterialDTO> sessions;
    private Integer totalMaterials;
}