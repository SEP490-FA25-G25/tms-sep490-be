package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
  /**
   * Tìm tất cả sessions của class, ordered by date ascending
   * Dùng cho STEP 2: Review sessions (Xem lại buổi học)
   * Fetch courseSession và timeSlotTemplate để tránh lazy loading issues
   */
  @Query("SELECT s FROM Session s " +
      "LEFT JOIN FETCH s.courseSession " +
      "LEFT JOIN FETCH s.timeSlotTemplate " +
      "WHERE s.classEntity.id = :classId " +
      "ORDER BY s.date ASC")
  List<Session> findByClassEntityIdOrderByDateAsc(@Param("classId") Long classId);

  /**
   * Tìm tất cả future sessions của class (date >= today, status = PLANNED)
   * Dùng để auto-generate student_session khi enroll
   */
  List<Session> findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
      Long classId,
      LocalDate date,
      SessionStatus status);

  /**
   * Get upcoming sessions for a class (next sessions from today)
   */
  @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
      "AND s.date >= CURRENT_DATE AND s.status = 'PLANNED' " +
      "ORDER BY s.date ASC")
  List<Session> findUpcomingSessions(@Param("classId") Long classId, Pageable pageable);

  // ==================== CREATE CLASS WORKFLOW - PHASE 1.3: TIME SLOT ASSIGNMENT
  // ====================

  /**
   * Update time slot for sessions by day of week
   * Used in Phase 1.3: Assign Time Slots (STEP 3)
   */
  @Query(value = """
      UPDATE session
      SET time_slot_template_id = :timeSlotTemplateId
      WHERE class_id = :classId
        AND EXTRACT(DOW FROM date) = :dayOfWeek
      """, nativeQuery = true)
  @Modifying
  int updateTimeSlotByDayOfWeek(
      @Param("classId") Long classId,
      @Param("dayOfWeek") int dayOfWeek,
      @Param("timeSlotTemplateId") Long timeSlotTemplateId);

  /**
   * Find sessions by class ID and day of week (for Step 4 resource availability
   * check)
   * <p>
   * Uses PostgreSQL DOW format: 0=Sunday, 1=Monday, ..., 6=Saturday
   * </p>
   *
   * @param classId   class ID
   * @param dayOfWeek day of week (0-6)
   * @return list of sessions matching the criteria
   */
  @Query(value = """
      SELECT s.* FROM session s
      WHERE s.class_id = :classId
      AND EXTRACT(DOW FROM s.date) = :dayOfWeek
      """, nativeQuery = true)
  List<Session> findByClassIdAndDayOfWeek(
      @Param("classId") Long classId,
      @Param("dayOfWeek") int dayOfWeek);

  /**
   * Count total sessions for a class
   * Used in Phase 1.3: Assign Time Slots (STEP 3)
   */
  long countByClassEntityId(Long classId);

  /**
   * Count sessions with time slot assignments for a class
   * Used in Phase 1.4: Validation Service (STEP 6)
   */
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND s.timeSlotTemplate IS NOT NULL")
  long countSessionsWithTimeSlots(@Param("classId") Long classId);

  /**
   * Count sessions without time slot assignments for a class
   * Used in Phase 1.4: Validation Service (STEP 6)
   */
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND s.timeSlotTemplate IS NULL")
  long countSessionsWithoutTimeSlots(@Param("classId") Long classId);

  /**
   * Count sessions with resource assignments for a class
   * Used in Phase 1.4: Validation Service (STEP 6)
   */
  @Query("SELECT COUNT(DISTINCT s) FROM Session s LEFT JOIN s.sessionResources sr WHERE s.classEntity.id = :classId AND sr.id IS NOT NULL")
  long countSessionsWithResources(@Param("classId") Long classId);

  /**
   * Count sessions without resource assignments for a class
   * Used in Phase 1.4: Validation Service (STEP 6)
   */
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM SessionResource sr WHERE sr.session.id = s.id)")
  long countSessionsWithoutResources(@Param("classId") Long classId);

  /**
   * Count sessions with teacher assignments for a class
   * Used in Phase 1.4: Validation Service (STEP 6)
   */
  @Query("SELECT COUNT(DISTINCT s) FROM Session s LEFT JOIN s.teachingSlots ts WHERE s.classEntity.id = :classId AND ts.id IS NOT NULL")
  long countSessionsWithTeachers(@Param("classId") Long classId);

  /**
   * Count sessions without teacher assignments for a class
   * Used in Phase 1.4: Validation Service (STEP 6)
   */
  @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM TeachingSlot ts WHERE ts.session.id = s.id)")
  long countSessionsWithoutTeachers(@Param("classId") Long classId);

  // ==================== CREATE CLASS WORKFLOW - PHASE 2.1: RESOURCE ASSIGNMENT
  // (HYBRID) ====================

  /**
   * Find sessions on specific day of week without any resource assignment
   * <p>
   * Used in Phase 2 (Java Analysis) to identify sessions that need conflict
   * resolution
   * </p>
   *
   * @param classId   class ID
   * @param dayOfWeek day of week (PostgreSQL DOW: 0=Sunday, 1=Monday, ...,
   *                  6=Saturday)
   * @return list of unassigned sessions
   */
  @Query(value = """
      SELECT s.id, s.date, s.time_slot_template_id, s.class_id
      FROM session s
      WHERE s.class_id = :classId
        AND EXTRACT(DOW FROM s.date) = :dayOfWeek
        AND NOT EXISTS (
          SELECT 1 FROM session_resource sr
          WHERE sr.session_id = s.id
        )
      ORDER BY s.date ASC
      """, nativeQuery = true)
  List<Object[]> findUnassignedSessionsByDayOfWeek(
      @Param("classId") Long classId,
      @Param("dayOfWeek") int dayOfWeek);

  /**
   * Find session with resource assignment
   * <p>
   * Returns full session details with time slot for conflict analysis
   * </p>
   *
   * @param sessionId session ID
   * @return session details with time slot
   */
  @Query("SELECT s FROM Session s " +
      "LEFT JOIN FETCH s.timeSlotTemplate " +
      "LEFT JOIN FETCH s.sessionResources " +
      "WHERE s.id = :sessionId")
  Session findSessionWithResourcesAndTimeSlot(@Param("sessionId") Long sessionId);

  // ==================== CREATE CLASS WORKFLOW - PHASE 2.3: TEACHER ASSIGNMENT
  // (PRE-CHECK) ====================

  /**
   * Find all distinct skills required by sessions in a class
   * <p>
   * Returns all unique skills from skillSet arrays in course_session templates
   * Used to validate teacher has all required skills before assignment
   * </p>
   *
   * @param classId class ID
   * @return list of distinct skills required
   */
  @Query(value = """
      SELECT DISTINCT UNNEST(cs.skill_set) as skill
      FROM session s
      JOIN course_session cs ON s.course_session_id = cs.id
      WHERE s.class_id = :classId
        AND cs.skill_set IS NOT NULL
      ORDER BY skill
      """, nativeQuery = true)
  List<String> findDistinctSkillNamesByClassId(@Param("classId") Long classId);

  /**
   * Find sessions without any teacher assignment
   * <p>
   * Used to identify remaining sessions after teacher assignment
   * </p>
   *
   * @param classId class ID
   * @return list of session IDs without teacher
   */
  @Query(value = """
      SELECT s.id
      FROM session s
      WHERE s.class_id = :classId
        AND NOT EXISTS (
          SELECT 1 FROM teaching_slot ts
          WHERE ts.session_id = s.id
        )
      ORDER BY s.date ASC
      """, nativeQuery = true)
  List<Long> findSessionsWithoutTeacher(@Param("classId") Long classId);

  // ==================== TRANSFER/REQUEST WORKFLOW METHODS ====================

  /**
   * Find sessions for a specific student on a given date
   * Using native query for better performance and clarity
   */
  @Query(value = "SELECT s.* FROM session s " +
      "JOIN class c ON s.class_id = c.id " +
      "JOIN enrollment e ON e.class_id = c.id " +
      "WHERE e.student_id = :studentId " +
      "AND s.date = :date " +
      "AND s.status = 'PLANNED' " +
      "AND e.status = 'ENROLLED'", nativeQuery = true)
  List<Session> findSessionsForStudentByDate(@Param("studentId") Long studentId, @Param("date") LocalDate date);

  /**
   * Find session by date and class
   */
  List<Session> findByClassEntityIdAndDate(Long classId, LocalDate date);

  /**
   * Find all missed (absent) sessions for a student within a time window
   * Used for makeup request - get sessions student can make up
   */
  @Query(value = """
      SELECT s.* FROM session s
      JOIN student_session ss ON ss.session_id = s.id
      WHERE ss.student_id = :studentId
        AND ss.attendance_status = 'ABSENT'
        AND s.date >= :fromDate
        AND s.date <= :toDate
      ORDER BY s.date DESC
      """, nativeQuery = true)
  List<Session> findMissedSessionsForStudent(
      @Param("studentId") Long studentId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);

  /**
   * Find available makeup sessions for a specific course session
   * Returns sessions with same courseSessionId, in future, with available
   * capacity
   * For OFFLINE modality: filters by same branch
   * For ONLINE/HYBRID modality: allows different branches
   * Only returns sessions within 2 weeks from today
   */
  @Query(value = """
      SELECT s.* FROM session s
      JOIN class c ON s.class_id = c.id
      WHERE s.course_session_id = :courseSessionId
        AND s.date >= CURRENT_DATE
        AND s.date <= CURRENT_DATE + INTERVAL '2 weeks'
        AND s.status = 'PLANNED'
        AND s.id != :excludeSessionId
        AND (
          :targetModality != 'OFFLINE'
          OR c.branch_id = :targetBranchId
        )
      ORDER BY s.date ASC
      """, nativeQuery = true)
  List<Session> findMakeupSessionOptions(
      @Param("courseSessionId") Long courseSessionId,
      @Param("excludeSessionId") Long excludeSessionId,
      @Param("targetBranchId") Long targetBranchId,
      @Param("targetModality") String targetModality);

  /**
   * Find future sessions for a class after a specific date (for transfer)
   */
  List<Session> findByClassEntityIdAndDateAfter(Long classId, LocalDate date);

  /**
   * Find completed/cancelled sessions by class ID and status
   * Used for content gap analysis - get sessions already done in a class
   */
  @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
      "AND s.status IN :statuses " +
      "ORDER BY s.date ASC")
  List<Session> findByClassIdAndStatusIn(
      @Param("classId") Long classId,
      @Param("statuses") List<SessionStatus> statuses);

  /**
   * Find past sessions by class ID (date < today)
   * Used for content gap analysis - get sessions target class already covered
   */
  @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
      "AND s.date < :date " +
      "ORDER BY s.date ASC")
  List<Session> findByClassIdAndDateBefore(
      @Param("classId") Long classId,
      @Param("date") LocalDate date);

  @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId ORDER BY s.date ASC, s.timeSlotTemplate.startTime ASC")
  List<Session> findAllByClassIdOrderByDateAndTime(@Param("classId") Long classId);

  /**
   * Find the previous session in the same class before the given date
   * Used to get homework assignment from previous session
   */
  @Query("SELECT s FROM Session s " +
      "WHERE s.classEntity.id = :classId " +
      "AND s.date < :date " +
      "ORDER BY s.date DESC, s.timeSlotTemplate.startTime DESC")
  List<Session> findPreviousSessionsByClassIdAndDate(
      @Param("classId") Long classId,
      @Param("date") LocalDate date);

  /**
   * Find sessions that have passed their date and are still in PLANNED status
   * Used to automatically mark them as DONE
   */
  @Query("SELECT s FROM Session s " +
      "WHERE s.date < :today " +
      "AND s.status = :status")
  List<Session> findPastSessionsByStatus(
      @Param("today") LocalDate today,
      @Param("status") SessionStatus status);

  /**
   * Delete all sessions for a class
   * Used when regenerating sessions during class update
   */
  @Modifying
  @Query("DELETE FROM Session s WHERE s.classEntity.id = :classId")
  void deleteByClassEntityId(@Param("classId") Long classId);
}
