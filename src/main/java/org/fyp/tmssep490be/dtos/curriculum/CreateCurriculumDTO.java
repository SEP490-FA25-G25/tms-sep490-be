package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCurriculumDTO {
    @NotBlank(message = "Mã chương trình là bắt buộc")
    private String code;

    @NotBlank(message = "Tên chương trình là bắt buộc")
    private String name;

    private String description;

    @lombok.Builder.Default
    private String language = "English";

    // Danh sách PLO (Program Learning Outcomes)
    private List<CreatePLODTO> plos;
}