package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Student-specific status for a session")
public class StudentStatusDTO {

    @Schema(description = "Student attendance status", example = "PLANNED")
    private AttendanceStatus attendanceStatus;

    @Schema(description = "Homework submission status", example = "NOT_SUBMITTED")
    private HomeworkStatus homeworkStatus;

    @Schema(description = "Homework due date", example = "2025-11-06")
    private LocalDate homeworkDueDate;

    @Schema(description = "Homework description", example = "Complete Workbook pages 10-15")
    private String homeworkDescription;
}