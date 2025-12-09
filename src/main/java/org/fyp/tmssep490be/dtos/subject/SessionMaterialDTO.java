package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionMaterialDTO {
    private Long id;
    private Integer sequenceNo;
    private String topic;
    private List<SubjectMaterialDTO> materials;
    private List<String> skills;
    private Integer totalMaterials;
}