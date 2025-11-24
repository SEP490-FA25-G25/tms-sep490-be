package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {

    @Query("SELECT m FROM CourseMaterial m WHERE m.course.id = :courseId AND m.phase IS NULL AND m.courseSession IS NULL")
    List<CourseMaterial> findCourseLevelMaterials(@Param("courseId") Long courseId);

    @Query("SELECT m FROM CourseMaterial m WHERE m.phase.id = :phaseId AND m.courseSession IS NULL")
    List<CourseMaterial> findPhaseLevelMaterials(@Param("phaseId") Long phaseId);

    @Query("SELECT m FROM CourseMaterial m WHERE m.courseSession.id = :sessionId")
    List<CourseMaterial> findSessionLevelMaterials(@Param("sessionId") Long sessionId);

    @Query("SELECT m FROM CourseMaterial m WHERE m.course.id = :courseId")
    List<CourseMaterial> findByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(m) FROM CourseMaterial m WHERE m.course.id = :courseId")
    long countByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(m) FROM CourseMaterial m WHERE m.courseSession.id = :sessionId")
    long countByCourseSessionId(@Param("sessionId") Long sessionId);
}
