package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class TeacherStudentScoreDTO {
    private Long scoreId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Double score;
    private String feedback;
    private String gradedBy;
    private OffsetDateTime gradedAt;
    private Double maxScore;
    private Double scorePercentage;
    private boolean isGraded;
}

