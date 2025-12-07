package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlotResponseDTO {
    private Long id;
    private Long branchId;
    private String branchName;
    private String name;
    private String startTime;
    private String endTime;
    private String createdAt;
    private String updatedAt;
    private String status;
    private Long activeClassesCount;
    private Long totalSessionsCount;
    private Boolean hasAnySessions;
    private Boolean hasFutureSessions;
    private Boolean hasTeacherAvailability;
}