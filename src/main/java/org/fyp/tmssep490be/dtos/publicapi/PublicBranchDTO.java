package org.fyp.tmssep490be.dtos.publicapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for public branch display in consultation form
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicBranchDTO {
    private Long id;
    private String name;
    private String address;
}
