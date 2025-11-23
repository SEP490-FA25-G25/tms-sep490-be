package org.fyp.tmssep490be.dtos.teachergrade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for inputting or updating a student score
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Input DTO for student score")
public class ScoreInputDTO {
    
    @NotNull(message = "Student ID is required")
    @Schema(description = "Student ID", example = "100", required = true)
    private Long studentId;
    
    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be greater than or equal to 0")
    @Schema(description = "Score value", example = "85.50", required = true)
    private BigDecimal score;
    
    @Schema(description = "Feedback from teacher", example = "Good work, but needs improvement in grammar")
    private String feedback;
}

