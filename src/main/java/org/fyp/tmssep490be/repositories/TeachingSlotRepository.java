package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
}
