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

    // Nếu strategy = PARTIAL → phải có
    private List<Long> selectedStudentIds;

    // Nếu strategy = OVERRIDE → phải có
    @Size(min = 20, message = "Override reason must be at least 20 characters")
    private String overrideReason;

    // Students từ preview
    @NotEmpty(message = "Students list cannot be empty")
    private List<StudentEnrollmentData> students;
}

