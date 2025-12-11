package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.*;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.QAReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qa/reports")
@RequiredArgsConstructor
@Slf4j
public class QAReportController {

    private final QAReportService qaReportService;

    @PostMapping
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> createQAReport(
        @Valid @RequestBody CreateQAReportRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            log.info("Creating QA report - Type: {}, Status: {}, Class: {}, Session: {}",
                     request.getReportType(), request.getStatus(),
                     request.getClassId(), request.getSessionId());

            QAReportDetailDTO report = qaReportService.createQAReport(request, currentUser.getId());

            log.info("Successfully created QA report with ID: {}", report.getId());

            return ResponseEntity.ok(ResponseObject.<QAReportDetailDTO>builder()
                .success(true)
                .message("QA report created successfully")
                .data(report)
                .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value in QA report creation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseObject.<QAReportDetailDTO>builder()
                    .success(false)
                    .message("Invalid parameter: " + e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Error creating QA report", e);
            return ResponseEntity.internalServerError()
                .body(ResponseObject.<QAReportDetailDTO>builder()
                    .success(false)
                    .message("Error creating QA report: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('QA', 'CENTER_HEAD', 'MANAGER')")
    public ResponseEntity<ResponseObject<Page<QAReportListItemDTO>>> getQAReports(
        @RequestParam(required = false) Long classId,
        @RequestParam(required = false) Long sessionId,
        @RequestParam(required = false) Long phaseId,
        @RequestParam(required = false) QAReportType reportType,
        @RequestParam(required = false) QAReportStatus status,
        @RequestParam(required = false) Long reportedBy,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(defaultValue = "desc") String sortDir,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA reports list with filters: classId={}, sessionId={}, phaseId={}, reportType={}, status={}, search={}",
                 currentUser.getId(), classId, sessionId, phaseId, reportType, status, search);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<QAReportListItemDTO> reports = qaReportService.getQAReports(
            classId, sessionId, phaseId, reportType, status, reportedBy, search, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<Page<QAReportListItemDTO>>builder()
            .success(true)
            .message("QA reports retrieved successfully")
            .data(reports)
            .build());
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('QA', 'CENTER_HEAD', 'MANAGER')")
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> getQAReportDetail(
        @PathVariable Long reportId
    ) {
        log.info("Requesting QA report detail for reportId={}", reportId);

        QAReportDetailDTO report = qaReportService.getQAReportDetail(reportId);

        return ResponseEntity.ok(ResponseObject.<QAReportDetailDTO>builder()
            .success(true)
            .message("QA report detail retrieved successfully")
            .data(report)
            .build());
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> updateQAReport(
        @PathVariable Long reportId,
        @Valid @RequestBody UpdateQAReportRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            log.info("Updating QA report ID: {} by userId: {}", reportId, currentUser.getId());

            QAReportDetailDTO report = qaReportService.updateQAReport(reportId, request, currentUser.getId());

            log.info("Successfully updated QA report ID: {}", reportId);

            return ResponseEntity.ok(ResponseObject.<QAReportDetailDTO>builder()
                .success(true)
                .message("QA report updated successfully")
                .data(report)
                .build());

        } catch (Exception e) {
            log.error("Error updating QA report", e);
            return ResponseEntity.internalServerError()
                .body(ResponseObject.<QAReportDetailDTO>builder()
                    .success(false)
                    .message("Error updating QA report: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> changeReportStatus(
        @PathVariable Long reportId,
        @Valid @RequestBody ChangeQAReportStatusRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            log.info("Changing status for QA report ID: {} to {} by userId: {}",
                     reportId, request.getStatus(), currentUser.getId());

            QAReportDetailDTO report = qaReportService.changeReportStatus(reportId, request, currentUser.getId());

            log.info("Successfully changed status for QA report ID: {}", reportId);

            return ResponseEntity.ok(ResponseObject.<QAReportDetailDTO>builder()
                .success(true)
                .message("QA report status updated successfully")
                .data(report)
                .build());

        } catch (Exception e) {
            log.error("Error changing QA report status", e);
            return ResponseEntity.internalServerError()
                .body(ResponseObject.<QAReportDetailDTO>builder()
                    .success(false)
                    .message("Error changing status: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('QA')")
    public ResponseEntity<ResponseObject<Void>> deleteQAReport(
        @PathVariable Long reportId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            log.info("Deleting QA report ID: {} by userId: {}", reportId, currentUser.getId());

            qaReportService.deleteQAReport(reportId, currentUser.getId());

            log.info("Successfully deleted QA report ID: {}", reportId);

            return ResponseEntity.ok(ResponseObject.<Void>builder()
                .success(true)
                .message("QA report deleted successfully")
                .data(null)
                .build());

        } catch (Exception e) {
            log.error("Error deleting QA report", e);
            return ResponseEntity.internalServerError()
                .body(ResponseObject.<Void>builder()
                    .success(false)
                    .message("Error deleting QA report: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }
}
