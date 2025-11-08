package org.fyp.tmssep490be.dtos.createclass;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.util.List;

/**
 * Complex DTO for teacher availability with PRE-CHECK information
 * <p>
 * Shows detailed availability status BEFORE user selects a teacher
 * </p>
 * <p>
 * Example:
 * <pre>
 * {
 *   "teacherId": 5,
 *   "fullName": "Jane Smith",
 *   "skills": ["GENERAL", "SPEAKING"],
 *   "hasGeneralSkill": true,
 *   "totalSessions": 10,
 *   "availableSessions": 10,
 *   "availabilityPercentage": 100.0,
 *   "availabilityStatus": "FULLY_AVAILABLE",
 *   "conflicts": {
 *     "noAvailability": 0,
 *     "teachingConflict": 0,
 *     "leaveConflict": 0
 *   }
 * }
 * </pre>
 * </p>
 *
 * @see org.fyp.tmssep490be.repositories.TeacherRepository#findAvailableTeachersWithPrecheck
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailabilityDTO {

    /**
     * Teacher ID
     */
    private Long teacherId;

    /**
     * Teacher's full name
     */
    private String fullName;

    /**
     * Teacher's email
     */
    private String email;

    /**
     * List of skills this teacher possesses
     * <p>
     * If contains GENERAL → Can teach ANY session type (universal skill)
     * </p>
     */
    @Builder.Default
    private List<Skill> skills = List.of();

    /**
     * Whether teacher has GENERAL skill (universal - can teach any session type)
     */
    private Boolean hasGeneralSkill;

    /**
     * Total number of sessions in the class
     */
    private Integer totalSessions;

    /**
     * Number of sessions this teacher is available to teach
     * <p>
     * Range: 0 to totalSessions
     * </p>
     */
    private Integer availableSessions;

    /**
     * Availability percentage (0-100)
     * <p>
     * Calculation: (availableSessions / totalSessions) * 100
     * </p>
     */
    private Double availabilityPercentage;

    /**
     * Overall availability status
     */
    private AvailabilityStatus availabilityStatus;

    /**
     * Detailed breakdown of conflicts by type
     */
    private ConflictBreakdown conflicts;

    /**
     * Availability status enum
     */
    public enum AvailabilityStatus {
        /**
         * Teacher can teach ALL sessions (100% availability)
         */
        FULLY_AVAILABLE,

        /**
         * Teacher can teach SOME sessions (1-99% availability)
         */
        PARTIALLY_AVAILABLE,

        /**
         * Teacher cannot teach ANY sessions (0% availability)
         */
        UNAVAILABLE
    }

    /**
     * Detailed conflict breakdown
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictBreakdown {

        /**
         * Number of sessions where teacher has not registered availability
         * <p>
         * Condition: No matching record in teacher_availability table
         * </p>
         */
        @Builder.Default
        private Integer noAvailability = 0;

        /**
         * Number of sessions where teacher is already teaching another class
         * <p>
         * Condition: Existing teaching_slot record at same time
         * </p>
         */
        @Builder.Default
        private Integer teachingConflict = 0;

        /**
         * Number of sessions where teacher is on leave
         * <p>
         * Condition: Active teacher_request with type LEAVE overlapping session time
         * </p>
         */
        @Builder.Default
        private Integer leaveConflict = 0;

        /**
         * Number of sessions where teacher doesn't have required skill
         * <p>
         * Condition: Session requires specific skill that teacher doesn't have
         * (Unless teacher has GENERAL skill)
         * </p>
         */
        @Builder.Default
        private Integer skillMismatch = 0;

        /**
         * Total number of conflicts (sum of all conflict types)
         */
        @Builder.Default
        private Integer totalConflicts = 0;
    }
}
