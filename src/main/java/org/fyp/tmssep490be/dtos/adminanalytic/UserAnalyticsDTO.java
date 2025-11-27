package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * User analytics data for Admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsDTO {
    private Map<String, Long> usersByRole; // Role code -> count
    private List<UserGrowthDTO> userGrowth; // Monthly growth
    private Long totalActiveUsers;
    private Long totalInactiveUsers;
}

