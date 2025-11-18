package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.teacherrequest.*;
import org.fyp.tmssep490be.entities.enums.RequestStatus;

import java.util.List;

public interface TeacherRequestService {
    
    /**
     * Create a new teacher request
     * @param createDTO Request data
     * @param userId Current authenticated user ID
     * @return Created teacher request
     */
    TeacherRequestResponseDTO createRequest(TeacherRequestCreateDTO createDTO, Long userId);
    
    /**
     * Get all requests for the current teacher
     * @param userId Current authenticated user ID
     * @return List of teacher requests
     */
    List<TeacherRequestListDTO> getMyRequests(Long userId);

    /**
     * Get pending teacher requests for Academic Affairs staff review
     * @return Pending requests newest first
     */
    List<TeacherRequestListDTO> getPendingRequestsForStaff();

    /**
     * Get teacher requests for Academic Affairs staff with optional status filter
     * @param status Filter by request status (null = all)
     * @return Requests newest first
     */
    List<TeacherRequestListDTO> getRequestsForStaff(RequestStatus status);
    
    /**
     * Get teacher request by ID (for detail view)
     * @param requestId Request ID
     * @param userId Current authenticated user ID (for authorization check)
     * @return Teacher request details
     */
    TeacherRequestResponseDTO getRequestById(Long requestId, Long userId);
    
    /**
     * Approve a teacher request (Staff only)
     * For MODALITY_CHANGE: updates session_resource and class.modality
     * @param requestId Request ID
     * @param approveDTO Approval data (can override Teacher's choices)
     * @param userId Current authenticated staff user ID
     * @return Approved teacher request
     */
    TeacherRequestResponseDTO approveRequest(Long requestId, TeacherRequestApproveDTO approveDTO, Long userId);
    
    /**
     * Reject a teacher request (Staff only)
     * @param requestId Request ID
     * @param reason Rejection reason
     * @param userId Current authenticated staff user ID
     * @return Rejected teacher request
     */
    TeacherRequestResponseDTO rejectRequest(Long requestId, String reason, Long userId);

    /**
     * Get teacher request detail for Academic Affairs staff
     * @param requestId Request ID
     * @return Teacher request details
     */
    TeacherRequestResponseDTO getRequestForStaff(Long requestId);

    /**
     * Suggest valid time slots for rescheduling a session on a given date
     */
    List<RescheduleSlotSuggestionDTO> suggestSlots(Long sessionId, java.time.LocalDate date, Long userId);

    /**
     * Suggest valid resources for rescheduling with given date and time slot
     */
    List<RescheduleResourceSuggestionDTO> suggestResources(Long sessionId, java.time.LocalDate date, Long timeSlotId, Long userId);

    /**
     * Suggest compatible resources for modality change on the current session schedule
     */
    List<ModalityResourceSuggestionDTO> suggestModalityResources(Long sessionId, Long userId);

    /**
     * Suggest valid time slots for rescheduling a request (Staff only)
     */
    List<RescheduleSlotSuggestionDTO> suggestSlotsForStaff(Long requestId, java.time.LocalDate date);

    /**
     * Suggest valid resources for rescheduling a request (Staff only)
     */
    List<RescheduleResourceSuggestionDTO> suggestResourcesForStaff(Long requestId, java.time.LocalDate date, Long timeSlotId);

    /**
     * Suggest compatible resources for modality change request (Staff only)
     */
    List<ModalityResourceSuggestionDTO> suggestModalityResourcesForStaff(Long requestId);

    /**
     * Get teacher's future sessions (7 days from today or specific date)
     * @param userId Current authenticated user ID
     * @param date Optional date filter (if null, returns next 7 days)
     * @return List of teacher sessions
     */
    List<TeacherSessionDTO> getMySessions(Long userId, java.time.LocalDate date);

    /**
     * Suggest swap candidate teachers for a session
     * @param sessionId Session ID to find replacement for
     * @param userId Current authenticated user ID
     * @return List of candidate teachers sorted by priority
     */
    List<SwapCandidateDTO> suggestSwapCandidates(Long sessionId, Long userId);

    /**
     * Suggest swap candidate teachers for a request (Staff only)
     * @param requestId Request ID to find replacement for
     * @return List of candidate teachers sorted by priority
     */
    List<SwapCandidateDTO> suggestSwapCandidatesForStaff(Long requestId);

    /**
     * Confirm swap request (Replacement Teacher only)
     * @param requestId Request ID
     * @param userId Current authenticated replacement teacher user ID
     * @return Confirmed teacher request
     */
    TeacherRequestResponseDTO confirmSwap(Long requestId, Long userId);

    /**
     * Decline swap request (Replacement Teacher only)
     * @param requestId Request ID
     * @param reason Decline reason
     * @param userId Current authenticated replacement teacher user ID
     * @return Declined teacher request
     */
    TeacherRequestResponseDTO declineSwap(Long requestId, String reason, Long userId);
}
