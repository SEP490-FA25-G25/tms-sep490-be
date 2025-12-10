package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDetailDTO {
    private Long id;
    private String code;
    private String name;
    private SubjectInfo subject; 
    private BranchInfo branch;
    private String modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;
    private List<Integer> scheduleDays;
    private Integer maxCapacity;
    private String status;
    private List<TeacherSummary> teachers;
    private String scheduleSummary;
    private List<ScheduleDetailDTO> scheduleDetails;
    private EnrollmentSummary enrollmentSummary;
    private SessionDTO nextSession;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDetailDTO {
        private String day;
        private String startTime;
        private String endTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurriculumInfo {  
        private Long id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectInfo { 
        private Long id;
        private String name;
        private String code;
        private String description;
        private Integer totalHours;
        private Integer numberOfSessions;
        private BigDecimal hoursPerSession;
        private String prerequisites;
        private String targetAudience;
        private CurriculumInfo curriculum;  
        private LevelInfo level;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private Long id;
        private String name;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private Long teacherId;
        private String teacherName;
        private String teacherEmail;
        private Boolean isPrimaryInstructor;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentSummary {
        private Integer totalEnrolled;
        private Integer maxCapacity;
    }
}
