package org.fyp.tmssep490be.dtos.curriculum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for Subject with its levels
 * Used for dropdown/select components in student creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectWithLevelsDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private List<LevelDTO> levels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Integer expectedDurationHours;
        private Integer sortOrder;
        private String status;
    }
}