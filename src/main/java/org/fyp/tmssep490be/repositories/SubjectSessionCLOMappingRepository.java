package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectSessionCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectSessionCLOMappingRepository extends JpaRepository<SubjectSessionCLOMapping, SubjectSessionCLOMapping.SubjectSessionCLOMappingId> {
    // Tìm mapping theo subjectSession
    List<SubjectSessionCLOMapping> findBySubjectSessionId(Long subjectSessionId);
}
