package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AARequestResponseDTO {

    private Long id;
    private String requestType; // ABSENCE, MAKEUP, TRANSFER
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    // Student information
    private StudentSummaryDTO student;

    // Class information
    private ClassSummaryDTO currentClass;
    private ClassSummaryDTO targetClass; // For TRANSFER requests only

    // Session information
    private SessionSummaryDTO targetSession; // For ABSENCE/MAKEUP: the missed session; For TRANSFER: effective session
    private SessionSummaryDTO makeupSession; // For MAKEUP requests only - the session student wants to attend

    // Transfer-specific fields
    private String effectiveDate; // For TRANSFER requests - date when transfer takes effect

    // Request details
    private String requestReason; // Student's reason
    private String note; // AA's decision note

    // Submission details
    private OffsetDateTime submittedAt;
    private UserSummaryDTO submittedBy;

    // Decision details
    private OffsetDateTime decidedAt;
    private UserSummaryDTO decidedBy;

    // Additional information for AA decision making
    private Long daysUntilSession;
    private AttendanceStatsDTO attendanceStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceStatsDTO {
        private Integer totalSessions;
        private Integer presentCount;
        private Integer absentCount;
        private Integer excusedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummaryDTO {
        private Long id;
        private String studentCode;
        private String fullName;
        private String email;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSummaryDTO {
        private Long id;
        private String code;
        private String name;
        private BranchSummaryDTO branch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchSummaryDTO {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummaryDTO {
        private Long id;
        private String date;
        private String dayOfWeek;
        private Integer courseSessionNumber;
        private String courseSessionTitle;
        private TimeSlotSummaryDTO timeSlot;
        private String status; // PLANNED, COMPLETED, CANCELLED
        private TeacherSummaryDTO teacher;
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
    public static class TeacherSummaryDTO {
        private Long id;
        private String fullName;
        private String email;
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
