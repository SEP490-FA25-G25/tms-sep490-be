package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTimeSlotsResponse {

    private boolean success;
    private String message;
    private Long classId;
    private String classCode;
    private int totalSessions;
    private int sessionsUpdated;
    private OffsetDateTime updatedAt;
    private List<AssignmentDetail> assignmentDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentDetail {
        private Short dayOfWeek;
        private String dayName;
        private Long timeSlotTemplateId;
        private String timeSlotName;
        private String startTime;
        private String endTime;
        private int sessionsAffected;
        private boolean successful;
        private String errorMessage;
    }
}
