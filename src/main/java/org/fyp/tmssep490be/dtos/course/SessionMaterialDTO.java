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
public class SessionMaterialDTO {
    private Long id;
    private Integer sequenceNo;
    private String topic;
    private List<CourseMaterialDTO> materials;
    private Integer totalMaterials;
}