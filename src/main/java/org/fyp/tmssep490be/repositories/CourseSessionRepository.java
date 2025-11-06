package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

    /**
     * Find course sessions by course ID ordered by phase ASC, sequence ASC
     * Used for session generation in Create Class workflow
     */
    List<CourseSession> findByPhase_Course_IdOrderByPhaseAscSequenceNoAsc(Long courseId);

    /**
     * Count course sessions for a course
     */
    @Query("SELECT COUNT(cs) FROM CourseSession cs WHERE cs.phase.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);
}
