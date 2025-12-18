package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.RegistrationWindowStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

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

    // Real-time registration status (calculated, not stored in DB)
    private RegistrationWindowStatus registrationStatus;

    // Matching info for teacher skills
    private Boolean isMatch; // true nếu giáo viên có skill phù hợp
    private String matchReason; // "Phù hợp: IELTS - English"
    private String curriculumName; // Curriculum name for matching (IELTS, TOEIC...)
    private String curriculumLanguage; // Language of curriculum

    // Time slot info for schedule display (legacy - first session only)
    private String timeSlotStart; // "14:00"
    private String timeSlotEnd; // "16:30"

    // Time slots grouped by day of week (supports different slots per day)
    // Key: day label (e.g., "T2", "T4, T6"), Value: time range (e.g., "07:00 -
    // 08:30")
    private Map<String, String> timeSlotsByDay;

    // Schedule conflict info
    private Boolean hasScheduleConflict;
    private String conflictDetails;
}
