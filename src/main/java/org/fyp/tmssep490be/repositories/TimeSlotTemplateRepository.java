package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {

    @Query("SELECT tst FROM TimeSlotTemplate tst WHERE tst.branch.id = :branchId ORDER BY tst.startTime ASC")
    List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(@Param("branchId") Long branchId);

    // Kiểm tra tên khung giờ đã tồn tại trong branch chưa
    @Query("SELECT COUNT(tst) > 0 FROM TimeSlotTemplate tst " +
            "WHERE tst.branch.id = :branchId " +
            "AND LOWER(tst.name) = LOWER(:name) " +
            "AND (:excludeId IS NULL OR tst.id != :excludeId)")
    boolean existsByBranchIdAndNameIgnoreCase(
            @Param("branchId") Long branchId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    // Kiểm tra khung giờ (startTime-endTime) đã tồn tại chưa
    @Query("SELECT COUNT(tst) > 0 FROM TimeSlotTemplate tst " +
            "WHERE tst.branch.id = :branchId " +
            "AND tst.startTime = :startTime " +
            "AND tst.endTime = :endTime " +
            "AND (:excludeId IS NULL OR tst.id != :excludeId)")
    boolean existsByBranchIdAndStartTimeAndEndTime(
            @Param("branchId") Long branchId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}