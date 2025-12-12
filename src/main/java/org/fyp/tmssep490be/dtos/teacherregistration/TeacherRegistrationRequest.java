package org.fyp.tmssep490be.dtos.teacherregistration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request để giáo viên đăng ký dạy lớp
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRegistrationRequest {

    @NotNull(message = "classId không được để trống")
    private Long classId;

    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
    private String note;
}
