package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// DTO hiển thị lớp có thể đăng ký dạy
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableClassDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private String branchName;
    private String modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private Short[] scheduleDays;
    private Integer maxCapacity;
    private OffsetDateTime registrationOpenDate;
    private OffsetDateTime registrationCloseDate;
    private int totalRegistrations; // Tổng số giáo viên đã đăng ký
    private boolean alreadyRegistered; // Giáo viên hiện tại đã đăng ký chưa
}
