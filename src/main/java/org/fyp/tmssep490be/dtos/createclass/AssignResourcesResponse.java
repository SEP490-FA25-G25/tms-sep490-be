package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for resource assignment with HYBRID approach
 * <p>
 * Contains assignment results from:
 * <ul>
 *   <li>Phase 1: SQL bulk insert (fast path - ~90% sessions)</li>
 *   <li>Phase 2: Java conflict analysis (detailed - ~10% conflicts)</li>
 * </ul>
 * </p>
 *
 * @see AssignResourcesRequest
 * @see org.fyp.tmssep490be.utils.AssignResourcesResponseUtil
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResourcesResponse {

    /**
     * Class ID
     */
    private Long classId;

    /**
     * Total number of sessions in the class
     */
    private Integer totalSessions;

    /**
     * Number of sessions successfully assigned resources (Phase 1 SQL bulk)
     */
    private Integer successCount;

    /**
     * Number of sessions with resource conflicts
     */
    private Integer conflictCount;

    /**
     * Detailed conflict information for manual resolution
     * <p>
     * Empty if all sessions assigned successfully
     * </p>
     */
    @Builder.Default
    private List<ResourceConflictDetail> conflicts = List.of();

    /**
     * Processing time in milliseconds
     * <p>
     * Target: <200ms for 36 sessions
     * </p>
     */
    private Long processingTimeMs;

    /**
     * Detailed conflict information for a session that couldn't be assigned a resource
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceConflictDetail {

        /**
         * Session ID with conflict
         */
        private Long sessionId;

        /**
         * Session sequence number (e.g., "Session 5")
         */
        private Integer sessionNumber;

        /**
         * Session date
         */
        private LocalDate date;

        /**
         * Day of week (PostgreSQL DOW: 0=Sun, 1=Mon, ..., 6=Sat)
         */
        private Short dayOfWeek;

        /**
         * Time slot start time
         */
        private LocalTime timeSlotStart;

        /**
         * Time slot end time
         */
        private LocalTime timeSlotEnd;

        /**
         * Requested resource ID
         */
        private Long requestedResourceId;

        /**
         * Requested resource name (e.g., "Room 203")
         */
        private String requestedResourceName;

        /**
         * Conflict type
         */
        private ConflictType conflictType;

        /**
         * Detailed conflict reason
         * <p>
         * Examples:
         * <ul>
         *   <li>"Room 203 is booked by class 'IELTS Basic A1' on 2025-01-15 at 08:00-10:00"</li>
         *   <li>"Room 203 is under maintenance on 2025-01-15"</li>
         *   <li>"Room 203 capacity (20) is less than class max capacity (25)"</li>
         * </ul>
         * </p>
         */
        private String conflictReason;

        /**
         * ID of the conflicting class (if conflict type is CLASS_BOOKING)
         */
        private Long conflictingClassId;

        /**
         * Name of the conflicting class (if conflict type is CLASS_BOOKING)
         */
        private String conflictingClassName;
    }

    /**
     * Types of resource conflicts
     */
    public enum ConflictType {
        /**
         * Resource is already booked by another class at the same time
         */
        CLASS_BOOKING,

        /**
         * Resource is under maintenance
         */
        MAINTENANCE,

        /**
         * Resource capacity is insufficient
         */
        INSUFFICIENT_CAPACITY,

        /**
         * Resource is unavailable for unknown reason
         */
        UNAVAILABLE,

        /**
         * Resource not found
         */
        RESOURCE_NOT_FOUND
    }
}
