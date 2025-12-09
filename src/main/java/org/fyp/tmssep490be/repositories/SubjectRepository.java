package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // Find by Curriculum/Level
    List<Subject> findByCurriculumId(Long curriculumId);
    List<Subject> findByLevelId(Long levelId);
    List<Subject> findByCurriculumIdAndLevelId(Long curriculumId, Long levelId);

    // Ordered by updatedAt DESC
    List<Subject> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);
    List<Subject> findByLevelIdOrderByUpdatedAtDesc(Long levelId);
    List<Subject> findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(Long curriculumId, Long levelId);

    long countByLevelId(Long levelId);

    // Tìm môn học cần kích hoạt (for SubjectActivationJob)
    @Query("SELECT s FROM Subject s " +
            "WHERE s.effectiveDate <= :date " +
            "AND s.status = :status " +
            "AND s.approvalStatus = :approvalStatus " +
            "ORDER BY s.effectiveDate ASC")
    List<Subject> findByEffectiveDateBeforeOrEqualAndStatusAndApprovalStatus(
            @Param("date") LocalDate date,
            @Param("status") SubjectStatus status,
            @Param("approvalStatus") ApprovalStatus approvalStatus);

    // Đếm môn học có lớp trong các chi nhánh
    @Query("SELECT COUNT(DISTINCT s) FROM Subject s " +
            "INNER JOIN s.classes cl " +
            "WHERE cl.branch.id IN :branchIds")
    long countDistinctByClassesInBranches(@Param("branchIds") List<Long> branchIds);

    // Kiểm tra curriculum/level có môn học với status
    boolean existsByCurriculumIdAndStatus(Long curriculumId, SubjectStatus status);
    boolean existsByLevelIdAndStatus(Long levelId, SubjectStatus status);

    // Versioning
    @Query("SELECT MAX(s.version) FROM Subject s WHERE s.logicalSubjectCode = :logicalSubjectCode")
    Integer findMaxVersionByLogicalSubjectCode(@Param("logicalSubjectCode") String logicalSubjectCode);
    List<Subject> findByLogicalSubjectCodeOrderByVersionDesc(String logicalSubjectCode);
    boolean existsByCode(String code);
}