package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary session information for weekly schedule view")
public class SessionSummaryDTO {

    @Schema(description = "Session ID", example = "1001")
    private Long sessionId;

    @Schema(description = "Student session ID", example = "5001")
    private Long studentSessionId;

    @Schema(description = "Session date", example = "2025-11-04")
    private LocalDate date;

    @Schema(description = "Day of week", example = "MONDAY")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Time slot template ID", example = "1")
    private Long timeSlotTemplateId;

    @Schema(description = "Session start time", example = "08:00:00")
    private LocalTime startTime;

    @Schema(description = "Session end time", example = "10:00:00")
    private LocalTime endTime;

    @Schema(description = "Class code", example = "HN-FOUND-O1")
    private String classCode;

    @Schema(description = "Class name", example = "IELTS Foundation - Oct 2025")
    private String className;

    @Schema(description = "Course ID", example = "1")
    private Long courseId;

    @Schema(description = "Course name", example = "IELTS Foundation")
    private String courseName;

    @Schema(description = "Session topic", example = "Introduction to IELTS")
    private String topic;

    @Schema(description = "Session type", example = "CLASS")
    private SessionType sessionType;

    @Schema(description = "Session status", example = "PLANNED")
    private SessionStatus sessionStatus;

    @Schema(description = "Class modality", example = "OFFLINE")
    private Modality modality;

    @Schema(description = "Location (room or online link)", example = "Room 301")
    private String location;

    @Schema(description = "Branch name", example = "Hanoi Branch")
    private String branchName;

    @Schema(description = "Student attendance status", example = "PLANNED")
    private AttendanceStatus attendanceStatus;

    @Schema(description = "Whether this is a makeup session", example = "false")
    private Boolean isMakeup;

    @Schema(description = "Makeup session information if applicable")
    private MakeupInfoDTO makeupInfo;
}