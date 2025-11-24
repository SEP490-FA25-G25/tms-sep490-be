package org.fyp.tmssep490be.dtos.teachergrade;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for student score information in teacher grade management
 * This DTO is specifically designed for teacher views, containing student identifying information
 * such as studentCode and studentName for displaying in lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Student score information for teacher grade management")
public class TeacherStudentScoreDTO {
    
    @Schema(description = "Score ID", example = "1")
    private Long scoreId;
    
    @Schema(description = "Student ID", example = "100")
    private Long studentId;
    
    @Schema(description = "Student code", example = "STU001")
    private String studentCode;
    
    @Schema(description = "Student full name", example = "Nguyễn Văn A")
    private String studentName;
    
    @Schema(description = "Score value", example = "85.50")
    private BigDecimal score;
    
    @Schema(description = "Feedback from teacher", example = "Good work, but needs improvement in grammar")
    private String feedback;
    
    @Schema(description = "Teacher who graded this score", example = "Trần Thị B")
    private String gradedBy;
    
    @Schema(description = "Date and time when score was graded")
    private OffsetDateTime gradedAt;
    
    @Schema(description = "Maximum possible score", example = "100.00")
    private BigDecimal maxScore;
    
    @Schema(description = "Score percentage", example = "85.5")
    private BigDecimal scorePercentage;
    
    @Schema(description = "Whether this score has been graded", example = "true")
    private Boolean isGraded;
}

