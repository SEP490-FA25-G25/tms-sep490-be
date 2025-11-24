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
public class PhaseMaterialDTO {
    private Long id;
    private Integer phaseNumber;
    private String name;
    private List<CourseMaterialDTO> materials;
    private List<SessionMaterialDTO> sessions;
    private Integer totalMaterials;
}