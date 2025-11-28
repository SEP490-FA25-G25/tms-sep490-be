package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.qa.StudentFeedbackDetailDTO;
import org.fyp.tmssep490be.dtos.qa.StudentFeedbackListResponse;
import org.springframework.data.domain.Pageable;

public interface StudentFeedbackService {
    StudentFeedbackListResponse getClassFeedbacks(Long classId, Long phaseId, Boolean isFeedback,
                                                  Pageable pageable, Long userId);

    StudentFeedbackDetailDTO getFeedbackDetail(Long feedbackId, Long userId);
}
