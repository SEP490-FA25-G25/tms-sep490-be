package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.classmanagement.RejectClassRequest;
import org.fyp.tmssep490be.dtos.classmanagement.RejectClassResponse;
import org.fyp.tmssep490be.dtos.classmanagement.SubmitClassResponse;

/**
 * Service interface for class approval functionality.
 * Provides comprehensive approval workflow management for classes.
 */
public interface ApprovalService {

    /**
     * Submits a class for approval after validation.
     * Validates that the class is complete and ready for submission,
     * then updates the submitted_at timestamp and approval status.
     *
     * @param classId the ID of the class to submit
     * @param submitterUserId the ID of the user submitting the class
     * @return SubmitClassResponse containing submission details and status
     */
    SubmitClassResponse submitForApproval(Long classId, Long submitterUserId);

    /**
     * Approves a submitted class.
     * Validates that the class is submitted and the approver has proper authority,
     * then updates the class status to SCHEDULED and sets approval details.
     *
     * @param classId the ID of the class to approve
     * @param approverUserId the ID of the user approving the class
     */
    void approveClass(Long classId, Long approverUserId);

    /**
     * Rejects a submitted class with a reason.
     * Validates that the class is submitted and the approver has proper authority,
     * then resets the class to DRAFT status and records rejection details.
     *
     * @param classId the ID of the class to reject
     * @param reason the rejection reason
     * @param rejecterUserId the ID of the user rejecting the class
     * @return RejectClassResponse containing rejection details and status
     */
    RejectClassResponse rejectClass(Long classId, String reason, Long rejecterUserId);
}