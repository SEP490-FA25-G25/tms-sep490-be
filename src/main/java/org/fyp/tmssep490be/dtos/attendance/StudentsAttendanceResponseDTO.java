package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentsAttendanceResponseDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String subjectCode;
    private String subjectName;
    private LocalDate date;
    private String timeSlotName;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String sessionTopic;
    private AttendanceSummaryDTO summary;
    private List<StudentAttendanceDTO> students;
    private Boolean hasHomework;
}

