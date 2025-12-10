package org.fyp.tmssep490be.dtos.teachergrade;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchScoreInputDTO {
    @NotEmpty
    private List<ScoreInputDTO> scores;
}

