package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing a missed (absent) session that can be made up
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissedSessionDTO {

    private Long sessionId;
    private LocalDate date;
    private Integer daysAgo;
    private Integer courseSessionNumber;
    private String courseSessionTitle;
    private Long courseSessionId;

    // Nested class info
    private ClassInfo classInfo;

    // Nested time slot info
    private TimeSlotInfo timeSlotInfo;

    private String attendanceStatus;
    private Long absenceRequestId;
    private String absenceRequestStatus;
    private Boolean hasExistingMakeupRequest;
    private Boolean isExcusedAbsence;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotInfo {
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
