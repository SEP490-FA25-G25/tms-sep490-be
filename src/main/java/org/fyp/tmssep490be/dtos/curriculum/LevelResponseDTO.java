package org.fyp.tmssep490be.dtos.curriculum;

import lombok.*;

import java.time.OffsetDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LevelResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private Long curriculumId;
    private String curriculumName;
    private String curriculumCode;
    private String status;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}