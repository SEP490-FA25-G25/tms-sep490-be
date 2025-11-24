package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.schedule.TeacherSessionDetailDTO;
import org.fyp.tmssep490be.dtos.schedule.TeacherWeeklyScheduleResponseDTO;

import java.time.LocalDate;

public interface TeacherScheduleService {

    /**
     * Get weekly schedule for a teacher
     * @param teacherId The teacher's ID
     * @param weekStart Monday of the target week
     * @return Weekly schedule organized by day and timeslot
     */
    TeacherWeeklyScheduleResponseDTO getWeeklySchedule(Long teacherId, LocalDate weekStart);

    /**
     * Get weekly schedule for a teacher filtered by specific class
     * @param teacherId The teacher's ID
     * @param classId The class ID to filter sessions by (optional)
     * @param weekStart Monday of the target week
     * @return Weekly schedule organized by day and timeslot for the specified class
     */
    TeacherWeeklyScheduleResponseDTO getWeeklyScheduleByClass(Long teacherId, Long classId, LocalDate weekStart);

    /**
     * Get detailed information for a specific session
     * @param teacherId The teacher's ID
     * @param sessionId The session's ID
     * @return Full session details
     */
    TeacherSessionDetailDTO getSessionDetail(Long teacherId, Long sessionId);

    /**
     * Helper: Calculate Monday of current week
     * @return LocalDate of current Monday
     */
    LocalDate getCurrentWeekStart();
}

