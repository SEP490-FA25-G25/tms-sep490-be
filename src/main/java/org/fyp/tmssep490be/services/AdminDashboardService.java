package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.dashboard.AdminStatsDTO;
import org.fyp.tmssep490be.dtos.dashboard.DailyUserStats;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserAccountRepository userAccountRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public AdminStatsDTO getAdminStats() {
        log.info("Đang lấy thống kê cho Admin Dashboard");

        // Thống kê Users
        long totalUsers = userAccountRepository.count();
        long activeUsers = userAccountRepository.countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = userAccountRepository.countByStatus(UserStatus.INACTIVE);

        // Thống kê Branches
        long totalBranches = branchRepository.count();

        // User theo Role
        Map<String, Long> usersByRole = getUsersByRole();

        // User theo Branch
        Map<String, Long> usersByBranch = getUsersByBranch();

        // User mới 7 ngày
        List<DailyUserStats> newUsersLast7Days = getNewUsersLast7Days();

        return AdminStatsDTO.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .totalBranches(totalBranches)
                .usersByRole(usersByRole)
                .usersByBranch(usersByBranch)
                .newUsersLast7Days(newUsersLast7Days)
                .build();
    }

    private Map<String, Long> getUsersByRole() {
        List<Object[]> results = userAccountRepository.countUsersByRole();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            String roleName = (String) row[0];
            Long count = (Long) row[1];
            map.put(roleName, count);
        }
        return map;
    }

    private Map<String, Long> getUsersByBranch() {
        List<Object[]> results = userAccountRepository.countUsersByBranch();
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            String branchName = (String) row[0];
            Long count = (Long) row[1];
            map.put(branchName, count);
        }
        return map;
    }

    private List<DailyUserStats> getNewUsersLast7Days() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);

        List<Object[]> results = userAccountRepository.countNewUsersByDay(startDate, endDate);

        // Tạo map từ kết quả
        Map<LocalDate, Long> resultMap = new HashMap<>();
        for (Object[] row : results) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            Long count = (Long) row[1];
            resultMap.put(date, count);
        }

        // Đảm bảo có đủ 7 ngày (kể cả ngày không có user mới)
        List<DailyUserStats> stats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            Long count = resultMap.getOrDefault(date, 0L);
            stats.add(DailyUserStats.builder().date(date).count(count).build());
        }

        return stats;
    }
}
