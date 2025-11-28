package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.common.CourseDTO;
import org.fyp.tmssep490be.dtos.course.*;

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

    CourseDTO createCourse(CreateCourseRequestDTO request, Long userId);

    CourseDetailDTO getCourseDetails(Long id);

    CourseDetailDTO updateCourse(Long id, CreateCourseRequestDTO request, Long userId);

    /**
     * Submit a course for approval
     * 
     * @param id Course ID
     */
    void submitCourse(Long id);

    void approveCourse(Long id);

    void rejectCourse(Long id, String reason);

    void deactivateCourse(Long id);

    void reactivateCourse(Long id);

    // Student/View methods
    List<StudentCourseDTO> getStudentCourses(Long studentId);

    List<StudentCourseDTO> getStudentCoursesByUserId(Long userId);

    CourseDetailDTO getCourseDetail(Long courseId);

    CourseDetailDTO getCourseSyllabus(Long courseId);

    MaterialHierarchyDTO getCourseMaterials(Long courseId, Long studentId);

    List<CoursePLODTO> getCoursePLOs(Long courseId);

    List<CourseCLODTO> getCourseCLOs(Long courseId);
}
