package org.fyp.tmssep490be.dtos.teacherregistration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// Response sau khi đăng ký dạy lớp
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRegistrationResponse {
    private Long id;
    private Long classId;
    private String classCode;
    private String className;
    private String status;
    private String note;
    private OffsetDateTime registeredAt;
}
