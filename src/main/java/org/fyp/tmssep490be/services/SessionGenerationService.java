package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Course;
import org.fyp.tmssep490be.entities.Session;

import java.util.List;

/**
 * Service interface for generating sessions based on class and course templates
 * Auto-generates sessions when creating a new class (STEP 2 of Create Class workflow)
 */
public interface SessionGenerationService {

    /**
     * Generate sessions for a class based on course template
     *
     * Algorithm:
     * 1. Get course sessions ordered by phase ASC, sequence ASC
     * 2. Start from class start_date
     * 3. For each course session:
     *    - Find next occurrence of target day from schedule_days
     *    - Create session with date = calculated date
     *    - Link to course session template
     *    - Set type = CLASS, status = PLANNED
     *
     * @param classEntity The class entity with schedule configuration
     * @param course The course with session templates
     * @return List of generated Session entities (not saved yet)
     */
    List<Session> generateSessionsForClass(ClassEntity classEntity, Course course);

    /**
     * Calculate end date based on generated sessions
     *
     * @param sessions List of generated sessions
     * @return Latest session date or null if no sessions
     */
    java.time.LocalDate calculateEndDate(List<Session> sessions);

    /**
     * Validate schedule configuration
     *
     * @param classEntity Class entity to validate
     * @return true if schedule is valid for session generation
     */
    boolean isValidSchedule(ClassEntity classEntity);
}