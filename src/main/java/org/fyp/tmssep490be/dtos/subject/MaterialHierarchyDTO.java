package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MaterialHierarchyDTO {
    private List<SubjectMaterialDTO> subjectLevel;
    private List<PhaseMaterialDTO> phases;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
}