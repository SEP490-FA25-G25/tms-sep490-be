package org.fyp.tmssep490be.dtos.studentportal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSessionDTO {
    private Long sessionId;
    private Long studentId;
    private String attendanceStatus;
    private String homeworkStatus;
    private Boolean isMakeup;
    private Long makeupSessionId;
    private Long originalSessionId;
    private String note;
    private OffsetDateTime recordedAt;
}
