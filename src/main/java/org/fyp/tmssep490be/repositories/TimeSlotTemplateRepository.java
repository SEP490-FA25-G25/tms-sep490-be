package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {

    /**
     * Find time slot templates by branch ordered by start time
     * Used in Phase 1.3: Assign Time Slots (STEP 3)
     */
    @Query("SELECT tst FROM TimeSlotTemplate tst WHERE tst.branch.id = :branchId ORDER BY tst.startTime ASC")
    List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(@Param("branchId") Long branchId);
}
