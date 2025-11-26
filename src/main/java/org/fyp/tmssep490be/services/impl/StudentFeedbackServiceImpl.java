package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
import org.fyp.tmssep490be.entities.StudentFeedback;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
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

    @Override
    @Transactional(readOnly = true)
    public StudentFeedbackListResponse getClassFeedbacks(Long classId, Long phaseId, Boolean isFeedback,
                                                          Pageable pageable) {
        log.info("Getting feedbacks for classId={}, phaseId={}, isFeedback={}", classId, phaseId, isFeedback);

        Page<StudentFeedback> feedbacks = studentFeedbackRepository.findByClassIdWithFilters(
                classId, phaseId, isFeedback, pageable
        );

        long totalStudents = studentFeedbackRepository.countActiveStudentsByClassId(classId);
        long submittedCount = studentFeedbackRepository.countSubmittedFeedbacksByClassId(classId);
        long notSubmittedCount = totalStudents - submittedCount;
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

            return StudentFeedbackListResponse.StudentFeedbackItemDTO.builder()
                    .feedbackId(sf.getId())
                    .studentId(sf.getStudent().getId())
                    .studentName(sf.getStudent().getUserAccount().getFullName())
                    .phaseId(sf.getPhase() != null ? sf.getPhase().getId() : null)
                    .phaseName(sf.getPhase() != null ? sf.getPhase().getName() : null)
                    .isFeedback(sf.getIsFeedback())
                    .submittedAt(sf.getSubmittedAt())
                    .responsePreview(responsePreview)
                    .build();
        });

        return StudentFeedbackListResponse.builder()
                .statistics(statistics)
                .feedbacks(feedbackItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFeedbackDetailDTO getFeedbackDetail(Long feedbackId) {
        log.info("Getting feedback detail id={}", feedbackId);

        StudentFeedback feedback = studentFeedbackRepository.findByIdWithDetails(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback không tồn tại"));

        List<StudentFeedbackDetailDTO.FeedbackResponseItem> detailedResponses = feedback.getStudentFeedbackResponses().stream()
                .map(sfr -> StudentFeedbackDetailDTO.FeedbackResponseItem.builder()
                        .questionId(sfr.getQuestion().getId())
                        .questionText(sfr.getQuestion().getQuestionText())
                        .answerText(sfr.getRating() != null ? sfr.getRating().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return StudentFeedbackDetailDTO.builder()
                .feedbackId(feedback.getId())
                .studentId(feedback.getStudent().getId())
                .studentName(feedback.getStudent().getUserAccount().getFullName())
                .classId(feedback.getClassEntity().getId())
                .classCode(feedback.getClassEntity().getName())
                .phaseId(feedback.getPhase() != null ? feedback.getPhase().getId() : null)
                .phaseName(feedback.getPhase() != null ? feedback.getPhase().getName() : null)
                .isFeedback(feedback.getIsFeedback())
                .submittedAt(feedback.getSubmittedAt())
                .response(feedback.getResponse())
                .detailedResponses(detailedResponses)
                .build();
    }
}
