package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for TeachingSlot entity
 */
@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, TeachingSlot.TeachingSlotId> {

    /**
     * Check if a session has any teacher assignment
     * Used for STEP 2: Review sessions status check
     */
    @Query("SELECT COUNT(ts) > 0 FROM TeachingSlot ts WHERE ts.session.id = :sessionId")
    boolean existsBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT ts FROM TeachingSlot ts WHERE ts.session.classEntity.id = :classId AND ts.status = :status")
    List<TeachingSlot> findByClassEntityIdAndStatus(@Param("classId") Long classId, @Param("status") TeachingSlotStatus status);
    
    /**
     * Get all teaching slots for a specific session with given status
     * Used for STEP 2 & STEP 5: Show which teachers are assigned to each session
     */
    @Query("SELECT ts FROM TeachingSlot ts " +
           "LEFT JOIN FETCH ts.teacher t " +
           "LEFT JOIN FETCH t.userAccount " +
           "WHERE ts.session.id = :sessionId AND ts.status = :status")
    List<TeachingSlot> findBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") TeachingSlotStatus status);
    
    /**
     * Check if teacher owns (is assigned to) a session
     * Teacher owns session if there's a teaching_slot with status SCHEDULED or SUBSTITUTED
     */
    boolean existsByIdSessionIdAndIdTeacherIdAndStatusIn(
            Long sessionId,
            Long teacherId,
            List<TeachingSlotStatus> statuses
    );

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.3: TEACHER ASSIGNMENT (BULK INSERT) ====================

    /**
     * Bulk assign teacher to ALL sessions in a class
     * <p>
     * Direct INSERT without conflict checking (PRE-CHECK already done)
     * Inserts teaching_slot records with status = 'SCHEDULED'
     * </p>
     * <p>
     * <b>IMPORTANT:</b> This method assumes PRE-CHECK was done before calling
     * </p>
     *
     * @param classId Class ID
     * @param teacherId Teacher ID to assign
     * @return List of assigned session IDs
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO teaching_slot (session_id, teacher_id, status)
        SELECT s.id, :teacherId, 'SCHEDULED'
        FROM session s
        WHERE s.class_id = :classId
          AND NOT EXISTS (
            SELECT 1 FROM teaching_slot ts
            WHERE ts.session_id = s.id
              AND ts.teacher_id = :teacherId
          )
        RETURNING session_id
        """, nativeQuery = true)
    List<Long> bulkAssignTeacher(
            @Param("classId") Long classId,
            @Param("teacherId") Long teacherId
    );

    /**
     * Bulk assign teacher to SPECIFIC sessions
     * <p>
     * Used for partial assignment (teacher can't teach all sessions)
     * </p>
     *
     * @param sessionIds List of session IDs to assign
     * @param teacherId Teacher ID to assign
     * @return List of assigned session IDs
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO teaching_slot (session_id, teacher_id, status)
        SELECT s.id, :teacherId, 'SCHEDULED'
        FROM session s
        WHERE s.id IN :sessionIds
          AND NOT EXISTS (
            SELECT 1 FROM teaching_slot ts
            WHERE ts.session_id = s.id
              AND ts.teacher_id = :teacherId
          )
        RETURNING session_id
        """, nativeQuery = true)
    List<Long> bulkAssignTeacherToSessions(
            @Param("sessionIds") List<Long> sessionIds,
            @Param("teacherId") Long teacherId
    );

    // ==================== TEACHER SCHEDULE & REQUEST WORKFLOW METHODS ====================

    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            LEFT JOIN FETCH s.courseSession cs
            WHERE ts.teacher.id = :teacherId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
              AND s.date = :date
              AND s.status <> 'CANCELLED'
            ORDER BY tst.startTime ASC
            """)
    List<TeachingSlot> findByTeacherIdAndDate(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date
    );

    /**
     * Find teacher's future sessions within date range
     * Returns sessions with status PLANNED, within 7 days from today (or specific date range)
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            LEFT JOIN FETCH s.courseSession cs
            WHERE ts.teacher.id = :teacherId
              AND ts.status = 'SCHEDULED'
              AND s.status = 'PLANNED'
              AND s.date >= :fromDate
              AND s.date <= :toDate
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<TeachingSlot> findByTeacherIdAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Find teaching slot by session ID with teacher loaded
     * Used to get teacher from session when request.teacher is null
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.teacher t
            JOIN FETCH t.userAccount ua
            WHERE ts.session.id = :sessionId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
            """)
    List<TeachingSlot> findBySessionIdWithTeacher(@Param("sessionId") Long sessionId);

    /**
     * Find distinct classes that a teacher is teaching
     * Returns classes where teacher has at least one teaching slot with status SCHEDULED or SUBSTITUTED
     */
    @Query("""
            SELECT DISTINCT c FROM TeachingSlot ts
            JOIN ts.session s
            JOIN s.classEntity c
            JOIN FETCH c.course
            JOIN FETCH c.branch
            WHERE ts.teacher.id = :teacherId
              AND ts.status = 'SCHEDULED'
            ORDER BY c.code ASC
            """)
    List<org.fyp.tmssep490be.entities.ClassEntity> findDistinctClassesByTeacherId(@Param("teacherId") Long teacherId);
}
