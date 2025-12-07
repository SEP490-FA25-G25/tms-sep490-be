package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResult {
    private int enrolledCount;
    private int studentsCreated;
    private int sessionsGeneratedPerStudent;
    private int totalStudentSessionsCreated;
    private List<String> warnings;
}
