package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectSessionRepository extends JpaRepository<SubjectSession, Long> {

    // Tìm session theo subject, sắp xếp theo phase.phaseNumber và sequenceNo
    @Query("SELECT ss FROM SubjectSession ss WHERE ss.phase.subject.id = :subjectId ORDER BY ss.phase.phaseNumber ASC, ss.sequenceNo ASC")
    List<SubjectSession> findBySubjectIdOrderByPhaseAndSequence(@Param("subjectId") Long subjectId);

    // Đếm số session theo subject
    @Query("SELECT COUNT(ss) FROM SubjectSession ss WHERE ss.phase.subject.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);

    // Tìm session theo phase, sắp xếp theo sequenceNo
    @Query("SELECT ss FROM SubjectSession ss WHERE ss.phase.id = :phaseId ORDER BY ss.sequenceNo")
    List<SubjectSession> findByPhaseIdOrderBySequenceNo(@Param("phaseId") Long phaseId);

    // Tìm session theo subject, sắp xếp theo phase và sequenceNo
    @Query("SELECT ss FROM SubjectSession ss WHERE ss.phase.subject.id = :subjectId ORDER BY ss.phase.id, ss.sequenceNo")
    List<SubjectSession> findBySubjectIdOrderByPhaseIdAndSequenceNo(@Param("subjectId") Long subjectId);
}
