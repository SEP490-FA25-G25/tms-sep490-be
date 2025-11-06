package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Course;
import org.fyp.tmssep490be.entities.CourseSession;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;
import org.fyp.tmssep490be.repositories.CourseSessionRepository;
import org.fyp.tmssep490be.services.SessionGenerationService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of SessionGenerationService
 * Generates sessions based on course template and class schedule
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionGenerationServiceImpl implements SessionGenerationService {

    private final CourseSessionRepository courseSessionRepository;

    @Override
    public List<Session> generateSessionsForClass(ClassEntity classEntity, Course course) {
        log.info("Generating sessions for class {} (ID: {}) based on course {} (ID: {})",
                classEntity.getCode(), classEntity.getId(), course.getCode(), course.getId());

        // Get course sessions ordered by phase ASC, sequence ASC
        List<CourseSession> courseSessions = courseSessionRepository
                .findByCourseIdOrderByPhaseAscSequenceAsc(course.getId());

        if (courseSessions.isEmpty()) {
            log.warn("No course sessions found for course {} (ID: {})", course.getCode(), course.getId());
            return new ArrayList<>();
        }

        // Initialize session generation
        LocalDate currentDate = classEntity.getStartDate();
        Short[] scheduleDays = classEntity.getScheduleDays();
        List<Session> sessions = new ArrayList<>();
        int sessionIndex = 0;

        log.debug("Starting session generation from date: {} with schedule days: {}",
                currentDate, java.util.Arrays.toString(scheduleDays));

        // Generate sessions for each course session
        for (CourseSession courseSession : courseSessions) {
            // Get target day of week using modulo to cycle through schedule days
            Short targetDayOfWeek = scheduleDays[sessionIndex % scheduleDays.length];

            // Find next occurrence of target day
            LocalDate sessionDate = findNextDateForDayOfWeek(currentDate, targetDayOfWeek);

            // Create session entity
            Session session = Session.builder()
                    .classEntity(classEntity)
                    .courseSession(courseSession)
                    .date(sessionDate)
                    .type(SessionType.CLASS)
                    .status(SessionStatus.PLANNED)
                    .build();

            sessions.add(session);

            log.debug("Generated session {} for course session {} on {} (Day: {})",
                    sessionIndex + 1, courseSession.getId(), sessionDate, targetDayOfWeek);

            // Move to next session and next date
            sessionIndex++;
            currentDate = sessionDate.plusDays(1);
        }

        log.info("Successfully generated {} sessions for class {} (ID: {})",
                sessions.size(), classEntity.getCode(), classEntity.getId());

        return sessions;
    }

    @Override
    public LocalDate calculateEndDate(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }

        return sessions.stream()
                .map(Session::getDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    @Override
    public boolean isValidSchedule(ClassEntity classEntity) {
        if (classEntity.getStartDate() == null) {
            log.warn("Class start date is null");
            return false;
        }

        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            log.warn("Class schedule days are empty");
            return false;
        }

        // Validate schedule days are valid (1-7 for Monday-Sunday)
        for (Short day : classEntity.getScheduleDays()) {
            if (day < 1 || day > 7) {
                log.warn("Invalid schedule day: {}. Must be 1-7 (Monday-Sunday)", day);
                return false;
            }
        }

        // Validate start date is in the future (optional, can be current date too)
        if (classEntity.getStartDate().isBefore(LocalDate.now().minusDays(1))) {
            log.warn("Class start date {} is in the past", classEntity.getStartDate());
            // Not a fatal error, just a warning
        }

        return true;
    }

    /**
     * Find the next occurrence of a specific day of week from a given date
     *
     * @param fromDate Starting date
     * @param dayOfWeek Target day of week (1=Monday, 7=Sunday)
     * @return Next date matching the target day of week
     */
    private LocalDate findNextDateForDayOfWeek(LocalDate fromDate, Short dayOfWeek) {
        LocalDate currentDate = fromDate;

        // Convert to Java DayOfWeek (Monday=1, Sunday=7 - matches ISODOW)
        DayOfWeek targetDay = DayOfWeek.of(dayOfWeek);

        // Find next occurrence of target day
        while (currentDate.getDayOfWeek() != targetDay) {
            currentDate = currentDate.plusDays(1);
        }

        return currentDate;
    }
}