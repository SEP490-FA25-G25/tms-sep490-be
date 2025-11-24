package org.fyp.tmssep490be.dtos.teachergrade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for assessment information in teacher grade management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Assessment information for teacher grade management")
public class TeacherAssessmentDTO {
    
    @Schema(description = "Assessment ID", example = "1")
    private Long id;
    
    @Schema(description = "Class ID", example = "100")
    private Long classId;
    
    @Schema(description = "Course Assessment ID", example = "50")
    private Long courseAssessmentId;
    
    @Schema(description = "Assessment name", example = "Midterm Exam")
    private String name;
    
    @Schema(description = "Assessment description", example = "Midterm exam covering chapters 1-5")
    private String description;
    
    @Schema(description = "Assessment kind", example = "MIDTERM")
    private String kind;
    
    @Schema(description = "Maximum score", example = "100.00")
    private BigDecimal maxScore;
    
    @Schema(description = "Duration in minutes", example = "90")
    private Integer durationMinutes;
    
    @Schema(description = "Scheduled date and time")
    private OffsetDateTime scheduledDate;
    
    @Schema(description = "Actual date and time when assessment was conducted")
    private OffsetDateTime actualDate;
    
    @Schema(description = "Number of students who have been graded", example = "15")
    private Integer gradedCount;
    
    @Schema(description = "Total number of enrolled students", example = "20")
    private Integer totalStudents;
    
    @Schema(description = "Whether all students have been graded", example = "false")
    private Boolean allGraded;
}

