package org.fyp.tmssep490be.dtos.teacherregistration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request để AA gán trực tiếp giáo viên (không qua đăng ký)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectAssignRequest {

    @NotNull(message = "classId không được để trống")
    private Long classId;

    @NotNull(message = "teacherId không được để trống")
    private Long teacherId;

    @NotNull(message = "Lý do gán trực tiếp không được để trống")
    @Size(min = 10, max = 500, message = "Lý do phải từ 10-500 ký tự")
    private String reason;
}
