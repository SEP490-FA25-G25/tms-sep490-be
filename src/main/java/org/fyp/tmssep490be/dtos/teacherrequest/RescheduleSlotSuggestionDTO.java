package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleSlotSuggestionDTO {
    private Long timeSlotId;
    private String label;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean hasAvailableResource;
    private Integer availableResourceCount;
}






