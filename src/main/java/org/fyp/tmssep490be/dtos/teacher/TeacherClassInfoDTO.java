package org.fyp.tmssep490be.dtos.teacher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// DTO cho thông tin lớp học của giáo viên trong profile
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassInfoDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private String branchName;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private String status;
    private OffsetDateTime assignedAt;
}

