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
public class MaterialHierarchyDTO {
    private List<CourseMaterialDTO> courseLevel;
    private List<PhaseMaterialDTO> phases;
    private Integer totalMaterials;
    private Integer accessibleMaterials;
}