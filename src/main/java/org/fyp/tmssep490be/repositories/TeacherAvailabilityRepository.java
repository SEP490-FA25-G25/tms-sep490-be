package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, TeacherAvailability.TeacherAvailabilityId> {

    // Kiểm tra giáo viên có đăng ký rảnh khung giờ này không
    @Query("SELECT COUNT(ta) > 0 FROM TeacherAvailability ta WHERE ta.id.timeSlotTemplateId = :timeSlotTemplateId")
    boolean existsById_TimeSlotTemplateId(@Param("timeSlotTemplateId") Long timeSlotTemplateId);
}