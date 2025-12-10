package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectSessionCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectSessionCLOMappingRepository extends JpaRepository<SubjectSessionCLOMapping, Long> {
    // Tìm mapping theo session - dùng derived query như deprecated
    List<SubjectSessionCLOMapping> findBySessionId(Long sessionId);
}