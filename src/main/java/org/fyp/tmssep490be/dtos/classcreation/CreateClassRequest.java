package org.fyp.tmssep490be.dtos.classcreation;

import jakarta.validation.constraints.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @Size(max = 50, message = "Class code must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9\\-]*$", message = "Class code must contain only uppercase letters, numbers, and hyphens")
    private String code;

    @NotBlank(message = "Class name is required")
    @Size(max = 255, message = "Class name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Modality is required")
    private Modality modality;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotEmpty(message = "Schedule days are required")
    @Size(min = 1, max = 7, message = "Schedule days must contain 1-7 days")
    private List<@NotNull Short> scheduleDays;

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 1000, message = "Max capacity must not exceed 1000")
    private Integer maxCapacity;
}
