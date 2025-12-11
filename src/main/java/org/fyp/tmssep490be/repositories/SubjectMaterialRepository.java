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
    @Query("SELECT m FROM SubjectMaterial m WHERE m.subject.id = :subjectId AND m.phase IS NULL AND m.subjectSession IS NULL")
    List<SubjectMaterial> findSubjectLevelMaterials(@Param("subjectId") Long subjectId);

    // Tìm material ở cấp phase
    @Query("SELECT m FROM SubjectMaterial m WHERE m.phase.id = :phaseId AND m.subjectSession IS NULL")
    List<SubjectMaterial> findPhaseLevelMaterials(@Param("phaseId") Long phaseId);

    // Tìm material ở cấp session
    @Query("SELECT m FROM SubjectMaterial m WHERE m.subjectSession.id = :subjectSessionId")
    List<SubjectMaterial> findSessionLevelMaterials(@Param("subjectSessionId") Long subjectSessionId);

    // Tìm tất cả material theo subject
    @Query("SELECT m FROM SubjectMaterial m WHERE m.subject.id = :subjectId")
    List<SubjectMaterial> findBySubjectId(@Param("subjectId") Long subjectId);

    // Đếm số material theo subject
    @Query("SELECT COUNT(m) FROM SubjectMaterial m WHERE m.subject.id = :subjectId")
    long countBySubjectId(@Param("subjectId") Long subjectId);

    // Đếm số material theo session
    @Query("SELECT COUNT(m) FROM SubjectMaterial m WHERE m.subjectSession.id = :subjectSessionId")
    long countBySubjectSessionId(@Param("subjectSessionId") Long subjectSessionId);

    // Tìm material theo session id
    List<SubjectMaterial> findBySubjectSessionId(Long subjectSessionId);

    // Tìm material ở cấp phase (không thuộc session)
    List<SubjectMaterial> findByPhaseIdAndSubjectSessionIsNull(Long phaseId);

    // Xóa material ở subject (không thuộc phase hay session)
    @Modifying
    @Query("DELETE FROM SubjectMaterial m WHERE m.subject.id = :subjectId AND m.phase IS NULL AND m.subjectSession IS NULL")
    void deleteBySubjectIdAndPhaseIsNullAndSubjectSessionIsNull(@Param("subjectId") Long subjectId);

    // Tìm material ở subject (không thuộc phase hay session) - dùng cho clone
    List<SubjectMaterial> findBySubjectIdAndPhaseIsNullAndSubjectSessionIsNull(Long subjectId);
}
