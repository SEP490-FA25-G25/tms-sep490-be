package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResourcesResponse {

    private Long classId;
    private Integer totalSessions;
    private Integer successCount;
    private Integer conflictCount;
    @Builder.Default
    private List<ResourceConflictDetail> conflicts = List.of();
    private Long processingTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceConflictDetail {
        private Long sessionId;
        private Integer sessionNumber;
        private LocalDate date;
        private Short dayOfWeek;
        private Long timeSlotTemplateId;
        private String timeSlotName;
        private LocalTime timeSlotStart;
        private LocalTime timeSlotEnd;
        private Long requestedResourceId;
        private String requestedResourceName;
        private ConflictType conflictType;
        private String conflictReason;
        private Long conflictingClassId;
        private String conflictingClassName;
        @Builder.Default
        private List<AvailableResourceDTO> suggestions = List.of();
    }

    public enum ConflictType {
        CLASS_BOOKING,
        MAINTENANCE,
        INSUFFICIENT_CAPACITY,
        UNAVAILABLE,
        RESOURCE_NOT_FOUND
    }
}
