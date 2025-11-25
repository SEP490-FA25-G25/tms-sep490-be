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

import java.time.LocalDate;
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
   * If approvalStatus or status is null, returns all classes regardless of that
   * filter
   * 
   * Note: Using explicit entity reference to avoid PostgreSQL bytea cast issues
   * with schedule_days
   */
  @Query("SELECT c FROM ClassEntity c " +
      "INNER JOIN c.branch b " +
      "INNER JOIN c.course co " +
      "WHERE (:branchIds IS NULL OR b.id IN :branchIds) " +
      "AND (:approvalStatus IS NULL OR c.approvalStatus = :approvalStatus) " +
      "AND (:status IS NULL OR c.status = :status) " +
      "AND (:courseId IS NULL OR co.id = :courseId) " +
      "AND (:modality IS NULL OR c.modality = :modality) " +
      "AND (:search IS NULL OR :search = '' OR " +
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
      Pageable pageable);

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

  // ==================== CREATE CLASS WORKFLOW METHODS ====================

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

  /**
   * Find highest class code by prefix pattern for sequence generation
   * Uses regex to match exact format and ORDER BY to get highest
   * 
   * @param branchId Branch ID to filter
   * @param regex    Regex pattern to match code format (e.g.,
   *                 "^IELTSFOUND-HN01-25-[0-9]{3}$")
   * @return Optional containing the highest code, or empty if none found
   */
  @Query(value = """
      SELECT c.code FROM class c
      WHERE c.branch_id = :branchId
        AND c.code ~ :regex
      ORDER BY c.code DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<String> findHighestCodeByPrefix(@Param("branchId") Long branchId, @Param("regex") String regex);

  /**
   * Find highest class code by prefix pattern (read-only, without lock)
   * Used for preview functionality
   * Uses LIKE for better performance than regex
   * 
   * @param branchId Branch ID to filter
   * @param prefix   Prefix pattern to match (e.g., "IELTSFOUND-HN01-25")
   * @return Optional containing the highest code, or empty if none found
   */
  @Query(value = """
      SELECT c.code FROM class c
      WHERE c.branch_id = :branchId
        AND c.code LIKE :prefix || '-%'
      ORDER BY c.code DESC
      LIMIT 1
      """, nativeQuery = true)
  Optional<String> findHighestCodeByPrefixReadOnly(@Param("branchId") Long branchId, @Param("prefix") String prefix);

  // ==================== TRANSFER/REQUEST WORKFLOW METHODS ====================

  /**
   * Find classes by course ID and status for transfer options
   */
  List<ClassEntity> findByCourseIdAndStatusIn(Long courseId, List<ClassStatus> statuses);

  /**
   * Find class by ID with course eagerly fetched
   * Used for transfer operations to avoid lazy loading issues
   * Enhanced to fetch branch, subject, and level to prevent lazy loading
   */
  @Query("SELECT c FROM ClassEntity c " +
      "JOIN FETCH c.course co " +
      "LEFT JOIN FETCH co.subject " +
      "LEFT JOIN FETCH co.level " +
      "JOIN FETCH c.branch " +
      "WHERE c.id = :classId")
  Optional<ClassEntity> findByIdWithCourse(@Param("classId") Long classId);

  /**
   * Find classes by flexible criteria for AA transfer options
   * Supports filtering by course, branch, modality, and capacity
   *
   * Filter Logic:
   * - Base: course_id = criteria.courseId AND id != criteria.excludeClassId AND
   * status IN criteria.statuses
   * - Optional: branch_id = criteria.branchId (if specified)
   * - Optional: modality = criteria.modality (if specified)
   * - Optional: has available capacity (if criteria.hasCapacity = true)
   *
   * Note: Schedule comparison is done at presentation layer since time slots vary
   * per session
   */
  @Query("SELECT c FROM ClassEntity c " +
      "WHERE c.course.id = :#{#criteria.courseId} " +
      "AND c.id != :#{#criteria.excludeClassId} " +
      "AND c.status IN :#{#criteria.statuses} " +
      "AND (:#{#criteria.branchId} IS NULL OR c.branch.id = :#{#criteria.branchId}) " +
      "AND (:#{#criteria.modality} IS NULL OR c.modality = :#{#criteria.modality})")
  List<ClassEntity> findByFlexibleCriteria(
      @Param("criteria") org.fyp.tmssep490be.dtos.classes.ClassSearchCriteria criteria);

  // ==================== SCHEDULER JOB METHODS ====================

  /**
   * Find classes with startDate <= given date and specific status
   * Used by ClassStatusUpdateJob to transition SCHEDULED -> ONGOING
   */
  @Query("SELECT c FROM ClassEntity c WHERE c.startDate <= :date AND c.status = :status")
  List<ClassEntity> findByStartDateBeforeOrEqualAndStatus(
      @Param("date") LocalDate date,
      @Param("status") ClassStatus status
  );

  /**
   * Find classes with plannedEndDate <= given date and specific status
   * Used by ClassStatusUpdateJob to transition ONGOING -> COMPLETED
   */
  @Query("SELECT c FROM ClassEntity c WHERE c.plannedEndDate <= :date AND c.status = :status")
  List<ClassEntity> findByPlannedEndDateBeforeOrEqualAndStatus(
      @Param("date") LocalDate date,
      @Param("status") ClassStatus status
  );
}
