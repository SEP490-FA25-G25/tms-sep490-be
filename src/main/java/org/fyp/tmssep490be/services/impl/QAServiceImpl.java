package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.QAClassDetailDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.dtos.qa.QADashboardDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.CLO;
import org.fyp.tmssep490be.entities.CoursePhase;
import org.fyp.tmssep490be.entities.CourseSession;
import org.fyp.tmssep490be.entities.QAReport;
import org.fyp.tmssep490be.entities.Score;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.QAReportRepository;
import org.fyp.tmssep490be.repositories.ScoreRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.services.QAService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAServiceImpl implements QAService {

    private final ClassRepository classRepository;
    private final QAReportRepository qaReportRepository;
    private final ScoreRepository scoreRepository;
    private final SessionRepository sessionRepository;
    private final StudentFeedbackRepository studentFeedbackRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserBranchesRepository userBranchesRepository;

    // ========== Helper Methods for Business Logic ==========

    /**
     * Tính toán tỷ lệ điểm danh cho một lớp
     * Formula: (Số buổi có mặt / Tổng số buổi đã điểm danh) * 100
     */
    private double calculateAttendanceRate(Long classId) {
        try {
            List<Object[]> attendanceData = studentSessionRepository.getAttendanceSummaryByClassId(classId);

            long presentCount = 0;
            long totalCount = 0;

            for (Object[] row : attendanceData) {
                AttendanceStatus status = (AttendanceStatus) row[0];
                Long count = (Long) row[1];

                if (status == AttendanceStatus.PRESENT) {
                    presentCount += count;
                }
                totalCount += count;
            }

            if (totalCount == 0) {
                log.warn("No attendance data found for classId={}", classId);
                return 0.0;
            }

            double rate = (presentCount * 100.0) / totalCount;
            log.debug("Calculated attendance rate for classId={}: {}% (present={}, total={})",
                     classId, String.format("%.1f", rate), presentCount, totalCount);

            return rate;
        } catch (Exception e) {
            log.error("Error calculating attendance rate for classId={}: {}", classId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Tính toán tỷ lệ hoàn thành bài tập cho một lớp
     * Formula: (Số buổi hoàn thành bài tập / Tổng số buổi có bài tập) * 100
     */
    private double calculateHomeworkCompletionRate(Long classId) {
        try {
            List<Object[]> homeworkData = studentSessionRepository.getHomeworkSummaryByClassId(classId);

            long completedCount = 0;
            long totalCount = 0;

            for (Object[] row : homeworkData) {
                HomeworkStatus status = (HomeworkStatus) row[0];
                Long count = (Long) row[1];

                if (status == HomeworkStatus.COMPLETED) {
                    completedCount += count;
                }
                totalCount += count;
            }

            if (totalCount == 0) {
                log.warn("No homework data found for classId={}", classId);
                return 0.0;
            }

            double rate = (completedCount * 100.0) / totalCount;
            log.debug("Calculated homework completion rate for classId={}: {}% (completed={}, total={})",
                     classId, String.format("%.1f", rate), completedCount, totalCount);

            return rate;
        } catch (Exception e) {
            log.error("Error calculating homework completion rate for classId={}: {}", classId, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get user accessible branch IDs for permission filtering
     */
    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    /**
     * Tính toán tỷ lệ điểm danh trung bình cho nhiều lớp
     */
    private Map<Long, Double> calculateAttendanceRatesForClasses(List<Long> classIds) {
        Map<Long, Double> rates = new HashMap<>();

        if (classIds.isEmpty()) {
            return rates;
        }

        try {
            List<Object[]> attendanceData = studentSessionRepository.getAttendanceSummaryByClassIds(classIds);
            Map<Long, Long> presentCounts = new HashMap<>();
            Map<Long, Long> totalCounts = new HashMap<>();

            for (Object[] row : attendanceData) {
                Long classId = (Long) row[0];
                AttendanceStatus status = (AttendanceStatus) row[1];
                Long count = (Long) row[2];

                totalCounts.put(classId, totalCounts.getOrDefault(classId, 0L) + count);
                if (status == AttendanceStatus.PRESENT) {
                    presentCounts.put(classId, presentCounts.getOrDefault(classId, 0L) + count);
                }
            }

            for (Long classId : classIds) {
                long presentCount = presentCounts.getOrDefault(classId, 0L);
                long totalCount = totalCounts.getOrDefault(classId, 0L);

                if (totalCount > 0) {
                    double rate = (presentCount * 100.0) / totalCount;
                    rates.put(classId, rate);
                } else {
                    rates.put(classId, 0.0);
                }
            }
        } catch (Exception e) {
            log.error("Error calculating attendance rates for classes {}: {}", classIds, e.getMessage());
            classIds.forEach(classId -> rates.put(classId, 0.0));
        }

        return rates;
    }

    /**
     * Tính toán tỷ lệ hoàn thành bài tập trung bình cho nhiều lớp
     */
    private Map<Long, Double> calculateHomeworkRatesForClasses(List<Long> classIds) {
        Map<Long, Double> rates = new HashMap<>();

        if (classIds.isEmpty()) {
            return rates;
        }

        try {
            List<Object[]> homeworkData = studentSessionRepository.getHomeworkSummaryByClassIds(classIds);
            Map<Long, Long> completedCounts = new HashMap<>();
            Map<Long, Long> totalCounts = new HashMap<>();

            for (Object[] row : homeworkData) {
                Long classId = (Long) row[0];
                HomeworkStatus status = (HomeworkStatus) row[1];
                Long count = (Long) row[2];

                totalCounts.put(classId, totalCounts.getOrDefault(classId, 0L) + count);
                if (status == HomeworkStatus.COMPLETED) {
                    completedCounts.put(classId, completedCounts.getOrDefault(classId, 0L) + count);
                }
            }

            for (Long classId : classIds) {
                long completedCount = completedCounts.getOrDefault(classId, 0L);
                long totalCount = totalCounts.getOrDefault(classId, 0L);

                if (totalCount > 0) {
                    double rate = (completedCount * 100.0) / totalCount;
                    rates.put(classId, rate);
                } else {
                    rates.put(classId, 0.0);
                }
            }
        } catch (Exception e) {
            log.error("Error calculating homework rates for classes {}: {}", classIds, e.getMessage());
            classIds.forEach(classId -> rates.put(classId, 0.0));
        }

        return rates;
    }

    /**
     * Xác định lớp có rủi ro (dưới ngưỡng an toàn)
     * Risk thresholds: Attendance < 80% OR Homework < 70%
     */
    private String getRiskWarning(double attendanceRate, double homeworkRate) {
        if (attendanceRate < 80.0 && homeworkRate < 70.0) {
            return "Cảnh báo cao: Điểm danh < 80% và Bài tập < 70%";
        } else if (attendanceRate < 80.0) {
            return "Cảnh báo: Tỷ lệ điểm danh thấp < 80%";
        } else if (homeworkRate < 70.0) {
            return "Cảnh báo: Tỷ lệ hoàn thành bài tập thấp < 70%";
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public QADashboardDTO getQADashboard(List<Long> branchIds, LocalDate dateFrom, LocalDate dateTo, Long userId) {
        log.info("Getting QA dashboard for branchIds={}, dateFrom={}, dateTo={}", branchIds, dateFrom, dateTo);

        // Get user accessible branch IDs if not provided
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
        }

        // Get ongoing classes with branch filtering using existing method
        List<ClassEntity> ongoingClasses = classRepository.findClassesForAcademicAffairs(
                branchIds, null, ClassStatus.ONGOING, null, null, null,
                PageRequest.of(0, 1000)).getContent();

        int ongoingClassesCount = ongoingClasses.size();

        // Count QA reports created this month
        OffsetDateTime startOfMonth = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<QAReport> recentReports = qaReportRepository.findRecentReports(startOfMonth, PageRequest.of(0, 1000));
        int qaReportsCreatedThisMonth = recentReports.size();

        // Calculate real average attendance and homework completion rates
        List<Long> ongoingClassIds = ongoingClasses.stream()
                .map(ClassEntity::getId)
                .collect(Collectors.toList());

        Map<Long, Double> attendanceRates = calculateAttendanceRatesForClasses(ongoingClassIds);
        Map<Long, Double> homeworkRates = calculateHomeworkRatesForClasses(ongoingClassIds);

        double avgAttendanceRate = attendanceRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgHomeworkRate = homeworkRates.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        log.info("Calculated KPIs - Avg Attendance: {}%, Avg Homework: {}% for {} ongoing classes",
                String.format("%.1f", avgAttendanceRate), String.format("%.1f", avgHomeworkRate), ongoingClassesCount);

        QADashboardDTO.KPIMetrics kpiMetrics = QADashboardDTO.KPIMetrics.builder()
                .ongoingClassesCount(ongoingClassesCount)
                .qaReportsCreatedThisMonth(qaReportsCreatedThisMonth)
                .averageAttendanceRate(avgAttendanceRate)
                .averageHomeworkCompletionRate(avgHomeworkRate)
                .build();

        // Classes requiring attention (attendance < 80% OR homework < 70% OR no QA report)
        List<QADashboardDTO.ClassRequiringAttention> classesRequiringAttention = ongoingClasses.stream()
                .map(c -> {
                    double attendanceRate = attendanceRates.getOrDefault(c.getId(), 0.0);
                    double homeworkRate = homeworkRates.getOrDefault(c.getId(), 0.0);
                    long qaReportCount = qaReportRepository.countByClassEntityId(c.getId());

                    String warningReason = getRiskWarning(attendanceRate, homeworkRate);
                    if (warningReason == null && qaReportCount == 0) {
                        warningReason = "Chưa có QA report";
                    } else if (warningReason == null) {
                        warningReason = "Cần theo dõi";
                    }

                    return QADashboardDTO.ClassRequiringAttention.builder()
                            .classId(c.getId())
                            .classCode(c.getCode() != null ? c.getCode() : "N/A")
                            .courseName(c.getCourse() != null ? c.getCourse().getName() : "N/A")
                            .branchName(c.getBranch() != null ? c.getBranch().getName() : "N/A")
                            .attendanceRate(attendanceRate)
                            .homeworkCompletionRate(homeworkRate)
                            .qaReportCount((int) qaReportCount)
                            .warningReason(warningReason)
                            .build();
                })
                .filter(c -> c.getWarningReason() != null && !c.getWarningReason().equals("Cần theo dõi"))
                .sorted((a, b) -> {
                    // Sort by risk severity: both metrics low -> attendance low -> homework low
                    double aRisk = (100 - a.getAttendanceRate()) * 0.6 + (100 - a.getHomeworkCompletionRate()) * 0.4;
                    double bRisk = (100 - b.getAttendanceRate()) * 0.6 + (100 - b.getHomeworkCompletionRate()) * 0.4;
                    return Double.compare(bRisk, aRisk);
                })
                .limit(15)
                .collect(Collectors.toList());

        // Recent QA reports
        List<QADashboardDTO.QAReportSummary> qaReportSummaries = recentReports.stream()
                .limit(10)
                .map(r -> QADashboardDTO.QAReportSummary.builder()
                        .reportId(r.getId())
                        .reportType(r.getReportType())
                        .classId(r.getClassEntity().getId())
                        .classCode(r.getClassEntity().getCode() != null ? r.getClassEntity().getCode() : "N/A")
                        .sessionId(r.getSession() != null ? r.getSession().getId() : null)
                        .sessionDate(r.getSession() != null ? r.getSession().getDate().toString() : null)
                        .status(r.getStatus())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return QADashboardDTO.builder()
                .kpiMetrics(kpiMetrics)
                .classesRequiringAttention(classesRequiringAttention)
                .recentQAReports(qaReportSummaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QAClassListItemDTO> getQAClasses(List<Long> branchIds, String status, String search,
                                                   Pageable pageable, Long userId) {
        log.info("Getting QA classes list with branchIds={}, status={}, search={}", branchIds, status, search);

        // Get classes with pagination and filters
        Page<ClassEntity> classes = classRepository.findAll(pageable);

        // Calculate metrics for all classes in this page
        List<Long> classIds = classes.getContent().stream()
                .map(ClassEntity::getId)
                .collect(Collectors.toList());

        Map<Long, Double> attendanceRates = calculateAttendanceRatesForClasses(classIds);
        Map<Long, Double> homeworkRates = calculateHomeworkRatesForClasses(classIds);

        return classes.map(c -> {
            long totalSessions = sessionRepository.countByClassEntityId(c.getId());
            long completedSessions = sessionRepository.countByClassEntityIdExcludingCancelled(c.getId());
            long qaReportCount = qaReportRepository.countByClassEntityId(c.getId());

            double attendanceRate = attendanceRates.getOrDefault(c.getId(), 0.0);
            double homeworkRate = homeworkRates.getOrDefault(c.getId(), 0.0);

            log.debug("Class {} - Sessions: {}/{}, Attendance: {}%, Homework: {}%, QA Reports: {}",
                     c.getName(), completedSessions, totalSessions,
                     String.format("%.1f", attendanceRate), String.format("%.1f", homeworkRate), qaReportCount);

            return QAClassListItemDTO.builder()
                    .classId(c.getId())
                    .classCode(c.getCode() != null ? c.getCode() : "N/A")
                    .className(c.getName() != null ? c.getName() : "N/A")
                    .courseName(c.getCourse() != null ? c.getCourse().getName() : "N/A")
                    .branchName(c.getBranch() != null ? c.getBranch().getName() : "N/A")
                    .modality(c.getModality() != null ? c.getModality().name() : null)
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .startDate(c.getStartDate())
                    .totalSessions((int) totalSessions)
                    .completedSessions((int) completedSessions)
                    .attendanceRate(attendanceRate)
                    .homeworkCompletionRate(homeworkRate)
                    .qaReportCount((int) qaReportCount)
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public QAClassDetailDTO getQAClassDetail(Long classId, Long userId) {
        log.info("Getting QA class detail for classId={}", classId);

        // Get class entity
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        // Session summary
        long totalSessions = sessionRepository.countByClassEntityId(classId);
        long completedSessions = sessionRepository.countByClassEntityIdExcludingCancelled(classId); // Use existing method
        List<Session> classSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
        long cancelledSessions = classSessions.stream()
            .filter(s -> s.getStatus() == SessionStatus.CANCELLED)
            .count();
        long upcomingSessions = classSessions.stream()
            .filter(s -> s.getStatus() == SessionStatus.PLANNED)
            .count();

        // Get next session date using classSessions list
        LocalDate nextSessionDate = classSessions.stream()
            .filter(s -> s.getStatus() == SessionStatus.PLANNED)
            .map(Session::getDate)
            .filter(d -> d.isAfter(LocalDate.now()) || d.isEqual(LocalDate.now()))
            .sorted()
            .findFirst()
            .orElse(null);

        QAClassDetailDTO.SessionSummary sessionSummary = QAClassDetailDTO.SessionSummary.builder()
                .totalSessions((int) totalSessions)
                .completedSessions((int) completedSessions)
                .upcomingSessions((int) upcomingSessions)
                .cancelledSessions((int) cancelledSessions)
                .nextSessionDate(nextSessionDate)
                .build();

        // Calculate real performance metrics for this class
        double classAttendanceRate = calculateAttendanceRate(classId);
        double classHomeworkRate = calculateHomeworkCompletionRate(classId);

        // Calculate total absences and students at risk
        List<StudentSession> classStudentSessions = studentSessionRepository.findByClassIdWithSessionAndStudent(classId);

        // Count absences (excluding cancelled sessions)
        long totalAbsences = classStudentSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();

        // Count unique students at risk (attendance < 80% OR homework < 70%)
        Map<Long, List<StudentSession>> studentSessionsMap = classStudentSessions.stream()
                .collect(Collectors.groupingBy(ss -> ss.getStudent().getId()));

        long studentsAtRisk = studentSessionsMap.entrySet().stream()
                .filter(entry -> {
                    Long studentId = entry.getKey();
                    List<StudentSession> sessions = entry.getValue();

                    long presentCount = sessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                            .count();

                    long completedHomeworkCount = sessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                            .count();

                    long totalCount = sessions.size();
                    long homeworkTotalCount = sessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() != null && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK)
                            .count();

                    double studentAttendanceRate = totalCount > 0 ? (presentCount * 100.0 / totalCount) : 0.0;
                    double studentHomeworkRate = homeworkTotalCount > 0 ? (completedHomeworkCount * 100.0 / homeworkTotalCount) : 100.0;

                    return studentAttendanceRate < 80.0 || studentHomeworkRate < 70.0;
                })
                .count();

        QAClassDetailDTO.QAPerformanceMetrics performanceMetrics = QAClassDetailDTO.QAPerformanceMetrics.builder()
                .attendanceRate(classAttendanceRate)
                .homeworkCompletionRate(classHomeworkRate)
                .totalAbsences((int) totalAbsences)
                .studentsAtRisk((int) studentsAtRisk)
                .build();

        log.info("Class {} Performance - Attendance: {}%, Homework: {}%, Absences: {}, Students at Risk: {}",
                 classEntity.getName(),
                 String.format("%.1f", classAttendanceRate),
                 String.format("%.1f", classHomeworkRate),
                 totalAbsences,
                 studentsAtRisk);

        // Get QA reports for this class
        List<QAReport> qaReports = qaReportRepository.findByClassEntityId(classId).stream()
            .sorted((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()))
            .collect(Collectors.toList());
        List<QAClassDetailDTO.QAReportSummary> qaReportSummaries = qaReports.stream()
                .map(r -> {
                    String reportLevel = "Class";
                    if (r.getSession() != null) {
                        reportLevel = "Session";
                    } else if (r.getPhase() != null) {
                        reportLevel = "Phase";
                    }

                    return QAClassDetailDTO.QAReportSummary.builder()
                            .reportId(r.getId())
                            .reportType(r.getReportType())
                            .reportLevel(reportLevel)
                            .status(r.getStatus())
                            .createdAt(r.getCreatedAt())
                            .reportedByName(r.getReportedBy() != null ? r.getReportedBy().getFullName() : "Unknown")
                            .build();
                })
                .collect(Collectors.toList());

        // Teachers (placeholder - would need ClassTeacher entity)
        List<QAClassDetailDTO.TeacherInfo> teachers = new ArrayList<>();

        return QAClassDetailDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                .className(classEntity.getName() != null ? classEntity.getName() : "N/A")
                .courseName(classEntity.getCourse() != null ? classEntity.getCourse().getName() : "N/A")
                .courseId(classEntity.getCourse() != null ? classEntity.getCourse().getId() : null)
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : "N/A")
                .branchId(classEntity.getBranch() != null ? classEntity.getBranch().getId() : null)
                .modality(classEntity.getModality() != null ? classEntity.getModality().name() : null)
                .status(classEntity.getStatus() != null ? classEntity.getStatus().name() : null)
                .startDate(classEntity.getStartDate())
                .endDate(classEntity.getActualEndDate() != null ? classEntity.getActualEndDate() : classEntity.getPlannedEndDate())
                .maxCapacity(classEntity.getMaxCapacity())
                .currentEnrollment(classEntity.getEnrollments() != null ? classEntity.getEnrollments().size() : 0)
                .sessionSummary(sessionSummary)
                .performanceMetrics(performanceMetrics)
                .qaReports(qaReportSummaries)
                .teachers(teachers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public QASessionListResponse getQASessionList(Long classId, Long userId) {
        log.info("Getting QA session list for classId={}", classId);

        // Get class entity
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        // Get all sessions for this class using existing method
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        // Map to QASessionItemDTO with real metrics
        List<QASessionListResponse.QASessionItemDTO> sessionItems = sessions.stream()
                .map(s -> {
                    // Get real student session data for this session
                    List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(s.getId());

                    // Calculate real attendance metrics
                    long presentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                            .count();

                    long absentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                            .count();

                    long totalStudents = studentSessions.size();
                    double attendanceRate = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;

                    // Calculate real homework completion metrics (exclude NO_HOMEWORK)
                    long homeworkCompletedCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                            .count();

                    long homeworkTotalCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() != null && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK)
                            .count();

                    double homeworkCompletionRate = homeworkTotalCount > 0 ? (homeworkCompletedCount * 100.0 / homeworkTotalCount) : 0.0;

                    // Check QA reports for this session using existing method
                    long qaReportCount = qaReportRepository.findWithFilters(
                        null, s.getId(), null, null, null, null, Pageable.unpaged()).getTotalElements();

                    // Get session info from related entities
                    Integer sequenceNumber = s.getCourseSession() != null ? s.getCourseSession().getSequenceNo() : null;
                    String timeSlot = s.getTimeSlotTemplate() != null ? s.getTimeSlotTemplate().getName() : "TBA";
                    String topic = s.getCourseSession() != null ? s.getCourseSession().getTopic() : "N/A";

                    // Get teacher from teaching slots
                    String teacherName = s.getTeachingSlots() != null && !s.getTeachingSlots().isEmpty()
                        ? s.getTeachingSlots().stream()
                            .findFirst()
                            .map(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null
                                ? ts.getTeacher().getUserAccount().getFullName()
                                : "TBA")
                            .orElse("TBA")
                        : "TBA";

                    log.debug("Session {} - Students: {}, Present: {}, Absent: {}, Attendance: {}%, Homework: {}%",
                             s.getId(), totalStudents, presentCount, absentCount,
                             String.format("%.1f", attendanceRate), String.format("%.1f", homeworkCompletionRate));

                    return QASessionListResponse.QASessionItemDTO.builder()
                            .sessionId(s.getId())
                            .sequenceNumber(sequenceNumber)
                            .date(s.getDate())
                            .dayOfWeek(s.getDate() != null ? s.getDate().getDayOfWeek().name() : null)
                            .timeSlot(timeSlot)
                            .topic(topic)
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .teacherName(teacherName)
                            .totalStudents((int) totalStudents)
                            .presentCount((int) presentCount)
                            .absentCount((int) absentCount)
                            .attendanceRate(attendanceRate)
                            .homeworkCompletedCount((int) homeworkCompletedCount)
                            .homeworkCompletionRate(homeworkCompletionRate)
                            .hasQAReport(qaReportCount > 0)
                            .qaReportCount((int) qaReportCount)
                            .build();
                })
                .collect(Collectors.toList());

        return QASessionListResponse.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                .totalSessions(sessions.size())
                .sessions(sessionItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionDetailDTO getQASessionDetail(Long sessionId, Long userId) {
        log.info("Getting QA session detail for sessionId={} by userId={}", sessionId, userId);

        // Get session with full details
        Session session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại với id: " + sessionId));

        ClassEntity classEntity = session.getClassEntity();

        // Get student sessions for attendance and homework data
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);

        // Build student attendance list
        List<SessionDetailDTO.StudentAttendanceDTO> studentAttendanceList = studentSessions.stream()
                .map(ss -> SessionDetailDTO.StudentAttendanceDTO.builder()
                        .studentId(ss.getStudent().getId())
                        .studentCode(ss.getStudent().getStudentCode())
                        .studentName(ss.getStudent().getUserAccount().getFullName())
                        .attendanceStatus(ss.getAttendanceStatus() != null ? ss.getAttendanceStatus().name() : null)
                        .homeworkStatus(ss.getHomeworkStatus() != null ? ss.getHomeworkStatus().name() : null)
                        .isMakeup(ss.getIsMakeup())
                        .note(ss.getNote())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        // Calculate attendance and homework statistics
        long totalStudents = studentSessions.size();
        long presentCount = studentSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();
        long absentCount = studentSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();
        long homeworkCompletedCount = studentSessions.stream()
                .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                .count();
        long homeworkTotalCount = studentSessions.stream()
                .filter(ss -> ss.getHomeworkStatus() != null && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK)
                .count();

        double attendanceRate = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;
        double homeworkCompletionRate = homeworkTotalCount > 0 ? (homeworkCompletedCount * 100.0 / homeworkTotalCount) : 0.0;

        SessionDetailDTO.AttendanceStats attendanceStats = SessionDetailDTO.AttendanceStats.builder()
                .totalStudents((int) totalStudents)
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .attendanceRate(attendanceRate)
                .homeworkCompletedCount((int) homeworkCompletedCount)
                .homeworkCompletionRate(homeworkCompletionRate)
                .build();

        // Get CLO information if available
        List<SessionDetailDTO.CLOInfo> closCovered = new ArrayList<>();
        if (session.getCourseSession() != null && session.getCourseSession().getPhase() != null) {
            // Get CLOs from course phase
            CoursePhase phase = session.getCourseSession().getPhase();
            if (phase.getCourse() != null) {
                closCovered = phase.getCourse().getClos().stream()
                        .map(clo -> SessionDetailDTO.CLOInfo.builder()
                                .cloId(clo.getId())
                                .cloCode(clo.getCode())
                                .description(clo.getDescription())
                                .build())
                        .collect(java.util.stream.Collectors.toList());
            }
        }

        // Get student feedback summary
        long feedbackSubmissions = studentFeedbackRepository.countBySessionId(sessionId);
        double feedbackRate = totalStudents > 0 ? (feedbackSubmissions * 100.0 / totalStudents) : 0.0;

        // Get detailed feedback data for average rating and common feedback
        List<org.fyp.tmssep490be.entities.StudentFeedback> sessionFeedbacks =
                studentFeedbackRepository.findBySessionIdWithDetails(sessionId);

        Double averageRating = sessionFeedbacks.stream()
                .filter(sf -> sf.getResponse() != null && !sf.getResponse().trim().isEmpty())
                .mapToDouble(sf -> {
                    // Simple rating extraction from feedback text (if numeric)
                    try {
                        String response = sf.getResponse().toLowerCase();
                        if (response.contains("1") || response.contains("kém")) return 1.0;
                        if (response.contains("2") || response.contains("tb")) return 2.0;
                        if (response.contains("3") || response.contains("khá")) return 3.0;
                        if (response.contains("4") || response.contains("tốt")) return 4.0;
                        if (response.contains("5") || response.contains("rất tốt")) return 5.0;
                        return 3.0; // Default rating
                    } catch (Exception e) {
                        return 3.0;
                    }
                })
                .average()
                .orElse(0.0);

        String commonFeedback = sessionFeedbacks.stream()
                .filter(sf -> sf.getResponse() != null && sf.getResponse().length() > 10)
                .map(org.fyp.tmssep490be.entities.StudentFeedback::getResponse)
                .collect(java.util.stream.Collectors.joining("; "));

        String teacherName = session.getTeachingSlots() != null && !session.getTeachingSlots().isEmpty()
                ? session.getTeachingSlots().stream()
                        .findFirst()
                        .map(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null
                                ? ts.getTeacher().getUserAccount().getFullName()
                                : "TBA")
                        .orElse("TBA")
                : "TBA";

        // Get session info from related entities
        Integer sequenceNumber = session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null;
        String timeSlot = session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : "TBA";
        String topic = session.getCourseSession() != null ? session.getCourseSession().getTopic() : "N/A";

        log.info("Session {} detail - Class: {}, Students: {}, Present: {}, Attendance: {}%, Homework: {}%, Feedback: {}",
                sessionId, classEntity.getCode() != null ? classEntity.getCode() : "N/A",
                totalStudents, presentCount,
                String.format("%.1f", attendanceRate),
                String.format("%.1f", homeworkCompletionRate),
                feedbackSubmissions);

        SessionDetailDTO.StudentFeedbackSummary feedbackSummary = SessionDetailDTO.StudentFeedbackSummary.builder()
                .totalStudents((int) totalStudents)
                .feedbackSubmissions((int) feedbackSubmissions)
                .feedbackRate(feedbackRate)
                .averageRating(averageRating)
                .commonFeedback(commonFeedback)
                .build();

        return SessionDetailDTO.builder()
                .sessionId(session.getId())
                .classId(classEntity.getId())
                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                .courseName(classEntity.getCourse() != null ? classEntity.getCourse().getName() : "N/A")
                .date(session.getDate())
                .timeSlot(timeSlot)
                .topic(topic)
                .studentTask(session.getCourseSession() != null ? session.getCourseSession().getStudentTask() : null)
                .status(session.getStatus() != null ? session.getStatus().name() : null)
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .attendanceStats(attendanceStats)
                .students(studentAttendanceList)
                .closCovered(closCovered)
                .studentFeedbackSummary(feedbackSummary)
                .build();
    }
}
