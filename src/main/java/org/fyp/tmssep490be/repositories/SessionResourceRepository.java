package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.SessionResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessionResourceRepository extends JpaRepository<SessionResource, SessionResource.SessionResourceId> {

        // Kiểm tra resource có được dùng trong session nào không
        boolean existsByResourceId(Long resourceId);

        // Đếm số lớp đang dùng resource
        @Query("SELECT COUNT(DISTINCT s.classEntity.id) FROM SessionResource sr " +
                        "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED'")
        Long countDistinctClassesByResourceId(@Param("resourceId") Long resourceId);

        // Đếm tổng số sessions dùng resource
        @Query("SELECT COUNT(sr) FROM SessionResource sr " +
                        "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED'")
        Long countSessionsByResourceId(@Param("resourceId") Long resourceId);

        // Tìm session tiếp theo của resource
        @Query(value = """
                        SELECT s.* FROM session s
                        JOIN session_resource sr ON s.id = sr.session_id
                        JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
                        WHERE sr.resource_id = :resourceId
                        AND s.status != 'CANCELLED'
                        AND (s.date > :currentDate OR (s.date = :currentDate AND tst.start_time > :currentTime))
                        ORDER BY s.date ASC, tst.start_time ASC
                        LIMIT 1
                        """, nativeQuery = true)
        Session findNextSessionByResourceId(
                        @Param("resourceId") Long resourceId,
                        @Param("currentDate") LocalDate currentDate,
                        @Param("currentTime") LocalTime currentTime);

        // Lấy tất cả sessions của resource
        @Query("SELECT s FROM SessionResource sr " +
                        "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED' " +
                        "ORDER BY s.date DESC")
        List<Session> findSessionsByResourceId(@Param("resourceId") Long resourceId);

        // Tìm sức chứa lớn nhất của các lớp đang dùng resource (để validate khi giảm
        // capacity)
        @Query("SELECT MAX(s.classEntity.maxCapacity) FROM SessionResource sr " +
                        "JOIN sr.session s WHERE sr.resource.id = :resourceId")
        Integer findMaxClassCapacityByResourceId(@Param("resourceId") Long resourceId);

        // Tìm các session resources theo resource, ngày, và time slot (để check
        // conflict)
        @Query("SELECT sr FROM SessionResource sr " +
                        "JOIN sr.session s " +
                        "WHERE sr.resource.id = :resourceId " +
                        "AND s.date = :sessionDate " +
                        "AND s.timeSlotTemplate.id = :timeSlotId " +
                        "AND s.status != org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED")
        List<SessionResource> findByResourceIdAndSessionDateAndSessionTimeSlotId(
                        @Param("resourceId") Long resourceId,
                        @Param("sessionDate") LocalDate sessionDate,
                        @Param("timeSlotId") Long timeSlotId);

        // ==================== FROM DEPRECATED - CONFLICT CHECK ====================

        /**
         * Check if resource is booked at a specific date and time slot
         * Used for validation before assigning resources
         *
         * @param excludeSessionId Session ID to exclude from check (when updating
         *                         existing session)
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
                        @Param("statuses") List<org.fyp.tmssep490be.entities.enums.SessionStatus> statuses,
                        @Param("excludeSessionId") Long excludeSessionId);

        /**
         * HYBRID APPROACH - Phase 1: SQL Bulk Insert (Fast Path)
         * Bulk assigns resource to all sessions on specific day of week that:
         * - Belong to the specified class
         * - Occur on the specified day of week (PostgreSQL DOW format)
         * - Have no existing resource assignment for this resource
         * - Have no conflict with other classes using this resource at same date/time
         *
         * @param classId    class ID
         * @param dayOfWeek  day of week (PostgreSQL DOW: 0=Sunday, 1=Monday, ...,
         *                   6=Saturday)
         * @param resourceId resource ID to assign
         * @return number of sessions successfully assigned (excluding conflicts)
         */
        @org.springframework.data.jpa.repository.Modifying
        @Query(value = """
                        INSERT INTO session_resource (session_id, resource_id)
                        SELECT s.id, :resourceId
                        FROM session s
                        WHERE s.class_id = :classId
                          AND EXTRACT(DOW FROM s.date) = :dayOfWeek
                          AND NOT EXISTS (
                            SELECT 1 FROM session_resource sr_existing
                            WHERE sr_existing.session_id = s.id
                              AND sr_existing.resource_id = :resourceId
                          )
                          AND NOT EXISTS (
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
                        @Param("resourceId") Long resourceId);

        /**
         * Find sessions with resource conflicts for a specific day of week
         * Used in Phase 2 (Java Analysis) to identify sessions that couldn't be
         * assigned
         * in Phase 1 (SQL Bulk) due to conflicts.
         *
         * @param classId    class ID
         * @param dayOfWeek  day of week (PostgreSQL DOW: 0=Sunday, 1=Monday, ...,
         *                   6=Saturday)
         * @param resourceId resource ID that was attempted
         * @return list of [sessionId, sessionDate, timeSlotId, conflictingSessionId,
         *         conflictingClassId]
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
                        @Param("resourceId") Long resourceId);

        /**
         * Find detailed conflict information for a specific session and resource
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
                        @Param("resourceId") Long resourceId);
}