package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentFeedbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentFeedbackResponseRepository extends JpaRepository<StudentFeedbackResponse, Long> {
}
