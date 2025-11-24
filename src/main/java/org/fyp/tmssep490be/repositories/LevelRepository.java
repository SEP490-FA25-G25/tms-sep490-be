package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {

    /**
     * Find level by code (case-insensitive)
     */
    java.util.Optional<Level> findByCodeIgnoreCase(String code);

    /**
     * Find levels by subject ID, ordered by sort order ascending
     */
    List<Level> findBySubjectIdOrderBySortOrderAsc(Long subjectId);

    @Query("SELECT MAX(l.sortOrder) FROM Level l WHERE l.subject.id = :subjectId")
    Integer findMaxSortOrderBySubjectId(Long subjectId);
}
