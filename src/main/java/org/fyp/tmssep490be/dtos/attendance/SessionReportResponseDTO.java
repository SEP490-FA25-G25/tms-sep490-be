package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class SessionReportResponseDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String className;
    private String courseCode;
    private String courseName;
    private LocalDate date;
    private String timeSlotName;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String sessionTopic;
    private String teacherName;
    private String teacherNote;
    private AttendanceSummaryDTO summary;
}


