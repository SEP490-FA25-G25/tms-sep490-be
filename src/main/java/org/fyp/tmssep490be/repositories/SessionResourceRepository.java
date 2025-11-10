package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SessionResource;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionResourceRepository extends JpaRepository<SessionResource, SessionResource.SessionResourceId> {
    
    /**
     * Check if a session has any resource assignment
     * Used for STEP 2: Review sessions status check
     */
    @Query("SELECT COUNT(sr) > 0 FROM SessionResource sr WHERE sr.session.id = :sessionId")
    boolean existsBySessionId(@Param("sessionId") Long sessionId);
    
    /**
     * Check if resource is booked at a specific date and time slot
     * Used for validation before approving MODALITY_CHANGE or RESCHEDULE
     * @param excludeSessionId Session ID to exclude from check (when updating existing session)
     */
    @Query("SELECT COUNT(sr) > 0 FROM SessionResource sr " +
           "JOIN sr.session s " +
           "WHERE sr.resource.id = :resourceId " +
           "AND s.date = :date " +
           "AND s.timeSlotTemplate.id = :timeSlotTemplateId " +
           "AND s.status IN :statuses " +
           "AND (:excludeSessionId IS NULL OR s.id != :excludeSessionId)")
    boolean existsByResourceIdAndDateAndTimeSlotAndStatusIn(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("timeSlotTemplateId") Long timeSlotTemplateId,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );
    
    /**
     * Find all session resources for a session
     */
    @Query("SELECT sr FROM SessionResource sr WHERE sr.session.id = :sessionId")
    List<SessionResource> findBySessionId(@Param("sessionId") Long sessionId);
    
    /**
     * Delete all session resources for a session
     */
    @Modifying
    @Query("DELETE FROM SessionResource sr WHERE sr.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.1: RESOURCE ASSIGNMENT (HYBRID) ====================

    /**
     * HYBRID APPROACH - Phase 1: SQL Bulk Insert (Fast Path)
     * <p>
     * Bulk assigns resource to all sessions on specific day of week that:
     * <ul>
     *   <li>Belong to the specified class</li>
     *   <li>Occur on the specified day of week (PostgreSQL DOW format)</li>
     *   <li>Have no existing resource assignment for this resource</li>
     *   <li>Have no conflict with other classes using this resource at same date/time</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Performance Target:</strong> ~50-100ms for 36 sessions (90% success rate)
     * </p>
     *
     * @param classId     class ID
     * @param dayOfWeek   day of week (PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday)
     * @param resourceId  resource ID to assign
     * @return number of sessions successfully assigned (excluding conflicts)
     */
    @Modifying
    @Query(value = """
        INSERT INTO session_resource (session_id, resource_id)
        SELECT s.id, :resourceId
        FROM session s
        WHERE s.class_id = :classId
          AND EXTRACT(DOW FROM s.date) = :dayOfWeek
          AND NOT EXISTS (
            -- Skip sessions already assigned this resource
            SELECT 1 FROM session_resource sr_existing
            WHERE sr_existing.session_id = s.id
              AND sr_existing.resource_id = :resourceId
          )
          AND NOT EXISTS (
            -- Skip sessions where resource has conflict (booked by another class)
            SELECT 1 FROM session_resource sr2
            JOIN session s2 ON sr2.session_id = s2.id
            WHERE sr2.resource_id = :resourceId
              AND s2.date = s.date
              AND s2.time_slot_template_id = s.time_slot_template_id
              AND s2.id != s.id
          )
        """, nativeQuery = true)
    int bulkInsertResourcesForDayOfWeek(
            @Param("classId") Long classId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("resourceId") Long resourceId
    );

    /**
     * Find sessions with resource conflicts for a specific day of week
     * <p>
     * Used in Phase 2 (Java Analysis) to identify sessions that couldn't be assigned
     * in Phase 1 (SQL Bulk) due to conflicts.
     * </p>
     *
     * @param classId     class ID
     * @param dayOfWeek   day of week (PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday)
     * @param resourceId  resource ID that was attempted
     * @return list of sessions with conflicts
     */
    @Query(value = """
        SELECT s.id as sessionId,
               s.date as sessionDate,
               s.time_slot_template_id as timeSlotId,
               s2.id as conflictingSessionId,
               s2.class_id as conflictingClassId
        FROM session s
        LEFT JOIN session_resource sr ON s.id = sr.session_id AND sr.resource_id = :resourceId
        LEFT JOIN session_resource sr2 ON sr2.resource_id = :resourceId
        LEFT JOIN session s2 ON sr2.session_id = s2.id
                            AND s2.date = s.date
                            AND s2.time_slot_template_id = s.time_slot_template_id
                            AND s2.id != s.id
        WHERE s.class_id = :classId
          AND EXTRACT(DOW FROM s.date) = :dayOfWeek
          AND sr.session_id IS NULL
          AND s2.id IS NOT NULL
        """, nativeQuery = true)
    List<Object[]> findSessionsWithResourceConflict(
            @Param("classId") Long classId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("resourceId") Long resourceId
    );

    /**
     * Find detailed conflict information for a specific session and resource
     * <p>
     * Returns information about which class is blocking the resource
     * </p>
     *
     * @param sessionId   session ID with conflict
     * @param resourceId  resource ID
     * @return conflict details [conflicting_session_id, conflicting_class_id, conflicting_class_name]
     */
    @Query(value = """
        SELECT s2.id as conflicting_session_id,
               c2.id as conflicting_class_id,
               c2.name as conflicting_class_name,
               s2.date as conflicting_date,
               tst.start_time as conflicting_time_start,
               tst.end_time as conflicting_time_end
        FROM session s1
        JOIN session_resource sr2 ON sr2.resource_id = :resourceId
        JOIN session s2 ON sr2.session_id = s2.id
                       AND s2.date = s1.date
                       AND s2.time_slot_template_id = s1.time_slot_template_id
                       AND s2.id != s1.id
        JOIN class c2 ON s2.class_id = c2.id
        JOIN time_slot_template tst ON s2.time_slot_template_id = tst.id
        WHERE s1.id = :sessionId
        LIMIT 1
        """, nativeQuery = true)
    Object[] findConflictingSessionDetails(
            @Param("sessionId") Long sessionId,
            @Param("resourceId") Long resourceId
    );
}
