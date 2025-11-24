package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.CourseAssessment;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    @Query("SELECT s FROM Score s WHERE s.student.id = :studentId AND s.assessment.courseAssessment.id = :courseAssessmentId")
    Optional<Score> findByEnrollmentAndAssessment(@Param("studentId") Long studentId, @Param("courseAssessmentId") Long courseAssessmentId);

    /**
     * Find score by student and assessment
     */
    Optional<Score> findByStudentIdAndAssessment(Long studentId, Assessment assessment);

    /**
     * Find scores by student and assessment
     */
    @Query("SELECT s FROM Score s WHERE s.student.id = :studentId AND s.assessment.id = :assessmentId")
    Optional<Score> findByStudentIdAndAssessmentId(@Param("studentId") Long studentId, @Param("assessmentId") Long assessmentId);

    /**
     * Find all scores for a student in a specific class
     */
    @Query("SELECT s FROM Score s " +
           "WHERE s.student.id = :studentId " +
           "AND s.assessment.classEntity.id = :classId")
    List<Score> findByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);

  /**
     * Calculate average percentage score for a student across all graded assessments
     * Normalizes different assessment scales (e.g., quiz max 20, exam max 100) to percentage
     */
    @Query("SELECT COALESCE(AVG((s.score / ca.maxScore) * 100), 0) " +
           "FROM Score s " +
           "JOIN s.assessment a " +
           "JOIN a.courseAssessment ca " +
           "WHERE s.student.id = :studentId AND s.gradedAt IS NOT NULL")
    java.math.BigDecimal calculateAverageScore(@Param("studentId") Long studentId);
}
