package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// DTO hiển thị danh sách đăng ký của giáo viên
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyRegistrationDTO {
    private Long id;
    private Long classId;
    private String classCode;
    private String className;
    private String subjectName;
    private String branchName;
    private String modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private Short[] scheduleDays;
    private String status;
    private String note;
    private OffsetDateTime registeredAt;
    private OffsetDateTime registrationCloseDate;
    private String rejectionReason;
    private boolean canCancel; // Có thể hủy không (chưa hết hạn đăng ký)
}
