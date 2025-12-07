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
public class EnrollExistingStudentsRequest {

    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotEmpty(message = "At least one student must be selected")
    private List<Long> studentIds;

    private Boolean overrideCapacity;

    @Size(min = 20, max = 500, message = "Override reason must be between 20 and 500 characters")
    private String overrideReason;
}

