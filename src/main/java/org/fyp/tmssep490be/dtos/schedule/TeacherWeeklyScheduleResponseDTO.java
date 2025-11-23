package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Weekly schedule response containing all sessions for a teacher")
public class TeacherWeeklyScheduleResponseDTO {

    @Schema(description = "Monday of the week", example = "2025-11-04")
    private LocalDate weekStart;

    @Schema(description = "Sunday of the week", example = "2025-11-10")
    private LocalDate weekEnd;

    @Schema(description = "Teacher ID", example = "50")
    private Long teacherId;

    @Schema(description = "Teacher full name", example = "Nguyen Van A")
    private String teacherName;

    @Schema(description = "List of time slots used in this week (for Y-axis)")
    private List<TimeSlotDTO> timeSlots;

    @Schema(description = "Map of day of week to list of sessions")
    private Map<DayOfWeek, List<TeacherSessionSummaryDTO>> schedule;
}

