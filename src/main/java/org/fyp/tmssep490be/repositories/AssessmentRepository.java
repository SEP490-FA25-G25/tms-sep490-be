package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByClassEntityId(Long classId);
}
