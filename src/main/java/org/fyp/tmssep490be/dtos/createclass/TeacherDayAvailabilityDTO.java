package org.fyp.tmssep490be.dtos.createclass;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Teacher Availability by Day of Week
 * <p>
 * Used for "Phân công nhiều giáo viên" mode where teachers can be assigned
 * to specific days of the week (e.g., only Mondays) instead of all sessions.
 * </p>
 * <p>
 * A teacher is considered available for a specific day if they are available
 * for ALL sessions on that day of week from start to end of the course.
 * </p>
 * 
 * @see org.fyp.tmssep490be.services.TeacherAssignmentService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDayAvailabilityDTO {
    
    /**
     * Teacher ID
     */
    private Long teacherId;
    
    /**
     * Teacher full name
     */
    private String fullName;
    
    /**
     * Teacher email
     */
    private String email;
    
    /**
     * Teacher employee code
     */
    private String employeeCode;
    
    /**
     * List of skills the teacher has
     */
    private List<Skill> skills;
    
    /**
     * Whether teacher has GENERAL skill (can teach any subject)
     */
    private Boolean hasGeneralSkill;
    
    /**
     * List of days of week where teacher is fully available
     * <p>
     * Empty list means teacher is not available for any full day
     * </p>
     */
    private List<DayAvailability> availableDays;
    
    /**
     * Total number of sessions in the class
     */
    private Integer totalClassSessions;
    
    /**
     * Availability information for a specific day of week
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayAvailability {
        
        /**
         * Day of week (1=Monday, 2=Tuesday, ..., 7=Sunday)
         * Uses ISO-8601 standard
         */
        private Short dayOfWeek;
        
        /**
         * Day name in Vietnamese (e.g., "Thứ Hai", "Thứ Ba")
         */
        private String dayName;
        
        /**
         * Total number of sessions on this day of week
         */
        private Integer totalSessions;
        
        /**
         * Number of sessions teacher is available for (should equal totalSessions)
         */
        private Integer availableSessions;
        
        /**
         * First date of sessions on this day of week
         */
        private LocalDate firstDate;
        
        /**
         * Last date of sessions on this day of week
         */
        private LocalDate lastDate;
        
        /**
         * Whether teacher is available for ALL sessions on this day
         * <p>
         * true = availableSessions == totalSessions (no conflicts)
         * false = has at least 1 conflict (should not appear in results)
         * </p>
         */
        private Boolean isFullyAvailable;
        
        /**
         * Time slot information for sessions on this day
         */
        private String timeSlotDisplay; // e.g., "18:00 - 20:00"
    }
}
