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
}
