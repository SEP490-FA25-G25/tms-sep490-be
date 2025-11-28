package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.StudentFeedback;
import org.fyp.tmssep490be.exceptions.InvalidRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.services.StudentFeedbackService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentFeedbackServiceImpl implements StudentFeedbackService {

    private final StudentFeedbackRepository studentFeedbackRepository;
    private final ClassRepository classRepository;
    private final UserBranchesRepository userBranchesRepository;

    private ClassEntity ensureClassAccessible(Long classId, Long userId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class không tồn tại"));

        var branch = classEntity.getBranch();
        if (branch == null) {
            throw new InvalidRequestException("Lớp không có thông tin chi nhánh");
        }

        boolean hasAccess = userBranchesRepository.findBranchIdsByUserId(userId)
                .stream()
                .anyMatch(id -> id.equals(branch.getId()));

        if (!hasAccess) {
            throw new InvalidRequestException("Bạn không có quyền truy cập lớp thuộc chi nhánh này");
        }

        return classEntity;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFeedbackListResponse getClassFeedbacks(Long classId, Long phaseId, Boolean isFeedback,
                                                          Pageable pageable, Long userId) {
        log.info("Getting feedbacks for classId={}, phaseId={}, isFeedback={}", classId, phaseId, isFeedback);

        ensureClassAccessible(classId, userId);

        Page<StudentFeedback> feedbacks = studentFeedbackRepository.findByClassIdWithFilters(
                classId, phaseId, isFeedback, pageable
        );

        // Nếu có lọc phase/status, sử dụng tổng theo tập dữ liệu đã lọc để tránh lệch thống kê
        long totalStudents = (phaseId != null || isFeedback != null)
                ? feedbacks.getTotalElements()
                : studentFeedbackRepository.countActiveStudentsByClassId(classId);

        long submittedCount = studentFeedbackRepository.countSubmittedFeedbacksByClassIdAndPhase(classId, phaseId);
        long notSubmittedCount = Math.max(0, totalStudents - submittedCount);
        double submissionRate = totalStudents > 0 ? (double) submittedCount / totalStudents * 100 : 0.0;

        StudentFeedbackListResponse.FeedbackStatistics statistics = StudentFeedbackListResponse.FeedbackStatistics.builder()
                .totalStudents((int) totalStudents)
                .submittedCount((int) submittedCount)
                .notSubmittedCount((int) notSubmittedCount)
                .submissionRate(submissionRate)
                .build();

        Page<StudentFeedbackListResponse.StudentFeedbackItemDTO> feedbackItems = feedbacks.map(sf -> {
            String responsePreview = sf.getResponse() != null && sf.getResponse().length() > 100
                    ? sf.getResponse().substring(0, 100) + "..."
                    : sf.getResponse();

            Double rating = null;
            if (sf.getStudentFeedbackResponses() != null && !sf.getStudentFeedbackResponses().isEmpty()) {
                rating = sf.getStudentFeedbackResponses().stream()
                        .filter(r -> r.getRating() != null)
                        .mapToInt(r -> r.getRating())
                        .average()
                        .orElse(Double.NaN);
                if (rating.isNaN()) {
                    rating = null;
                }
            }

            String sentiment = null;
            if (rating != null) {
                if (rating >= 4.0) sentiment = "positive";
                else if (rating <= 2.5) sentiment = "negative";
                else sentiment = "neutral";
            }

            return StudentFeedbackListResponse.StudentFeedbackItemDTO.builder()
                    .feedbackId(sf.getId())
                    .studentId(sf.getStudent().getId())
                    .studentName(sf.getStudent().getUserAccount().getFullName())
                    .phaseId(sf.getPhase() != null ? sf.getPhase().getId() : null)
                    .phaseName(sf.getPhase() != null ? sf.getPhase().getName() : null)
                    .isFeedback(sf.getIsFeedback())
                    .submittedAt(sf.getSubmittedAt())
                    .responsePreview(responsePreview)
                    .rating(rating)
                    .sentiment(sentiment)
                    .build();
        });

        return StudentFeedbackListResponse.builder()
                .statistics(statistics)
                .feedbacks(feedbackItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFeedbackDetailDTO getFeedbackDetail(Long feedbackId, Long userId) {
        log.info("Getting feedback detail id={}", feedbackId);

        StudentFeedback feedback = studentFeedbackRepository.findByIdWithDetails(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback không tồn tại"));

        var branch = feedback.getClassEntity().getBranch();
        if (branch == null || userBranchesRepository.findBranchIdsByUserId(userId).stream().noneMatch(id -> id.equals(branch.getId()))) {
            throw new InvalidRequestException("Bạn không có quyền truy cập phản hồi thuộc chi nhánh này");
        }

        List<StudentFeedbackDetailDTO.FeedbackResponseItem> detailedResponses = feedback.getStudentFeedbackResponses().stream()
                .map(sfr -> StudentFeedbackDetailDTO.FeedbackResponseItem.builder()
                        .questionId(sfr.getQuestion().getId())
                        .questionText(sfr.getQuestion().getQuestionText())
                        .answerText(sfr.getRating() != null ? sfr.getRating().toString() : null)
                        .build())
                .collect(Collectors.toList());

        Double rating = null;
        if (feedback.getStudentFeedbackResponses() != null && !feedback.getStudentFeedbackResponses().isEmpty()) {
            rating = feedback.getStudentFeedbackResponses().stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(r -> r.getRating())
                    .average()
                    .orElse(Double.NaN);
            if (rating.isNaN()) {
                rating = null;
            }
        }

        String sentiment = null;
        if (rating != null) {
            if (rating >= 4.0) sentiment = "positive";
            else if (rating <= 2.5) sentiment = "negative";
            else sentiment = "neutral";
        }

        return StudentFeedbackDetailDTO.builder()
                .feedbackId(feedback.getId())
                .studentId(feedback.getStudent().getId())
                .studentName(feedback.getStudent().getUserAccount().getFullName())
                .classId(feedback.getClassEntity().getId())
                .classCode(feedback.getClassEntity().getCode())
                .phaseId(feedback.getPhase() != null ? feedback.getPhase().getId() : null)
                .phaseName(feedback.getPhase() != null ? feedback.getPhase().getName() : null)
                .isFeedback(feedback.getIsFeedback())
                .submittedAt(feedback.getSubmittedAt())
                .response(feedback.getResponse())
                .rating(rating)
                .sentiment(sentiment)
                .detailedResponses(detailedResponses)
                .build();
    }
}
