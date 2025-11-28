package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.adminanalytic.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final CenterRepository centerRepository;
    private final BranchRepository branchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SessionRepository sessionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final org.fyp.tmssep490be.repositories.UserBranchesRepository userBranchesRepository;

    @Override
    public AnalyticsResponseDTO getSystemAnalytics() {
        log.info("Generating system analytics for Admin dashboard");

        SystemOverviewDTO overview = buildSystemOverview();
        UserAnalyticsDTO userAnalytics = buildUserAnalytics();
        ClassAnalyticsDTO classAnalytics = buildClassAnalytics();
        BranchAnalyticsDTO branchAnalytics = buildBranchAnalytics();

        return AnalyticsResponseDTO.builder()
                .overview(overview)
                .userAnalytics(userAnalytics)
                .classAnalytics(classAnalytics)
                .branchAnalytics(branchAnalytics)
                .build();
    }

    private SystemOverviewDTO buildSystemOverview() {
        long totalUsers = userAccountRepository.count();
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalClasses = classRepository.count();
        long activeClasses = classRepository.countByStatusIn(
                Arrays.asList(ClassStatus.ONGOING, ClassStatus.SCHEDULED));
        long totalCourses = courseRepository.count();
        long totalCenters = centerRepository.count();
        long totalBranches = branchRepository.count();

        LocalDate today = LocalDate.now();
        OffsetDateTime todayStart = today.atStartOfDay().atOffset(
                java.time.ZoneOffset.UTC);
        OffsetDateTime todayEnd = today.plusDays(1).atStartOfDay().atOffset(
                java.time.ZoneOffset.UTC);

        long todaySessions = sessionRepository.countByDate(today);
        long todayEnrollments = enrollmentRepository.countByEnrolledAtBetween(
                todayStart, todayEnd);

        long activeUsers = userAccountRepository.countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = userAccountRepository.countByStatus(UserStatus.INACTIVE);
        
        // Count pending approvals (classes with PENDING status)
        long pendingApprovals = classRepository.countByApprovalStatus(
                ApprovalStatus.PENDING);

        return SystemOverviewDTO.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .totalCourses(totalCourses)
                .totalCenters(totalCenters)
                .totalBranches(totalBranches)
                .todaySessions(todaySessions)
                .todayEnrollments(todayEnrollments)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .pendingApprovals(pendingApprovals)
                .build();
    }

    private UserAnalyticsDTO buildUserAnalytics() {
        // Get users by role - chỉ đếm những user có cả role và profile tương ứng
        Map<String, Long> usersByRole = new HashMap<>();
        List<org.fyp.tmssep490be.entities.Role> allRoles = roleRepository.findAll();
        
        for (org.fyp.tmssep490be.entities.Role role : allRoles) {
            long count;
            // Đối với STUDENT và TEACHER, chỉ đếm những user có cả role và profile
            if ("STUDENT".equals(role.getCode())) {
                // Đếm students có role STUDENT
                count = userAccountRepository.countStudentsWithRole();
            } else if ("TEACHER".equals(role.getCode())) {
                // Đếm teachers có role TEACHER
                count = userAccountRepository.countTeachersWithRole();
            } else {
                // Các role khác đếm bình thường
                count = userRoleRepository.countByRoleId(role.getId());
            }
            
            if (count > 0) {
                usersByRole.put(role.getCode(), count);
            }
        }

        // Get user growth (last 6 months)
        List<UserGrowthDTO> userGrowth = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);
            OffsetDateTime monthStartTime = monthStart.atStartOfDay().atOffset(
                    java.time.ZoneOffset.UTC);
            OffsetDateTime monthEndTime = monthEnd.atStartOfDay().atOffset(
                    java.time.ZoneOffset.UTC);
            
            long count = userAccountRepository.countByCreatedAtBetween(
                    monthStartTime, monthEndTime);
            
            userGrowth.add(UserGrowthDTO.builder()
                    .month(monthStart.format(formatter))
                    .count(count)
                    .build());
        }

        long totalActiveUsers = userAccountRepository.countByStatus(UserStatus.ACTIVE);
        long totalInactiveUsers = userAccountRepository.countByStatus(UserStatus.INACTIVE);

        return UserAnalyticsDTO.builder()
                .usersByRole(usersByRole)
                .userGrowth(userGrowth)
                .totalActiveUsers(totalActiveUsers)
                .totalInactiveUsers(totalInactiveUsers)
                .build();
    }

    private ClassAnalyticsDTO buildClassAnalytics() {
        long totalClasses = classRepository.count();
        long activeClasses = classRepository.countByStatusIn(
                Arrays.asList(ClassStatus.ONGOING, ClassStatus.SCHEDULED));
        long completedClasses = classRepository.countByStatus(ClassStatus.COMPLETED);
        long cancelledClasses = classRepository.countByStatus(ClassStatus.CANCELLED);

        Map<String, Long> classesByStatus = new HashMap<>();
        for (ClassStatus status : ClassStatus.values()) {
            long count = classRepository.countByStatus(status);
            if (count > 0) {
                classesByStatus.put(status.name(), count);
            }
        }

        long totalEnrollments = enrollmentRepository.count();
        double averageEnrollmentRate = totalClasses > 0 
                ? (double) totalEnrollments / totalClasses 
                : 0.0;

        return ClassAnalyticsDTO.builder()
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .completedClasses(completedClasses)
                .cancelledClasses(cancelledClasses)
                .classesByStatus(classesByStatus)
                .averageEnrollmentRate(averageEnrollmentRate)
                .totalEnrollments(totalEnrollments)
                .build();
    }

    private BranchAnalyticsDTO buildBranchAnalytics() {
        List<org.fyp.tmssep490be.entities.Branch> allBranches = branchRepository.findAll();
        List<BranchStatDTO> branchStats = new ArrayList<>();

        for (org.fyp.tmssep490be.entities.Branch branch : allBranches) {
            long studentCount = studentRepository.countByBranchId(branch.getId());
            long teacherCount = teacherRepository.countByBranchId(branch.getId());
            long classCount = classRepository.countByBranchId(branch.getId());
            long activeClassCount = classRepository.countByBranchIdAndStatusIn(
                    branch.getId(), 
                    Arrays.asList(ClassStatus.ONGOING, ClassStatus.SCHEDULED));

            branchStats.add(BranchStatDTO.builder()
                    .branchId(branch.getId())
                    .branchName(branch.getName())
                    .centerId(branch.getCenter().getId())
                    .centerName(branch.getCenter().getName())
                    .studentCount(studentCount)
                    .teacherCount(teacherCount)
                    .classCount(classCount)
                    .activeClassCount(activeClassCount)
                    .build());
        }

        return BranchAnalyticsDTO.builder()
                .branchStats(branchStats)
                .build();
    }

    @Override
    public AnalyticsResponseDTO getManagerAnalytics(Long managerUserId) {
        log.info("Generating analytics for Manager dashboard, userId: {}", managerUserId);

        // Get branches assigned to this manager
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(managerUserId);
        
        if (branchIds.isEmpty()) {
            log.warn("Manager {} has no assigned branches, returning empty analytics", managerUserId);
            // Return empty analytics if no branches assigned
            return AnalyticsResponseDTO.builder()
                    .overview(buildEmptySystemOverview())
                    .userAnalytics(buildEmptyUserAnalytics())
                    .classAnalytics(buildEmptyClassAnalytics())
                    .branchAnalytics(buildEmptyBranchAnalytics())
                    .build();
        }

        SystemOverviewDTO overview = buildSystemOverviewForManager(branchIds);
        UserAnalyticsDTO userAnalytics = buildUserAnalyticsForManager(branchIds);
        ClassAnalyticsDTO classAnalytics = buildClassAnalyticsForManager(branchIds);
        BranchAnalyticsDTO branchAnalytics = buildBranchAnalyticsForManager(branchIds);

        return AnalyticsResponseDTO.builder()
                .overview(overview)
                .userAnalytics(userAnalytics)
                .classAnalytics(classAnalytics)
                .branchAnalytics(branchAnalytics)
                .build();
    }

    private SystemOverviewDTO buildSystemOverviewForManager(List<Long> branchIds) {
        // Count students in manager's branches
        long totalStudents = studentRepository.countByBranchIdIn(branchIds);
        
        // Count teachers assigned to classes in manager's branches
        long totalTeachers = teacherRepository.countByAssignedClassesInBranches(branchIds);
        
        // Count classes in manager's branches
        long totalClasses = classRepository.countByBranchIdIn(branchIds);
        long activeClasses = classRepository.countByBranchIdInAndStatusIn(
                branchIds, Arrays.asList(ClassStatus.ONGOING, ClassStatus.SCHEDULED));
        
        // Count courses that have classes in manager's branches
        long totalCourses = courseRepository.countDistinctByClassesInBranches(branchIds);
        
        // Count centers and branches
        long totalCenters = centerRepository.countDistinctByBranches(branchIds);
        long totalBranches = (long) branchIds.size();

        LocalDate today = LocalDate.now();
        OffsetDateTime todayStart = today.atStartOfDay().atOffset(
                java.time.ZoneOffset.UTC);
        OffsetDateTime todayEnd = today.plusDays(1).atStartOfDay().atOffset(
                java.time.ZoneOffset.UTC);

        // Count sessions in manager's branches
        long todaySessions = sessionRepository.countByDateAndBranchIdIn(today, branchIds);
        
        // Count enrollments in manager's branches
        long todayEnrollments = enrollmentRepository.countByEnrolledAtBetweenAndBranchIdIn(
                todayStart, todayEnd, branchIds);

        // Count users in manager's branches (students + teachers)
        long activeUsers = userAccountRepository.countActiveUsersInBranches(branchIds);
        long inactiveUsers = userAccountRepository.countInactiveUsersInBranches(branchIds);
        
        // Count pending approvals in manager's branches
        long pendingApprovals = classRepository.countByApprovalStatusAndBranchIdIn(
                ApprovalStatus.PENDING, branchIds);

        return SystemOverviewDTO.builder()
                .totalUsers(totalStudents + totalTeachers) // Approximate
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .totalCourses(totalCourses)
                .totalCenters(totalCenters)
                .totalBranches(totalBranches)
                .todaySessions(todaySessions)
                .todayEnrollments(todayEnrollments)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .pendingApprovals(pendingApprovals)
                .build();
    }

    private UserAnalyticsDTO buildUserAnalyticsForManager(List<Long> branchIds) {
        // Simplified: count students and teachers in branches
        Map<String, Long> usersByRole = new HashMap<>();
        usersByRole.put("STUDENT", studentRepository.countByBranchIdIn(branchIds));
        usersByRole.put("TEACHER", teacherRepository.countByAssignedClassesInBranches(branchIds));
        
        // User growth (simplified - last 6 months)
        List<UserGrowthDTO> userGrowth = buildUserGrowthForBranches(branchIds);
        
        long totalActiveUsers = userAccountRepository.countActiveUsersInBranches(branchIds);
        long totalInactiveUsers = userAccountRepository.countInactiveUsersInBranches(branchIds);

        return UserAnalyticsDTO.builder()
                .usersByRole(usersByRole)
                .userGrowth(userGrowth)
                .totalActiveUsers(totalActiveUsers)
                .totalInactiveUsers(totalInactiveUsers)
                .build();
    }

    private ClassAnalyticsDTO buildClassAnalyticsForManager(List<Long> branchIds) {
        long totalClasses = classRepository.countByBranchIdIn(branchIds);
        long activeClasses = classRepository.countByBranchIdInAndStatusIn(
                branchIds, Arrays.asList(ClassStatus.ONGOING, ClassStatus.SCHEDULED));
        long completedClasses = classRepository.countByBranchIdInAndStatusIn(
                branchIds, Arrays.asList(ClassStatus.COMPLETED));
        long cancelledClasses = classRepository.countByBranchIdInAndStatusIn(
                branchIds, Arrays.asList(ClassStatus.CANCELLED));

        Map<String, Long> classesByStatus = new HashMap<>();
        classesByStatus.put("ONGOING", classRepository.countByBranchIdInAndStatusIn(
                branchIds, Arrays.asList(ClassStatus.ONGOING)));
        classesByStatus.put("SCHEDULED", classRepository.countByBranchIdInAndStatusIn(
                branchIds, Arrays.asList(ClassStatus.SCHEDULED)));
        classesByStatus.put("COMPLETED", completedClasses);
        classesByStatus.put("CANCELLED", cancelledClasses);

        // Average enrollment rate (simplified)
        double averageEnrollmentRate = enrollmentRepository.calculateAverageEnrollmentRateForBranches(branchIds);
        long totalEnrollments = enrollmentRepository.countByBranchIdIn(branchIds);

        return ClassAnalyticsDTO.builder()
                .totalClasses(totalClasses)
                .activeClasses(activeClasses)
                .completedClasses(completedClasses)
                .cancelledClasses(cancelledClasses)
                .classesByStatus(classesByStatus)
                .averageEnrollmentRate(averageEnrollmentRate)
                .totalEnrollments(totalEnrollments)
                .build();
    }

    private BranchAnalyticsDTO buildBranchAnalyticsForManager(List<Long> branchIds) {
        List<BranchStatDTO> branchStats = new ArrayList<>();
        
        for (Long branchId : branchIds) {
            org.fyp.tmssep490be.entities.Branch branch = branchRepository.findById(branchId)
                    .orElse(null);
            if (branch == null) continue;

            long studentCount = studentRepository.countByBranchId(branchId);
            long teacherCount = teacherRepository.countByAssignedClassesInBranch(branchId);
            long classCount = classRepository.countByBranchId(branchId);
            long activeClassCount = classRepository.countByBranchIdAndStatusIn(
                    branchId, Arrays.asList(ClassStatus.ONGOING, ClassStatus.SCHEDULED));

            branchStats.add(BranchStatDTO.builder()
                    .branchId(branch.getId())
                    .branchName(branch.getName())
                    .centerId(branch.getCenter().getId())
                    .centerName(branch.getCenter().getName())
                    .studentCount(studentCount)
                    .teacherCount(teacherCount)
                    .classCount(classCount)
                    .activeClassCount(activeClassCount)
                    .build());
        }

        return BranchAnalyticsDTO.builder()
                .branchStats(branchStats)
                .build();
    }

    private List<UserGrowthDTO> buildUserGrowthForBranches(List<Long> branchIds) {
        // Simplified: return empty or basic growth data
        // Can be enhanced later with actual monthly counts
        return new ArrayList<>();
    }

    private SystemOverviewDTO buildEmptySystemOverview() {
        return SystemOverviewDTO.builder()
                .totalUsers(0L).totalStudents(0L).totalTeachers(0L)
                .totalClasses(0L).activeClasses(0L).totalCourses(0L)
                .totalCenters(0L).totalBranches(0L)
                .todaySessions(0L).todayEnrollments(0L)
                .activeUsers(0L).inactiveUsers(0L).pendingApprovals(0L)
                .build();
    }

    private UserAnalyticsDTO buildEmptyUserAnalytics() {
        return UserAnalyticsDTO.builder()
                .usersByRole(new HashMap<>())
                .userGrowth(new ArrayList<>())
                .totalActiveUsers(0L)
                .totalInactiveUsers(0L)
                .build();
    }

    private ClassAnalyticsDTO buildEmptyClassAnalytics() {
        return ClassAnalyticsDTO.builder()
                .totalClasses(0L).activeClasses(0L).completedClasses(0L).cancelledClasses(0L)
                .classesByStatus(new HashMap<>())
                .averageEnrollmentRate(0.0)
                .totalEnrollments(0L)
                .build();
    }

    private BranchAnalyticsDTO buildEmptyBranchAnalytics() {
        return BranchAnalyticsDTO.builder()
                .branchStats(new ArrayList<>())
                .build();
    }
}

