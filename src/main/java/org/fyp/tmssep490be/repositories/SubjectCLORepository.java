package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectCLORepository extends JpaRepository<CLO, Long> {

    @Query("SELECT c FROM CLO c WHERE c.subject.id = :subjectId")
    List<CLO> findBySubjectId(@Param("subjectId") Long subjectId);

    @Modifying
    @Query("DELETE FROM CLO c WHERE c.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Long subjectId);
}