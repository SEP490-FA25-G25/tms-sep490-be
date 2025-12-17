package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.feedbackquestion.CreateFeedbackQuestionRequest;
import org.fyp.tmssep490be.dtos.feedbackquestion.FeedbackQuestionDTO;
import org.fyp.tmssep490be.dtos.feedbackquestion.UpdateFeedbackQuestionRequest;
import org.fyp.tmssep490be.entities.FeedbackQuestion;
import org.fyp.tmssep490be.entities.enums.FeedbackQuestionStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.FeedbackQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackQuestionService {

    private final FeedbackQuestionRepository feedbackQuestionRepository;

    @Transactional(readOnly = true)
    public List<FeedbackQuestionDTO> getAllQuestions() {
        log.info("Getting all feedback questions");
        List<FeedbackQuestion> questions = feedbackQuestionRepository.findAllByOrderByDisplayOrderAsc();
        return questions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackQuestionDTO> getActiveQuestions() {
        log.info("Getting active feedback questions");
        List<FeedbackQuestion> questions = feedbackQuestionRepository
                .findAllByStatusOrderByDisplayOrderAsc(FeedbackQuestionStatus.ACTIVE);
        return questions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FeedbackQuestionDTO getQuestionById(Long id) {
        log.info("Getting feedback question by id={}", id);
        FeedbackQuestion question = feedbackQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi feedback không tồn tại"));
        return mapToDTO(question);
    }

    @Transactional
    public FeedbackQuestionDTO createQuestion(CreateFeedbackQuestionRequest request) {
        log.info("Creating new feedback question: {}", request.getQuestionText());
        
        FeedbackQuestion question = FeedbackQuestion.builder()
                .questionText(request.getQuestionText())
                .displayOrder(request.getDisplayOrder())
                .status(FeedbackQuestionStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        
        FeedbackQuestion saved = feedbackQuestionRepository.save(question);
        log.info("Created feedback question id={}", saved.getId());
        
        return mapToDTO(saved);
    }

    @Transactional
    public FeedbackQuestionDTO updateQuestion(Long id, UpdateFeedbackQuestionRequest request) {
        log.info("Updating feedback question id={}", id);
        
        FeedbackQuestion question = feedbackQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi feedback không tồn tại"));
        
        question.setQuestionText(request.getQuestionText());
        question.setDisplayOrder(request.getDisplayOrder());
        question.setUpdatedAt(OffsetDateTime.now());
        
        FeedbackQuestion saved = feedbackQuestionRepository.save(question);
        log.info("Updated feedback question id={}", saved.getId());
        
        return mapToDTO(saved);
    }

    @Transactional
    public FeedbackQuestionDTO toggleStatus(Long id) {
        log.info("Toggling status for feedback question id={}", id);
        
        FeedbackQuestion question = feedbackQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi feedback không tồn tại"));
        
        FeedbackQuestionStatus newStatus = question.getStatus() == FeedbackQuestionStatus.ACTIVE
                ? FeedbackQuestionStatus.INACTIVE
                : FeedbackQuestionStatus.ACTIVE;
        
        question.setStatus(newStatus);
        question.setUpdatedAt(OffsetDateTime.now());
        
        FeedbackQuestion saved = feedbackQuestionRepository.save(question);
        log.info("Toggled feedback question id={} status to {}", saved.getId(), newStatus);
        
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        log.info("Deleting feedback question id={}", id);
        
        FeedbackQuestion question = feedbackQuestionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Câu hỏi feedback không tồn tại"));
        
        feedbackQuestionRepository.delete(question);
        log.info("Deleted feedback question id={}", id);
    }

    private FeedbackQuestionDTO mapToDTO(FeedbackQuestion question) {
        return FeedbackQuestionDTO.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .displayOrder(question.getDisplayOrder())
                .status(question.getStatus().name())
                .usageCount(question.getStudentFeedbackResponses() != null ? 
                        question.getStudentFeedbackResponses().size() : 0)
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
