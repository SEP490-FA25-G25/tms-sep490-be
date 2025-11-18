package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for student request response (student's own requests)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentRequestResponseDTO {

    private Long id;
    private String requestType; // ABSENCE, MAKEUP, TRANSFER
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    // Class information
    private ClassSummaryDTO currentClass;
    private ClassSummaryDTO targetClass; // For TRANSFER requests only

    // Session information
    private SessionSummaryDTO targetSession; // For ABSENCE/MAKEUP: the missed session; For TRANSFER: effective session
    private SessionSummaryDTO makeupSession; // For MAKEUP requests only - the session student wants to attend

    // Transfer-specific fields
    private String effectiveDate; // For TRANSFER requests - date when transfer takes effect

    // Request details
    private String requestReason;
    private String note;

    // Submission details
    private OffsetDateTime submittedAt;
    private UserSummaryDTO submittedBy;

    // Decision details
    private OffsetDateTime decidedAt;
    private UserSummaryDTO decidedBy;
    private String rejectionReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSummaryDTO {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummaryDTO {
        private Long id;
        private String date;
        private Integer courseSessionNumber;
        private String courseSessionTitle;
        private TimeSlotSummaryDTO timeSlot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotSummaryDTO {
        private String startTime;
        private String endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummaryDTO {
        private Long id;
        private String fullName;
        private String email;
    }
}