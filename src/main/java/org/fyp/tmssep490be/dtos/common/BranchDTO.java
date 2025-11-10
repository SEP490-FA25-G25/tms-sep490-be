package org.fyp.tmssep490be.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Branch information used in dropdowns and selects
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {
    
    private Long id;
    private String name;
    private String code;
}
