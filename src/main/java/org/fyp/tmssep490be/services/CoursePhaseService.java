package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.course.CoursePhaseDTO;
import org.fyp.tmssep490be.entities.CoursePhase;

import java.util.List;

public interface CoursePhaseService {
    /**
     * Get all phases for a specific course
     */
    List<CoursePhaseDTO> getPhasesByCourseId(Long courseId);

    /**
     * Get all phases across all courses (for QA reports)
     */
    List<CoursePhaseDTO> getAllPhases();
}
