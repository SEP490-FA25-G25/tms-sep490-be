package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User growth data by month
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGrowthDTO {
    private String month; // Format: "YYYY-MM"
    private Long count;
}

