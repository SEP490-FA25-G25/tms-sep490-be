package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO representing a makeup session option with smart ranking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MakeupOptionDTO {

    private Long sessionId;
    private LocalDate date;
    private Long courseSessionId;
    private String courseSessionTitle;
    private Integer courseSessionNumber;

    // Class info
    private ClassInfo classInfo;

    // Time slot info
    private TimeSlotInfo timeSlotInfo;

    // Match score for ranking
    private MatchScore matchScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassInfo {
        private Long id;
        private String code;
        private String name;
        private Long branchId;
        private String branchName;
        private String modality;
        private Integer availableSlots;
        private Integer maxCapacity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotInfo {
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchScore {
        private Boolean branchMatch;
        private Boolean modalityMatch;
        private Integer totalScore;
        private String priority; // HIGH, MEDIUM, LOW
    }
}
