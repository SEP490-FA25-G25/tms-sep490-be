package org.fyp.tmssep490be.dtos.publicapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple DTO for public course dropdown (different from full PublicCourseDTO)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCourseSimpleDTO {
    private Long id;
    private String name;
    private String code;
}
