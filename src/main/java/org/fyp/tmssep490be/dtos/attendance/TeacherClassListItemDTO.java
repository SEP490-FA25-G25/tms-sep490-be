package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherClassListItemDTO {
    private Long id;
    private String code;
    private String name;
    private String subjectName;
    private String subjectCode;
    private String branchName;
    private String branchCode;
    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private String status;
    private Integer totalSessions;
    private Double attendanceRate;
}

