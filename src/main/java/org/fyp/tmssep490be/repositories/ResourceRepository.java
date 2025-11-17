package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.1: RESOURCE ASSIGNMENT ====================

    /**
     * Find available resources for a class session
     * <p>
     * Filters resources by:
     * <ul>
     *   <li>Branch match (resource belongs to same branch as class)</li>
     *   <li>Resource type match (ROOM/ONLINE_ACCOUNT)</li>
     *   <li>Capacity >= class max capacity</li>
     *   <li>No conflict (not booked at same date + time slot)</li>
     * </ul>
     * </p>
     *
     * @param branchId          branch ID
     * @param resourceType      resource type filter (ROOM/ONLINE_ACCOUNT)
     * @param minCapacity       minimum capacity required
     * @param date              session date
     * @param timeSlotTemplateId time slot ID
     * @return list of available resources
     */
    @Query("SELECT r FROM Resource r " +
           "WHERE r.branch.id = :branchId " +
           "AND r.resourceType = :resourceType " +
           "AND r.capacity >= :minCapacity " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM SessionResource sr " +
           "  JOIN sr.session s " +
           "  WHERE sr.resource.id = r.id " +
           "  AND s.date = :date " +
           "  AND s.timeSlotTemplate.id = :timeSlotTemplateId" +
           ") " +
           "ORDER BY r.capacity ASC, r.name ASC")
    List<Resource> findAvailableResourcesForSession(
            @Param("branchId") Long branchId,
            @Param("resourceType") ResourceType resourceType,
            @Param("minCapacity") Integer minCapacity,
            @Param("date") LocalDate date,
            @Param("timeSlotTemplateId") Long timeSlotTemplateId
    );

    /**
     * Check if resource has sufficient capacity for class
     *
     * @param resourceId  resource ID
     * @param minCapacity minimum capacity required
     * @return true if capacity is sufficient
     */
    @Query("SELECT CASE WHEN r.capacity >= :minCapacity THEN true ELSE false END " +
           "FROM Resource r WHERE r.id = :resourceId")
    Boolean hasSufficientCapacity(
            @Param("resourceId") Long resourceId,
            @Param("minCapacity") Integer minCapacity
    );

    /**
     * Find resource by ID with branch information
     *
     * @param resourceId resource ID
     * @return resource with branch loaded
     */
    @Query("SELECT r FROM Resource r " +
           "LEFT JOIN FETCH r.branch " +
           "WHERE r.id = :resourceId")
    Resource findByIdWithBranch(@Param("resourceId") Long resourceId);

    /**
     * Find resources by branch, type, and minimum capacity (for Step 4 query)
     * <p>
     * Returns all resources that match basic criteria without checking conflicts.
     * Conflicts are checked separately in the service layer.
     * </p>
     *
     * @param branchId     branch ID
     * @param resourceType resource type filter
     * @param minCapacity  minimum capacity required
     * @return list of matching resources ordered by capacity
     */
    @Query("SELECT r FROM Resource r " +
           "WHERE r.branch.id = :branchId " +
           "AND r.resourceType = :resourceType " +
           "AND r.capacity >= :minCapacity " +
           "ORDER BY r.capacity ASC, r.name ASC")
    List<Resource> findByBranchAndTypeAndCapacity(
            @Param("branchId") Long branchId,
            @Param("resourceType") ResourceType resourceType,
            @Param("minCapacity") Integer minCapacity
    );
}
