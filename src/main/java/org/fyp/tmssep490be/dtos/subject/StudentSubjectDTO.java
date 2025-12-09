package org.fyp.tmssep490be.dtos.subject;

import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StudentSubjectDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String curriculumName;
    private String levelName;
    private String logicalSubjectCode;
    private Integer totalHours;
    private String targetAudience;
    private String teachingMethods;
    private LocalDate effectiveDate;
    private String status;
    private String approvalStatus;
    private Long classId;
    private String classCode;
    private String centerName;
    private String roomName;
    private String modality;
    private LocalDate classStartDate;
    private LocalDate classEndDate;
    private String teacherName;
    private String enrollmentStatus;
    private LocalDate enrolledAt;
    private Double progressPercentage;
    private Integer completedSessions;
    private Integer totalSessions;
    private String attendanceRate;
}