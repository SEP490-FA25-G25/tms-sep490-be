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

    // Frontend sends only the selected students (not all students)
    @NotEmpty(message = "Selected students list cannot be empty")
    private List<StudentEnrollmentData> selectedStudents;

    // Required only when strategy = OVERRIDE
    @Size(min = 20, message = "Override reason must be at least 20 characters")
    private String overrideReason;
}

