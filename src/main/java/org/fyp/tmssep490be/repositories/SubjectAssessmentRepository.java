package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssessmentRepository extends JpaRepository<SubjectAssessment, Long> {

    List<SubjectAssessment> findBySubjectIdOrderByIdAsc(Long subjectId);

    void deleteBySubjectId(Long subjectId);
}