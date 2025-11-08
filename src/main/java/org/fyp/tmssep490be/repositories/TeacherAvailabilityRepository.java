package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TeacherAvailability entity
 * <p>
 * Used in Phase 2.3 Teacher Assignment (PRE-CHECK) to verify teacher availability
 * before assignment. Supports composite primary key (teacher_id, time_slot_template_id, day_of_week).
 * </p>
 */
@Repository
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, TeacherAvailability.TeacherAvailabilityId> {

    // ==================== PHASE 2.2: TEACHER AVAILABILITY QUERIES ====================

    /**
     * Find teacher availability by composite key components
     * <p>
     * Used to check if teacher is available for specific time slot on specific day
     * </p>
     *
     * @param teacherId           teacher ID
     * @param timeSlotTemplateId  time slot template ID
     * @param dayOfWeek           day of week (1=Mon, 2=Tue, ..., 7=Sun)
     * @return Optional of TeacherAvailability
     */
    @Query("SELECT ta FROM TeacherAvailability ta " +
           "WHERE ta.id.teacherId = :teacherId " +
           "AND ta.id.timeSlotTemplateId = :timeSlotTemplateId " +
           "AND ta.id.dayOfWeek = :dayOfWeek")
    Optional<TeacherAvailability> findByTeacherIdAndTimeSlotTemplateIdAndDayOfWeek(
            @Param("teacherId") Long teacherId,
            @Param("timeSlotTemplateId") Long timeSlotTemplateId,
            @Param("dayOfWeek") Short dayOfWeek
    );

    /**
     * Find all availabilities for a teacher
     * <p>
     * Returns all time slots and days when teacher is available
     * </p>
     *
     * @param teacherId teacher ID
     * @return list of teacher availabilities
     */
    @Query("SELECT ta FROM TeacherAvailability ta " +
           "LEFT JOIN FETCH ta.timeSlotTemplate " +
           "WHERE ta.id.teacherId = :teacherId " +
           "ORDER BY ta.id.dayOfWeek ASC, ta.timeSlotTemplate.startTime ASC")
    List<TeacherAvailability> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Quick check if teacher is available for specific time slot on specific day
     * <p>
     * Lightweight existence check without loading full entity
     * </p>
     *
     * @param teacherId           teacher ID
     * @param timeSlotTemplateId  time slot template ID
     * @param dayOfWeek           day of week (1=Mon, 2=Tue, ..., 7=Sun)
     * @return true if teacher is available
     */
    @Query("SELECT CASE WHEN COUNT(ta) > 0 THEN true ELSE false END " +
           "FROM TeacherAvailability ta " +
           "WHERE ta.id.teacherId = :teacherId " +
           "AND ta.id.timeSlotTemplateId = :timeSlotTemplateId " +
           "AND ta.id.dayOfWeek = :dayOfWeek")
    boolean existsByTeacherIdAndTimeSlotTemplateIdAndDayOfWeek(
            @Param("teacherId") Long teacherId,
            @Param("timeSlotTemplateId") Long timeSlotTemplateId,
            @Param("dayOfWeek") Short dayOfWeek
    );

    /**
     * Find all teachers available for specific time slot on specific day
     * <p>
     * Used to query available teachers for a session
     * </p>
     *
     * @param timeSlotTemplateId time slot template ID
     * @param dayOfWeek          day of week (1=Mon, 2=Tue, ..., 7=Sun)
     * @return list of teacher IDs
     */
    @Query("SELECT ta.id.teacherId FROM TeacherAvailability ta " +
           "WHERE ta.id.timeSlotTemplateId = :timeSlotTemplateId " +
           "AND ta.id.dayOfWeek = :dayOfWeek")
    List<Long> findTeacherIdsByTimeSlotTemplateIdAndDayOfWeek(
            @Param("timeSlotTemplateId") Long timeSlotTemplateId,
            @Param("dayOfWeek") Short dayOfWeek
    );

    /**
     * Count availabilities for a teacher
     * <p>
     * Used to check if teacher has any availability registered
     * </p>
     *
     * @param teacherId teacher ID
     * @return count of availabilities
     */
    @Query("SELECT COUNT(ta) FROM TeacherAvailability ta WHERE ta.id.teacherId = :teacherId")
    long countByTeacherId(@Param("teacherId") Long teacherId);
}
