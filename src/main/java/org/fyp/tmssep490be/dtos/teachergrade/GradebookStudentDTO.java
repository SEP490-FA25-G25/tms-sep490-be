package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GradebookStudentDTO {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private List<GradebookStudentScoreDTO> scores;
    private Double averageScore;
    private Integer gradedCount;
    private Integer totalAssessments;
    private Integer attendedSessions;
    private Integer totalSessions;
    private Double attendanceRate;
    private Double attendanceScore;
    private boolean attendanceFinalized;
}

