package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, StudentSession.StudentSessionId> {

    List<StudentSession> findBySessionId(Long sessionId);

}
