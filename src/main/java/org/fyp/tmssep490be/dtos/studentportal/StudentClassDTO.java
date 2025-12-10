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
    private String branchAddress;

    private String modality;
    private String status;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;

    private String scheduleSummary;
    private List<ScheduleDetailDTO> scheduleDetails;

    private Long enrollmentId;
    private OffsetDateTime enrollmentDate;
    private String enrollmentStatus; 

    private Integer totalSessions;
    private Integer completedSessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDetailDTO {
        private String day;        
        private String startTime;  
        private String endTime;   
    }
}

