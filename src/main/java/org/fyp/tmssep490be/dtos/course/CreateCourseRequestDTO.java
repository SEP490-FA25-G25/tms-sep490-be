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
public class CreateCourseRequestDTO {
    private CourseBasicInfoDTO basicInfo;
    private List<CourseCLODTO> clos;
    private CourseStructureDTO structure;
    private List<CourseAssessmentDTO> assessments;
    private List<CourseMaterialDTO> materials;
    private String status;
}
