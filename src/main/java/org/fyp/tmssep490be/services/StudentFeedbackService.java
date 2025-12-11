package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
import org.fyp.tmssep490be.dtos.studentfeedback.*;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.FeedbackQuestion;
import org.fyp.tmssep490be.entities.StudentFeedback;
import org.fyp.tmssep490be.entities.StudentFeedbackResponse;
import org.fyp.tmssep490be.exceptions.InvalidRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.FeedbackQuestionRepository;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentFeedbackService {

    private final StudentFeedbackRepository studentFeedbackRepository;
    private final FeedbackQuestionRepository feedbackQuestionRepository;
    private final ClassRepository classRepository;
    private final UserBranchesRepository userBranchesRepository;

    @Transactional(readOnly = true)
    public List<StudentFeedbackQuestionDTO> getFeedbackQuestions() {
        List<FeedbackQuestion> questions = feedbackQuestionRepository.findAll(
                Sort.by(Sort.Order.asc("displayOrder"), Sort.Order.asc("id")));

        return questions.stream()
                .map(q -> StudentFeedbackQuestionDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .questionType(q.getQuestionType())
                        .options(q.getOptions())
                        .displayOrder(q.getDisplayOrder())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentFeedbackListItemDTO> getStudentFeedbacks(Long studentId, String status, Long classId, Long phaseId, String search) {
        log.info("Getting feedbacks for student={}, status={}, classId={}, phaseId={}, search={}", studentId, status, classId, phaseId, search);

        Boolean isFeedback = null;
        if ("PENDING".equalsIgnoreCase(status)) {
            isFeedback = false;
        } else if ("SUBMITTED".equalsIgnoreCase(status)) {
            isFeedback = true;
        }

        List<StudentFeedback> feedbacks = studentFeedbackRepository.findAllByStudentIdWithFilters(
                studentId, isFeedback, classId, phaseId, search);

        return feedbacks.stream()
                .map(this::mapToListItemDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentFeedbackListItemDTO getStudentFeedbackDetail(Long feedbackId, Long studentId) {
        log.info("Getting feedback detail id={} for student={}", feedbackId, studentId);

        StudentFeedback feedback = studentFeedbackRepository.findByIdForStudent(feedbackId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback không tồn tại hoặc không thuộc về bạn"));

        return mapToListItemDTO(feedback);
    }

    @Transactional(readOnly = true)
    public List<StudentPendingFeedbackDTO> getPendingFeedbacksForStudent(Long studentId) {
        log.info("Student {} lấy danh sách feedback pending", studentId);

        List<StudentFeedback> feedbacks = studentFeedbackRepository.findPendingByStudentId(studentId);

        return feedbacks.stream()
                .map(sf -> StudentPendingFeedbackDTO.builder()
                        .feedbackId(sf.getId())
                        .classId(sf.getClassEntity().getId())
                        .classCode(sf.getClassEntity().getCode())
                        .className(sf.getClassEntity().getName())
                        .subjectName(sf.getClassEntity().getSubject().getName())
                        .phaseId(sf.getPhase() != null ? sf.getPhase().getId() : null)
                        .phaseName(sf.getPhase() != null ? sf.getPhase().getName() : null)
                        .createdAt(sf.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countPendingFeedbacks(Long studentId) {
        return studentFeedbackRepository.countPendingByStudentId(studentId);
    }

    @Transactional
    public StudentFeedbackSubmitResponse submitFeedback(Long feedbackId, Long studentId, StudentFeedbackSubmitRequest request) {
        log.info("Student {} nộp feedback {}", studentId, feedbackId);

        StudentFeedback feedback = studentFeedbackRepository.findByIdForStudent(feedbackId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback không tồn tại hoặc không thuộc về bạn"));

        if (Boolean.TRUE.equals(feedback.getIsFeedback())) {
            throw new InvalidRequestException("Feedback này đã được nộp rồi");
        }

        // Validate questions
        List<Long> questionIds = request.getResponses().stream()
                .map(StudentFeedbackSubmitRequest.ResponseItem::getQuestionId)
                .toList();

        List<FeedbackQuestion> questions = feedbackQuestionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()) {
            throw new InvalidRequestException("Một số câu hỏi không hợp lệ");
        }

        Map<Long, FeedbackQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(FeedbackQuestion::getId, q -> q));

        // Clear old responses and add new ones
        if (feedback.getStudentFeedbackResponses() != null) {
            feedback.getStudentFeedbackResponses().clear();
        } else {
            feedback.setStudentFeedbackResponses(new HashSet<>());
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (StudentFeedbackSubmitRequest.ResponseItem item : request.getResponses()) {
            FeedbackQuestion question = questionMap.get(item.getQuestionId());
            if (question == null) {
                throw new InvalidRequestException("Câu hỏi không hợp lệ");
            }

            StudentFeedbackResponse response = StudentFeedbackResponse.builder()
                    .feedback(feedback)
                    .question(question)
                    .rating(item.getRating())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            feedback.getStudentFeedbackResponses().add(response);
        }

        feedback.setResponse(request.getComment());
        feedback.setIsFeedback(true);
        feedback.setSubmittedAt(now);
        feedback.setUpdatedAt(now);

        StudentFeedback saved = studentFeedbackRepository.save(feedback);
        Double avg = calculateAverageRatingForFeedback(saved);

        return StudentFeedbackSubmitResponse.builder()
                .feedbackId(saved.getId())
                .isFeedback(saved.getIsFeedback())
                .submittedAt(saved.getSubmittedAt())
                .averageRating(avg)
                .build();
    }

    private StudentFeedbackListItemDTO mapToListItemDTO(StudentFeedback sf) {
        List<StudentFeedbackListItemDTO.FeedbackResponseItem> responses = null;

        // Only include responses if feedback was submitted
        if (Boolean.TRUE.equals(sf.getIsFeedback()) && sf.getStudentFeedbackResponses() != null) {
            responses = sf.getStudentFeedbackResponses().stream()
                    .filter(r -> r.getQuestion() != null)
                    .sorted((a, b) -> {
                        Integer orderA = a.getQuestion().getDisplayOrder();
                        Integer orderB = b.getQuestion().getDisplayOrder();
                        if (orderA == null && orderB == null) return 0;
                        if (orderA == null) return 1;
                        if (orderB == null) return -1;
                        return orderA.compareTo(orderB);
                    })
                    .map(r -> StudentFeedbackListItemDTO.FeedbackResponseItem.builder()
                            .questionId(r.getQuestion().getId())
                            .questionText(r.getQuestion().getQuestionText())
                            .rating(r.getRating() != null ? r.getRating().intValue() : null)
                            .displayOrder(r.getQuestion().getDisplayOrder())
                            .build())
                    .collect(Collectors.toList());
        }

        return StudentFeedbackListItemDTO.builder()
                .feedbackId(sf.getId())
                .classId(sf.getClassEntity().getId())
                .classCode(sf.getClassEntity().getCode())
                .className(sf.getClassEntity().getName())
                .subjectName(sf.getClassEntity().getSubject().getName())
                .phaseId(sf.getPhase() != null ? sf.getPhase().getId() : null)
                .phaseName(sf.getPhase() != null ? sf.getPhase().getName() : null)
                .isFeedback(sf.getIsFeedback())
                .submittedAt(sf.getSubmittedAt())
                .averageRating(calculateAverageRatingForFeedback(sf))
                .comment(sf.getResponse())
                .responses(responses)
                .createdAt(sf.getCreatedAt())
                .build();
    }

    private Double calculateAverageRatingForFeedback(StudentFeedback feedback) {
        if (feedback.getStudentFeedbackResponses() == null || feedback.getStudentFeedbackResponses().isEmpty()) {
            return null;
        }

        double rating = feedback.getStudentFeedbackResponses().stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(r -> r.getRating())
                .average()
                .orElse(Double.NaN);

        if (Double.isNaN(rating)) {
            return null;
        }
        return rating;
    }

    private ClassEntity ensureClassAccessible(Long classId, Long userId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Lớp không tồn tại"));

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

    @Transactional(readOnly = true)
    public StudentFeedbackListResponse getClassFeedbacks(Long classId, Long phaseId, Boolean isFeedback,
                                                          Pageable pageable, Long userId) {
        log.info("QA getting feedbacks for classId={}, phaseId={}, isFeedback={}", classId, phaseId, isFeedback);

        ensureClassAccessible(classId, userId);

        Page<StudentFeedback> feedbacks = studentFeedbackRepository.findByClassIdWithFilters(
                classId, phaseId, isFeedback, pageable
        );

        long submittedCount = studentFeedbackRepository.countSubmittedFeedbacksByClassIdAndPhase(classId, phaseId);
        
        // Thống kê thông minh: xử lý trường hợp có filter
        long totalStudents;
        if (phaseId != null || isFeedback != null) {
            totalStudents = feedbacks.getTotalElements();
        } else {
            long activeEnrollments = studentFeedbackRepository.countActiveStudentsByClassId(classId);
            long totalFeedbackRecords = studentFeedbackRepository.countTotalFeedbacksByClassId(classId);
            totalStudents = Math.max(activeEnrollments, totalFeedbackRecords);
        }

        long notSubmittedCount = Math.max(0, totalStudents - submittedCount);
        double submissionRate = totalStudents > 0 ? (double) submittedCount / totalStudents * 100 : 0.0;

        List<Double> ratings = feedbacks.getContent().stream()
                .map(this::calculateAverageRatingForFeedback)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        double averageRating = ratings.isEmpty()
                ? 0.0
                : ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        int positiveFeedbackCount = (int) ratings.stream().filter(r -> r >= 4.0).count();
        int negativeFeedbackCount = (int) ratings.stream().filter(r -> r <= 2.5).count();

        StudentFeedbackListResponse.FeedbackStatistics statistics = StudentFeedbackListResponse.FeedbackStatistics.builder()
                .totalStudents((int) totalStudents)
                .submittedCount((int) submittedCount)
                .notSubmittedCount((int) notSubmittedCount)
                .submissionRate(submissionRate)
                .averageRating(averageRating)
                .positiveFeedbackCount(positiveFeedbackCount)
                .negativeFeedbackCount(negativeFeedbackCount)
                .build();

        Page<StudentFeedbackListResponse.StudentFeedbackItemDTO> feedbackItems = feedbacks.map(sf -> {
            String responsePreview = sf.getResponse() != null && sf.getResponse().length() > 100
                    ? sf.getResponse().substring(0, 100) + "..."
                    : sf.getResponse();

            Double rating = calculateAverageRatingForFeedback(sf);

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

    @Transactional(readOnly = true)
    public StudentFeedbackDetailDTO getFeedbackDetail(Long feedbackId, Long userId) {
        log.info("QA getting feedback detail id={}", feedbackId);

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

        Double rating = calculateAverageRatingForFeedback(feedback);

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
