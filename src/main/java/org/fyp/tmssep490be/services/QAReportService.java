package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QAReportService {
    QAReportDetailDTO createQAReport(CreateQAReportRequest request, Long userId);

    QAReportDetailDTO updateQAReport(Long reportId, UpdateQAReportRequest request, Long userId);

    QAReportDetailDTO changeReportStatus(Long reportId, ChangeQAReportStatusRequest request, Long userId);

    Page<QAReportListItemDTO> getQAReports(Long classId, Long sessionId, Long phaseId,
                                            String reportType, String status, Long reportedBy,
                                            Pageable pageable);

    QAReportDetailDTO getQAReportDetail(Long reportId);

    void deleteQAReport(Long reportId, Long userId);
}
