package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Attendance summary for a session")
public class AttendanceSummaryDTO {

    @Schema(description = "Total number of students", example = "20")
    private Long totalStudents;

    @Schema(description = "Number of present students", example = "18")
    private Long presentCount;

    @Schema(description = "Number of absent students", example = "2")
    private Long absentCount;

    @Schema(description = "Number of late students", example = "0")
    private Long lateCount;

    @Schema(description = "Number of excused students", example = "0")
    private Long excusedCount;

    @Schema(description = "Whether attendance has been submitted", example = "true")
    private Boolean attendanceSubmitted;
}

