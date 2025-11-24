package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long> {

    @Query("SELECT ca FROM CourseAssessment ca WHERE ca.course.id = :courseId")
    List<CourseAssessment> findByCourseId(@Param("courseId") Long courseId);
}
