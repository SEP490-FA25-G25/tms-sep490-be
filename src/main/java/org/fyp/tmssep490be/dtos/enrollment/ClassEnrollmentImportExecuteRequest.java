package org.fyp.tmssep490be.dtos.enrollment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentImportExecuteRequest {
    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Enrollment strategy is required")
    private EnrollmentStrategy strategy;

    // Chỉ gửi IDs của students đã tồn tại (FOUND)
    private List<Long> existingStudentIds;

    // Chỉ gửi data của students cần tạo mới (CREATE)
    private List<StudentEnrollmentData> newStudents;

    // Required only when strategy = OVERRIDE
    @Size(min = 20, message = "Override reason must be at least 20 characters")
    private String overrideReason;
}

