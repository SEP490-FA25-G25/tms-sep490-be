package org.fyp.tmssep490be.dtos.curriculum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private int levelCount;
    private String status;
    private String createdAt;
    private java.util.List<CreatePLODTO> plos;
    private java.util.List<LevelResponseDTO> levels;
}
