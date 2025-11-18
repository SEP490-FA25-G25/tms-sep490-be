package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentHistoryDTO {
    private Long id;
    private Long studentId;
    private String studentCode;
    private String studentName;

    // Enrollment Information
    private Long classId;
    private String classCode;
    private String className;
    private String courseName;
    private String branchName;
    private String status;
    private OffsetDateTime enrolledAt;
    private OffsetDateTime leftAt;
    private String enrolledByName;

    // Session Boundary Information (for tracking transfer/enrollment periods)
    private Long joinSessionId;
    private LocalDate joinSessionDate;
    private Long leftSessionId;
    private LocalDate leftSessionDate;

    // Class Period Information
    private LocalDate classStartDate;
    private LocalDate classEndDate;
    private String modality;

    // Progress Information
    private Integer totalSessions;
    private Integer attendedSessions;
    private Double attendanceRate;
    private Double averageScore;
}