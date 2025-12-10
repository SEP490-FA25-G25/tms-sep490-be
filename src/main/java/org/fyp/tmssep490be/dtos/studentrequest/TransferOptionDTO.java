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
    private ContentGapAnalysis contentGapAnalysis;
    private Changes changes;
    private List<UpcomingSession> upcomingSessions;
    private List<SessionInfo> allSessions; // All sessions for timeline view

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentGapAnalysis {
        private String gapLevel; // NONE, MINOR, MODERATE, MAJOR
        private Integer missedSessions;
        private Integer totalSessions;
        private List<ContentGapSession> gapSessions;
        private List<String> recommendedActions;
        private String impactDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentGapSession {
        private Integer courseSessionNumber;
        private String courseSessionTitle;
        private String scheduledDate;
    }

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
        private String status; // PLANNED, DONE, CANCELLED
        private Boolean isPast;
        private Boolean isUpcoming; // Next session to attend
    }
}
