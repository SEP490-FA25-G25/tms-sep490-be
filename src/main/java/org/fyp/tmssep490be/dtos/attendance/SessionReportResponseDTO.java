package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionReportResponseDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String className;
    private String subjectCode;
    private String subjectName;
    private LocalDate date;
    private String timeSlotName;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String sessionTopic;
    private String teacherName;
    private String teacherNote;
    private AttendanceSummaryDTO summary;
}

