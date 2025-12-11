package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.QAClassDetailDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.InvalidRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAService {

    private final ClassRepository classRepository;
    private final QAReportRepository qaReportRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final TeachingSlotRepository teachingSlotRepository;

    @Transactional(readOnly = true)
    public Page<QAClassListItemDTO> getQAClasses(
            List<Long> branchIds,
            String status,
            String search,
            Pageable pageable,
            Long userId
    ) {
        log.info("Getting QA classes list: branchIds={}, status={}, search={}", branchIds, status, search);

        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
            if (branchIds.isEmpty()) {
                throw new InvalidRequestException("Bạn không được phân công chi nhánh nào");
            }
        }

        ClassStatus classStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                classStatus = ClassStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid class status: {}. Ignoring filter.", status);
            }
        }

        Page<ClassEntity> classes = classRepository.findClassesForAcademicAffairs(
                branchIds, null, classStatus, null, null, search, pageable
        );

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

            return QAClassListItemDTO.builder()
                    .classId(c.getId())
                    .classCode(c.getCode() != null ? c.getCode() : "N/A")
                    .className(c.getName() != null ? c.getName() : "N/A")
                    .courseId(c.getSubject() != null ? c.getSubject().getId() : null)
                    .courseName(c.getSubject() != null ? c.getSubject().getName() : "N/A")
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

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    private Map<Long, Double> calculateAttendanceRatesForClasses(List<Long> classIds) {
        Map<Long, Double> rates = new HashMap<>();
        if (classIds.isEmpty()) {
            return rates;
        }

        try {
            List<Object[]> data = studentSessionRepository.getAttendanceSummaryByClassIds(classIds);
            Map<Long, Long> presentCounts = new HashMap<>();
            Map<Long, Long> totalCounts = new HashMap<>();

            for (Object[] row : data) {
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
                rates.put(classId, totalCount > 0 ? (presentCount * 100.0) / totalCount : 0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating attendance rates for classes {}: {}", classIds, e.getMessage());
            classIds.forEach(classId -> rates.put(classId, 0.0));
        }

        return rates;
    }

    private Map<Long, Double> calculateHomeworkRatesForClasses(List<Long> classIds) {
        Map<Long, Double> rates = new HashMap<>();
        if (classIds.isEmpty()) {
            return rates;
        }

        try {
            List<Object[]> data = studentSessionRepository.getHomeworkSummaryByClassIds(classIds);
            Map<Long, Long> completedCounts = new HashMap<>();
            Map<Long, Long> totalCounts = new HashMap<>();

            for (Object[] row : data) {
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
                rates.put(classId, totalCount > 0 ? (completedCount * 100.0) / totalCount : 0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating homework rates for classes {}: {}", classIds, e.getMessage());
            classIds.forEach(classId -> rates.put(classId, 0.0));
        }

        return rates;
    }

    @Transactional(readOnly = true)
    public QAClassDetailDTO getQAClassDetail(Long classId, Long userId) {
        log.info("Getting QA class detail for classId={}", classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId));

        ensureUserHasAccessToClass(classEntity, userId);

        long totalSessions = sessionRepository.countByClassEntityId(classId);
        long completedSessions = sessionRepository.countByClassEntityIdExcludingCancelled(classId);
        List<Session> classSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
        
        long cancelledSessions = classSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.CANCELLED)
                .count();
        long upcomingSessions = classSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.PLANNED)
                .count();

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

        double classAttendanceRate = calculateAttendanceRate(classId);
        double classHomeworkRate = calculateHomeworkCompletionRate(classId);

        List<StudentSession> classStudentSessions = studentSessionRepository.findByClassIdWithSessionAndStudent(classId);

        long totalAbsences = classStudentSessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();

        Map<Long, List<StudentSession>> studentSessionsMap = classStudentSessions.stream()
                .collect(Collectors.groupingBy(ss -> ss.getStudent().getId()));

        long studentsAtRisk = studentSessionsMap.entrySet().stream()
                .filter(entry -> {
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

        List<QAReport> qaReports = qaReportRepository.findByClassEntityIdOrderByCreatedAtDesc(classId);
        List<QAClassDetailDTO.QAReportSummary> qaReportSummaries = qaReports.stream()
                .limit(10)
                .map(report -> QAClassDetailDTO.QAReportSummary.builder()
                        .reportId(report.getId())
                        .reportType(report.getReportType() != null ? report.getReportType().name() : null)
                        .reportLevel(determineReportLevel(report))
                        .status(report.getStatus() != null ? report.getStatus().name() : null)
                        .createdAt(report.getCreatedAt())
                        .reportedByName(report.getReportedBy() != null 
                                ? report.getReportedBy().getFullName() 
                                : "Unknown")
                        .build())
                .collect(Collectors.toList());

        List<TeachingSlot> teachingSlots = teachingSlotRepository.findBySessionClassEntityId(classId);
        Map<Long, TeacherStats> teacherStatsMap = new HashMap<>();

        for (TeachingSlot slot : teachingSlots) {
            if (slot.getTeacher() == null) continue;
            
            Long teacherId = slot.getTeacher().getId();
            teacherStatsMap.putIfAbsent(teacherId, new TeacherStats(
                slot.getTeacher().getUserAccount() != null 
                    ? slot.getTeacher().getUserAccount().getFullName() 
                    : "Unknown"));
            
            TeacherStats stats = teacherStatsMap.get(teacherId);
            stats.sessionsAssigned++;
            
            if (slot.getSession() != null && slot.getSession().getStatus() == SessionStatus.DONE) {
                stats.sessionsCompleted++;
            }
        }

        List<QAClassDetailDTO.TeacherInfo> teachers = teacherStatsMap.entrySet().stream()
                .map(entry -> QAClassDetailDTO.TeacherInfo.builder()
                        .teacherId(entry.getKey())
                        .teacherName(entry.getValue().name)
                        .sessionsAssigned(entry.getValue().sessionsAssigned)
                        .sessionsCompleted(entry.getValue().sessionsCompleted)
                        .build())
                .collect(Collectors.toList());

        return QAClassDetailDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                .className(classEntity.getName() != null ? classEntity.getName() : "N/A")
                .courseId(classEntity.getSubject() != null ? classEntity.getSubject().getId() : null)
                .courseName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : "N/A")
                .branchId(classEntity.getBranch() != null ? classEntity.getBranch().getId() : null)
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : "N/A")
                .modality(classEntity.getModality() != null ? classEntity.getModality().name() : null)
                .status(classEntity.getStatus() != null ? classEntity.getStatus().name() : null)
                .startDate(classEntity.getStartDate())
                .endDate(classEntity.getActualEndDate() != null ? classEntity.getActualEndDate() : classEntity.getPlannedEndDate())
                .maxCapacity(classEntity.getMaxCapacity())
                .currentEnrollment(classEntity.getEnrollments() != null ? (int) classEntity.getEnrollments().stream()
                        .filter(e -> e.getStatus() == org.fyp.tmssep490be.entities.enums.EnrollmentStatus.ENROLLED)
                        .count() : 0)
                .sessionSummary(sessionSummary)
                .performanceMetrics(performanceMetrics)
                .qaReports(qaReportSummaries)
                .teachers(teachers)
                .build();
    }

    @Transactional(readOnly = true)
    public QASessionListResponse getQASessionList(Long classId, Long userId) {
        log.info("Getting QA session list for classId={}", classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + classId));

        ensureUserHasAccessToClass(classEntity, userId);

        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        List<QASessionListResponse.QASessionItemDTO> sessionItems = sessions.stream()
                .map(s -> {
                    List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(s.getId());

                    long presentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                            .count();

                    long absentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                            .count();

                    long totalStudents = studentSessions.size();
                    double attendanceRate = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;

                    long homeworkCompletedCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                            .count();

                    long homeworkTotalCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() != null && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK)
                            .count();

                    double homeworkCompletionRate = homeworkTotalCount > 0 ? (homeworkCompletedCount * 100.0 / homeworkTotalCount) : 0.0;

                    long qaReportCount = qaReportRepository.countBySessionId(s.getId());

                    Integer sequenceNumber = s.getSubjectSession() != null ? s.getSubjectSession().getSequenceNo() : null;
                    String timeSlot = s.getTimeSlotTemplate() != null ? s.getTimeSlotTemplate().getName() : "TBA";
                    String topic = s.getSubjectSession() != null ? s.getSubjectSession().getTopic() : "N/A";

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
                            .totalStudents((int) totalStudents)
                            .presentCount((int) presentCount)
                            .absentCount((int) absentCount)
                            .homeworkCompletedCount((int) homeworkCompletedCount)
                            .hasQAReport(qaReportCount > 0)
                            .qaReportCount((int) qaReportCount)
                            .build();
                })
                .collect(Collectors.toList());

        return QASessionListResponse.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .totalSessions(sessions.size())
                .sessions(sessionItems)
                .build();
    }

    private void ensureUserHasAccessToClass(ClassEntity classEntity, Long userId) {
        List<Long> userBranches = getUserAccessibleBranches(userId);
        if (!userBranches.contains(classEntity.getBranch().getId())) {
            throw new InvalidRequestException("Bạn không có quyền truy cập lớp học này");
        }
    }

    private double calculateAttendanceRate(Long classId) {
        List<Object[]> data = studentSessionRepository.getAttendanceSummaryByClassId(classId);
        
        long presentCount = 0;
        long totalCount = 0;

        for (Object[] row : data) {
            AttendanceStatus status = (AttendanceStatus) row[0];
            Long count = (Long) row[1];
            
            totalCount += count;
            if (status == AttendanceStatus.PRESENT) {
                presentCount += count;
            }
        }

        return totalCount > 0 ? (presentCount * 100.0 / totalCount) : 0.0;
    }

    private double calculateHomeworkCompletionRate(Long classId) {
        List<Object[]> data = studentSessionRepository.getHomeworkSummaryByClassId(classId);
        
        long completedCount = 0;
        long totalCount = 0;

        for (Object[] row : data) {
            HomeworkStatus status = (HomeworkStatus) row[0];
            Long count = (Long) row[1];
            
            totalCount += count;
            if (status == HomeworkStatus.COMPLETED) {
                completedCount += count;
            }
        }

        return totalCount > 0 ? (completedCount * 100.0 / totalCount) : 0.0;
    }

    private String determineReportLevel(QAReport report) {
        if (report.getPhase() != null) return "Phase";
        if (report.getSession() != null) return "Session";
        return "Class";
    }

    private static class TeacherStats {
        String name;
        int sessionsAssigned = 0;
        int sessionsCompleted = 0;

        TeacherStats(String name) {
            this.name = name;
        }
    }
}
