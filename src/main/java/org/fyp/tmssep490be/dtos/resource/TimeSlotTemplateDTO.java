package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlotTemplateDTO {
    private Long id;
    private String name;
    private String startTime;
    private String endTime;
    private String displayName;
}