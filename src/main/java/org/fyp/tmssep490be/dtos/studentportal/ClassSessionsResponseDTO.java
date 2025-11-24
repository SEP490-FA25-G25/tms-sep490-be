package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for class sessions response combining upcoming and past sessions with student attendance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionsResponseDTO {
    private List<SessionDTO> upcomingSessions;
    private List<SessionDTO> pastSessions;
    private List<StudentSessionDTO> studentSessions;
}