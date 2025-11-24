package org.fyp.tmssep490be.dtos.teacherrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;

/**
 * DTO for creating Teacher Request (used for all request types: REPLACEMENT, RESCHEDULE, MODALITY_CHANGE)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestCreateDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Request type is required")
    private TeacherRequestType requestType; // REPLACEMENT/RESCHEDULE/MODALITY_CHANGE

    // For REPLACEMENT
    private Long replacementTeacherId;

    // For RESCHEDULE
    private LocalDate newDate;
    private Long newTimeSlotId;
    private Long newResourceId; // Also used for MODALITY_CHANGE

    // Required reason
    @NotBlank(message = "Reason is required")
    private String reason;
}

