package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Long> {

    /**
     * Find assessments for a specific class
     */
    List<Assessment> findByClassEntityId(Long classId);

    /**
     * Find assessment by ID with class entity loaded
     */
    @Query("SELECT a FROM Assessment a JOIN FETCH a.classEntity c WHERE a.id = :assessmentId")
    Assessment findByIdWithClass(@Param("assessmentId") Long assessmentId);
}
