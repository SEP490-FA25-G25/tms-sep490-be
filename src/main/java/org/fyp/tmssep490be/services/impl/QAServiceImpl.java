package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.QAClassDetailDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.dtos.qa.QADashboardDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.QAReport;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.QAReportRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.QAService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAServiceImpl implements QAService {

    private final ClassRepository classRepository;
    private final QAReportRepository qaReportRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public QADashboardDTO getQADashboard(List<Long> branchIds, LocalDate dateFrom, LocalDate dateTo, Long userId) {
        log.info("Getting QA dashboard for branchIds={}, dateFrom={}, dateTo={}", branchIds, dateFrom, dateTo);

        // Get ongoing classes
        List<ClassEntity> ongoingClasses = classRepository.findAll().stream()
                .filter(c -> c.getStatus() == ClassStatus.ONGOING)
                .filter(c -> branchIds == null || branchIds.isEmpty() || branchIds.contains(c.getBranch().getId()))
                .collect(Collectors.toList());

        int ongoingClassesCount = ongoingClasses.size();

        // Count QA reports created this month
        OffsetDateTime startOfMonth = OffsetDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<QAReport> recentReports = qaReportRepository.findRecentReports(startOfMonth, PageRequest.of(0, 1000));
        int qaReportsCreatedThisMonth = recentReports.size();

        // Calculate average attendance and homework completion rates (simplified)
        double avgAttendanceRate = 85.0; // Placeholder
        double avgHomeworkRate = 75.0; // Placeholder

        QADashboardDTO.KPIMetrics kpiMetrics = QADashboardDTO.KPIMetrics.builder()
                .ongoingClassesCount(ongoingClassesCount)
                .qaReportsCreatedThisMonth(qaReportsCreatedThisMonth)
                .averageAttendanceRate(avgAttendanceRate)
                .averageHomeworkCompletionRate(avgHomeworkRate)
                .build();

        // Classes requiring attention (attendance < 80% OR no QA report)
        List<QADashboardDTO.ClassRequiringAttention> classesRequiringAttention = ongoingClasses.stream()
                .limit(10)
                .map(c -> {
                    long qaReportCount = qaReportRepository.countByClassEntityId(c.getId());
                    String warningReason = qaReportCount == 0 ? "Chua co QA report" : "Can theo doi";

                    return QADashboardDTO.ClassRequiringAttention.builder()
                            .classId(c.getId())
                            .classCode(c.getName())
                            .courseName(c.getCourse() != null ? c.getCourse().getName() : "N/A")
                            .branchName(c.getBranch() != null ? c.getBranch().getName() : "N/A")
                            .attendanceRate(80.0) // Placeholder
                            .qaReportCount((int) qaReportCount)
                            .warningReason(warningReason)
                            .build();
                })
                .collect(Collectors.toList());

        // Recent QA reports
        List<QADashboardDTO.QAReportSummary> qaReportSummaries = recentReports.stream()
                .limit(10)
                .map(r -> QADashboardDTO.QAReportSummary.builder()
                        .reportId(r.getId())
                        .reportType(r.getReportTypeEnum() != null ?
                                   r.getReportTypeEnum().getDisplayName() : r.getReportType())
                        .classId(r.getClassEntity().getId())
                        .classCode(r.getClassEntity().getName())
                        .sessionId(r.getSession() != null ? r.getSession().getId() : null)
                        .sessionDate(r.getSession() != null ? r.getSession().getDate().toString() : null)
                        .status(r.getStatusEnum() != null ?
                                r.getStatusEnum().getDisplayName() : r.getStatus())
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
        log.info("Getting QA classes list");

        // Simplified version - just get all classes and filter
        Page<ClassEntity> classes = classRepository.findAll(pageable);

        return classes.map(c -> {
            long totalSessions = sessionRepository.countByClassEntityId(c.getId());
            long completedSessions = sessionRepository.countByClassEntityIdExcludingCancelled(c.getId());
            long qaReportCount = qaReportRepository.countByClassEntityId(c.getId());

            return QAClassListItemDTO.builder()
                    .classId(c.getId())
                    .classCode(c.getName())
                    .className(c.getCourse() != null ? c.getCourse().getName() : "N/A")
                    .courseName(c.getCourse() != null ? c.getCourse().getName() : "N/A")
                    .branchName(c.getBranch() != null ? c.getBranch().getName() : "N/A")
                    .modality(c.getModality() != null ? c.getModality().name() : null)
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .startDate(c.getStartDate())
                    .totalSessions((int) totalSessions)
                    .completedSessions((int) completedSessions)
                    .attendanceRate(85.0) // Placeholder
                    .homeworkCompletionRate(75.0) // Placeholder
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
        long cancelledSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getClassEntity().getId().equals(classId) && s.getStatus() == SessionStatus.CANCELLED)
            .count();
        long upcomingSessions = sessionRepository.findAll().stream()
            .filter(s -> s.getClassEntity().getId().equals(classId) && s.getStatus() == SessionStatus.PLANNED)
            .count();

        // Get next session date
        LocalDate nextSessionDate = sessionRepository.findAll().stream()
            .filter(s -> s.getClassEntity().getId().equals(classId))
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

        // Performance metrics (placeholders)
        QAClassDetailDTO.QAPerformanceMetrics performanceMetrics = QAClassDetailDTO.QAPerformanceMetrics.builder()
                .attendanceRate(85.0)
                .homeworkCompletionRate(75.0)
                .totalAbsences(15)
                .studentsAtRisk(3)
                .build();

        // Get QA reports for this class
        List<QAReport> qaReports = qaReportRepository.findAll().stream()
            .filter(r -> r.getClassEntity().getId().equals(classId))
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
                            .reportType(r.getReportTypeEnum() != null ?
                                       r.getReportTypeEnum().getDisplayName() : r.getReportType())
                            .reportLevel(reportLevel)
                            .status(r.getStatusEnum() != null ?
                                    r.getStatusEnum().getDisplayName() : r.getStatus())
                            .createdAt(r.getCreatedAt())
                            .reportedByName(r.getReportedBy() != null ? r.getReportedBy().getFullName() : "Unknown")
                            .build();
                })
                .collect(Collectors.toList());

        // Teachers (placeholder - would need ClassTeacher entity)
        List<QAClassDetailDTO.TeacherInfo> teachers = new ArrayList<>();

        return QAClassDetailDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getName())
                .className(classEntity.getCourse() != null ? classEntity.getCourse().getName() : "N/A")
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

        // Get all sessions for this class
        List<Session> sessions = sessionRepository.findAll().stream()
            .filter(s -> s.getClassEntity().getId().equals(classId))
            .sorted((s1, s2) -> s1.getDate().compareTo(s2.getDate()))
            .collect(Collectors.toList());

        // Map to QASessionItemDTO with metrics
        List<QASessionListResponse.QASessionItemDTO> sessionItems = sessions.stream()
                .map(s -> {
                    // Calculate attendance metrics (placeholders)
                    int totalStudents = classEntity.getEnrollments() != null ? classEntity.getEnrollments().size() : 0;
                    int presentCount = (int) (totalStudents * 0.85); // Placeholder
                    int absentCount = totalStudents - presentCount;
                    double attendanceRate = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;

                    int homeworkCompletedCount = (int) (totalStudents * 0.75); // Placeholder
                    double homeworkCompletionRate = totalStudents > 0 ? (homeworkCompletedCount * 100.0 / totalStudents) : 0.0;

                    // Check QA reports for this session
                    long qaReportCount = qaReportRepository.findAll().stream()
                        .filter(r -> r.getSession() != null && r.getSession().getId().equals(s.getId()))
                        .count();

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

                    return QASessionListResponse.QASessionItemDTO.builder()
                            .sessionId(s.getId())
                            .sequenceNumber(sequenceNumber)
                            .date(s.getDate())
                            .dayOfWeek(s.getDate() != null ? s.getDate().getDayOfWeek().name() : null)
                            .timeSlot(timeSlot)
                            .topic(topic)
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .teacherName(teacherName)
                            .totalStudents(totalStudents)
                            .presentCount(presentCount)
                            .absentCount(absentCount)
                            .attendanceRate(attendanceRate)
                            .homeworkCompletedCount(homeworkCompletedCount)
                            .homeworkCompletionRate(homeworkCompletionRate)
                            .hasQAReport(qaReportCount > 0)
                            .qaReportCount((int) qaReportCount)
                            .build();
                })
                .collect(Collectors.toList());

        return QASessionListResponse.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getName())
                .totalSessions(sessions.size())
                .sessions(sessionItems)
                .build();
    }
}
