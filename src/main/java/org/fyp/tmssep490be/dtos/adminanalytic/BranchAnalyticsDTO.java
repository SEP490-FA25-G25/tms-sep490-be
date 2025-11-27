package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Branch analytics data for Admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchAnalyticsDTO {
    private List<BranchStatDTO> branchStats;
}

