package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestListDTO {

    private Long id;
    private TeacherRequestType requestType;
    private RequestStatus status;

    // Thông tin session gốc
    private Long sessionId;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private LocalTime sessionEndTime;
    private String className;
    private String classCode;
    private String sessionTopic;

    // Thông tin giáo viên tạo yêu cầu
    private Long teacherId;
    private String teacherName;
    private String teacherEmail;

    // Giáo viên thay thế phù hợp với yêu cầu dạy thay
    private String replacementTeacherName;

    // Lịch học phù hợp với yêu cầu rời lịch 1 buổi học
    private LocalDate newSessionDate;
    private LocalTime newSessionStartTime;
    private LocalTime newSessionEndTime;

    private String requestReason;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private Long decidedById;
    private String decidedByName;
    private String decidedByEmail;

    // Phòng học/link zoom phù hợp với yêu cầu đổi hình thức học
    private Modality currentModality;
    private Modality newModality;
}

