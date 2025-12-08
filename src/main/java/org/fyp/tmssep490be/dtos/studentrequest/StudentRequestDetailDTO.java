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
public class StudentRequestDetailDTO {

    private Long id;
    private String requestType; // ABSENCE, MAKEUP, TRANSFER
    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    // Student information
    private StudentSummaryDTO student;

    // Class information
    private ClassDetailDTO currentClass;
    private ClassDetailDTO targetClass; // For TRANSFER requests only

    // Session information
    private SessionDetailDTO targetSession; // For ABSENCE/MAKEUP: the missed session; For TRANSFER: effective session
    private SessionDetailDTO makeupSession; // For MAKEUP requests only - the session student wants to attend

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
    private String decisionNote; // AA's decision note (approve/reject reason)

    // Additional information (for AA staff)
    private AdditionalInfoDTO additionalInfo;

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
    public static class ClassDetailDTO {
        private Long id;
        private String code;
        private String name;
        private BranchSummaryDTO branch;
        private TeacherSummaryDTO teacher;
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
    public static class TeacherSummaryDTO {
        private Long id;
        private String fullName;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionDetailDTO {
        private Long id;
        private String date;
        private String dayOfWeek;
        private Integer courseSessionNumber;
        private String courseSessionTitle;
        private TimeSlotDTO timeSlot;
        private String status; // PLANNED, COMPLETED, CANCELLED
        private TeacherSummaryDTO teacher;
        private Long enrolledCount; // Current enrolled students
        private Integer maxCapacity; // Maximum capacity of the class
        private ClassInfoDTO classInfo; // Class information for this session
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfoDTO {
        private Long classId;
        private String classCode;
        private String branchName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotDTO {
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalInfoDTO {
        private Long daysUntilSession;
        private StudentAbsenceStatsDTO studentAbsenceStats;
        private PreviousRequestsDTO previousRequests;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAbsenceStatsDTO {
        private Integer totalAbsences;
        private Integer totalSessions;
        private Double absenceRate;
        private Integer excusedAbsences;
        private Integer unexcusedAbsences;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreviousRequestsDTO {
        private Integer totalRequests;
        private Integer approvedRequests;
        private Integer rejectedRequests;
        private Integer cancelledRequests;
    }
}
