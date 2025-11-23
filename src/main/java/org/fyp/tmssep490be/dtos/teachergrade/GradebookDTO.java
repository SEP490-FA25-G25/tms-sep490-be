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
 * DTO for gradebook (matrix view of all students and assessments)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Gradebook matrix for a class")
public class GradebookDTO {
    
    @Schema(description = "Class ID")
    private Long classId;
    
    @Schema(description = "Class name")
    private String className;
    
    @Schema(description = "Class code")
    private String classCode;
    
    @Schema(description = "List of assessments in the class")
    private List<GradebookAssessmentDTO> assessments;
    
    @Schema(description = "List of students with their scores")
    private List<GradebookStudentDTO> students;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Assessment information in gradebook")
    public static class GradebookAssessmentDTO {
        @Schema(description = "Assessment ID")
        private Long assessmentId;
        
        @Schema(description = "Assessment name")
        private String assessmentName;
        
        @Schema(description = "Assessment kind (QUIZ, MIDTERM, FINAL, etc.)")
        private String kind;
        
        @Schema(description = "Maximum score")
        private BigDecimal maxScore;
        
        @Schema(description = "Scheduled date")
        private String scheduledDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Student with scores in gradebook")
    public static class GradebookStudentDTO {
        @Schema(description = "Student ID")
        private Long studentId;
        
        @Schema(description = "Student code")
        private String studentCode;
        
        @Schema(description = "Student name")
        private String studentName;
        
        @Schema(description = "Map of assessment ID to score (null if not graded)")
        private Map<Long, GradebookScoreDTO> scores;
        
        @Schema(description = "Average score across all assessments")
        private BigDecimal averageScore;
        
        @Schema(description = "Number of graded assessments")
        private Integer gradedCount;
        
        @Schema(description = "Total number of assessments")
        private Integer totalAssessments;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Score information in gradebook")
    public static class GradebookScoreDTO {
        @Schema(description = "Score value")
        private BigDecimal score;
        
        @Schema(description = "Maximum score")
        private BigDecimal maxScore;
        
        @Schema(description = "Feedback/comment")
        private String feedback;
        
        @Schema(description = "Whether this score has been graded")
        private Boolean isGraded;
        
        @Schema(description = "Graded by (teacher name)")
        private String gradedBy;
        
        @Schema(description = "Graded at (timestamp)")
        private String gradedAt;
    }
}

