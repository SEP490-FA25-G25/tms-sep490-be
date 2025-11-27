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

    /**
     * Find scores by session (via assessments linked to course sessions)
     * Used for QA session detail functionality
     */
    @Query("SELECT s FROM Score s " +
           "JOIN FETCH s.student st " +
           "JOIN FETCH s.assessment a " +
           "JOIN FETCH a.classEntity c " +
           "JOIN FETCH c.sessions sess " +
           "JOIN FETCH sess.courseSession cs " +
           "WHERE cs.id = :courseSessionId " +
           "ORDER BY st.userAccount.fullName")
    List<Score> findByCourseSessionIdWithDetails(@Param("courseSessionId") Long courseSessionId);

    /**
     * Count total scores for a course session
     * Fix: Correct JPA query path through Session entity
     */
    @Query("SELECT COUNT(s) FROM Score s " +
           "JOIN s.assessment a " +
           "JOIN a.classEntity c " +
           "JOIN c.sessions sess " +
           "JOIN sess.courseSession cs " +
           "WHERE cs.id = :courseSessionId")
    long countByCourseSessionId(@Param("courseSessionId") Long courseSessionId);

    /**
     * Count scores with passing grades for a course session
     * Fix: Simplified query using 60% of maxScore as passing threshold
     * Note: passingScore logic should be implemented based on business requirements
     */
    @Query("SELECT COUNT(s) FROM Score s " +
           "JOIN s.assessment a " +
           "JOIN a.classEntity c " +
           "JOIN c.sessions sess " +
           "JOIN sess.courseSession cs " +
           "JOIN a.courseAssessment ca " +
           "WHERE cs.id = :courseSessionId " +
           "AND (s.score / ca.maxScore) >= 0.6")
    long countPassingByCourseSessionId(@Param("courseSessionId") Long courseSessionId);

    @Query("SELECT s FROM Score s WHERE s.student.id = :studentId AND s.assessment.courseAssessment.id = :courseAssessmentId")
    Optional<Score> findByEnrollmentAndAssessment(@Param("studentId") Long studentId, @Param("courseAssessmentId") Long courseAssessmentId);

    /**
     * Find assessments for a specific course session
     * Helper method for better query performance
     */
    @Query("SELECT a FROM Assessment a " +
           "JOIN a.classEntity c " +
           "JOIN c.sessions sess " +
           "JOIN sess.courseSession cs " +
           "WHERE cs.id = :courseSessionId")
    List<Assessment> findAssessmentsByCourseSessionId(@Param("courseSessionId") Long courseSessionId);

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
     * Fix: Simplified query without problematic join paths
     */
    @Query("SELECT COALESCE(AVG((s.score / a.courseAssessment.maxScore) * 100), 0) " +
           "FROM Score s " +
           "JOIN s.assessment a " +
           "WHERE s.student.id = :studentId AND s.gradedAt IS NOT NULL")
    java.math.BigDecimal calculateAverageScore(@Param("studentId") Long studentId);
}
