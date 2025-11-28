package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fyp.tmssep490be.dtos.qa.QAExportRequest;
import org.fyp.tmssep490be.dtos.qa.QADashboardDTO;
import org.fyp.tmssep490be.services.ExcelExportService;
import org.fyp.tmssep490be.services.QAService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelExportServiceImpl implements ExcelExportService {

    private final QAService qaService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    @Transactional(readOnly = true)
    public Resource generateQAExport(QAExportRequest exportRequest, Long userId) {
        log.info("Generating QA export for userId={} with dateFrom={}, dateTo={}, sections={}",
                userId, exportRequest.getDateFrom(), exportRequest.getDateTo(), exportRequest.getIncludeSections());

        try {
            // Get dashboard data
            QADashboardDTO dashboard = qaService.getQADashboard(
                    exportRequest.getBranchIds(),
                    exportRequest.getDateFrom(),
                    exportRequest.getDateTo(),
                    userId
            );

            // Create workbook
            Workbook workbook = new XSSFWorkbook();

            // Generate sheets based on requested sections
            for (QAExportRequest.ExportSection section : exportRequest.getIncludeSections()) {
                switch (section) {
                    case KPI_OVERVIEW:
                        createKPISheet(workbook, dashboard);
                        break;
                    case CLASSES_REQUIRING_ATTENTION:
                        createClassesRequiringAttentionSheet(workbook, dashboard);
                        break;
                    case RECENT_QA_REPORTS:
                        createRecentQAReportsSheet(workbook, dashboard);
                        break;
                }
            }

            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            byte[] excelBytes = outputStream.toByteArray();
            log.info("Successfully generated QA export: {} bytes", excelBytes.length);

            return new ByteArrayResource(excelBytes) {
                @Override
                public String getFilename() {
                    return generateExportFilename(exportRequest);
                }
            };

        } catch (IOException e) {
            log.error("Error generating QA export for userId={}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Không thể tạo file xuất dữ liệu QA: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateExportFilename(QAExportRequest exportRequest) {
        LocalDate today = LocalDate.now();
        String dateRange = String.format("%s_to_%s",
                exportRequest.getDateFrom().format(DATE_FORMATTER),
                exportRequest.getDateTo().format(DATE_FORMATTER));

        return String.format("qa-dashboard-%s.xlsx", dateRange);
    }

    private void createKPISheet(Workbook workbook, QADashboardDTO dashboard) {
        Sheet sheet = workbook.createSheet("Tổng Quan KPIs");

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);

        AtomicInteger rowNum = new AtomicInteger(0);

        // Title
        Row titleRow = sheet.createRow(rowNum.getAndIncrement());
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("TỔNG QUAN KPIs CHẤT LƯỢNG ĐÀO TẠO");
        titleCell.setCellStyle(titleStyle);

        // Date range info
        if (dashboard.getDateRangeInfo() != null) {
            Row dateRow = sheet.createRow(rowNum.getAndIncrement());
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue(String.format("Khoảng thời gian: %s - %s",
                    dashboard.getDateRangeInfo().getDateFrom(),
                    dashboard.getDateRangeInfo().getDateTo()));
        }

        rowNum.addAndGet(1); // Empty row

        // KPI Metrics
        QADashboardDTO.KPIMetrics kpi = dashboard.getKpiMetrics();
        if (kpi != null) {
            // KPI Header
            Row kpiHeaderRow = sheet.createRow(rowNum.getAndIncrement());
            Cell kpiHeaderCell = kpiHeaderRow.createCell(0);
            kpiHeaderCell.setCellValue("CHỈ SỐ HIỆU SUẤT CHÍNH");
            kpiHeaderCell.setCellStyle(headerStyle);

            // KPI Data
            createKPIRow(sheet, rowNum, "Lớp đang diễn ra", kpi.getOngoingClassesCount(), "lớp", numberStyle);
            createKPIRow(sheet, rowNum, "Báo cáo QA tháng này", kpi.getQaReportsCreatedThisMonth(), "báo cáo", numberStyle);
            createKPIRow(sheet, rowNum, "Tỷ lệ điểm danh trung bình", kpi.getAverageAttendanceRate(), "%", numberStyle);
            createKPIRow(sheet, rowNum, "Tỷ lệ hoàn thành bài tập", kpi.getAverageHomeworkCompletionRate(), "%", numberStyle);
        }

        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createKPIRow(Sheet sheet, AtomicInteger rowNum, String label, Object value, String unit, CellStyle numberStyle) {
        Row row = sheet.createRow(rowNum.getAndIncrement());

        row.createCell(0).setCellValue(label);

        Cell valueCell = row.createCell(1);
        if (value instanceof Number) {
            valueCell.setCellValue(((Number) value).doubleValue());
            valueCell.setCellStyle(numberStyle);
        } else {
            valueCell.setCellValue(value.toString());
        }

        row.createCell(2).setCellValue(unit);
    }

    private void createClassesRequiringAttentionSheet(Workbook workbook, QADashboardDTO dashboard) {
        Sheet sheet = workbook.createSheet("Lớp Cần Chú Ý");

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle numberStyle = createNumberStyle(workbook);
        CellStyle warningStyle = createWarningStyle(workbook);

        AtomicInteger rowNum = new AtomicInteger(0);

        // Title
        Row titleRow = sheet.createRow(rowNum.getAndIncrement());
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("LỚP CẦN CHÚ Ý");
        titleCell.setCellStyle(titleStyle);

        rowNum.addAndGet(1); // Empty row

        // Headers
        Row headerRow = sheet.createRow(rowNum.getAndIncrement());
        String[] headers = {"Mã Lớp", "Khóa Học", "Chi Nhánh", "Tỷ Lệ Điểm Danh (%)", "Tỷ Lệ Hoàn Thành BT (%)", "Số Báo Cáo QA", "Lý Do Cảnh Báo"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        List<QADashboardDTO.ClassRequiringAttention> classes = dashboard.getClassesRequiringAttention();
        if (classes != null && !classes.isEmpty()) {
            for (QADashboardDTO.ClassRequiringAttention cls : classes) {
                Row row = sheet.createRow(rowNum.getAndIncrement());

                row.createCell(0).setCellValue(cls.getClassCode());
                row.createCell(1).setCellValue(cls.getCourseName());
                row.createCell(2).setCellValue(cls.getBranchName());

                // Attendance rate with conditional formatting
                Cell attendanceCell = row.createCell(3);
                attendanceCell.setCellValue(cls.getAttendanceRate());
                attendanceCell.setCellStyle(numberStyle);
                if (cls.getAttendanceRate() < 80.0) {
                    attendanceCell.setCellStyle(warningStyle);
                }

                // Homework completion rate with conditional formatting
                Cell homeworkCell = row.createCell(4);
                homeworkCell.setCellValue(cls.getHomeworkCompletionRate());
                homeworkCell.setCellStyle(numberStyle);
                if (cls.getHomeworkCompletionRate() < 80.0) {
                    homeworkCell.setCellStyle(warningStyle);
                }

                row.createCell(5).setCellValue(cls.getQaReportCount());
                row.createCell(6).setCellValue(cls.getWarningReason());
            }
        } else {
            // No data row
            Row noDataRow = sheet.createRow(rowNum.getAndIncrement());
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("Không có lớp nào cần chú ý trong khoảng thời gian đã chọn.");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum.get(), rowNum.get(), 0, headers.length - 1));
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createRecentQAReportsSheet(Workbook workbook, QADashboardDTO dashboard) {
        Sheet sheet = workbook.createSheet("Báo Cáo QA Gần Đây");

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle dateStyle = createDateStyle(workbook);

        AtomicInteger rowNum = new AtomicInteger(0);

        // Title
        Row titleRow = sheet.createRow(rowNum.getAndIncrement());
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BÁO CÁO QA GẦN ĐÂY");
        titleCell.setCellStyle(titleStyle);

        rowNum.addAndGet(1); // Empty row

        // Headers
        Row headerRow = sheet.createRow(rowNum.getAndIncrement());
        String[] headers = {"ID", "Loại Báo Cáo", "Mã Lớp", "Buổi Học", "Trạng Thái", "Ngày Tạo"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        List<QADashboardDTO.QAReportSummary> reports = dashboard.getRecentQAReports();
        if (reports != null && !reports.isEmpty()) {
            for (QADashboardDTO.QAReportSummary report : reports) {
                Row row = sheet.createRow(rowNum.getAndIncrement());

                row.createCell(0).setCellValue(report.getReportId());
                row.createCell(1).setCellValue(report.getReportType() != null ? report.getReportType().toString() : "");
                row.createCell(2).setCellValue(report.getClassCode());
                row.createCell(3).setCellValue(report.getSessionDate() != null ? report.getSessionDate() : "");
                row.createCell(4).setCellValue(report.getStatus() != null ? report.getStatus().toString() : "");

                Cell createdCell = row.createCell(5);
                if (report.getCreatedAt() != null) {
                    createdCell.setCellValue(report.getCreatedAt().toLocalDateTime());
                    createdCell.setCellStyle(dateStyle);
                }
            }
        } else {
            // No data row
            Row noDataRow = sheet.createRow(rowNum.getAndIncrement());
            Cell noDataCell = noDataRow.createCell(0);
            noDataCell.setCellValue("Không có báo cáo QA nào trong khoảng thời gian đã chọn.");
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowNum.get(), rowNum.get(), 0, headers.length - 1));
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);

        return style;
    }

    private CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Format with 1 decimal place for percentages
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.0"));

        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Date format
        style.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

        return style;
    }

    private CellStyle createWarningStyle(Workbook workbook) {
        CellStyle style = createNumberStyle(workbook);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);

        return style;
    }
}