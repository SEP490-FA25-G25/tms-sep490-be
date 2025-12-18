package org.fyp.tmssep490be.dtos.publicapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for landing page - groups courses by curriculum
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCourseCatalogDTO {
    private List<CurriculumGroup> curriculums;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurriculumGroup {
        private Long id;
        private String code;
        private String name;
        private String description;
        private List<PublicCourseDTO> courses;
    }
}
