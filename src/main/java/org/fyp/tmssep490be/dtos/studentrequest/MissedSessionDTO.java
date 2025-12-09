package org.fyp.tmssep490be.dtos.studentrequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Missed session details for makeup request")
public class MissedSessionDTO {

    private Long sessionId;

    private LocalDate date;

    private Integer daysAgo;

    private Integer subjectSessionNumber;

    private String subjectSessionTitle;

    private Long subjectSessionId;

    private ClassInfo classInfo;
    private TimeSlotInfo timeSlotInfo;

    private String attendanceStatus;

    @JsonProperty("hasExistingMakeupRequest")
    private Boolean hasExistingMakeupRequest;

    @JsonProperty("isExcusedAbsence")
    private Boolean isExcusedAbsence;

    private Long absenceRequestId;

    private String absenceRequestStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClassInfo {
        private Long classId;

        private String classCode;

        private String className;

        private Long branchId;

        private String branchName;

        private String branchAddress;

        private String modality;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSlotInfo {
        private String startTime;

        private String endTime;
    }
}
