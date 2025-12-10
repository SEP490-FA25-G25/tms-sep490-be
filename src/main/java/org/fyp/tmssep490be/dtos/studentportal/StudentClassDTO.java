package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentClassDTO {

    private Long classId;
    private String classCode;
    private String className;

    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private Long branchId;
    private String branchName;

    private String modality;
    private String status;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;

    private String scheduleSummary; 

    private Long enrollmentId;
    private OffsetDateTime enrollmentDate;
    private String enrollmentStatus; 

    private Integer totalSessions;
    private Integer completedSessions;

    private List<String> instructorNames;
}

