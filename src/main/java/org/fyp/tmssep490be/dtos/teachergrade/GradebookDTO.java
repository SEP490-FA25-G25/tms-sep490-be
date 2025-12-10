package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GradebookDTO {
    private Long classId;
    private String className;
    private String classCode;
    private List<GradebookAssessmentDTO> assessments;
    private List<GradebookStudentDTO> students;
}

