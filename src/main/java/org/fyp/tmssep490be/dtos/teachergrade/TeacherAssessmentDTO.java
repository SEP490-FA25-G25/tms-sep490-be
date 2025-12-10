package org.fyp.tmssep490be.dtos.teachergrade;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class TeacherAssessmentDTO {
    private Long id;
    private Long classId;
    private Long subjectAssessmentId;
    private String name;
    private String description;
    private String kind;
    private Integer durationMinutes;
    private Double maxScore;
    private OffsetDateTime scheduledDate;
    private OffsetDateTime actualDate;
    private Integer gradedCount;
    private Integer totalStudents;
    private boolean allGraded;
}

