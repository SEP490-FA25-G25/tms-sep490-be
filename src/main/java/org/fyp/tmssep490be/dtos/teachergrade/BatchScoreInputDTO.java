package org.fyp.tmssep490be.dtos.teachergrade;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for batch inputting or updating student scores
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch input DTO for student scores")
public class BatchScoreInputDTO {
    
    @NotEmpty(message = "At least one score is required")
    @Valid
    @Schema(description = "List of scores to input or update", required = true)
    private List<ScoreInputDTO> scores;
}

