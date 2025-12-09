package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findByStudentIdAndAssessmentId(Long studentId, Long assessmentId);

    @Query("SELECT sc FROM Score sc JOIN sc.assessment a WHERE sc.student.id = :studentId AND a.classEntity.id = :classId")
    List<Score> findByStudentIdAndClassId(@Param("studentId") Long studentId, @Param("classId") Long classId);
}
