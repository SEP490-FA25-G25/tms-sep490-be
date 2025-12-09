package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectMaterialRepository extends JpaRepository<SubjectMaterial, Long> {

    @Query("SELECT m FROM SubjectMaterial m WHERE m.subject.id = :subjectId AND m.phase IS NULL AND m.subjectSession IS NULL")
    List<SubjectMaterial> findSubjectLevelMaterials(@Param("subjectId") Long subjectId);

    @Query("SELECT m FROM SubjectMaterial m WHERE m.phase.id = :phaseId AND m.subjectSession IS NULL")
    List<SubjectMaterial> findPhaseLevelMaterials(@Param("phaseId") Long phaseId);

    @Query("SELECT m FROM SubjectMaterial m WHERE m.subjectSession.id = :sessionId")
    List<SubjectMaterial> findSessionLevelMaterials(@Param("sessionId") Long sessionId);

    void deleteBySubjectId(Long subjectId);
}