package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectAssessmentCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssessmentCLOMappingRepository extends JpaRepository<SubjectAssessmentCLOMapping, Long> {
    // Tìm mapping theo assessment - dùng derived query như deprecated
    List<SubjectAssessmentCLOMapping> findByAssessmentId(Long assessmentId);
}