package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentfeedback.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentFeedbackService;
import org.fyp.tmssep490be.utils.StudentContextHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/feedbacks")
@RequiredArgsConstructor
@Slf4j
public class StudentFeedbackStudentController {

    private final StudentFeedbackService studentFeedbackService;
    private final StudentContextHelper studentContextHelper;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentFeedbackListItemDTO>>> getAllFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long phaseId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        log.info("Student {} lấy danh sách feedback, status={}, classId={}, phaseId={}, search={}",
                studentId, status, classId, phaseId, search);

        List<StudentFeedbackListItemDTO> data = studentFeedbackService.getStudentFeedbacks(
                studentId, status, classId, phaseId, search);

        return ResponseEntity.ok(ResponseObject.<List<StudentFeedbackListItemDTO>>builder()
                .success(true)
                .message("Lấy danh sách feedback thành công")
                .data(data)
                .build());
    }

    @GetMapping("/{feedbackId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentFeedbackListItemDTO>> getFeedbackDetail(
            @PathVariable Long feedbackId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        log.info("Student {} lấy chi tiết feedback {}", studentId, feedbackId);

        StudentFeedbackListItemDTO data = studentFeedbackService.getStudentFeedbackDetail(feedbackId, studentId);

        return ResponseEntity.ok(ResponseObject.<StudentFeedbackListItemDTO>builder()
                .success(true)
                .message("Lấy chi tiết feedback thành công")
                .data(data)
                .build());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentPendingFeedbackDTO>>> getPendingFeedbacks(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        log.info("Student {} lấy danh sách feedback pending", studentId);

        List<StudentPendingFeedbackDTO> data = studentFeedbackService.getPendingFeedbacksForStudent(studentId);

        return ResponseEntity.ok(ResponseObject.<List<StudentPendingFeedbackDTO>>builder()
                .success(true)
                .message("Lấy danh sách feedback chưa hoàn thành thành công")
                .data(data)
                .build());
    }

    @PostMapping("/{feedbackId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentFeedbackSubmitResponse>> submitFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody StudentFeedbackSubmitRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        log.info("Student {} nộp feedback {}", studentId, feedbackId);

        StudentFeedbackSubmitResponse data = studentFeedbackService.submitFeedback(feedbackId, studentId, request);

        return ResponseEntity.ok(ResponseObject.<StudentFeedbackSubmitResponse>builder()
                .success(true)
                .message("Nộp feedback thành công")
                .data(data)
                .build());
    }

    @GetMapping("/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<StudentFeedbackQuestionDTO>>> getQuestions() {
        List<StudentFeedbackQuestionDTO> questions = studentFeedbackService.getFeedbackQuestions();
        return ResponseEntity.ok(ResponseObject.<List<StudentFeedbackQuestionDTO>>builder()
                .success(true)
                .message("Lấy danh sách câu hỏi thành công")
                .data(questions)
                .build());
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<Long>> countPending(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long studentId = studentContextHelper.getStudentId(currentUser);
        long count = studentFeedbackService.countPendingFeedbacks(studentId);

        return ResponseEntity.ok(ResponseObject.<Long>builder()
                .success(true)
                .message("Lấy số lượng feedback pending thành công")
                .data(count)
                .build());
    }
}
