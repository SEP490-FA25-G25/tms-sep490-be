package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateLevelDTO {
    @NotNull(message = "ID chương trình là bắt buộc")
    private Long curriculumId;

    @NotBlank(message = "Mã cấp độ là bắt buộc")
    private String code;

    @NotBlank(message = "Tên cấp độ là bắt buộc")
    private String name;

    private String description;
}