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
public class CreateSubjectDTO {
    @NotBlank(message = "Subject code is required")
    private String code;

    @NotBlank(message = "Subject name is required")
    private String name;

    private String description;

    private java.util.List<CreatePLODTO> plos;
}
