package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for teacher's future sessions (for request creation)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSessionDTO {
    private Long sessionId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String className;
    private String classCode;
    private String courseName;
    private String topic;
    private Integer daysFromNow; // Số ngày từ hôm nay
    private String requestStatus; // "Có thể tạo request" hoặc "Đang chờ xử lý"
    private Boolean hasPendingRequest; // true nếu đã có request pending/waiting_confirm/approved
}

