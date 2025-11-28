package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.QAClassDetailDTO;
import org.fyp.tmssep490be.dtos.qa.QAClassListItemDTO;
import org.fyp.tmssep490be.dtos.qa.QADashboardDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface QAService {
    QADashboardDTO getQADashboard(List<Long> branchIds, LocalDate dateFrom, LocalDate dateTo, Long userId);

    Page<QAClassListItemDTO> getQAClasses(List<Long> branchIds, String status, String search,
                                           Pageable pageable, Long userId);

    /**
     * Get QA-specific class detail with metrics and reports
     */
    QAClassDetailDTO getQAClassDetail(Long classId, Long userId);

    /**
     * Get QA-specific session list for a class with attendance metrics
     */
    QASessionListResponse getQASessionList(Long classId, Long userId);

    /**
     * Get detailed session information with student data, CLO achievement, and feedback summary
     * Used for QA session detail view and report generation
     */
    SessionDetailDTO getQASessionDetail(Long sessionId, Long userId);
}
