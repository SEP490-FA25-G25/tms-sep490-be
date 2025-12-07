package org.fyp.tmssep490be.dtos.classdetail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Modality;

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
    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;
    private String status;
    private Integer maxCapacity;
    private List<Short> scheduleDays;

    // Thông tin môn học
    private CourseInfo course;

    // Thông tin chi nhánh
    private BranchInfo branch;

    // Danh sách giáo viên
    private List<TeacherInfo> teachers;

    // Tóm tắt đăng ký
    private EnrollmentSummary enrollmentSummary;

    // Các buổi học sắp tới
    private List<SessionInfo> upcomingSessions;

    // Tóm tắt lịch học
    private String scheduleSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseInfo {
        private Long id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchInfo {
        private Long id;
        private String code;
        private String name;
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentSummary {
        private Integer currentEnrolled;
        private Integer maxCapacity;
        private Integer availableSlots;
        private Double utilizationRate;
        private Boolean canEnrollStudents;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionInfo {
        private Long id;
        private LocalDate date;
        private String timeSlot;
        private String topic;
    }
}

