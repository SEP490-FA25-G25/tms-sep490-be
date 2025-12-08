package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    /**
     * Find all subjects sorted by specified order
     */
    List<Subject> findAll(Sort sort);
    
    /**
     * Find subjects by curriculum ID ordered by updatedAt
     */
    List<Subject> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);
    
    /**
     * Find subjects by level ID ordered by updatedAt
     */
    List<Subject> findByLevelIdOrderByUpdatedAtDesc(Long levelId);
    
    /**
     * Find subjects by both curriculum and level ordered by updatedAt
     */
    List<Subject> findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(Long curriculumId, Long levelId);
}
