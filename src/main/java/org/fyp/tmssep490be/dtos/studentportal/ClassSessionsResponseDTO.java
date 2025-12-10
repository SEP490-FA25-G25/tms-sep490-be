package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionsResponseDTO {
    private List<SessionDTO> upcomingSessions;
    private List<SessionDTO> pastSessions;
    private List<StudentSessionDTO> studentSessions;
    
    private String enrollmentStatus;  // ENROLLED, TRANSFERRED, COMPLETED
    private LocalDate leftAt;  // Null if not transferred
}
