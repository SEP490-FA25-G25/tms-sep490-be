package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Complete analytics response for Admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDTO {
    private SystemOverviewDTO overview;
    private UserAnalyticsDTO userAnalytics;
    private ClassAnalyticsDTO classAnalytics;
    private BranchAnalyticsDTO branchAnalytics;
}

