package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentGradeSummaryDTO {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Double averageScore;
    private Integer gradedCount;
}

