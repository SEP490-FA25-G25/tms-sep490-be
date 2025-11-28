package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.exceptions.InvalidRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.QAReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAReportServiceImpl implements QAReportService {

    private final QAReportRepository qaReportRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final CoursePhaseRepository coursePhaseRepository;
    private final UserAccountRepository userAccountRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserBranchesRepository userBranchesRepository;

    // ========== Helper Methods ==========

    /**
     * Get user accessible branch IDs for permission filtering
     */
    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    // ========== Enhanced Validation Methods ==========

    /**
     * Kiểm tra xem lớp có đủ dữ liệu để tạo QA report không
     * - Session report: Cần có session đã hoàn thành với dữ liệu điểm danh và bài tập
     * - Phase report: Cần có ít nhất 1 session đã hoàn thành
     * - Class report: Cần có ít nhất 1 session đã hoàn thành
     */
    private void validateClassForQAReport(Long classId, QAReportType reportType, Long sessionId, Long phaseId) {
        if (reportType == QAReportType.CLASSROOM_OBSERVATION && sessionId != null) {
            // Validate session exists and is completed
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));

            if (!session.getClassEntity().getId().equals(classId)) {
                throw new InvalidRequestException("Session không thuộc class này");
            }

            if (session.getStatus() != org.fyp.tmssep490be.entities.enums.SessionStatus.DONE) {
                throw new InvalidRequestException("Session chưa hoàn thành, không thể tạo QA report");
            }

            // Check if session has student data
            long studentSessionCount = studentSessionRepository.countBySessionId(sessionId);
            if (studentSessionCount == 0) {
                throw new InvalidRequestException("Session không có dữ liệu học sinh, không thể tạo QA report");
            }

            log.info("Validated session {} for QA report - Students: {}", sessionId, studentSessionCount);
        } else if (reportType == QAReportType.PHASE_REVIEW && phaseId != null) {
            // Validate phase exists
            CoursePhase phase = coursePhaseRepository.findById(phaseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Phase không tồn tại"));

            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class không tồn tại"));

            if (!phase.getCourse().getId().equals(classEntity.getCourse().getId())) {
                throw new InvalidRequestException("Phase không thuộc course của class này");
            }

            // Check if phase has completed sessions
            long completedSessionsInPhase = sessionRepository.findByClassId(classId).stream()
                    .filter(s -> s.getCourseSession() != null &&
                               s.getCourseSession().getPhase().getId().equals(phaseId) &&
                               s.getStatus() == org.fyp.tmssep490be.entities.enums.SessionStatus.DONE)
                    .count();

            if (completedSessionsInPhase == 0) {
                throw new InvalidRequestException("Phase không có session nào hoàn thành, không thể tạo QA report");
            }

            log.info("Validated phase {} for QA report - Completed sessions: {}", phaseId, completedSessionsInPhase);
        } else {
            // Validate class has completed sessions
            long completedSessions = sessionRepository.findByClassId(classId).stream()
                    .filter(s -> s.getStatus() == org.fyp.tmssep490be.entities.enums.SessionStatus.DONE)
                    .count();

            if (completedSessions == 0) {
                throw new InvalidRequestException("Class không có session nào hoàn thành, không thể tạo QA report");
            }

            log.info("Validated class {} for QA report - Completed sessions: {}", classId, completedSessions);
        }
    }

    /**
     * Kiểm tra xem user có quyền tạo QA report cho lớp này không
     * - QA role: có thể tạo report cho bất kỳ lớp nào
     * - Teacher role: chỉ có thể tạo report cho lớp mình dạy
     * - Các role khác: không có quyền tạo report
     */
    private void validateUserPermission(Long userId, Long classId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        boolean hasPermission = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode().equals("QA"));

        if (!hasPermission) {
            throw new InvalidRequestException("Bạn không có quyền tạo QA report");
        }

        log.info("Validated user {} permission for class {} - Has QA role: {}",
                 userId, classId, hasPermission);
    }

    /**
     * Lấy metrics hiện tại của class để kèm theo QA report
     */
    private QAClassMetrics getClassMetrics(Long classId) {
        try {
            // Get attendance data
            var attendanceData = studentSessionRepository.getAttendanceSummaryByClassId(classId);
            long presentCount = 0;
            long totalCount = 0;

            for (Object[] row : attendanceData) {
                var status = row[0];
                Long count = (Long) row[1];

                totalCount += count;
                if (status == org.fyp.tmssep490be.entities.enums.AttendanceStatus.PRESENT) {
                    presentCount += count;
                }
            }

            // Get homework data
            var homeworkData = studentSessionRepository.getHomeworkSummaryByClassId(classId);
            long completedHomework = 0;
            long totalHomework = 0;

            for (Object[] row : homeworkData) {
                var status = row[0];
                Long count = (Long) row[1];

                totalHomework += count;
                if (status == org.fyp.tmssep490be.entities.enums.HomeworkStatus.COMPLETED) {
                    completedHomework += count;
                }
            }

            double attendanceRate = totalCount > 0 ? (presentCount * 100.0 / totalCount) : 0.0;
            double homeworkRate = totalHomework > 0 ? (completedHomework * 100.0 / totalHomework) : 0.0;

            // Get session counts
            long totalSessions = sessionRepository.countByClassEntityId(classId);
            long completedSessions = sessionRepository.countByClassEntityIdExcludingCancelled(classId);

            return QAClassMetrics.builder()
                    .attendanceRate(attendanceRate)
                    .homeworkCompletionRate(homeworkRate)
                    .totalSessions((int) totalSessions)
                    .completedSessions((int) completedSessions)
                    .totalStudents((int) totalCount)
                    .presentStudents((int) presentCount)
                    .completedHomeworkStudents((int) completedHomework)
                    .build();

        } catch (Exception e) {
            log.error("Error calculating metrics for class {}: {}", classId, e.getMessage());
            return QAClassMetrics.builder().build();
        }
    }

    @Override
    @Transactional
    public QAReportDetailDTO createQAReport(CreateQAReportRequest request, Long userId) {
        log.info("Creating QA report for classId={} type={} by userId={}",
                 request.getClassId(), request.getReportType(), userId);

        // Enhanced validation
        validateClassForQAReport(request.getClassId(), request.getReportType(),
                              request.getSessionId(), request.getPhaseId());
        validateUserPermission(userId, request.getClassId());
        validateReportTypeAndLevel(request.getReportType(), request.getSessionId(), request.getPhaseId());

        ClassEntity classEntity = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class không tồn tại"));

        Session session = null;
        if (request.getSessionId() != null) {
            session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));
            if (!session.getClassEntity().getId().equals(request.getClassId())) {
                throw new InvalidRequestException("Session không thuộc class này");
            }
        }

        CoursePhase phase = null;
        if (request.getPhaseId() != null) {
            phase = coursePhaseRepository.findById(request.getPhaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phase không tồn tại"));
        }

        UserAccount reportedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        // Direct enum assignment from request DTOs
        QAReportType reportType = request.getReportType();
        QAReportStatus status = request.getStatus();

        // Get current class metrics
        QAClassMetrics metrics = getClassMetrics(request.getClassId());

        QAReport report = QAReport.builder()
                .classEntity(classEntity)
                .session(session)
                .phase(phase)
                .reportedBy(reportedBy)
                .reportType(reportType)
                .status(status)
                .findings(request.getFindings())
                .actionItemsText(request.getActionItems())
                .build();

        QAReport saved = qaReportRepository.save(report);

        log.info("QA report created successfully - ID: {}, Class: {}, Type: {}, Status: {}, " +
                 "Class Metrics - Attendance: {}%, Homework: {}%, Sessions: {}/{}",
                 saved.getId(), classEntity.getName(), reportType, status,
                 String.format("%.1f", metrics.getAttendanceRate()),
                 String.format("%.1f", metrics.getHomeworkCompletionRate()),
                 metrics.getCompletedSessions(), metrics.getTotalSessions());

        return mapToDetailDTO(saved, metrics);
    }

    @Override
    @Transactional
    public QAReportDetailDTO updateQAReport(Long reportId, UpdateQAReportRequest request, Long userId) {
        log.info("Updating QA report id={} by userId={}", reportId, userId);

        QAReport report = qaReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        if (!report.getReportedBy().getId().equals(userId)) {
            throw new InvalidRequestException("Bạn không có quyền sửa report này");
        }

        // Only allow updates for DRAFT status reports
        if (report.getStatus() != QAReportStatus.DRAFT) {
            throw new InvalidRequestException("Chỉ có thể sửa report ở trạng thái BẢN NHÁP (DRAFT)");
        }

        // Direct enum assignment from request DTOs
        QAReportType reportType = request.getReportType();
        QAReportStatus status = request.getStatus();

        // FIXED: Validate reportType change against current sessionId/phaseId
        if (!report.getReportType().equals(reportType)) {
            Long currentSessionId = report.getSession() != null ? report.getSession().getId() : null;
            Long currentPhaseId = report.getPhase() != null ? report.getPhase().getId() : null;

            log.info("Report type change detected: {} -> {}. Validating against sessionId={}, phaseId={}",
                    report.getReportType(), reportType, currentSessionId, currentPhaseId);

            // Re-validate the new reportType against existing context
            validateReportTypeAndLevel(reportType, currentSessionId, currentPhaseId);
        }

        report.setReportType(reportType);  // Direct enum assignment
        report.setFindings(request.getFindings());
        report.setActionItemsText(request.getActionItems());
        report.setStatus(status);          // Direct enum assignment

        QAReport updated = qaReportRepository.save(report);
        return mapToDetailDTO(updated);
    }

    @Override
    @Transactional
    public QAReportDetailDTO changeReportStatus(Long reportId, ChangeQAReportStatusRequest request, Long userId) {
        log.info("Changing QA report id={} status to {} by userId={}", reportId, request.getStatus(), userId);

        QAReport report = qaReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        if (!report.getReportedBy().getId().equals(userId)) {
            throw new InvalidRequestException("Bạn không có quyền thay đổi status report này");
        }

        QAReportStatus status = request.getStatus();
        report.setStatus(status);  // Direct enum assignment
        QAReport updated = qaReportRepository.save(report);
        return mapToDetailDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QAReportListItemDTO> getQAReports(Long classId, Long sessionId, Long phaseId,
                                                    QAReportType reportType, QAReportStatus status, Long reportedBy,
                                                    Pageable pageable, Long userId) {
        log.info("Getting QA reports with filters - userId={}", userId);

        // Get current user's accessible branches
        List<Long> branchIds = getUserAccessibleBranches(userId);

        // Handle empty branches case - user has no accessible branches
        if (branchIds.isEmpty()) {
            log.warn("User {} không có branch nào được phân quyền, trả về danh sách rỗng", userId);
            return Page.empty(pageable);
        }

        Page<QAReport> reports = qaReportRepository.findWithFilters(
                classId, sessionId, phaseId, reportType, status, reportedBy, branchIds, pageable
        );

        return reports.map(this::mapToListItemDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public QAReportDetailDTO getQAReportDetail(Long reportId) {
        log.info("Getting QA report detail id={}", reportId);

        QAReport report = qaReportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        return mapToDetailDTO(report);
    }

    @Override
    @Transactional
    public void deleteQAReport(Long reportId, Long userId) {
        log.info("Deleting QA report id={} by userId={}", reportId, userId);

        QAReport report = qaReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        if (!report.getReportedBy().getId().equals(userId)) {
            throw new InvalidRequestException("Bạn không có quyền xóa report này");
        }

        qaReportRepository.delete(report);
    }

    private QAReportDetailDTO mapToDetailDTO(QAReport report) {
        return mapToDetailDTO(report, null);
    }

    private QAReportDetailDTO mapToDetailDTO(QAReport report, QAClassMetrics metrics) {
        if (metrics == null) {
            metrics = getClassMetrics(report.getClassEntity().getId());
        }

        return QAReportDetailDTO.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .classId(report.getClassEntity().getId())
                .classCode(report.getClassEntity().getCode() != null ? report.getClassEntity().getCode() : "N/A")
                .className(report.getClassEntity().getName() != null ? report.getClassEntity().getName() : "N/A")
                .sessionId(report.getSession() != null ? report.getSession().getId() : null)
                .sessionDate(report.getSession() != null ? report.getSession().getDate().toString() : null)
                .phaseId(report.getPhase() != null ? report.getPhase().getId() : null)
                .phaseName(report.getPhase() != null ? report.getPhase().getName() : null)
                .findings(report.getFindings())
                .actionItems(report.getActionItemsText())
                .reportedById(report.getReportedBy().getId())
                .reportedByName(report.getReportedBy().getFullName())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .classMetrics(metrics)
                .build();
    }

    private QAReportListItemDTO mapToListItemDTO(QAReport report) {
        String reportLevel = "Class";
        if (report.getSession() != null) {
            reportLevel = "Session";
        } else if (report.getPhase() != null) {
            reportLevel = "Phase";
        }

        String findingsPreview = report.getFindings() != null && report.getFindings().length() > 100
                ? report.getFindings().substring(0, 100) + "..."
                : report.getFindings();

        return QAReportListItemDTO.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .reportLevel(reportLevel)
                .classId(report.getClassEntity().getId())
                .classCode(report.getClassEntity().getCode() != null ? report.getClassEntity().getCode() : "N/A")
                .sessionId(report.getSession() != null ? report.getSession().getId() : null)
                .sessionDate(report.getSession() != null ? report.getSession().getDate().toString() : null)
                .phaseId(report.getPhase() != null ? report.getPhase().getId() : null)
                .phaseName(report.getPhase() != null ? report.getPhase().getName() : null)
                .status(report.getStatus())
                .reportedByName(report.getReportedBy().getFullName())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .findingsPreview(findingsPreview)
                .build();
    }

    /**
     * Validate report type and level combinations
     */
    private void validateReportTypeAndLevel(QAReportType reportType, Long sessionId, Long phaseId) {
        switch (reportType) {
            case CLASSROOM_OBSERVATION:
                if (sessionId == null) {
                    throw new InvalidRequestException("CLASSROOM_OBSERVATION requires session ID");
                }
                break;
            case PHASE_REVIEW:
                if (phaseId == null) {
                    throw new InvalidRequestException("PHASE_REVIEW requires phase ID");
                }
                break;
            case CLO_ACHIEVEMENT_ANALYSIS:
                if (sessionId == null && phaseId == null) {
                    throw new InvalidRequestException("CLO_ACHIEVEMENT_ANALYSIS requires session ID or phase ID");
                }
                break;
            case STUDENT_FEEDBACK_ANALYSIS:
            case ATTENDANCE_ENGAGEMENT_REVIEW:
                if (sessionId == null && phaseId == null) {
                    throw new InvalidRequestException(reportType + " requires session ID or phase ID");
                }
                break;
            case TEACHING_QUALITY_ASSESSMENT:
                // Class-level report, no specific session or phase required
                break;
            default:
                break;
        }
    }
}
