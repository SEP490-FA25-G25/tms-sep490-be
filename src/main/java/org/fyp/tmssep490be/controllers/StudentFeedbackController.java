package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentFeedbackService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Feedback", description = "Student Feedback APIs for QA - View and analyze student feedback")
public class StudentFeedbackController {

    private final StudentFeedbackService studentFeedbackService;

    /**
     * Get Student Feedbacks for a Class
     */
    @GetMapping("/classes/{classId}/feedbacks")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get Class Feedbacks",
        description = "Retrieve student feedbacks for a specific class with optional filters. " +
                      "Includes feedback statistics (total students, submitted count, submission rate) " +
                      "and paginated list of feedbacks. Can filter by phase and submission status."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feedbacks retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Class not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden - user does not have access to this class")
    })
    public ResponseEntity<ResponseObject<StudentFeedbackListResponse>> getClassFeedbacks(
        @PathVariable Long classId,

        @Parameter(description = "Filter by phase ID (optional)")
        @RequestParam(required = false) Long phaseId,

        @Parameter(description = "Filter by submission status: true (submitted), false (not submitted), null (all)")
        @RequestParam(required = false) Boolean isFeedback,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") int size,

        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting feedbacks for classId={}, phaseId={}, isFeedback={}",
                 currentUser.getId(), classId, phaseId, isFeedback);

        Pageable pageable = PageRequest.of(page, size);

        StudentFeedbackListResponse response = studentFeedbackService.getClassFeedbacks(
            classId, phaseId, isFeedback, pageable
        );

        return ResponseEntity.ok(ResponseObject.<StudentFeedbackListResponse>builder()
            .success(true)
            .message("Class feedbacks retrieved successfully")
            .data(response)
            .build());
    }

    /**
     * Get Student Feedback Detail
     */
    @GetMapping("/feedbacks/{feedbackId}")
    @PreAuthorize("hasRole('QA')")
    @Operation(
        summary = "Get Feedback Detail",
        description = "Retrieve detailed information of a specific student feedback including: " +
                      "student info, class/phase info, submission status, full response text, " +
                      "and detailed responses to individual feedback questions (if template-based)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feedback detail retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Feedback not found")
    })
    public ResponseEntity<ResponseObject<StudentFeedbackDetailDTO>> getFeedbackDetail(
        @PathVariable Long feedbackId
    ) {
        log.info("Requesting feedback detail for feedbackId={}", feedbackId);

        StudentFeedbackDetailDTO feedback = studentFeedbackService.getFeedbackDetail(feedbackId);

        return ResponseEntity.ok(ResponseObject.<StudentFeedbackDetailDTO>builder()
            .success(true)
            .message("Feedback detail retrieved successfully")
            .data(feedback)
            .build());
    }
}
