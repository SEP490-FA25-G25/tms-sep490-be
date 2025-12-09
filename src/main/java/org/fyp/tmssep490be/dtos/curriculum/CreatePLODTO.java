package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreatePLODTO {
    @NotBlank(message = "Mã PLO là bắt buộc")
    private String code;

    @NotBlank(message = "Mô tả PLO là bắt buộc")
    private String description;
}