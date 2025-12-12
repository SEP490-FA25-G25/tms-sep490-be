package org.fyp.tmssep490be.dtos.teacherregistration;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request để AA duyệt chọn giáo viên
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRegistrationRequest {

    @NotNull(message = "registrationId không được để trống")
    private Long registrationId;
}
