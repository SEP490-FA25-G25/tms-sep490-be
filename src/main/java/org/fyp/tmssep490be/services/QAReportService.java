package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.InvalidRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QAReportService {

    private final QAReportRepository qaReportRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final SubjectPhaseRepository subjectPhaseRepository;
    private final UserAccountRepository userAccountRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserBranchesRepository userBranchesRepository;

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    private boolean userHasBranchAccess(Long userId, Long branchId) {
        return getUserAccessibleBranches(userId).stream().anyMatch(id -> id.equals(branchId));
    }

    private void validateClassForQAReport(Long classId, QAReportType reportType, Long sessionId, Long phaseId) {
        if (reportType == QAReportType.CLASSROOM_OBSERVATION && sessionId != null) {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));

            if (!session.getClassEntity().getId().equals(classId)) {
                throw new InvalidRequestException("Session không thuộc class này");
            }

            if (session.getStatus() != SessionStatus.DONE) {
                throw new InvalidRequestException("Session chưa hoàn thành, không thể tạo QA report");
            }

            long studentSessionCount = studentSessionRepository.countBySessionId(sessionId);
            if (studentSessionCount == 0) {
                throw new InvalidRequestException("Session không có dữ liệu học sinh, không thể tạo QA report");
            }

            log.info("Validated session {} for QA report - Students: {}", sessionId, studentSessionCount);
        } else if (reportType == QAReportType.PHASE_REVIEW && phaseId != null) {
            SubjectPhase phase = subjectPhaseRepository.findById(phaseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Phase không tồn tại"));

            log.info("Validated phase {} for QA report", phaseId);
        }
    }

    private void validateUserPermission(Long userId, Long classId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        boolean hasPermission = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode().equals("QA"));

        if (!hasPermission) {
            throw new InvalidRequestException("Bạn không có quyền tạo QA report");
        }

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class không tồn tại"));

        if (classEntity.getBranch() == null || !userHasBranchAccess(userId, classEntity.getBranch().getId())) {
            throw new InvalidRequestException("Bạn không có quyền truy cập lớp thuộc chi nhánh này");
        }

        log.info("Validated user {} permission for class {} - Has QA role: {}",
                userId, classId, hasPermission);
    }

    @Transactional
    public QAReportDetailDTO createQAReport(CreateQAReportRequest request, Long userId) {
        log.info("Creating QA report for classId={} type={} by userId={}",
                request.getClassId(), request.getReportType(), userId);

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

        SubjectPhase phase = null;
        if (request.getPhaseId() != null) {
            phase = subjectPhaseRepository.findById(request.getPhaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phase không tồn tại"));
        }

        UserAccount reportedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        QAReport report = QAReport.builder()
                .classEntity(classEntity)
                .session(session)
                .phase(phase)
                .reportedBy(reportedBy)
                .reportType(request.getReportType())
                .status(request.getStatus())
                .content(request.getContent())
                .build();

        QAReport saved = qaReportRepository.save(report);

        log.info("QA report created successfully - ID: {}, Class: {}, Type: {}, Status: {}",
                saved.getId(), classEntity.getName(), request.getReportType(), request.getStatus());

        return mapToDetailDTO(saved);
    }

    @Transactional
    public QAReportDetailDTO updateQAReport(Long reportId, UpdateQAReportRequest request, Long userId) {
        log.info("Updating QA report id={} by userId={}", reportId, userId);

        QAReport report = qaReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        if (report.getClassEntity() == null || report.getClassEntity().getBranch() == null ||
                !userHasBranchAccess(userId, report.getClassEntity().getBranch().getId())) {
            throw new InvalidRequestException("Bạn không có quyền sửa report của lớp thuộc chi nhánh này");
        }

        if (!report.getReportedBy().getId().equals(userId)) {
            throw new InvalidRequestException("Bạn không có quyền sửa report này");
        }

        if (report.getStatus() != QAReportStatus.DRAFT) {
            throw new InvalidRequestException("Chỉ có thể sửa report ở trạng thái BẢN NHÁP (DRAFT)");
        }

        if (!report.getReportType().equals(request.getReportType())) {
            Long currentSessionId = report.getSession() != null ? report.getSession().getId() : null;
            Long currentPhaseId = report.getPhase() != null ? report.getPhase().getId() : null;

            log.info("Report type change detected: {} -> {}. Validating against sessionId={}, phaseId={}",
                    report.getReportType(), request.getReportType(), currentSessionId, currentPhaseId);

            validateReportTypeAndLevel(request.getReportType(), currentSessionId, currentPhaseId);
        }

        report.setReportType(request.getReportType());
        report.setContent(request.getContent());
        report.setStatus(request.getStatus());

        QAReport updated = qaReportRepository.save(report);
        return mapToDetailDTO(updated);
    }

    @Transactional
    public QAReportDetailDTO changeReportStatus(Long reportId, ChangeQAReportStatusRequest request, Long userId) {
        log.info("Changing QA report id={} status to {} by userId={}", reportId, request.getStatus(), userId);

        QAReport report = qaReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        if (report.getClassEntity() == null || report.getClassEntity().getBranch() == null ||
                !userHasBranchAccess(userId, report.getClassEntity().getBranch().getId())) {
            throw new InvalidRequestException("Bạn không có quyền thay đổi status report của lớp thuộc chi nhánh này");
        }

        if (!report.getReportedBy().getId().equals(userId)) {
            throw new InvalidRequestException("Bạn không có quyền thay đổi status report này");
        }

        report.setStatus(request.getStatus());
        QAReport updated = qaReportRepository.save(report);
        return mapToDetailDTO(updated);
    }

    @Transactional(readOnly = true)
    public Page<QAReportListItemDTO> getQAReports(Long classId, Long sessionId, Long phaseId,
            QAReportType reportType, QAReportStatus status, Long reportedBy,
            String search, Pageable pageable, Long userId) {
        log.info("Getting QA reports with filters - userId={}, search={}", userId, search);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        boolean isManager = user.getUserRoles().stream()
                .anyMatch(ur -> "MANAGER".equals(ur.getRole().getCode()));

        List<Long> branchIds;
        if (isManager) {
            branchIds = null;
            log.info("User {} is MANAGER, returning reports from all branches", userId);
        } else {
            branchIds = getUserAccessibleBranches(userId);

            if (branchIds.isEmpty()) {
                log.warn("User {} không có branch nào được phân quyền, trả về danh sách rỗng", userId);
                return Page.empty(pageable);
            }
        }

        Page<QAReport> reports = qaReportRepository.findWithFilters(
                classId, sessionId, phaseId, reportType, status, reportedBy, search, branchIds, pageable);

        return reports.map(this::mapToListItemDTO);
    }

    @Transactional(readOnly = true)
    public QAReportDetailDTO getQAReportDetail(Long reportId) {
        log.info("Getting QA report detail id={}", reportId);

        QAReport report = qaReportRepository.findByIdWithDetails(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        return mapToDetailDTO(report);
    }

    @Transactional
    public void deleteQAReport(Long reportId, Long userId) {
        log.info("Deleting QA report id={} by userId={}", reportId, userId);

        QAReport report = qaReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("QA Report không tồn tại"));

        if (report.getClassEntity() == null || report.getClassEntity().getBranch() == null ||
                !userHasBranchAccess(userId, report.getClassEntity().getBranch().getId())) {
            throw new InvalidRequestException("Bạn không có quyền xóa report của lớp thuộc chi nhánh này");
        }

        if (!report.getReportedBy().getId().equals(userId)) {
            throw new InvalidRequestException("Bạn không có quyền xóa report này");
        }

        qaReportRepository.delete(report);
    }

    private QAReportDetailDTO mapToDetailDTO(QAReport report) {
        return QAReportDetailDTO.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .status(report.getStatus())
                .classId(report.getClassEntity().getId())
                .classCode(report.getClassEntity().getCode() != null ? report.getClassEntity().getCode() : "N/A")
                .className(report.getClassEntity().getName() != null ? report.getClassEntity().getName() : "N/A")
                .branchId(report.getClassEntity().getBranch() != null ? report.getClassEntity().getBranch().getId()
                        : null)
                .branchName(report.getClassEntity().getBranch() != null ? report.getClassEntity().getBranch().getName()
                        : null)
                .sessionId(report.getSession() != null ? report.getSession().getId() : null)
                .sessionDate(report.getSession() != null ? report.getSession().getDate().toString() : null)
                .phaseId(report.getPhase() != null ? report.getPhase().getId() : null)
                .phaseName(report.getPhase() != null ? report.getPhase().getName() : null)
                .content(report.getContent())
                .reportedById(report.getReportedBy().getId())
                .reportedByName(report.getReportedBy().getFullName())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private QAReportListItemDTO mapToListItemDTO(QAReport report) {
        String reportLevel = "Class";
        if (report.getSession() != null) {
            reportLevel = "Session";
        } else if (report.getPhase() != null) {
            reportLevel = "Phase";
        }

        String contentPreview = report.getContent() != null && report.getContent().length() > 100
                ? report.getContent().substring(0, 100) + "..."
                : report.getContent();

        return QAReportListItemDTO.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .reportLevel(reportLevel)
                .classId(report.getClassEntity().getId())
                .classCode(report.getClassEntity().getCode() != null ? report.getClassEntity().getCode() : "N/A")
                .branchId(report.getClassEntity().getBranch() != null ? report.getClassEntity().getBranch().getId()
                        : null)
                .branchName(report.getClassEntity().getBranch() != null ? report.getClassEntity().getBranch().getName()
                        : null)
                .sessionId(report.getSession() != null ? report.getSession().getId() : null)
                .sessionDate(report.getSession() != null ? report.getSession().getDate().toString() : null)
                .phaseId(report.getPhase() != null ? report.getPhase().getId() : null)
                .phaseName(report.getPhase() != null ? report.getPhase().getName() : null)
                .status(report.getStatus())
                .reportedByName(report.getReportedBy().getFullName())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .contentPreview(contentPreview)
                .build();
    }

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
                break;
            default:
                break;
        }
    }
}
