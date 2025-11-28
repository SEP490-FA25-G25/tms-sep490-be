package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System overview statistics for Admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewDTO {
    private Long totalUsers;
    private Long totalStudents;
    private Long totalTeachers;
    private Long totalClasses;
    private Long activeClasses;
    private Long totalCourses;
    private Long totalCenters;
    private Long totalBranches;
    
    // Today's activities
    private Long todaySessions;
    private Long todayEnrollments;
    
    // Active counts
    private Long activeUsers;
    private Long inactiveUsers;
    private Long pendingApprovals;
}

