package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.ReplacementSkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplacementSkillAssessmentRepository extends JpaRepository<ReplacementSkillAssessment, Long> {

    /**
     * Find all skill assessments for multiple students (batch fetch)
     */
    @Query("SELECT rsa FROM ReplacementSkillAssessment rsa " +
           "WHERE rsa.student.id IN :studentIds " +
           "ORDER BY rsa.assessmentDate DESC")
    List<ReplacementSkillAssessment> findByStudentIdIn(@Param("studentIds") List<Long> studentIds);

}
