package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePLODTO {
    @NotBlank(message = "PLO code is required")
    private String code;

    @NotBlank(message = "PLO description is required")
    private String description;
}
