package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectSessionRepository extends JpaRepository<SubjectSession, Long> {

    List<SubjectSession> findByPhaseIdOrderBySequenceNoAsc(Long phaseId);


    @Query("SELECT COUNT(ss) FROM SubjectSession ss WHERE ss.phase.subject.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);
}