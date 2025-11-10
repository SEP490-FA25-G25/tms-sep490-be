package org.fyp.tmssep490be.dtos.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for TimeSlotTemplate response
 * Used in STEP 3: Assign Time Slots to display available time slots in dropdown
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotTemplateDTO {
    private Long id;
    private String name;
    private String startTime;  // HH:mm:ss format
    private String endTime;    // HH:mm:ss format
    private String displayName; // "08:00 - 10:00" for easy display
}
