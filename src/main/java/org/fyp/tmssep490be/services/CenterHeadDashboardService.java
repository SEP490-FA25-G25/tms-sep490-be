package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.dashboard.CenterHeadDashboardDTO;
import org.fyp.tmssep490be.dtos.dashboard.ManagerDashboardDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service cung cấp dữ liệu cho Center Head Dashboard.
 *
 * Hiện tại, để triển khai nhanh và tái sử dụng logic có sẵn,
 * service này dựa trên số liệu tổng từ {@link ManagerDashboardService}
 * và ánh xạ sang cấu trúc DTO dành riêng cho Center Head.
 *
 * Trong tương lai có thể mở rộng để lọc dữ liệu theo chi nhánh
 * mà Center Head phụ trách (dựa vào UserBranches).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CenterHeadDashboardService {

    private final ManagerDashboardService managerDashboardService;
    private final UserBranchesRepository userBranchesRepository;
    private final ClassRepository classRepository;

    /**
     * Lấy dashboard cho Center Head.
     * Hiện tại tái sử dụng số liệu từ ManagerDashboardService và trong tương lai
     * có thể filter theo chi nhánh mà Center Head phụ trách.
     */
    @Transactional(readOnly = true)
    public CenterHeadDashboardDTO getDashboard(Long centerHeadUserId, LocalDate fromDate, LocalDate toDate) {
        // Tính khoảng thời gian hiệu lực giống logic trong ManagerDashboardService
        final LocalDate effectiveTo;
        final LocalDate effectiveFrom;
        if (fromDate == null || toDate == null) {
            effectiveTo = LocalDate.now();
            effectiveFrom = effectiveTo.minusDays(6);
        } else {
            effectiveFrom = fromDate;
            effectiveTo = toDate;
        }

        ManagerDashboardDTO managerDashboard = managerDashboardService.getDashboard(centerHeadUserId, effectiveFrom, effectiveTo);

        // Lấy danh sách branch mà Center Head phụ trách (hiện tại chỉ dùng để log/debug)
        List<Long> branchIds = userBranchesRepository.findBranchIdsByUserId(centerHeadUserId);
        log.info("Building Center Head dashboard for user {} with branches {}", centerHeadUserId, branchIds);

        // ===== Summary mapping =====
        ManagerDashboardDTO.Summary mSummary = managerDashboard.getSummary();

        CenterHeadDashboardDTO.ClassSummary classSummary = CenterHeadDashboardDTO.ClassSummary.builder()
                .activeTotal(mSummary != null && mSummary.getClasses() != null
                        ? mSummary.getClasses().getActiveTotal()
                        : 0L)
                // Tạm thời chưa có tách riêng "tuần này" -> dùng 0 (có thể mở rộng sau)
                .upcomingThisWeek(0L)
                .build();

        CenterHeadDashboardDTO.StudentSummary studentSummary = CenterHeadDashboardDTO.StudentSummary.builder()
                .activeTotal(mSummary != null && mSummary.getStudents() != null
                        ? mSummary.getStudents().getActiveTotal()
                        : 0L)
                .newEnrollmentsThisWeek(mSummary != null && mSummary.getStudents() != null
                        ? mSummary.getStudents().getNewEnrollmentsInRange()
                        : 0L)
                .build();

        CenterHeadDashboardDTO.TeacherSummary teacherSummary = CenterHeadDashboardDTO.TeacherSummary.builder()
                .total(mSummary != null && mSummary.getTeachers() != null
                        ? mSummary.getTeachers().getTotal()
                        : 0L)
                // Thông điệp đơn giản, có thể thay bằng logic kiểm tra thật sau
                .scheduleStatus("Đã cập nhật theo lịch hệ thống")
                .build();

        CenterHeadDashboardDTO.PendingReportsSummary pendingReportsSummary =
                CenterHeadDashboardDTO.PendingReportsSummary.builder()
                        .totalPending(mSummary != null && mSummary.getPendingRequests() != null
                                ? mSummary.getPendingRequests().getTotalPending()
                                : 0L)
                        // Hiện tại chưa phân loại chi tiết -> dùng cùng giá trị
                        .requiresAttention(mSummary != null && mSummary.getPendingRequests() != null
                                ? mSummary.getPendingRequests().getTotalPending()
                                : 0L)
                        .build();

        CenterHeadDashboardDTO.Summary summary = CenterHeadDashboardDTO.Summary.builder()
                .classes(classSummary)
                .students(studentSummary)
                .teachers(teacherSummary)
                .pendingReports(pendingReportsSummary)
                .build();

        // ===== Classes per date (trong khoảng đã chọn) =====
        List<CenterHeadDashboardDTO.ClassesPerDayItem> classesPerDay = new ArrayList<>();
        if (!branchIds.isEmpty()) {
            List<ClassEntity> classesInRange = classRepository.findByBranchesAndStartDateBetween(
                    branchIds, effectiveFrom, effectiveTo);

            Map<LocalDate, Long> countByDate = classesInRange.stream()
                    .filter(c -> c.getStartDate() != null)
                    .collect(Collectors.groupingBy(ClassEntity::getStartDate, Collectors.counting()));

            LocalDate cursor = effectiveFrom;
            while (!cursor.isAfter(effectiveTo)) {
                long count = countByDate.getOrDefault(cursor, 0L);
                classesPerDay.add(
                        CenterHeadDashboardDTO.ClassesPerDayItem.builder()
                                .date(cursor.toString())
                                .classCount(count)
                                .build()
                );
                cursor = cursor.plusDays(1);
            }
        }

        // ===== Teacher workload =====
        ManagerDashboardDTO.TeachingWorkload mWorkload = managerDashboard.getTeachingWorkload();
        CenterHeadDashboardDTO.TeacherWorkloadSummary workloadSummary =
                CenterHeadDashboardDTO.TeacherWorkloadSummary.builder()
                        .totalTeachers(mWorkload != null ? mWorkload.getTotalTeachers() : 0L)
                        .teachingTeachers(mWorkload != null ? mWorkload.getTeachingTeachers() : 0L)
                        .availableTeachers(mWorkload != null ? mWorkload.getAvailableTeachers() : 0L)
                        .teachingPercent(mWorkload != null ? mWorkload.getTeachingPercent() : 0.0)
                        .availablePercent(mWorkload != null ? mWorkload.getAvailablePercent() : 0.0)
                        .totalTeachingHoursThisWeek(mWorkload != null ? mWorkload.getTotalTeachingHoursInRange() : 0.0)
                        .build();

        // ===== Upcoming classes (7-14 ngày tới) =====
        List<CenterHeadDashboardDTO.UpcomingClassItem> upcomingClasses = new ArrayList<>();
        if (!branchIds.isEmpty()) {
            LocalDate today = LocalDate.now();
            LocalDate fromUpcoming = today.plusDays(7);
            LocalDate toUpcoming = today.plusDays(14);

            List<ClassEntity> upcomingClassEntities = classRepository.findUpcomingClassesForBranches(
                    branchIds, fromUpcoming, toUpcoming);

            for (ClassEntity c : upcomingClassEntities) {
                Subject subject = c.getSubject();
                Teacher teacher = c.getAssignedTeacher();

                String teacherName = null;
                if (teacher != null && teacher.getUserAccount() != null) {
                    teacherName = teacher.getUserAccount().getFullName();
                }

                upcomingClasses.add(
                        CenterHeadDashboardDTO.UpcomingClassItem.builder()
                                .classId(c.getId())
                                .className(c.getName() != null ? c.getName() : c.getCode())
                                .courseCode(subject != null ? subject.getCode() : null)
                                .startDate(c.getStartDate() != null ? c.getStartDate().toString() : null)
                                .teacherName(teacherName)
                                // Room sẽ được bổ sung sau nếu cần lấy từ session/resource
                                .roomName(null)
                                .build()
                );
            }
        }

        // ===== Attendance summary + trend =====
        double todayRate = 0.0;
        long lowAttendanceClassCount = 0L;
        List<CenterHeadDashboardDTO.AttendanceTrendPoint> attendanceTrend = new ArrayList<>();

        if (managerDashboard.getAttendanceTrend() != null && !managerDashboard.getAttendanceTrend().isEmpty()) {
            // Map attendance trend
            for (ManagerDashboardDTO.AttendanceTrendPoint p : managerDashboard.getAttendanceTrend()) {
                attendanceTrend.add(
                        CenterHeadDashboardDTO.AttendanceTrendPoint.builder()
                                .date(p.getDate())
                                .attendanceRate(p.getAttendanceRate())
                                .build()
                );
                if (p.getAttendanceRate() < 70.0) {
                    lowAttendanceClassCount++;
                }
            }

            // Lấy điểm có date == hôm nay nếu có, nếu không dùng điểm cuối cùng
            LocalDate today = LocalDate.now();
            String todayStr = today.toString();
            ManagerDashboardDTO.AttendanceTrendPoint todayPoint = managerDashboard.getAttendanceTrend()
                    .stream()
                    .filter(p -> todayStr.equals(p.getDate()))
                    .findFirst()
                    .orElseGet(() -> managerDashboard.getAttendanceTrend()
                            .get(managerDashboard.getAttendanceTrend().size() - 1));
            if (todayPoint != null) {
                todayRate = todayPoint.getAttendanceRate();
            }
        }

        CenterHeadDashboardDTO.AttendanceSummary attendanceSummary =
                CenterHeadDashboardDTO.AttendanceSummary.builder()
                        .todayRate(todayRate)
                        .lowAttendanceClassCount(lowAttendanceClassCount)
                        .build();

        return CenterHeadDashboardDTO.builder()
                .summary(summary)
                .classesPerDay(classesPerDay)
                .teacherWorkload(workloadSummary)
                .upcomingClasses(upcomingClasses)
                .attendance(attendanceSummary)
                .attendanceTrend(attendanceTrend)
                .build();
    }
}


