package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectAssessmentCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssessmentCLOMappingRepository extends JpaRepository<SubjectAssessmentCLOMapping, SubjectAssessmentCLOMapping.SubjectAssessmentCLOMappingId> {
    // Tìm mapping theo subjectAssessment
    List<SubjectAssessmentCLOMapping> findBySubjectAssessmentId(Long subjectAssessmentId);
}
