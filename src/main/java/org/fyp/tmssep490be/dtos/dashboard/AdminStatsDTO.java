package org.fyp.tmssep490be.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO thống kê tổng quan cho Admin Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {

    // Thống kê Users
    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;

    // Thống kê Branches
    private Long totalBranches;

    // Phân bố User theo Role (code -> số lượng)
    private Map<String, Long> usersByRole;

    // Phân bố User theo Branch (tên -> số lượng)
    private Map<String, Long> usersByBranch;

    // User mới trong 7 ngày gần đây
    private List<DailyUserStats> newUsersLast7Days;
}
