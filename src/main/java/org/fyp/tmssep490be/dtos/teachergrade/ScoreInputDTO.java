package org.fyp.tmssep490be.dtos.teachergrade;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScoreInputDTO {
    @NotNull
    private Long studentId;
    private Double score;
    private String feedback;
}

