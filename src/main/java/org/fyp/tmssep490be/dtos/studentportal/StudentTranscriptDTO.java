package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for student transcript information
 * Contains comprehensive academic record including scores and progress for each class
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentTranscriptDTO {
    private Long classId;
    private String classCode;
    private String className;  // e.g., "IELTS 5.0 - Lớp 3"
    private String courseName;  // e.g., "IELTS 5.0"
    private String teacherName;  // Primary teacher
    private String status;  // ONGOING, COMPLETED, DROPPED
    private BigDecimal averageScore;  // Calculated average of all component scores
    private Map<String, BigDecimal> componentScores;  // e.g., {"Lab 1": 8.5, "Midterm": 9.0, "Final": 8.0}
    private LocalDate completedDate;  // If status is COMPLETED
    private Integer totalSessions;
    private Integer completedSessions;
}