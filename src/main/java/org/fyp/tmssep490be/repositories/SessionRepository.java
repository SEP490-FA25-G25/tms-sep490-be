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
}
