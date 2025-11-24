package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.course.CourseDetailDTO;
import org.fyp.tmssep490be.dtos.course.CreateCourseRequestDTO;
import java.util.List;

/**
 * Service interface for course operations
 */
public interface CourseService {

    /**
     * Get all approved courses for dropdown selection
     * 
     * @return List of CourseDTO containing id, name, and code
     */
    List<CourseDTO> getAllCourses(Long subjectId, Long levelId);

    CourseDTO createCourse(CreateCourseRequestDTO request);

    CourseDetailDTO getCourseDetails(Long id);

    void updateCourse(Long id, CreateCourseRequestDTO request);
}
