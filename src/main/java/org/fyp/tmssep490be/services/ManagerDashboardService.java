package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.dashboard.ManagerDashboardDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.BranchStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service cung cấp dữ liệu cho Manager Dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardService {

    private final BranchRepository branchRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final TeacherRequestRepository teacherRequestRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;

    @Transactional(readOnly = true)
    public ManagerDashboardDTO getDashboard(Long managerUserId, LocalDate fromDate, LocalDate toDate) {
        // Nếu không có date range, dùng mặc định (7 ngày gần nhất)
        final LocalDate finalFromDate;
        final LocalDate finalToDate;
        if (fromDate == null || toDate == null) {
            finalToDate = LocalDate.now();
            finalFromDate = finalToDate.minusDays(6);
        } else {
            finalFromDate = fromDate;
            finalToDate = toDate;
        }
        
        log.info("Generating Manager dashboard for user {} from {} to {}", managerUserId, finalFromDate, finalToDate);

        // ===== Branch summary =====
        List<Branch> branches = branchRepository.findAll();
        long totalBranches = branches.size();
        long activeBranches = branches.stream()
                .filter(b -> b.getStatus() == BranchStatus.ACTIVE)
                .count();
        long inactiveBranches = totalBranches - activeBranches;

        ManagerDashboardDTO.BranchSummary branchSummary = ManagerDashboardDTO.BranchSummary.builder()
                .total(totalBranches)
                .active(activeBranches)
                .inactive(inactiveBranches)
                .build();

        // ===== Class summary =====
        long ongoingClasses = classRepository.findByStatus(ClassStatus.ONGOING).size();
        long scheduledClasses = classRepository.findByStatus(ClassStatus.SCHEDULED).size();
        long activeClasses = ongoingClasses + scheduledClasses;

        ManagerDashboardDTO.ClassSummary classSummary = ManagerDashboardDTO.ClassSummary.builder()
                .activeTotal(activeClasses)
                // Hiện tại chưa tính so sánh với khoảng trước -> 0
                .activeChangeVsPrevRangePercent(0.0)
                .build();

        // ===== Student summary =====
        long totalStudents = studentRepository.count();

        // Số lượt ghi danh mới trong khoảng thời gian
        OffsetDateTime fromDateTime = finalFromDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime toDateTime = finalToDate.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);
        long newEnrollmentsInRange = enrollmentRepository.findAll().stream()
                .filter(e -> e.getEnrolledAt() != null
                        && !e.getEnrolledAt().isBefore(fromDateTime)
                        && !e.getEnrolledAt().isAfter(toDateTime)
                        && e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();

        ManagerDashboardDTO.StudentSummary studentSummary = ManagerDashboardDTO.StudentSummary.builder()
                .activeTotal(totalStudents)
                .newEnrollmentsInRange(newEnrollmentsInRange)
                .build();

        // ===== Teacher summary =====
        long totalTeachers = userAccountRepository.findUsersByRole("TEACHER").size();

        ManagerDashboardDTO.TeacherSummary teacherSummary = ManagerDashboardDTO.TeacherSummary.builder()
                .total(totalTeachers)
                .build();

        // ===== Pending requests summary (student + teacher requests PENDING) =====
        long pendingStudentRequests = studentRequestRepository.countByStatus(RequestStatus.PENDING);
        long pendingTeacherRequests = teacherRequestRepository.findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING).size();
        long totalPendingRequests = pendingStudentRequests + pendingTeacherRequests;

        ManagerDashboardDTO.PendingRequestSummary pendingSummary = ManagerDashboardDTO.PendingRequestSummary.builder()
                .totalPending(totalPendingRequests)
                .build();

        // ===== QA reports summary (chưa triển khai chi tiết) =====
        ManagerDashboardDTO.QAReportSummary qaSummary = ManagerDashboardDTO.QAReportSummary.builder()
                .totalInRange(0L)
                .needManagerReview(0L)
                .build();

        ManagerDashboardDTO.Summary summary = ManagerDashboardDTO.Summary.builder()
                .branches(branchSummary)
                .classes(classSummary)
                .students(studentSummary)
                .teachers(teacherSummary)
                .pendingRequests(pendingSummary)
                .qaReports(qaSummary)
                .build();

        // ===== Classes per branch =====
        List<ManagerDashboardDTO.ClassesPerBranchItem> classesPerBranch = branches.stream()
                .map(branch -> {
                    long activeClassCount = branch.getClasses() == null
                            ? 0
                            : branch.getClasses().stream()
                            .filter(c -> c.getStatus() == ClassStatus.ONGOING
                                    || c.getStatus() == ClassStatus.SCHEDULED)
                            .count();

                    return ManagerDashboardDTO.ClassesPerBranchItem.builder()
                            .branchId(branch.getId())
                            .branchName(branch.getName())
                            .activeClasses(activeClassCount)
                            .active(branch.getStatus() == BranchStatus.ACTIVE)
                            .build();
                })
                .collect(Collectors.toList());

        // ===== Teaching workload =====
        // Lấy tất cả sessions trong date range (không bị hủy)
        List<Session> sessionsInRange = sessionRepository.findAll().stream()
                .filter(s -> !s.getDate().isBefore(finalFromDate) && !s.getDate().isAfter(finalToDate)
                        && s.getStatus() != SessionStatus.CANCELLED)
                .collect(Collectors.toList());

        // Tính số giáo viên đang dạy (có ít nhất 1 teaching slot trong date range)
        Set<Long> teachingTeacherIds = new HashSet<>();
        double totalTeachingHours = 0.0;

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

        long teachingTeachers = teachingTeacherIds.size();
        long availableTeachers = Math.max(totalTeachers - teachingTeachers, 0L);

        double teachingPercent = totalTeachers > 0
                ? (teachingTeachers * 100.0) / totalTeachers
                : 0.0;
        double availablePercent = totalTeachers > 0
                ? (availableTeachers * 100.0) / totalTeachers
                : 0.0;

        ManagerDashboardDTO.TeachingWorkload teachingWorkload = ManagerDashboardDTO.TeachingWorkload.builder()
                .totalTeachers(totalTeachers)
                .teachingTeachers(teachingTeachers)
                .availableTeachers(availableTeachers)
                .teachingPercent(teachingPercent)
                .availablePercent(availablePercent)
                .totalTeachingHoursInRange(totalTeachingHours)
                .build();

        // ===== Attendance trend =====
        List<ManagerDashboardDTO.AttendanceTrendPoint> attendanceTrend = new ArrayList<>();
        LocalDate currentDate = finalFromDate;
        while (!currentDate.isAfter(finalToDate)) {
            List<Session> sessionsOnDate = sessionRepository.findByDate(currentDate).stream()
                    .filter(s -> s.getStatus() != SessionStatus.CANCELLED)
                    .collect(Collectors.toList());

            if (!sessionsOnDate.isEmpty()) {
                long totalPresent = 0;
                long totalStudentsOnDate = 0;

                for (Session session : sessionsOnDate) {
                    List<org.fyp.tmssep490be.entities.StudentSession> studentSessions =
                            studentSessionRepository.findBySessionId(session.getId());
                    for (org.fyp.tmssep490be.entities.StudentSession ss : studentSessions) {
                        totalStudentsOnDate++;
                        if (ss.getAttendanceStatus() == AttendanceStatus.PRESENT) {
                            totalPresent++;
                        }
                    }
                }

                double attendanceRate = totalStudentsOnDate > 0 ? (totalPresent * 100.0) / totalStudentsOnDate : 0.0;
                attendanceTrend.add(ManagerDashboardDTO.AttendanceTrendPoint.builder()
                        .date(currentDate.toString())
                        .attendanceRate(attendanceRate)
                        .build());
            } else {
                // Vẫn thêm điểm với rate 0 để biểu đồ hiển thị
                attendanceTrend.add(ManagerDashboardDTO.AttendanceTrendPoint.builder()
                        .date(currentDate.toString())
                        .attendanceRate(0.0)
                        .build());
            }

            currentDate = currentDate.plusDays(1);
        }

        // ===== Enrollment trend (theo tuần) =====
        List<ManagerDashboardDTO.EnrollmentTrendPoint> enrollmentTrend = new ArrayList<>();
        LocalDate weekStart = finalFromDate;
        while (!weekStart.isAfter(finalToDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(finalToDate)) {
                weekEnd = finalToDate;
            }

            OffsetDateTime weekStartDateTime = weekStart.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);

            long enrollmentsInWeek = enrollmentRepository.findAll().stream()
                    .filter(e -> e.getEnrolledAt() != null
                            && !e.getEnrolledAt().isBefore(weekStartDateTime)
                            && !e.getEnrolledAt().isAfter(weekEndDateTime)
                            && e.getStatus() == EnrollmentStatus.ENROLLED)
                    .count();

            String label = String.format("Tuần %d/%d", weekStart.get(java.time.temporal.WeekFields.ISO.weekOfYear()),
                    weekStart.getYear());

            enrollmentTrend.add(ManagerDashboardDTO.EnrollmentTrendPoint.builder()
                    .label(label)
                    .startDate(weekStart.toString())
                    .endDate(weekEnd.toString())
                    .enrollments(enrollmentsInWeek)
                    .build());

            weekStart = weekEnd.plusDays(1);
        }

        return ManagerDashboardDTO.builder()
                .summary(summary)
                .classesPerBranch(classesPerBranch)
                .teachingWorkload(teachingWorkload)
                .attendanceTrend(attendanceTrend)
                .enrollmentTrend(enrollmentTrend)
                .build();
    }
}


