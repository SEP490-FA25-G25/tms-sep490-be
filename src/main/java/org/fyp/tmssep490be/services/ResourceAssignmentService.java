package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ResourceType;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for assigning resources to class sessions (STEP 4 of Create Class workflow)
 * <p>
 * Implements HYBRID approach:
 * <ul>
 *   <li>Phase 1: SQL bulk insert for fast assignment (~90% sessions)</li>
 *   <li>Phase 2: Java conflict analysis for detailed error reporting (~10% conflicts)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Performance Target:</strong> <200ms for 36 sessions
 * </p>
 *
 * @see AssignResourcesRequest
 * @see AssignResourcesResponse
 */
public interface ResourceAssignmentService {

    /**
     * Assign resources to class sessions using HYBRID approach
     * <p>
     * <strong>Phase 1 (SQL Bulk Insert - Fast):</strong>
     * <ul>
     *   <li>Bulk INSERT for all non-conflict sessions (~90%)</li>
     *   <li>Uses native PostgreSQL query with conflict detection</li>
     *   <li>Performance: ~50-100ms</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Phase 2 (Java Analysis - Detailed):</strong>
     * <ul>
     *   <li>Identify sessions with conflicts (~10%)</li>
     *   <li>Analyze conflict reason (CLASS_BOOKING, INSUFFICIENT_CAPACITY, etc.)</li>
     *   <li>Return detailed conflict information for manual resolution</li>
     * </ul>
     * </p>
     *
     * @param classId class ID
     * @param request resource assignment pattern (day → resource mapping)
     * @return assignment response with success count and conflict details
     */
    AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request);

    /**
     * Query available resources for a specific session
     * <p>
     * Filters resources by:
     * <ul>
     *   <li>Branch match (resource belongs to same branch as class)</li>
     *   <li>Resource type (ROOM/ONLINE_ACCOUNT)</li>
     *   <li>Capacity >= class max capacity</li>
     *   <li>No conflict (not booked at same date + time slot)</li>
     * </ul>
     * </p>
     *
     * @param classId     class ID
     * @param sessionId   session ID (for date and time slot)
     * @param resourceType optional resource type filter
     * @return list of available resources sorted by capacity
     */
    List<Resource> queryAvailableResources(Long classId, Long sessionId, ResourceType resourceType);

    /**
     * Manually assign resource to a specific session (conflict resolution)
     * <p>
     * Used when Phase 1 bulk assignment encounters conflicts.
     * Academic Staff manually selects alternative resource for conflicting sessions.
     * </p>
     *
     * @param sessionId  session ID with conflict
     * @param resourceId alternative resource ID
     * @return true if assigned successfully
     * @throws org.fyp.tmssep490be.exceptions.CustomException if still has conflict
     */
    boolean assignResourceToSession(Long sessionId, Long resourceId);

    /**
     * Remove resource assignment from a session
     *
     * @param sessionId  session ID
     * @param resourceId resource ID to remove
     * @return true if removed successfully
     */
    boolean removeResourceFromSession(Long sessionId, Long resourceId);

    /**
     * Get resource assignment count for a class
     *
     * @param classId class ID
     * @return number of sessions with resource assignments
     */
    long getResourceAssignmentCount(Long classId);

    /**
     * Check if all sessions in a class have resource assignments
     *
     * @param classId class ID
     * @return true if all sessions have resources
     */
    boolean isFullyAssigned(Long classId);
}
