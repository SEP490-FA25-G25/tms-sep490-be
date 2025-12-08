package org.fyp.tmssep490be.dtos.curriculum;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CurriculumResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private String language;
    private int levelCount;
    private String status;
    private String createdAt;
    private List<CreatePLODTO> plos;
    private List<LevelResponseDTO> levels;
}