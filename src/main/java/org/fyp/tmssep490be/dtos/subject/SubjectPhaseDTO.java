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
public class SubjectPhaseDTO {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private Integer phaseNumber;
    private String name;
    private Integer durationWeeks;
    private String learningFocus;

    // Legacy fields for compatibility
    private String description;
    private Integer sequenceNo;
    private List<SubjectSessionDTO> sessions;
    private List<SubjectMaterialDTO> materials;
    private Integer totalSessions;
    private Integer totalMaterials;
}
