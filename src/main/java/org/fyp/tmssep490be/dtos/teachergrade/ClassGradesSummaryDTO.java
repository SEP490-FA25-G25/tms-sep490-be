package org.fyp.tmssep490be.dtos.teachergrade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO for class grades summary statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Class grades summary statistics")
public class ClassGradesSummaryDTO {
    
    @Schema(description = "Class ID", example = "100")
    private Long classId;
    
    @Schema(description = "Class name", example = "IELTS 5.0 - Lớp 3")
    private String className;
    
    @Schema(description = "Total number of assessments", example = "5")
    private Integer totalAssessments;
    
    @Schema(description = "Total number of enrolled students", example = "20")
    private Integer totalStudents;
    
    @Schema(description = "Average score across all assessments", example = "82.5")
    private BigDecimal averageScore;
    
    @Schema(description = "Highest score in class", example = "98.0")
    private BigDecimal highestScore;
    
    @Schema(description = "Lowest score in class", example = "65.0")
    private BigDecimal lowestScore;
    
    @Schema(description = "Score distribution (count by score ranges)", example = "{\"90-100\": 5, \"80-89\": 8, \"70-79\": 5, \"60-69\": 2}")
    private Map<String, Integer> scoreDistribution;
    
    @Schema(description = "Top 5 students by average score")
    private List<StudentGradeSummaryDTO> topStudents;
    
    @Schema(description = "Bottom 5 students by average score (needing attention)")
    private List<StudentGradeSummaryDTO> bottomStudents;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Student grade summary")
    public static class StudentGradeSummaryDTO {
        @Schema(description = "Student ID", example = "100")
        private Long studentId;
        
        @Schema(description = "Student code", example = "STU001")
        private String studentCode;
        
        @Schema(description = "Student name", example = "Nguyễn Văn A")
        private String studentName;
        
        @Schema(description = "Average score", example = "85.5")
        private BigDecimal averageScore;
        
        @Schema(description = "Number of assessments graded", example = "5")
        private Integer gradedCount;
    }
}

