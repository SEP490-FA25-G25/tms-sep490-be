package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.ActionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    /**
     * Find action items by QA Report with pagination
     */
    Page<ActionItem> findByQaReportIdOrderByCreatedAtDesc(Long qaReportId, Pageable pageable);

    /**
     * Find action items by assigned user with pagination
     */
    Page<ActionItem> findByAssignedToIdOrderByCreatedAtDesc(Long assignedToId, Pageable pageable);

    /**
     * Find action items by status with pagination
     */
    Page<ActionItem> findByStatusOrderByCreatedAtDesc(org.fyp.tmssep490be.entities.enums.ActionItemStatus status, Pageable pageable);

    /**
     * Find action items by QA Report and assigned user
     */
    List<ActionItem> findByQaReportIdAndAssignedToId(Long qaReportId, Long assignedToId);

    /**
     * Find overdue action items (due date passed and not completed)
     */
    @Query("SELECT ai FROM ActionItem ai WHERE ai.dueDate < :now AND ai.status NOT IN ('COMPLETED', 'CANCELLED')")
    List<ActionItem> findOverdueActionItems(@Param("now") OffsetDateTime now);

    /**
     * Find pending action items for a user
     */
    @Query("SELECT ai FROM ActionItem ai WHERE ai.assignedTo.id = :userId AND ai.status = 'PENDING' ORDER BY ai.priority DESC, ai.dueDate ASC")
    List<ActionItem> findPendingActionItemsForUser(@Param("userId") Long userId);

    /**
     * Count action items by status
     */
    @Query("SELECT ai.status, COUNT(ai) FROM ActionItem ai WHERE ai.qaReport.id = :qaReportId GROUP BY ai.status")
    List<Object[]> countActionItemsByStatus(@Param("qaReportId") Long qaReportId);

    /**
     * Find action items created in date range
     */
    @Query("SELECT ai FROM ActionItem ai WHERE ai.createdAt BETWEEN :startDate AND :endDate ORDER BY ai.createdAt DESC")
    List<ActionItem> findByDateRange(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);
}