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

@Service
@RequiredArgsConstructor
@Slf4j
public class QAReportServiceImpl implements QAReportService {

    private final QAReportRepository qaReportRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final CoursePhaseRepository coursePhaseRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional
    public QAReportDetailDTO createQAReport(CreateQAReportRequest request, Long userId) {
        log.info("Creating QA report for classId={} by userId={}", request.getClassId(), userId);

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

        // Validate enum values explicitly
        QAReportType reportType = QAReportType.fromString(request.getReportType());
        QAReportStatus status = QAReportStatus.fromString(request.getStatus());

        QAReport report = QAReport.builder()
                .classEntity(classEntity)
                .session(session)
                .phase(phase)
                .reportedBy(reportedBy)
                .reportType(reportType.getValue())  // Use getValue()
                .status(status.getValue())          // Use getValue()
                .findings(request.getFindings())
                .actionItems(request.getActionItems())
                .build();

        QAReport saved = qaReportRepository.save(report);
        return mapToDetailDTO(saved);
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

        // Validate enum values
        QAReportType reportType = QAReportType.fromString(request.getReportType());
        QAReportStatus status = QAReportStatus.fromString(request.getStatus());

        report.setReportType(reportType.getValue());
        report.setFindings(request.getFindings());
        report.setActionItems(request.getActionItems());
        report.setStatus(status.getValue());

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

        QAReportStatus status = QAReportStatus.fromString(request.getStatus());
        report.setStatus(status.getValue());
        QAReport updated = qaReportRepository.save(report);
        return mapToDetailDTO(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QAReportListItemDTO> getQAReports(Long classId, Long sessionId, Long phaseId,
                                                    String reportType, String status, Long reportedBy,
                                                    Pageable pageable) {
        log.info("Getting QA reports with filters");

        Page<QAReport> reports = qaReportRepository.findWithFilters(
                classId, sessionId, phaseId, reportType, status, reportedBy, pageable
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
        return QAReportDetailDTO.builder()
                .id(report.getId())
                .reportType(report.getReportTypeEnum() != null ?
                           report.getReportTypeEnum().getDisplayName() : null)
                .status(report.getStatusEnum() != null ?
                        report.getStatusEnum().getDisplayName() : null)
                .classId(report.getClassEntity().getId())
                .classCode(report.getClassEntity().getName())
                .className(report.getClassEntity().getCourse() != null ? report.getClassEntity().getCourse().getName() : null)
                .sessionId(report.getSession() != null ? report.getSession().getId() : null)
                .sessionDate(report.getSession() != null ? report.getSession().getDate().toString() : null)
                .phaseId(report.getPhase() != null ? report.getPhase().getId() : null)
                .phaseName(report.getPhase() != null ? report.getPhase().getName() : null)
                .findings(report.getFindings())
                .actionItems(report.getActionItems())
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

        String findingsPreview = report.getFindings() != null && report.getFindings().length() > 100
                ? report.getFindings().substring(0, 100) + "..."
                : report.getFindings();

        return QAReportListItemDTO.builder()
                .id(report.getId())
                .reportType(report.getReportTypeEnum() != null ?
                           report.getReportTypeEnum().getDisplayName() : null)
                .reportLevel(reportLevel)
                .classId(report.getClassEntity().getId())
                .classCode(report.getClassEntity().getName())
                .sessionId(report.getSession() != null ? report.getSession().getId() : null)
                .sessionDate(report.getSession() != null ? report.getSession().getDate().toString() : null)
                .phaseId(report.getPhase() != null ? report.getPhase().getId() : null)
                .phaseName(report.getPhase() != null ? report.getPhase().getName() : null)
                .status(report.getStatusEnum() != null ?
                        report.getStatusEnum().getDisplayName() : null)
                .reportedByName(report.getReportedBy().getFullName())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .findingsPreview(findingsPreview)
                .build();
    }
}
