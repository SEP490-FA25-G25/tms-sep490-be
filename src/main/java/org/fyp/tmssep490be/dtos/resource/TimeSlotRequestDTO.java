package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlotRequestDTO {
    private Long branchId;
    private String name;
    private String startTime;
    private String endTime;
}