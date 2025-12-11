package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.*;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QAReportService {
    QAReportDetailDTO createQAReport(CreateQAReportRequest request, Long userId);

    QAReportDetailDTO updateQAReport(Long reportId, UpdateQAReportRequest request, Long userId);

    QAReportDetailDTO changeReportStatus(Long reportId, ChangeQAReportStatusRequest request, Long userId);

    Page<QAReportListItemDTO> getQAReports(Long classId, Long sessionId, Long phaseId,
                                            QAReportType reportType, QAReportStatus status, Long reportedBy,
                                            String search, Pageable pageable, Long userId);

    QAReportDetailDTO getQAReportDetail(Long reportId);

    void deleteQAReport(Long reportId, Long userId);
}
