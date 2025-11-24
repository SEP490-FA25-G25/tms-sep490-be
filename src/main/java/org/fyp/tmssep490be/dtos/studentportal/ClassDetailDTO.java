package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for detailed class information in student portal
 * Contains comprehensive class data for detail page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDetailDTO {
    private Long id;
    private String code;
    private String name;
    private CourseInfo course;
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
    private EnrollmentSummary enrollmentSummary;
    private SessionDTO nextSession;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {
        private Long id;
        private String name;
        private String code;
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