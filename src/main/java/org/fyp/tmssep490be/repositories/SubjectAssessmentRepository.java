package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectAssessmentRepository extends JpaRepository<SubjectAssessment, Long> {

    // Tìm assessment theo subject
    @Query("SELECT ca FROM SubjectAssessment ca WHERE ca.subject.id = :subjectId")
    List<SubjectAssessment> findBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying
    @Query("DELETE FROM SubjectAssessment ca WHERE ca.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Long subjectId);
}