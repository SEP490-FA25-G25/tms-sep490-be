package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SubjectStructureDTO {
    private List<SubjectPhaseDTO> phases;
}