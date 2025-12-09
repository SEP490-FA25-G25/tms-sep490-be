package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectPhaseDTO {
    private Long id;
    private Integer phaseNumber;
    private String name;
    private String description;
    private Integer durationWeeks;
    private List<SubjectSessionDTO> sessions;
    private List<SubjectMaterialDTO> materials;
}