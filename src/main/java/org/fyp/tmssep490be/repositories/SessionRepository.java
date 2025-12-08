package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByClassEntityIdOrderByDateAsc(Long classId);

    List<Session> findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
            Long classId,
            LocalDate date,
            SessionStatus status);
    // Kiểm tra khung giờ có được dùng trong session không
    boolean existsByTimeSlotTemplateId(Long timeSlotTemplateId);

    // Đếm số lớp đang dùng khung giờ này
    @Query("SELECT COUNT(DISTINCT s.classEntity.id) FROM Session s WHERE s.timeSlotTemplate.id = :timeSlotId AND s.status != 'CANCELLED'")
    Long countDistinctClassesByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    // Đếm tổng số session dùng khung giờ này
    @Query("SELECT COUNT(s) FROM Session s WHERE s.timeSlotTemplate.id = :timeSlotId AND s.status != 'CANCELLED'")
    Long countSessionsByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    // Đếm session tương lai (kiểm tra trước khi ngưng hoạt động)
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

    // Tìm sessions theo khung giờ
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
}
