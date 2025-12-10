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
public class CreateSubjectRequestDTO {
    private SubjectBasicInfoDTO basicInfo;
    private List<SubjectCLODTO> clos;
    private SubjectStructureDTO structure;
    private List<SubjectAssessmentDTO> assessments;
    private List<SubjectMaterialDTO> materials;
    private String status;
}
