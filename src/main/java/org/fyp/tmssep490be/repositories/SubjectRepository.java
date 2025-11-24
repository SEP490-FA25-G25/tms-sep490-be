package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    /**
     * Find subjects by status, ordered by code
     */
    List<Subject> findByStatusOrderByCode(SubjectStatus status);

    boolean existsByCode(String code);
}
