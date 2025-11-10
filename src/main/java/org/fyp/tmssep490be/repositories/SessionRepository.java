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
            SessionStatus status
    );

    /**
     * Get upcoming sessions for a class (next sessions from today)
     */
    @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
           "AND s.date >= CURRENT_DATE AND s.status = 'PLANNED' " +
           "ORDER BY s.date ASC")
    List<Session> findUpcomingSessions(@Param("classId") Long classId, Pageable pageable);

    /**
     * Update time slot for sessions by day of week
     * Used in Phase 1.3: Assign Time Slots (STEP 3)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE session SET time_slot_template_id = :timeSlotId, updated_at = CURRENT_TIMESTAMP WHERE class_entity_id = :classId AND EXTRACT(DOW FROM date) = :dayOfWeek", nativeQuery = true)
    int updateTimeSlotByDayOfWeek(@Param("classId") Long classId, @Param("dayOfWeek") Integer dayOfWeek, @Param("timeSlotId") Long timeSlotId);

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

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.1: RESOURCE ASSIGNMENT (HYBRID) ====================

    /**
     * Find sessions on specific day of week without any resource assignment
     * <p>
     * Used in Phase 2 (Java Analysis) to identify sessions that need conflict resolution
     * </p>
     *
     * @param classId   class ID
     * @param dayOfWeek day of week (PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday)
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
            @Param("dayOfWeek") int dayOfWeek
    );

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

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.3: TEACHER ASSIGNMENT (PRE-CHECK) ====================

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
}
