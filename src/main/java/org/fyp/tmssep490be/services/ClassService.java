package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsResponse;
import org.fyp.tmssep490be.dtos.createclass.CreateClassRequest;
import org.fyp.tmssep490be.dtos.createclass.CreateClassResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
// import org.fyp.tmssep490be.dtos.createclass.SubmitClassResponse; // Removed - now using classmanagement package
// import org.fyp.tmssep490be.dtos.createclass.ValidateClassResponse; // Removed - now using classmanagement package
import org.fyp.tmssep490be.dtos.classmanagement.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for class management operations
 * Provides business logic for Academic Affairs staff to view and manage classes
 */
public interface ClassService {

    /**
     * Get list of classes accessible to academic affairs user
     * Filters by user's branch assignments and applies search/filter criteria
     *
     * @param branchIds List of branch IDs user has access to
     * @param courseId Optional course filter
     * @param status Optional class status filter (null = all statuses)
     * @param approvalStatus Optional approval status filter (null = all approval statuses)
     * @param modality Optional modality filter
     * @param search Optional search term (class code, name, course name, branch name)
     * @param pageable Pagination parameters
     * @param userId Current user ID for access control
     * @return Page of ClassListItemDTO
     */
    Page<ClassListItemDTO> getClasses(
            List<Long> branchIds,
            Long courseId,
            ClassStatus status,
            ApprovalStatus approvalStatus,
            Modality modality,
            String search,
            Pageable pageable,
            Long userId
    );

    /**
     * Get detailed information about a specific class
     * Includes enrollment summary and upcoming sessions
     *
     * @param classId Class ID to retrieve
     * @param userId Current user ID for access control
     * @return ClassDetailDTO with comprehensive class information
     */
    ClassDetailDTO getClassDetail(Long classId, Long userId);

    /**
     * Get list of students currently enrolled in a class
     * Supports search and pagination
     *
     * @param classId Class ID
     * @param search Optional search term (student code, name, email, phone)
     * @param pageable Pagination parameters
     * @param userId Current user ID for access control
     * @return Page of ClassStudentDTO
     */
    Page<ClassStudentDTO> getClassStudents(
            Long classId,
            String search,
            Pageable pageable,
            Long userId
    );

    /**
     * Get quick enrollment summary for a class
     * Lightweight endpoint for capacity checks and list views
     *
     * @param classId Class ID
     * @param userId Current user ID for access control
     * @return ClassEnrollmentSummaryDTO with capacity information
     */
    ClassEnrollmentSummaryDTO getClassEnrollmentSummary(Long classId, Long userId);

    /**
     * Get available students for enrollment in a class
     * Returns students from the same branch who are not yet enrolled
     * Smart sorting based on skill assessment matching:
     * - Priority 1: Students with assessment matching both Subject AND Level
     * - Priority 2: Students with assessment matching Subject only
     * - Priority 3: Students with no assessment or different Subject
     *
     * @param classId Class ID to enroll students into
     * @param search Optional search term (student code, name, email, phone)
     * @param pageable Pagination parameters (supports sorting)
     * @param userId Current user ID for access control
     * @return Page of AvailableStudentDTO with match priority
     */
    Page<AvailableStudentDTO> getAvailableStudentsForClass(
            Long classId,
            String search,
            Pageable pageable,
            Long userId
    );

    // Create Class Workflow methods (STEP 1, 3, 6, 7)

    /**
     * STEP 0 (Optional): Preview class code before creation
     * Generates a preview of what the next class code will be without creating the class
     *
     * @param branchId Branch ID where the class will be created
     * @param courseId Course ID for the class
     * @param startDate Start date of the class (for year extraction)
     * @param userId Current user ID for access control
     * @return PreviewClassCodeResponse with preview code and metadata
     */
    org.fyp.tmssep490be.dtos.createclass.PreviewClassCodeResponse previewClassCode(
            Long branchId, 
            Long courseId, 
            java.time.LocalDate startDate, 
            Long userId
    );

    /**
     * STEP 1: Create new class and auto-generate sessions
     * Creates a new class entity and automatically generates sessions based on course template
     *
     * @param request Create class request with all required information
     * @param userId Current user ID for access control and audit
     * @return CreateClassResponse with class information and session generation summary
     */
    CreateClassResponse createClass(CreateClassRequest request, Long userId);

    /**
     * STEP 2: Get list of generated sessions for review
     * Returns all sessions that were auto-generated for the class
     * Used for "Xem lại buổi học" step in frontend
     *
     * @param classId Class ID to get sessions for
     * @param userId Current user ID for access control
     * @return SessionListResponse with all session details grouped by week
     */
    org.fyp.tmssep490be.dtos.createclass.SessionListResponse listSessions(Long classId, Long userId);

    /**
     * STEP 3: Assign time slots to class sessions
     * Assigns time slots based on day of week patterns
     *
     * @param classId Class ID to assign time slots to
     * @param request Time slot assignment details
     * @param userId Current user ID for access control and audit
     * @return AssignTimeSlotsResponse with assignment results
     */
    AssignTimeSlotsResponse assignTimeSlots(Long classId, AssignTimeSlotsRequest request, Long userId);

    /**
     * STEP 4: Assign resources to class sessions using HYBRID approach
     * <p>
     * Uses HYBRID approach:
     * <ul>
     *   <li>Phase 1: SQL bulk insert for fast assignment (~90% sessions)</li>
     *   <li>Phase 2: Java conflict analysis for detailed error reporting (~10% conflicts)</li>
     * </ul>
     * </p>
     * <p>
     * Performance Target: <200ms for 36 sessions
     * </p>
     *
     * @param classId Class ID to assign resources to
     * @param request Resource assignment pattern (day → resource mapping)
     * @param userId Current user ID for access control and audit
     * @return AssignResourcesResponse with success count and conflict details
     */
    AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request, Long userId);

    /**
     * STEP 6: Validate class completeness before submission
     * Checks all required assignments (timeslot, resource, teacher)
     *
     * @param classId Class ID to validate
     * @param userId Current user ID for access control
     * @return ValidateClassResponse with validation results
     */
    ValidateClassResponse validateClass(Long classId, Long userId);

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.3: TEACHER ASSIGNMENT (PRE-CHECK) ====================

    /**
     * STEP 5A: Get available teachers with PRE-CHECK
     * <p>
     * Executes complex CTE query to show teachers' availability BEFORE assignment
     * Shows detailed conflict breakdown for each teacher
     * </p>
     * <p>
     * <b>PRE-CHECK Approach:</b> User sees availability first, then selects teacher
     * </p>
     * <p>
     * Performance Target: <100ms for PRE-CHECK query
     * </p>
     *
     * @param classId Class ID to check teachers for
     * @param userId Current user ID for access control
     * @return List of TeacherAvailabilityDTO sorted by availability (best matches first)
     */
    List<TeacherAvailabilityDTO> getAvailableTeachers(Long classId, Long userId);

    /**
     * STEP 5B: Assign teacher to class sessions
     * <p>
     * Direct bulk insert without re-checking (PRE-CHECK already done)
     * Supports two modes:
     * <ul>
     *   <li><b>Full Assignment:</b> Assign to ALL sessions (request.sessionIds = null)</li>
     *   <li><b>Partial Assignment:</b> Assign to specific sessions (request.sessionIds provided)</li>
     * </ul>
     * </p>
     * <p>
     * Performance Target: <50ms for bulk insert
     * </p>
     *
     * @param classId Class ID to assign teacher to
     * @param request Assignment request (teacherId + optional sessionIds)
     * @param userId Current user ID for access control and audit
     * @return AssignTeacherResponse with assignment results
     */
    AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest request, Long userId);

    // ==================== APPROVAL WORKFLOW ====================

    /**
     * STEP 7: Submit class for approval
     * Sets submitted_at timestamp and changes status for Center Head review
     *
     * @param classId Class ID to submit
     * @param userId Current user ID for access control and audit
     * @return SubmitClassResponse with submission results
     */
    SubmitClassResponse submitClass(Long classId, Long userId);

    /**
     * STEP 7: Approve submitted class (Center Head only)
     * Changes status to SCHEDULED and sets approval details
     *
     * @param classId Class ID to approve
     * @param approverUserId Center Head user ID approving the class
     */
    void approveClass(Long classId, Long approverUserId);

    /**
     * STEP 7: Reject submitted class (Center Head only)
     * Resets status to DRAFT and stores rejection reason
     *
     * @param classId Class ID to reject
     * @param reason Rejection reason (required)
     * @param rejecterUserId Center Head user ID rejecting the class
     * @return RejectClassResponse with rejection details
     */
    RejectClassResponse rejectClass(Long classId, String reason, Long rejecterUserId);
}
