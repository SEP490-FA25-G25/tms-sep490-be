package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "QA Reports", description = "QA Report Management APIs - CRUD operations for quality assurance reports")
public class QAReportController {

    private final QAReportService qaReportService;

    /**
     * Create QA Report
     */
    @PostMapping
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Create QA Report",
        description = "Create a new QA report for a class, session, or phase. " +
                      "Report can be at class-level (only classId), session-level (classId + sessionId), " +
                      "or phase-level (classId + phaseId). Findings must be at least 50 characters. " +
                      "Status can be 'draft' or 'submitted'."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA report created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation failed"),
        @ApiResponse(responseCode = "404", description = "Class/Session/Phase not found")
    })
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> createQAReport(
        @Valid @RequestBody CreateQAReportRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            log.info("Creating QA report - Type: {}, Status: {}, Class: {}, Session: {}",
                     request.getReportType(), request.getStatus(),
                     request.getClassId(), request.getSessionId());

            // Pre-validate enum values for better error messages
            validateEnumValues(request.getReportType(), request.getStatus());

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

    /**
     * Get QA Reports List with filters
     */
    @GetMapping
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Reports List",
        description = "Retrieve paginated list of QA reports with optional filters. " +
                      "Can filter by classId, sessionId, phaseId, reportType, status, and reportedBy."
    )
    public ResponseEntity<ResponseObject<Page<QAReportListItemDTO>>> getQAReports(
        @Parameter(description = "Filter by class ID")
        @RequestParam(required = false) Long classId,

        @Parameter(description = "Filter by session ID")
        @RequestParam(required = false) Long sessionId,

        @Parameter(description = "Filter by phase ID")
        @RequestParam(required = false) Long phaseId,

        @Parameter(description = "Filter by report type")
        @RequestParam(required = false) String reportType,

        @Parameter(description = "Filter by status (draft/submitted)")
        @RequestParam(required = false) String status,

        @Parameter(description = "Filter by reporter user ID")
        @RequestParam(required = false) Long reportedBy,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,

        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "createdAt") String sort,

        @Parameter(description = "Sort direction (asc/desc)")
        @RequestParam(defaultValue = "desc") String sortDir,

        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting QA reports list with filters: classId={}, sessionId={}, phaseId={}, reportType={}, status={}",
                 currentUser.getId(), classId, sessionId, phaseId, reportType, status);

        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<QAReportListItemDTO> reports = qaReportService.getQAReports(
            classId, sessionId, phaseId, reportType, status, reportedBy, pageable
        );

        return ResponseEntity.ok(ResponseObject.<Page<QAReportListItemDTO>>builder()
            .success(true)
            .message("QA reports retrieved successfully")
            .data(reports)
            .build());
    }

    /**
     * Get QA Report Detail
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get QA Report Detail",
        description = "Retrieve detailed information of a specific QA report including " +
                      "report type, status, related class/session/phase, findings, action items, and metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA report retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "QA report not found")
    })
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

    /**
     * Update QA Report
     */
    @PutMapping("/{reportId}")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Update QA Report",
        description = "Update an existing QA report. Only the creator can update the report. " +
                      "Report must be in 'draft' status to be updated. If status is 'submitted', " +
                      "use the change status endpoint to change it back to 'draft' first."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA report updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - report is submitted or validation failed"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user is not the creator"),
        @ApiResponse(responseCode = "404", description = "QA report not found")
    })
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> updateQAReport(
        @PathVariable Long reportId,
        @Valid @RequestBody UpdateQAReportRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        try {
            log.info("Updating QA report {} - Type: {}, Status: {}", reportId,
                     request.getReportType(), request.getStatus());

            validateEnumValues(request.getReportType(), request.getStatus());

            QAReportDetailDTO report = qaReportService.updateQAReport(reportId, request, currentUser.getId());

            log.info("Successfully updated QA report {}", reportId);

            return ResponseEntity.ok(ResponseObject.<QAReportDetailDTO>builder()
                .success(true)
                .message("QA report updated successfully")
                .data(report)
                .build());

        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value in QA report update: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ResponseObject.<QAReportDetailDTO>builder()
                    .success(false)
                    .message("Invalid parameter: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    /**
     * Change QA Report Status
     */
    @PatchMapping("/{reportId}/status")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Change QA Report Status",
        description = "Change QA report status between 'draft' and 'submitted'. " +
                      "Only the creator can change status. Use this to unlock a submitted report for editing."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Status changed successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user is not the creator"),
        @ApiResponse(responseCode = "404", description = "QA report not found")
    })
    public ResponseEntity<ResponseObject<QAReportDetailDTO>> changeReportStatus(
        @PathVariable Long reportId,
        @Valid @RequestBody ChangeQAReportStatusRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} changing status of QA report {} to {}",
                 currentUser.getId(), reportId, request.getStatus());

        QAReportDetailDTO report = qaReportService.changeReportStatus(reportId, request, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<QAReportDetailDTO>builder()
            .success(true)
            .message("QA report status changed successfully")
            .data(report)
            .build());
    }

    /**
     * Delete QA Report
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Delete QA Report",
        description = "Delete a QA report. Only the creator can delete the report. " +
                      "This is a hard delete and cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "QA report deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user is not the creator"),
        @ApiResponse(responseCode = "404", description = "QA report not found")
    })
    public ResponseEntity<ResponseObject<Void>> deleteQAReport(
        @PathVariable Long reportId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} deleting QA report {}", currentUser.getId(), reportId);

        qaReportService.deleteQAReport(reportId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("QA report deleted successfully")
            .data(null)
            .build());
    }

    // Helper method for enum validation
    private void validateEnumValues(String reportType, String status) {
        if (!QAReportType.isValidValue(reportType)) {
            throw new IllegalArgumentException("Invalid report type: " + reportType);
        }
        if (!QAReportStatus.isValidValue(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }
}
