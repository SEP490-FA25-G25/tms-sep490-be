package org.fyp.tmssep490be.dtos.branch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for branch basic information
 * Used for dropdowns and listing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDTO {
    private Long id;
    private String code;
    private String name;
    private String address;
    private String city;
    private String district;
    private String status;
}
