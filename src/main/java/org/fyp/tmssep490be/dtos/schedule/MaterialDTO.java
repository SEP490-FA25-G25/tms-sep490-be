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
public class MaterialDTO {

    private Long materialId;

    private String title;

    private String description;

    private String fileName;

    private String materialType;

    private String fileUrl;

    private LocalDateTime uploadedAt;
}
