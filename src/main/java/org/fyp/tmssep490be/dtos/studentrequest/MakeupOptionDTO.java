package org.fyp.tmssep490be.dtos.studentrequest;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakeupOptionDTO {

    private Long sessionId;

    private LocalDate date;

    private String dayOfWeek;

    private Integer subjectSessionNumber;

    private String subjectSessionTitle;

    private Long subjectSessionId;

    private ClassInfo classInfo;

    private TimeSlotInfo timeSlotInfo;

    private Integer availableSlots;

    private Integer maxCapacity;

    private String teacher;

    private List<String> warnings;

    private Boolean conflict;

    private MatchScore matchScore;

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

        private Integer availableSlots;

        private Integer maxCapacity;

        private String resourceName;

        private String resourceType;

        private String onlineLink;

        private String teacherName;

        private Integer sequenceNo;

        private Integer totalSessions;

        private Integer phaseNumber;

        private String phaseName;

        private java.util.List<String> skills;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeSlotInfo {
        private String startTime;

        private String endTime;

        private Long slotId;

        private String slotName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchScore {
        private Boolean modalityMatch;

        private Boolean capacityOk;

        private Integer dateProximityScore;

        private Integer totalScore;

        private String priority;
    }
}
