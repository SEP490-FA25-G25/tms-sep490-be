package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.dashboard.CenterHeadDashboardDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final UserBranchesRepository userBranchesRepository;
    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final TeacherRequestRepository teacherRequestRepository;
    private final SessionRepository sessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final AttendanceService attendanceService;

    /**
     * Lấy dashboard cho Center Head.
     * Hiện tại tái sử dụng số liệu từ ManagerDashboardService và trong tương lai
     * có thể filter theo chi nhánh mà Center Head phụ trách.
     */
    @Transactional(readOnly = true)
    public CenterHeadDashboardDTO getDashboard(Long centerHeadUserId, LocalDate fromDate, LocalDate toDate, Long branchId) {
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

        // Lấy danh sách branch mà Center Head phụ trách
        List<Long> allBranchIds = userBranchesRepository.findBranchIdsByUserId(centerHeadUserId);
        if (allBranchIds == null || allBranchIds.isEmpty()) {
            log.warn("Center Head {} không có chi nhánh nào được gán", centerHeadUserId);
            allBranchIds = Collections.emptyList();
        }

        // Nếu có branchId được chỉ định, kiểm tra xem center head có quyền quản lý branch đó không
        List<Long> branchIds;
        if (branchId != null) {
            if (!allBranchIds.contains(branchId)) {
                log.warn("Center Head {} không có quyền truy cập chi nhánh {}", centerHeadUserId, branchId);
                branchIds = Collections.emptyList();
            } else {
                branchIds = List.of(branchId);
                log.info("Lọc dashboard theo chi nhánh {} cho Center Head {}", branchId, centerHeadUserId);
            }
        } else {
            branchIds = allBranchIds;
            log.info("Lấy dashboard từ {} chi nhánh cho Center Head {}", branchIds.size(), centerHeadUserId);
        }

        // Không cần dùng ManagerDashboardService nữa vì đã tính lại tất cả metrics theo branchIds

        // Tính lại class summary dựa trên branchIds
        long activeClasses = 0L;
        if (!branchIds.isEmpty()) {
            List<ClassEntity> allClasses = classRepository.findAll().stream()
                    .filter(c -> c.getBranch() != null && branchIds.contains(c.getBranch().getId()))
                    .toList();
            activeClasses = allClasses.stream()
                    .filter(c -> c.getStatus() == ClassStatus.ONGOING || c.getStatus() == ClassStatus.SCHEDULED)
                    .count();
        }

        CenterHeadDashboardDTO.ClassSummary classSummary = CenterHeadDashboardDTO.ClassSummary.builder()
                .activeTotal(activeClasses)
                // Tạm thời chưa có tách riêng "tuần này" -> dùng 0 (có thể mở rộng sau)
                .upcomingThisWeek(0L)
                .build();

        // Tính lại student summary dựa trên branchIds
        long activeStudents = 0L;
        long newEnrollmentsInRange = 0L;
        if (!branchIds.isEmpty()) {
            // Đếm students có branch trong branchIds (students đang enrolled trong các lớp thuộc branchIds)
            List<ClassEntity> classesInBranches = classRepository.findAll().stream()
                    .filter(c -> c.getBranch() != null && branchIds.contains(c.getBranch().getId()))
                    .toList();
            Set<Long> classIds = classesInBranches.stream()
                    .map(ClassEntity::getId)
                    .collect(Collectors.toSet());
            
            // Đếm students đang enrolled trong các lớp thuộc branchIds
            List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                    .filter(e -> classIds.contains(e.getClassId()) 
                            && e.getStatus() == EnrollmentStatus.ENROLLED)
                    .toList();
            activeStudents = enrollments.stream()
                    .map(Enrollment::getStudent)
                    .map(s -> s != null ? s.getId() : null)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
            
            // Đếm enrollments mới trong date range
            OffsetDateTime fromDateTime = effectiveFrom.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime toDateTime = effectiveTo.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
            newEnrollmentsInRange = enrollments.stream()
                    .filter(e -> e.getEnrolledAt() != null
                            && !e.getEnrolledAt().isBefore(fromDateTime)
                            && !e.getEnrolledAt().isAfter(toDateTime))
                    .count();
        }

        CenterHeadDashboardDTO.StudentSummary studentSummary = CenterHeadDashboardDTO.StudentSummary.builder()
                .activeTotal(activeStudents)
                .newEnrollmentsThisWeek(newEnrollmentsInRange)
                .build();

        // Tính lại teacher summary dựa trên branchIds
        long totalTeachers = 0L;
        if (!branchIds.isEmpty()) {
            List<Teacher> teachers = teacherRepository.findByBranchIds(branchIds);
            totalTeachers = teachers.size();
        }

        CenterHeadDashboardDTO.TeacherSummary teacherSummary = CenterHeadDashboardDTO.TeacherSummary.builder()
                .total(totalTeachers)
                .scheduleStatus("Đã cập nhật theo lịch hệ thống")
                .build();

        // Tính lại pending reports summary dựa trên branchIds
        long totalPendingRequests = 0L;
        if (!branchIds.isEmpty()) {
            long pendingStudentRequests = studentRequestRepository.countByStatusAndBranches(
                    RequestStatus.PENDING, branchIds);
            long pendingTeacherRequests = teacherRequestRepository.findByStatusOrderBySubmittedAtDesc(
                    RequestStatus.PENDING).stream()
                    .filter(tr -> {
                        Session session = tr.getSession();
                        if (session != null && session.getClassEntity() != null 
                                && session.getClassEntity().getBranch() != null) {
                            return branchIds.contains(session.getClassEntity().getBranch().getId());
                        }
                        return false;
                    })
                    .count();
            totalPendingRequests = pendingStudentRequests + pendingTeacherRequests;
        }

        CenterHeadDashboardDTO.PendingReportsSummary pendingReportsSummary =
                CenterHeadDashboardDTO.PendingReportsSummary.builder()
                        .totalPending(totalPendingRequests)
                        .requiresAttention(totalPendingRequests)
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
        // Tính lại teacher workload dựa trên branchIds
        long teachingTeachers = 0L;
        double totalTeachingHours = 0.0;
        if (!branchIds.isEmpty()) {
            // Lấy tất cả sessions trong date range của các lớp thuộc branchIds
            List<Session> sessionsInRange = sessionRepository.findAll().stream()
                    .filter(s -> !s.getDate().isBefore(effectiveFrom) && !s.getDate().isAfter(effectiveTo)
                            && s.getStatus() != SessionStatus.CANCELLED
                            && s.getClassEntity() != null
                            && s.getClassEntity().getBranch() != null
                            && branchIds.contains(s.getClassEntity().getBranch().getId()))
                    .collect(Collectors.toList());

            // Tính số giáo viên đang dạy (có ít nhất 1 teaching slot trong date range)
            Set<Long> teachingTeacherIds = new HashSet<>();
            
            for (Session session : sessionsInRange) {
                List<TeachingSlot> teachingSlots = teachingSlotRepository.findBySessionIdWithTeacher(session.getId());
                for (TeachingSlot slot : teachingSlots) {
                    if (slot.getStatus() == TeachingSlotStatus.SCHEDULED || slot.getStatus() == TeachingSlotStatus.SUBSTITUTED) {
                        teachingTeacherIds.add(slot.getTeacher().getId());
                        
                        // Tính giờ dạy
                        if (session.getTimeSlotTemplate() != null) {
                            LocalTime startTime = session.getTimeSlotTemplate().getStartTime();
                            LocalTime endTime = session.getTimeSlotTemplate().getEndTime();
                            if (startTime != null && endTime != null) {
                                Duration duration = Duration.between(startTime, endTime);
                                totalTeachingHours += duration.toHours() + (duration.toMinutesPart() / 60.0);
                            }
                        }
                    }
                }
            }
            
            teachingTeachers = teachingTeacherIds.size();
        }
        
        long availableTeachers = Math.max(totalTeachers - teachingTeachers, 0L);
        double teachingPercent = totalTeachers > 0 
                ? (teachingTeachers * 100.0) / totalTeachers 
                : 0.0;
        double availablePercent = totalTeachers > 0 
                ? (availableTeachers * 100.0) / totalTeachers 
                : 0.0;

        CenterHeadDashboardDTO.TeacherWorkloadSummary workloadSummary =
                CenterHeadDashboardDTO.TeacherWorkloadSummary.builder()
                        .totalTeachers(totalTeachers)
                        .teachingTeachers(teachingTeachers)
                        .availableTeachers(availableTeachers)
                        .teachingPercent(teachingPercent)
                        .availablePercent(availablePercent)
                        .totalTeachingHoursThisWeek(totalTeachingHours)
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
        // Tính lại attendance trend dựa trên branchIds
        double todayRate = 0.0;
        Set<Long> lowAttendanceClassIds = new HashSet<>(); // Dùng Set để tránh đếm trùng
        List<CenterHeadDashboardDTO.AttendanceTrendPoint> attendanceTrend = new ArrayList<>();

        if (!branchIds.isEmpty()) {
            LocalDate currentDate = effectiveFrom;
            while (!currentDate.isAfter(effectiveTo)) {
                // Lấy sessions trong ngày của các lớp thuộc branchIds
                List<Session> sessionsOnDate = sessionRepository.findByDate(currentDate).stream()
                        .filter(s -> s.getStatus() != SessionStatus.CANCELLED
                                && s.getClassEntity() != null
                                && s.getClassEntity().getBranch() != null
                                && branchIds.contains(s.getClassEntity().getBranch().getId()))
                        .collect(Collectors.toList());

                if (!sessionsOnDate.isEmpty()) {
                    // Lấy danh sách các lớp có session trong ngày
                    Set<Long> classIds = sessionsOnDate.stream()
                            .map(s -> s.getClassEntity().getId())
                            .collect(Collectors.toSet());

                    // Tính tỷ lệ chuyên cần cho từng lớp và lấy trung bình
                    List<Double> classAttendanceRates = new ArrayList<>();
                    for (Long classId : classIds) {
                        double classRate = attendanceService.calculateClassAttendanceRate(classId);
                        classAttendanceRates.add(classRate * 100.0); // Convert to percentage
                        
                        // Đánh dấu lớp có tỷ lệ < 70%
                        if (classRate * 100.0 < 70.0) {
                            lowAttendanceClassIds.add(classId);
                        }
                    }

                    // Tính trung bình tỷ lệ chuyên cần của tất cả các lớp
                    double averageAttendanceRate = classAttendanceRates.isEmpty() 
                            ? 0.0 
                            : classAttendanceRates.stream()
                                    .mapToDouble(Double::doubleValue)
                                    .average()
                                    .orElse(0.0);

                    attendanceTrend.add(CenterHeadDashboardDTO.AttendanceTrendPoint.builder()
                            .date(currentDate.toString())
                            .attendanceRate(averageAttendanceRate)
                            .build());
                    
                    // Lấy rate của hôm nay
                    LocalDate today = LocalDate.now();
                    if (currentDate.equals(today)) {
                        todayRate = averageAttendanceRate;
                    }
                } else {
                    // Vẫn thêm điểm với rate 0 để biểu đồ hiển thị
                    attendanceTrend.add(CenterHeadDashboardDTO.AttendanceTrendPoint.builder()
                            .date(currentDate.toString())
                            .attendanceRate(0.0)
                            .build());
                }

                currentDate = currentDate.plusDays(1);
            }
            
            // Nếu chưa có todayRate, lấy điểm cuối cùng
            if (todayRate == 0.0 && !attendanceTrend.isEmpty()) {
                LocalDate today = LocalDate.now();
                String todayStr = today.toString();
                CenterHeadDashboardDTO.AttendanceTrendPoint todayPoint = attendanceTrend.stream()
                        .filter(p -> todayStr.equals(p.getDate()))
                        .findFirst()
                        .orElseGet(() -> attendanceTrend.get(attendanceTrend.size() - 1));
                if (todayPoint != null) {
                    todayRate = todayPoint.getAttendanceRate();
                }
            }
        }

        long lowAttendanceClassCount = lowAttendanceClassIds.size();

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


