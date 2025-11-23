package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete session details for teacher modal view")
public class TeacherSessionDetailDTO {

    @Schema(description = "Session ID", example = "1001")
    private Long sessionId;

    @Schema(description = "Session date", example = "2025-11-04")
    private LocalDate date;

    @Schema(description = "Day of week", example = "MONDAY")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Session start time", example = "08:00:00")
    private LocalTime startTime;

    @Schema(description = "Session end time", example = "10:00:00")
    private LocalTime endTime;

    @Schema(description = "Time slot name", example = "HN Morning 1")
    private String timeSlotName;

    @Schema(description = "Class information")
    private ClassInfoDTO classInfo;

    @Schema(description = "Session content information")
    private SessionInfoDTO sessionInfo;

    @Schema(description = "List of learning materials for this session")
    private List<MaterialDTO> materials;

    @Schema(description = "Classroom resource information (room, zoom link)")
    private ResourceDTO classroomResource;

    @Schema(description = "Makeup session information if applicable")
    private MakeupInfoDTO makeupInfo;

    @Schema(description = "Attendance summary for the session")
    private AttendanceSummaryDTO attendanceSummary;
}

