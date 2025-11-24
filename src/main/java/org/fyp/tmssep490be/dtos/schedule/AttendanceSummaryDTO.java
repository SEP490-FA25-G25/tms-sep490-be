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

    @Schema(description = "Total number of students", example = "25")
    private Integer totalStudents;

    @Schema(description = "Number of students present", example = "22")
    private Integer presentCount;

    @Schema(description = "Number of students absent", example = "3")
    private Integer absentCount;

    @Schema(description = "Number of students late", example = "0")
    private Integer lateCount;

    @Schema(description = "Number of students excused", example = "0")
    private Integer excusedCount;

    @Schema(description = "Whether attendance has been submitted", example = "true")
    private Boolean attendanceSubmitted;
}

