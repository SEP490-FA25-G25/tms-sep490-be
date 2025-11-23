package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
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
     * Find sessions for a specific student on a given date
     * Using native query for better performance and clarity
     */
    @Query(value = "SELECT s.* FROM session s " +
           "JOIN class c ON s.class_id = c.id " +
           "JOIN enrollment e ON e.class_id = c.id " +
           "WHERE e.student_id = :studentId " +
           "AND s.date = :date " +
           "AND s.status = 'PLANNED' " +
           "AND e.status = 'ENROLLED'",
           nativeQuery = true)
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
        @Param("toDate") LocalDate toDate
    );

    /**
     * Find available makeup sessions for a specific course session
     * Returns sessions with same courseSessionId, in future, with available capacity
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
        @Param("targetModality") String targetModality
    );

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
        @Param("statuses") List<SessionStatus> statuses
    );

    /**
     * Find past sessions by class ID (date < today)
     * Used for content gap analysis - get sessions target class already covered
     */
    @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
           "AND s.date < :date " +
           "ORDER BY s.date ASC")
    List<Session> findByClassIdAndDateBefore(
        @Param("classId") Long classId,
        @Param("date") LocalDate date
    );

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
            @Param("date") LocalDate date
    );

    /**
     * Find sessions that have passed their date and are still in PLANNED status
     * Used to automatically mark them as DONE
     */
    @Query("SELECT s FROM Session s " +
           "WHERE s.date < :today " +
           "AND s.status = :status")
    List<Session> findPastSessionsByStatus(
            @Param("today") LocalDate today,
            @Param("status") SessionStatus status
    );

    /**
     * Count total number of sessions for a class
     */
    long countByClassEntityId(Long classId);

    /**
     * Count total number of sessions for a class excluding CANCELLED sessions
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND s.status != 'CANCELLED'")
    long countByClassEntityIdExcludingCancelled(@Param("classId") Long classId);
}
