package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.QAExportRequest;
import org.springframework.core.io.Resource;

public interface ExcelExportService {

    /**
     * Generate QA Dashboard export based on request parameters
     *
     * @param exportRequest Export configuration including date range, format, and sections to include
     * @param userId User ID for data filtering and authorization
     * @return Resource containing the generated file (Excel/CSV)
     */
    Resource generateQAExport(QAExportRequest exportRequest, Long userId);

    /**
     * Generate filename for the export file
     *
     * @param exportRequest Export configuration
     * @return Formatted filename (e.g., "qa-dashboard-2025-01-28.xlsx")
     */
    String generateExportFilename(QAExportRequest exportRequest);
}