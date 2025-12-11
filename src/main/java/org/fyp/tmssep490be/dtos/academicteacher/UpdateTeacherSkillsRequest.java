package org.fyp.tmssep490be.dtos.academicteacher;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Request DTO để cập nhật skills của giáo viên
// Thay thế toàn bộ skills hiện tại bằng danh sách mới (full replacement strategy)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeacherSkillsRequest {
    
    @NotEmpty(message = "Danh sách skills không được để trống")
    @Valid
    private List<TeacherSkillDTO> skills;
}

