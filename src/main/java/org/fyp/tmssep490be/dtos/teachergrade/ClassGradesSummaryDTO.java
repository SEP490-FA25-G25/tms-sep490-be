package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ClassGradesSummaryDTO {
    private Long classId;
    private String className;
    private String classCode;
    private Integer totalAssessments;
    private Integer totalStudents;
    private Double averageScore;
    private Double highestScore;
    private Double lowestScore;
    private Map<String, Integer> scoreDistribution;
    private List<StudentGradeSummaryDTO> topStudents;
    private List<StudentGradeSummaryDTO> bottomStudents;
}

