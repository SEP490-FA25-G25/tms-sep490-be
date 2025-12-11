package org.fyp.tmssep490be.repositories;

import jakarta.persistence.LockModeType;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM ClassEntity c WHERE c.id = :classId")
  Optional<ClassEntity> findByIdWithLock(@Param("classId") Long classId);

  @Query("SELECT c FROM ClassEntity c " +
      "INNER JOIN c.branch b " +
      "INNER JOIN c.subject sj " +
      "WHERE (:branchIds IS NULL OR b.id IN :branchIds) " +
      "AND (:approvalStatus IS NULL OR c.approvalStatus = :approvalStatus) " +
      "AND (:status IS NULL OR c.status = :status) " +
      "AND (:subjectId IS NULL OR sj.id = :subjectId) " +
      "AND (:modality IS NULL OR c.modality = :modality) " +
      "AND (:search IS NULL OR :search = '' OR " +
      "  LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "  LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "  LOWER(sj.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
      "  LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
      ")")
  Page<ClassEntity> findClassesForAcademicAffairs(
      @Param("branchIds") List<Long> branchIds,
      @Param("approvalStatus") ApprovalStatus approvalStatus,
      @Param("status") ClassStatus status,
      @Param("subjectId") Long subjectId,
      @Param("modality") org.fyp.tmssep490be.entities.enums.Modality modality,
      @Param("search") String search,
      Pageable pageable);

  List<ClassEntity> findBySubjectIdAndStatusIn(Long subjectId, List<ClassStatus> statuses);

  @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classEntity.id = :classId AND e.status = 'ENROLLED'")
  Integer countEnrolledStudents(@Param("classId") Long classId);

  boolean existsByIdAndSubjectId(Long classId, Long subjectId);

  @Query("""
      SELECT c FROM ClassEntity c
      WHERE c.subject.id = :subjectId
      AND c.id <> :excludeClassId
      AND c.status IN :statuses
      AND (:branchId IS NULL OR c.branch.id = :branchId)
      AND (:modality IS NULL OR c.modality = :modality)
      """)
  List<ClassEntity> findByFlexibleCriteria(
      @Param("subjectId") Long subjectId,
      @Param("excludeClassId") Long excludeClassId,
      @Param("statuses") List<ClassStatus> statuses,
      @Param("branchId") Long branchId,
      @Param("modality") org.fyp.tmssep490be.entities.enums.Modality modality);

  // Tìm lớp theo branch và code
  Optional<ClassEntity> findByBranchIdAndCode(Long branchId, String code);

  // Kiểm tra tên lớp trùng trong branch
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClassEntity c " +
      "WHERE c.branch.id = :branchId AND LOWER(TRIM(c.name)) = LOWER(TRIM(:name))")
  boolean existsByBranchIdAndNameIgnoreCase(@Param("branchId") Long branchId, @Param("name") String name);

  // Kiểm tra tên lớp trùng (loại trừ lớp đang sửa)
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClassEntity c " +
      "WHERE c.branch.id = :branchId AND LOWER(TRIM(c.name)) = LOWER(TRIM(:name)) AND c.id != :excludeId")
  boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(@Param("branchId") Long branchId, @Param("name") String name,
      @Param("excludeId") Long excludeId);

  // Đếm sessions chưa có timeslot
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND s.timeSlotTemplate IS NULL")
  long countSessionsWithoutTimeslot(@Param("classId") Long classId);

  // Đếm sessions chưa có resource
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM SessionResource sr WHERE sr.session.id = s.id)")
  long countSessionsWithoutResource(@Param("classId") Long classId);

  // Đếm sessions chưa có giáo viên
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM TeachingSlot ts WHERE ts.session.id = s.id)")
  long countSessionsWithoutTeacher(@Param("classId") Long classId);

  // Tìm mã lớp cao nhất theo prefix
  @Query(value = """
      SELECT c.code FROM class c
      WHERE c.branch_id = :branchId
        AND c.code LIKE :prefix || '-%'
      ORDER BY c.code DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<String> findHighestCodeByPrefixReadOnly(@Param("branchId") Long branchId, @Param("prefix") String prefix);
}
