package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for student's active classes
 * Used in GET /api/v1/students/{studentId}/classes endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassDTO {

    private Long classId;
    private String classCode;
    private String className;

    // Course information
    private Long courseId;
    private String courseName;
    private String courseCode;

    // Branch information
    private Long branchId;
    private String branchName;

    // Class details
    private String modality; // ONLINE, OFFLINE, HYBRID
    private String status; // ONGOING, SCHEDULED, etc.
    private LocalDate startDate;
    private LocalDate endDate;

    // Schedule information
    private String scheduleSummary; // e.g., "Mon, Wed, Fri | 09:00-11:00"

    // Enrollment information
    private Long enrollmentId;
    private LocalDate enrollmentDate;
    private String enrollmentStatus; // ACTIVE, COMPLETED, etc.

    // Progress information
    private Integer totalSessions;
    private Integer completedSessions;
    private Integer attendedSessions;
    private Double attendanceRate;

    // Teacher information (all instructors with SCHEDULED status)
    private List<String> instructorNames;
}
