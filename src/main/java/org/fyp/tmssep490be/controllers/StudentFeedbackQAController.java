package org.fyp.tmssep490be.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
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
public class StudentFeedbackQAController {

    private final StudentFeedbackService studentFeedbackService;

    @GetMapping("/classes/{classId}/feedbacks")
    @PreAuthorize("hasAnyRole('QA','MANAGER','CENTER_HEAD')")
    public ResponseEntity<ResponseObject<StudentFeedbackListResponse>> getClassFeedbacks(
        @PathVariable Long classId,
        @RequestParam(required = false) Long phaseId,
        @RequestParam(required = false) Boolean isFeedback,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("User {} requesting feedbacks for classId={}, phaseId={}, isFeedback={}",
                 currentUser.getId(), classId, phaseId, isFeedback);

        Pageable pageable = PageRequest.of(page, size);

        StudentFeedbackListResponse response = studentFeedbackService.getClassFeedbacks(
            classId, phaseId, isFeedback, pageable, currentUser.getId()
        );

        return ResponseEntity.ok(ResponseObject.<StudentFeedbackListResponse>builder()
            .success(true)
            .message("Class feedbacks retrieved successfully")
            .data(response)
            .build());
    }

    @GetMapping("/feedbacks/{feedbackId}")
    @PreAuthorize("hasAnyRole('QA','MANAGER','CENTER_HEAD')")
    public ResponseEntity<ResponseObject<StudentFeedbackDetailDTO>> getFeedbackDetail(
        @PathVariable Long feedbackId,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Requesting feedback detail for feedbackId={}", feedbackId);

        StudentFeedbackDetailDTO feedback = studentFeedbackService.getFeedbackDetail(feedbackId, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<StudentFeedbackDetailDTO>builder()
            .success(true)
            .message("Feedback detail retrieved successfully")
            .data(feedback)
            .build());
    }
}
