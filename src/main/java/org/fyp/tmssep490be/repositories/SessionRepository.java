package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByClassEntityId(Long classId);

    List<Session> findByClassEntityIdOrderByDateAsc(Long classId);

    List<Session> findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
            Long classId,
            LocalDate date,
            SessionStatus status);

    List<Session> findByClassEntityIdInAndStatus(List<Long> classIds, SessionStatus status);

    boolean existsByTimeSlotTemplateId(Long timeSlotTemplateId);

    @Query("SELECT COUNT(DISTINCT s.classEntity.id) FROM Session s WHERE s.timeSlotTemplate.id = :timeSlotId AND s.status != 'CANCELLED'")
    Long countDistinctClassesByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.timeSlotTemplate.id = :timeSlotId AND s.status != 'CANCELLED'")
    Long countSessionsByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    @Query(value = """
            SELECT COUNT(s.id) FROM session s
            JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
            WHERE s.time_slot_template_id = :timeSlotId
            AND s.status IN ('PLANNED', 'ONGOING')
            AND (s.date > :currentDate OR (s.date = :currentDate AND tst.start_time > :currentTime))
            """, nativeQuery = true)
    Long countFutureSessionsByTimeSlotId(
            @Param("timeSlotId") Long timeSlotId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime);

    @Query(value = """
            SELECT s.* FROM session s
            JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
            WHERE tst.id = :timeSlotId AND s.status != 'CANCELLED'
            ORDER BY s.date DESC
            """, nativeQuery = true)
    List<Session> findByTimeSlotTemplateId(@Param("timeSlotId") Long timeSlotId);

    // Đếm số buổi học đã hoàn thành theo class ID
    @Query("""
            SELECT s.classEntity.id,
                   SUM(CASE WHEN s.status = 'DONE' THEN 1 ELSE 0 END),
                   COUNT(s)
            FROM Session s
            WHERE s.classEntity.id IN :classIds
              AND s.status != org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED
            GROUP BY s.classEntity.id
            """)
    List<Object[]> countSessionsByClassIds(@Param("classIds") List<Long> classIds);

    // Tìm buổi học sắp tới của giáo viên theo teacher ID
    @Query("""
            SELECT DISTINCT s FROM Session s
            JOIN s.teachingSlots ts
            JOIN ts.teacher t
            LEFT JOIN FETCH s.timeSlotTemplate tst
            LEFT JOIN FETCH s.classEntity c
            WHERE t.id = :teacherId
              AND s.status = org.fyp.tmssep490be.entities.enums.SessionStatus.PLANNED
              AND s.date BETWEEN :fromDate AND :toDate
              AND (:classId IS NULL OR c.id = :classId)
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<Session> findUpcomingSessionsForTeacher(@Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classId") Long classId);

    // Tìm tất cả buổi học của giáo viên trong tuần (tất cả status) cho schedule
    @Query("""
            SELECT DISTINCT s FROM Session s
            LEFT JOIN FETCH s.timeSlotTemplate tst
            LEFT JOIN FETCH s.classEntity c
            LEFT JOIN FETCH s.subjectSession ss
            LEFT JOIN FETCH s.sessionResources sr
            LEFT JOIN FETCH sr.resource r
            LEFT JOIN s.teachingSlots ts
            LEFT JOIN ts.teacher slotTeacher
            WHERE slotTeacher.id = :teacherId
              AND s.date BETWEEN :fromDate AND :toDate
              AND (:classId IS NULL OR c.id = :classId)
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<Session> findWeeklySessionsForTeacher(@Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("classId") Long classId);

    // Học viên chọn buổi học bù -> join với class để kiểm tra chi nhánh và hình
    // thức điều kiện cùng môn
    // Kiểm tra date trong vài tuần từ ngày hiện tại, trạng thái PLANNED, không phải
    // buổi học bị bỏ
    // Loại trừ buổi học bị mà học viên đã bỏ qua (excludeSessionId) tức là học viên
    // đang chọn buổi bị missed thì phỉa bỏ ra
    // CHỈ cho phép cùng chi nhánh (same-branch only) - không cho phép khác branch
    // dù là ONLINE
    // weeksLimit: số tuần tối đa để tìm buổi học bù (hardcoded constant)
    @Query(value = """
            SELECT s.* FROM session s
            JOIN class c ON s.class_id = c.id
            WHERE s.subject_session_id = :subjectSessionId
              AND s.date >= CURRENT_DATE
              AND s.date <= CURRENT_DATE + CAST((:weeksLimit || ' weeks') AS INTERVAL)
              AND s.status = 'PLANNED'
              AND s.id != :excludeSessionId
              AND c.branch_id = :targetBranchId
            ORDER BY s.date ASC
            """, nativeQuery = true)
    List<Session> findMakeupSessionOptions(
            @Param("subjectSessionId") Long subjectSessionId,
            @Param("excludeSessionId") Long excludeSessionId,
            @Param("targetBranchId") Long targetBranchId,
            @Param("weeksLimit") Integer weeksLimit);

    @Query("""
            SELECT s FROM Session s
            JOIN s.studentSessions ss
            WHERE ss.student.id = :studentId
              AND s.date = :date
              AND s.status IN ('PLANNED', 'ONGOING')
              AND ss.attendanceStatus != 'CANCELLED'
            """)
    List<Session> findSessionsForStudentByDate(
            @Param("studentId") Long studentId,
            @Param("date") LocalDate date);

    @Query("""
            SELECT DISTINCT s FROM Session s
            JOIN s.teachingSlots ts
            JOIN ts.teacher t
            WHERE t.id = :teacherId
              AND s.date = :date
              AND s.status IN ('PLANNED', 'ONGOING')
              AND s.id != :excludeSessionId
            """)
    List<Session> findSessionsForTeacherByDate(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date,
            @Param("excludeSessionId") Long excludeSessionId);

    @Query("SELECT s FROM Session s WHERE s.date < :today AND s.status = :status")
    List<Session> findPastSessionsByStatus(
            @Param("today") LocalDate today,
            @Param("status") SessionStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Session s SET s.status = :newStatus WHERE s.date < :today AND s.status = :oldStatus")
    int updatePastSessionsStatus(
            @Param("today") LocalDate today,
            @Param("oldStatus") SessionStatus oldStatus,
            @Param("newStatus") SessionStatus newStatus);

    @Query("SELECT s FROM Session s " +
            "LEFT JOIN FETCH s.timeSlotTemplate tst " +
            "LEFT JOIN FETCH s.teachingSlots ts " +
            "LEFT JOIN FETCH ts.teacher t " +
            "LEFT JOIN FETCH t.userAccount " +
            "LEFT JOIN FETCH s.sessionResources sr " +
            "LEFT JOIN FETCH sr.resource " +
            "WHERE s.classEntity.id = :classId " +
            "ORDER BY s.date ASC, tst.startTime ASC")
    List<Session> findAllByClassIdOrderByDateAndTime(@Param("classId") Long classId);

    List<Session> findByClassEntityIdAndStatusIn(Long classId, List<SessionStatus> statuses);

    @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId AND s.date >= :date ORDER BY s.date ASC")
    List<Session> findByClassEntityIdAndDateAfterOrEqual(@Param("classId") Long classId, @Param("date") LocalDate date);

    @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId AND s.date < :date ORDER BY s.date ASC")
    List<Session> findByClassEntityIdAndDateBefore(@Param("classId") Long classId, @Param("date") LocalDate date);

    /**
     * Find previous sessions in the same class before a date
     * Ordered desc by date/time for "latest previous" lookup
     */
    @Query("SELECT s FROM Session s " +
            "WHERE s.classEntity.id = :classId " +
            "AND s.date < :date " +
            "AND s.status <> 'CANCELLED' " +
            "ORDER BY s.date DESC, s.timeSlotTemplate.startTime DESC")
    List<Session> findPreviousSessionsByClassIdAndDate(
            @Param("classId") Long classId,
            @Param("date") java.time.LocalDate date);

    @Query("""
            SELECT COALESCE(MAX(ss.sequenceNo), 0)
            FROM Session s
            JOIN s.subjectSession ss
            WHERE s.classEntity.id = :classId
              AND s.status = 'DONE'
            """)
    Integer findLastCompletedSessionNumber(@Param("classId") Long classId);

    /**
     * Find session by ID with all related details (for QA Session Detail)
     */
    @Query("SELECT DISTINCT s FROM Session s " +
            "LEFT JOIN FETCH s.classEntity c " +
            "LEFT JOIN FETCH c.subject " +
            "LEFT JOIN FETCH c.branch " +
            "LEFT JOIN FETCH s.subjectSession ss " +
            "LEFT JOIN FETCH ss.phase phase " +
            "LEFT JOIN FETCH ss.subjectSessionCLOMappings cloMapping " +
            "LEFT JOIN FETCH cloMapping.clo " +
            "LEFT JOIN FETCH ss.subjectMaterials sm " +
            "LEFT JOIN FETCH s.timeSlotTemplate tst " +
            "LEFT JOIN FETCH s.teachingSlots ts " +
            "LEFT JOIN FETCH ts.teacher t " +
            "LEFT JOIN FETCH t.userAccount " +
            "LEFT JOIN FETCH s.sessionResources sr " +
            "LEFT JOIN FETCH sr.resource r " +
            "LEFT JOIN FETCH r.branch rb " +
            "WHERE s.id = :sessionId")
    Optional<Session> findByIdWithDetails(@Param("sessionId") Long sessionId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId")
    Long countByClassEntityId(@Param("classId") Long classId);

    @Query("SELECT COUNT(s) FROM Session s " +
            "WHERE s.classEntity.id = :classId " +
            "AND s.status != org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED")
    Long countByClassEntityIdExcludingCancelled(@Param("classId") Long classId);

    @Query("SELECT COUNT(s) FROM Session s " +
            "WHERE s.classEntity.id = :classId " +
            "AND s.status = :status")
    Long countByClassEntityIdAndStatus(@Param("classId") Long classId,
            @Param("status") org.fyp.tmssep490be.entities.enums.SessionStatus status);

    @Query("SELECT COUNT(s) FROM Session s " +
            "WHERE s.classEntity.id = :classId " +
            "AND s.status != org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED")
    Long countNonCancelledSessionsByClassId(@Param("classId") Long classId);

    @Query("SELECT COUNT(s) FROM Session s " +
            "WHERE s.classEntity.id = :classId " +
            "AND s.status IN :statuses")
    Long countByClassEntityIdAndStatusIn(@Param("classId") Long classId,
            @Param("statuses") List<org.fyp.tmssep490be.entities.enums.SessionStatus> statuses);

    /**
     * Update time slot for sessions by day of week
     * Used in Step 3: Assign Time Slots
     * PostgreSQL DOW: 0=Sunday, 1=Monday, ..., 6=Saturday
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
     * Clear all time slot assignments for a class (wizard reset)
     */
    @Query(value = """
            UPDATE session
            SET time_slot_template_id = NULL
            WHERE class_id = :classId
            """, nativeQuery = true)
    @Modifying
    int clearTimeSlotsForClass(@Param("classId") Long classId);
}
