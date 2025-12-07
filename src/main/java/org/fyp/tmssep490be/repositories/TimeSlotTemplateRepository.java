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
}