package org.fyp.tmssep490be.dtos.curriculum;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CurriculumWithLevelsDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<LevelDTO> levels;
    private List<PLODTO> plos;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LevelDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Integer sortOrder;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PLODTO {
        private String code;
        private String description;
    }
}