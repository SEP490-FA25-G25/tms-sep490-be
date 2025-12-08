package org.fyp.tmssep490be.dtos.teacherrequest;

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
public class TeacherSessionSuggestionDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String className;
    private LocalDate sessionDate;
    private Long timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;
}

