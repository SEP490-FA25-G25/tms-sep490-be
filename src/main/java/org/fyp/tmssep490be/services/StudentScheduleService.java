package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.schedule.SessionDetailDTO;
import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;

import java.time.LocalDate;

public interface StudentScheduleService {

    /**
     * Get weekly schedule for a student
     * @param studentId The student's ID
     * @param weekStart Monday of the target week
     * @return Weekly schedule organized by day and timeslot
     */
    WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart);

    /**
     * Get weekly schedule for a student filtered by specific class
     * @param studentId The student's ID
     * @param classId The class ID to filter sessions by (optional)
     * @param weekStart Monday of the target week
     * @return Weekly schedule organized by day and timeslot for the specified class
     */
    WeeklyScheduleResponseDTO getWeeklyScheduleByClass(Long studentId, Long classId, LocalDate weekStart);

    /**
     * Get detailed information for a specific session
     * @param studentId The student's ID
     * @param sessionId The session's ID
     * @return Full session details
     */
    SessionDetailDTO getSessionDetail(Long studentId, Long sessionId);

    /**
     * Helper: Calculate Monday of current week
     * @return LocalDate of current Monday
     */
    LocalDate getCurrentWeekStart();
}