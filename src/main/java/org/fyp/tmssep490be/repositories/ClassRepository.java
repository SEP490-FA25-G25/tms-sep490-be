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
    /**
     * Find class by ID with pessimistic write lock
     * Dùng để tránh race condition khi concurrent enrollment
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ClassEntity c WHERE c.id = :classId")
    Optional<ClassEntity> findByIdWithLock(@Param("classId") Long classId);

    /**
     * Find classes accessible to academic affairs user with filters
     * Filters by user's branch assignments, approval status, and class status
     * If approvalStatus or status is null, returns all classes regardless of that filter
     */
    @Query("SELECT DISTINCT c FROM ClassEntity c " +
           "INNER JOIN c.branch b " +
           "INNER JOIN c.course co " +
           "WHERE (:branchIds IS NULL OR b.id IN :branchIds) " +
           "AND (:approvalStatus IS NULL OR c.approvalStatus = :approvalStatus) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:courseId IS NULL OR co.id = :courseId) " +
           "AND (:modality IS NULL OR c.modality = :modality) " +
           "AND (:search IS NULL OR " +
           "  LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(co.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "  LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")")
    Page<ClassEntity> findClassesForAcademicAffairs(
            @Param("branchIds") List<Long> branchIds,
            @Param("approvalStatus") ApprovalStatus approvalStatus,
            @Param("status") ClassStatus status,
            @Param("courseId") Long courseId,
            @Param("modality") org.fyp.tmssep490be.entities.enums.Modality modality,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Count enrolled students for a class
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.classId = :classId AND e.status = 'ENROLLED'")
    Integer countEnrolledStudents(@Param("classId") Long classId);

    /**
     * Get upcoming sessions for a class (next 5 sessions from today)
     */
    @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
           "AND s.date >= CURRENT_DATE AND s.status = 'PLANNED' " +
           "ORDER BY s.date ASC")
    List<org.fyp.tmssep490be.entities.Session> findUpcomingSessions(@Param("classId") Long classId, Pageable pageable);

    // Create Class Workflow additional methods

    /**
     * Find class by branch and code for uniqueness validation
     */
    Optional<ClassEntity> findByBranchIdAndCode(Long branchId, String code);

    /**
     * Count sessions without timeslot assignment for validation
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND s.timeSlotTemplate IS NULL")
    long countSessionsWithoutTimeslot(@Param("classId") Long classId);

    /**
     * Count sessions without resource assignment for validation
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM SessionResource sr WHERE sr.session.id = s.id)")
    long countSessionsWithoutResource(@Param("classId") Long classId);

    /**
     * Count sessions without teacher assignment for validation
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM TeachingSlot ts WHERE ts.session.id = s.id)")
    long countSessionsWithoutTeacher(@Param("classId") Long classId);

    /**
     * Check if class exists and user has access to it
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClassEntity c " +
           "WHERE c.id = :classId AND (:branchIds IS NULL OR c.branch.id IN :branchIds)")
    boolean existsByIdAndUserHasAccess(@Param("classId") Long classId, @Param("branchIds") List<Long> branchIds);
}
