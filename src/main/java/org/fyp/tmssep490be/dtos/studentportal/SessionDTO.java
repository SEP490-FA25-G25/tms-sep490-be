package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for session information in student portal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDTO {
    private Long id;
    private Long classId;
    private String date;
    private String type;
    private String status;
    private String room;
    private String teacherNote;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<String> teachers;
    private OffsetDateTime recordedAt;
}