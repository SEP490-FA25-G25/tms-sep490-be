package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for student class listing in "My Classes" feature
 * Contains essential information for card-based display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseName;
    private String courseCode;
    private Long branchId;
    private String branchName;
    private String modality;
    private String status;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private Long enrollmentId;
    private OffsetDateTime enrollmentDate;
    private String enrollmentStatus;
    private Integer totalSessions;
    private Integer completedSessions;
    private List<String> instructorNames;
    private String scheduleSummary;
}