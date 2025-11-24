package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLevelDTO {
    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotBlank(message = "Level code is required")
    private String code;

    @NotBlank(message = "Level name is required")
    private String name;

    private String description;

    private Integer durationHours;
}
