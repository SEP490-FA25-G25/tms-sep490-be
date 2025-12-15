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

        @Query("SELECT c.code FROM ClassEntity c WHERE c.branch.id = :branchId AND c.code LIKE :likePattern ORDER BY c.code DESC LIMIT 1")
        String findHighestCodeByPrefixReadOnly(@Param("branchId") Long branchId,
                        @Param("likePattern") String likePattern);

        boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

        @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClassEntity c WHERE c.branch.id = :branchId AND LOWER(c.name) = LOWER(:name) AND c.id <> :excludeId")
        boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(@Param("branchId") Long branchId, @Param("name") String name,
                        @Param("excludeId") Long excludeId);

        // Lấy các lớp đang mở đăng ký cho giáo viên
        @Query("SELECT c FROM ClassEntity c " +
                        "LEFT JOIN FETCH c.subject s " +
                        "LEFT JOIN FETCH c.branch b " +
                        "WHERE c.branch.id IN :branchIds " +
                        "AND c.approvalStatus = :approvalStatus " +
                        "AND c.status = :status " +
                        "AND c.assignedTeacher IS NULL " +
                        "AND c.registrationOpenDate IS NOT NULL " +
                        "AND c.registrationCloseDate IS NOT NULL " +
                        "AND c.registrationOpenDate <= :now " +
                        "AND c.registrationCloseDate >= :now " +
                        "ORDER BY c.registrationCloseDate ASC")
        List<ClassEntity> findAvailableForTeacherRegistration(
                        @Param("branchIds") List<Long> branchIds,
                        @Param("now") java.time.OffsetDateTime now,
                        @Param("approvalStatus") ApprovalStatus approvalStatus,
                        @Param("status") ClassStatus status);

        // Lấy các lớp APPROVED + SCHEDULED chưa có teacher (cho AA)
        @Query("SELECT c FROM ClassEntity c " +
                        "LEFT JOIN FETCH c.subject s " +
                        "LEFT JOIN FETCH c.branch b " +
                        "WHERE c.branch.id IN :branchIds " +
                        "AND c.approvalStatus = 'APPROVED' " +
                        "AND c.status = 'SCHEDULED' " +
                        "AND c.assignedTeacher IS NULL " +
                        "ORDER BY c.startDate ASC")
        List<ClassEntity> findClassesNeedingTeacher(@Param("branchIds") List<Long> branchIds);


        // Lấy các lớp đang active mà teacher đang được assign (cho conflict detection)
        @Query("SELECT c FROM ClassEntity c " +
                        "WHERE c.assignedTeacher.id = :teacherId " +
                        "AND c.status IN ('SCHEDULED', 'ONGOING') " +
                        "AND c.plannedEndDate >= :fromDate")
        List<ClassEntity> findActiveClassesByAssignedTeacher(
                        @Param("teacherId") Long teacherId,
                        @Param("fromDate") java.time.LocalDate fromDate);

        // Find classes by status (for cronjob)
        List<ClassEntity> findByStatus(ClassStatus status);

}
