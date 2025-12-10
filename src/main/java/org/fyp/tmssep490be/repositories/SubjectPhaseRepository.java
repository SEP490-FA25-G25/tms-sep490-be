package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectPhaseRepository extends JpaRepository<SubjectPhase, Long> {

    // Tìm phase theo subject, sắp xếp theo phaseNumber
    @Query("SELECT cp FROM SubjectPhase cp WHERE cp.subject.id = :subjectId ORDER BY cp.phaseNumber")
    List<SubjectPhase> findBySubjectIdOrderByPhaseNumber(@Param("subjectId") Long subjectId);

    // Tìm phase theo subject và phaseNumber
    @Query("SELECT cp FROM SubjectPhase cp WHERE cp.subject.id = :subjectId AND cp.phaseNumber = :phaseNumber")
    SubjectPhase findBySubjectIdAndPhaseNumber(@Param("subjectId") Long subjectId,
            @Param("phaseNumber") Integer phaseNumber);

    @Modifying
    @Query("DELETE FROM SubjectPhase cp WHERE cp.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Long subjectId);
}