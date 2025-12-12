package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// DTO tổng hợp đăng ký cho 1 lớp (cho AA review)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRegistrationSummaryDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private String modality;
    private LocalDate startDate;
    private Short[] scheduleDays;
    private OffsetDateTime registrationCloseDate;
    private int pendingCount;
    private List<RegistrationDetailDTO> registrations;
    
    // Thông tin giáo viên đã được gán (nếu có)
    private Long assignedTeacherId;
    private String assignedTeacherName;
}
