package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
import org.fyp.tmssep490be.dtos.studentfeedback.StudentFeedbackQuestionDTO;
import org.fyp.tmssep490be.dtos.studentfeedback.StudentFeedbackSubmitRequest;
import org.fyp.tmssep490be.dtos.studentfeedback.StudentFeedbackSubmitResponse;
import org.fyp.tmssep490be.dtos.studentfeedback.StudentPendingFeedbackDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentFeedbackService {
    StudentFeedbackListResponse getClassFeedbacks(Long classId, Long phaseId, Boolean isFeedback,
                                                  Pageable pageable, Long userId);

    StudentFeedbackDetailDTO getFeedbackDetail(Long feedbackId, Long userId);

    List<StudentPendingFeedbackDTO> getPendingFeedbacksForStudent(Long studentId);

    StudentFeedbackSubmitResponse submitFeedback(Long feedbackId, Long studentId, StudentFeedbackSubmitRequest request);

    long countPendingFeedbacks(Long studentId);

    List<StudentFeedbackQuestionDTO> getFeedbackQuestions();
}
