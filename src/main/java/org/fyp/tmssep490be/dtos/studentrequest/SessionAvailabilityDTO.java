package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for available sessions for a specific date (used in Step 2 of absence request flow)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionAvailabilityDTO {

    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseName;
    private Long branchId;
    private String branchName;
    private String modality; // OFFLINE, ONLINE
    private Integer sessionCount;
    private List<SessionDTO> sessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionDTO {
        private Long sessionId;
        private String date;
        private Integer courseSessionNumber;
        private String courseSessionTitle;
        private TimeSlotDTO timeSlot;
        private String status; // PLANNED, COMPLETED, CANCELLED
        private String type; // CLASS, EXAM, BREAK
        private TeacherDTO teacher;
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
    public static class TeacherDTO {
        private Long id;
        private String fullName;
    }
}