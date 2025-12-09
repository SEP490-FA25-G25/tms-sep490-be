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
@Schema(description = "Detailed session information for teacher view")
public class TeacherSessionDetailDTO {

    @Schema(description = "Session ID")
    private Long sessionId;

    @Schema(description = "Session date")
    private LocalDate date;

    @Schema(description = "Day of week")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Start time")
    private LocalTime startTime;

    @Schema(description = "End time")
    private LocalTime endTime;

    @Schema(description = "Time slot name")
    private String timeSlotName;

    @Schema(description = "Class information")
    private ClassInfoDTO classInfo;

    @Schema(description = "Session information")
    private SessionInfoDTO sessionInfo;

    @Schema(description = "List of materials")
    private List<MaterialDTO> materials;

    @Schema(description = "Classroom resource")
    private ResourceDTO classroomResource;

    @Schema(description = "Makeup information")
    private MakeupInfoDTO makeupInfo;

    @Schema(description = "Attendance summary")
    private AttendanceSummaryDTO attendanceSummary;
}

