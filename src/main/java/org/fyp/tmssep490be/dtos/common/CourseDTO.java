package org.fyp.tmssep490be.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Course information used in dropdowns and selects
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    
    private Long id;
    private String name;
    private String code;
}
