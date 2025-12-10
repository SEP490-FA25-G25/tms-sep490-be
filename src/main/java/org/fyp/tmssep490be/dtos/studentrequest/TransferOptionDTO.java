package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOptionDTO {

    private Long classId;
    private String classCode;
    private String className;
    private Long subjectId;
    private String subjectName;
    private Long branchId;
    private String branchName;
    private String branchAddress;    
    private String modality;
    private String scheduleDays;
    private String scheduleTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer currentSession;
    private Integer maxCapacity;
    private Integer enrolledCount;
    private Integer availableSlots;
    private String classStatus;
    private Boolean canTransfer;
    private String progressNote; // Simple progress comparison, e.g., "Chênh 3 buổi"
    
    private Changes changes;
    private List<UpcomingSession> upcomingSessions;
    private List<SessionInfo> allSessions; // All sessions for timeline view

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Changes {
        private String branch;
        private String modality;
        private String schedule;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingSession {
        private Long sessionId;
        private LocalDate date;
        private Integer subjectSessionNumber;
        private String subjectSessionTitle;
        private String timeSlot;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private Long sessionId;
        private LocalDate date;
        private Integer subjectSessionNumber;
        private String subjectSessionTitle;
        private String timeSlot;
        private String status; 
        private Boolean isPast;
        private Boolean isUpcoming; 
    }
}
