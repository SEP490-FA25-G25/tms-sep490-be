package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CoursePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoursePhaseRepository extends JpaRepository<CoursePhase, Long> {

    @Query("SELECT cp FROM CoursePhase cp WHERE cp.course.id = :courseId ORDER BY cp.phaseNumber")
    List<CoursePhase> findByCourseIdOrderByPhaseNumber(@Param("courseId") Long courseId);

    @Query("SELECT cp FROM CoursePhase cp WHERE cp.course.id = :courseId AND cp.phaseNumber = :phaseNumber")
    CoursePhase findByCourseIdAndPhaseNumber(@Param("courseId") Long courseId, @Param("phaseNumber") Integer phaseNumber);
}
