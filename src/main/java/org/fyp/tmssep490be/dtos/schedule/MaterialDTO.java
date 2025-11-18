package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Material information for session")
public class MaterialDTO {

    @Schema(description = "Material ID", example = "201")
    private Long materialId;

    @Schema(description = "File name", example = "Slide_Intro_IELTS.pdf")
    private String fileName;

    @Schema(description = "Download URL", example = "/api/v1/materials/download/201")
    private String fileUrl;

    @Schema(description = "Upload timestamp", example = "2025-10-30T10:00:00")
    private LocalDateTime uploadedAt;
}