package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectPhaseRepository extends JpaRepository<SubjectPhase, Long> {

    List<SubjectPhase> findBySubjectIdOrderByPhaseNumberAsc(Long subjectId);

    void deleteBySubjectId(Long subjectId);
}