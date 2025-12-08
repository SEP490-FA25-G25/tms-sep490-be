package org.fyp.tmssep490be.dtos.teacherrequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestCreateDTO {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Request type is required")
    private TeacherRequestType requestType;

    // REPLACEMENT
    private Long replacementTeacherId;

    // RESCHEDULE
    private LocalDate newDate;
    private Long newTimeSlotId;

    // RESCHEDULE & MODALITY_CHANGE
    private Long newResourceId;

    @NotBlank(message = "Reason is required")
    private String reason;
}

