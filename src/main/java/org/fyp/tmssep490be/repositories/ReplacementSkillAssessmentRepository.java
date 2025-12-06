package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.ReplacementSkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplacementSkillAssessmentRepository extends JpaRepository<ReplacementSkillAssessment, Long> {
}
