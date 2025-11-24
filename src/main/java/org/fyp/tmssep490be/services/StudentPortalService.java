package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentportal.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for Student Portal feature
 * Provides data access methods for "My Classes" functionality
 */
public interface StudentPortalService {

    /**
     * Get classes enrolled by a student with filtering and pagination
     *
     * @param studentId Student ID
     * @param statusFilters Optional status filters
     * @param branchFilters Optional branch filters
     * @param courseFilters Optional course filters
     * @param modalityFilters Optional modality filters
     * @param pageable Pagination information
     * @return Paginated list of student classes
     */
    Page<StudentClassDTO> getStudentClasses(
            Long studentId,
            List<String> statusFilters,
            List<Long> branchFilters,
            List<Long> courseFilters,
            List<String> modalityFilters,
            Pageable pageable
    );

    /**
     * Get detailed information about a specific class
     *
     * @param classId Class ID
     * @return Detailed class information
     */
    ClassDetailDTO getClassDetail(Long classId);

    /**
     * Get sessions for a class including student attendance data
     *
     * @param classId Class ID
     * @param studentId Student ID for attendance information
     * @return Class sessions with student attendance
     */
    ClassSessionsResponseDTO getClassSessions(Long classId, Long studentId);

    /**
     * Get assessments for a class
     *
     * @param classId Class ID
     * @return List of assessments
     */
    List<AssessmentDTO> getClassAssessments(Long classId);

    /**
     * Get assessment scores for a specific student in a class
     *
     * @param classId Class ID
     * @param studentId Student ID
     * @return List of student assessment scores
     */
    List<StudentAssessmentScoreDTO> getStudentAssessmentScores(Long classId, Long studentId);

    /**
     * Get classmates for a class
     *
     * @param classId Class ID
     * @return List of classmates
     */
    List<ClassmateDTO> getClassmates(Long classId);

    /**
     * Get student transcript with all classes, scores, and progress
     *
     * @param studentId Student ID
     * @return List of transcript entries for all student's enrollments
     */
    List<StudentTranscriptDTO> getStudentTranscript(Long studentId);
}