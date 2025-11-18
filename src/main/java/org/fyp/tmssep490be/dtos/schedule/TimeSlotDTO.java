package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Time slot information for schedule grid rows")
public class TimeSlotDTO {

    @Schema(description = "Time slot template ID", example = "1")
    private Long timeSlotTemplateId;

    @Schema(description = "Name of the time slot", example = "HN Morning 1")
    private String name;

    @Schema(description = "Start time of the slot", example = "08:00:00")
    private LocalTime startTime;

    @Schema(description = "End time of the slot", example = "10:00:00")
    private LocalTime endTime;
}