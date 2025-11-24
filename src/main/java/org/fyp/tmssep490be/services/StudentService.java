package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    /**
     * Create a new student with auto-generated student code
     *
     * @param request Student creation request
     * @param currentUserId ID of the user creating the student (Academic Affair)
     * @return Created student information including temporary password if generated
     */
    CreateStudentResponse createStudent(CreateStudentRequest request, Long currentUserId);

    /**
     * Get students in accessible branches with filters
     */
    Page<StudentListItemDTO> getStudents(
            List<Long> branchIds,
            String search,
            UserStatus status,
            Long courseId,
            Pageable pageable,
            Long userId
    );

    /**
     * Get detailed information about a specific student
     */
    StudentDetailDTO getStudentDetail(Long studentId, Long userId);

    /**
     * Get student enrollment history
     */
    Page<StudentEnrollmentHistoryDTO> getStudentEnrollmentHistory(
            Long studentId,
            List<Long> branchIds,
            Pageable pageable,
            Long userId
    );

  /**
     * Get current student's own profile information
     */
    StudentProfileDTO getMyProfile(Long userAccountId);
}
