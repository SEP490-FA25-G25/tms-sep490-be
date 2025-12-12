package org.fyp.tmssep490be.dtos.teacherregistration;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// Request để AA mở đăng ký cho lớp
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenRegistrationRequest {

    @NotNull(message = "classId không được để trống")
    private Long classId;

    @NotNull(message = "registrationOpenDate không được để trống")
    private OffsetDateTime registrationOpenDate;

    @NotNull(message = "registrationCloseDate không được để trống")
    private OffsetDateTime registrationCloseDate;
}
