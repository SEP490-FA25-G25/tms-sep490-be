package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.feedbackquestion.CreateFeedbackQuestionRequest;
import org.fyp.tmssep490be.dtos.feedbackquestion.FeedbackQuestionDTO;
import org.fyp.tmssep490be.dtos.feedbackquestion.UpdateFeedbackQuestionRequest;
import org.fyp.tmssep490be.services.FeedbackQuestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback-questions")
@RequiredArgsConstructor
@Slf4j
public class FeedbackQuestionController {

    private final FeedbackQuestionService feedbackQuestionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_QA', 'ROLE_MANAGER', 'ROLE_CENTER_HEAD')")
    public ResponseEntity<ResponseObject> getAllQuestions() {
        log.info("GET /api/v1/feedback-questions - Get all feedback questions");
        List<FeedbackQuestionDTO> questions = feedbackQuestionService.getAllQuestions();
        return ResponseEntity.ok(ResponseObject.success("Lấy danh sách câu hỏi thành công", questions));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ROLE_STUDENT', 'ROLE_QA', 'ROLE_MANAGER', 'ROLE_CENTER_HEAD')")
    public ResponseEntity<ResponseObject> getActiveQuestions() {
        log.info("GET /api/v1/feedback-questions/active - Get active feedback questions");
        List<FeedbackQuestionDTO> questions = feedbackQuestionService.getActiveQuestions();
        return ResponseEntity.ok(ResponseObject.success("Lấy danh sách câu hỏi hoạt động thành công", questions));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_QA', 'ROLE_MANAGER', 'ROLE_CENTER_HEAD')")
    public ResponseEntity<ResponseObject> getQuestionById(@PathVariable Long id) {
        log.info("GET /api/v1/feedback-questions/{} - Get feedback question by id", id);
        FeedbackQuestionDTO question = feedbackQuestionService.getQuestionById(id);
        return ResponseEntity.ok(ResponseObject.success("Lấy thông tin câu hỏi thành công", question));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_QA', 'ROLE_MANAGER')")
    public ResponseEntity<ResponseObject> createQuestion(@Valid @RequestBody CreateFeedbackQuestionRequest request) {
        log.info("POST /api/v1/feedback-questions - Create new feedback question");
        FeedbackQuestionDTO question = feedbackQuestionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseObject.success("Tạo câu hỏi thành công", question));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_QA', 'ROLE_MANAGER')")
    public ResponseEntity<ResponseObject> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeedbackQuestionRequest request
    ) {
        log.info("PUT /api/v1/feedback-questions/{} - Update feedback question", id);
        FeedbackQuestionDTO question = feedbackQuestionService.updateQuestion(id, request);
        return ResponseEntity.ok(ResponseObject.success("Cập nhật câu hỏi thành công", question));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ROLE_QA', 'ROLE_MANAGER')")
    public ResponseEntity<ResponseObject> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /api/v1/feedback-questions/{}/toggle-status - Toggle question status", id);
        FeedbackQuestionDTO question = feedbackQuestionService.toggleStatus(id);
        return ResponseEntity.ok(ResponseObject.success("Thay đổi trạng thái câu hỏi thành công", question));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_QA', 'ROLE_MANAGER')")
    public ResponseEntity<ResponseObject> deleteQuestion(@PathVariable Long id) {
        log.info("DELETE /api/v1/feedback-questions/{} - Delete feedback question", id);
        feedbackQuestionService.deleteQuestion(id);
        return ResponseEntity.ok(ResponseObject.success("Xóa câu hỏi thành công", null));
    }
}
