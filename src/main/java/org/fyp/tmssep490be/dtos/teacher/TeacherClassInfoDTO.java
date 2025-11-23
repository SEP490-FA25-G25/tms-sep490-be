package org.fyp.tmssep490be.dtos.teacher;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassInfoDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String courseName;
    private String branchName;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private String status;
    private OffsetDateTime assignedAt;
}

