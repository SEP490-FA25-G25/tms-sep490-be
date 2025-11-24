package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String subjectName;
    private String levelName;
    private String logicalCourseCode;
    private Integer totalHours;
    private Integer durationWeeks;
    private Integer sessionPerWeek;
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