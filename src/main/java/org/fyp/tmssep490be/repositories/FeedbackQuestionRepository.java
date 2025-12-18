package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.FeedbackQuestion;
import org.fyp.tmssep490be.entities.enums.FeedbackQuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {
    List<FeedbackQuestion> findAllByStatusOrderByDisplayOrderAsc(FeedbackQuestionStatus status);
    
    List<FeedbackQuestion> findAllByOrderByDisplayOrderAsc();
}
