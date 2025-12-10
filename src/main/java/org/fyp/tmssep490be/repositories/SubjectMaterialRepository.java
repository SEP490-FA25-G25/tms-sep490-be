package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SubjectMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectMaterialRepository extends JpaRepository<SubjectMaterial, Long> {

    // Tìm material ở cấp subject (không thuộc phase hay session)
    @Query("SELECT m FROM SubjectMaterial m WHERE m.subject.id = :subjectId AND m.phase IS NULL AND m.session IS NULL")
    List<SubjectMaterial> findSubjectLevelMaterials(@Param("subjectId") Long subjectId);

    // Tìm material ở cấp phase
    @Query("SELECT m FROM SubjectMaterial m WHERE m.phase.id = :phaseId AND m.session IS NULL")
    List<SubjectMaterial> findPhaseLevelMaterials(@Param("phaseId") Long phaseId);

    // Tìm material ở cấp session
    @Query("SELECT m FROM SubjectMaterial m WHERE m.session.id = :sessionId")
    List<SubjectMaterial> findSessionLevelMaterials(@Param("sessionId") Long sessionId);

    // Tìm tất cả material theo subject
    @Query("SELECT m FROM SubjectMaterial m WHERE m.subject.id = :subjectId")
    List<SubjectMaterial> findBySubjectId(@Param("subjectId") Long subjectId);

    // Đếm số material theo subject
    @Query("SELECT COUNT(m) FROM SubjectMaterial m WHERE m.subject.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);

    // Đếm số material theo session
    @Query("SELECT COUNT(m) FROM SubjectMaterial m WHERE m.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
}