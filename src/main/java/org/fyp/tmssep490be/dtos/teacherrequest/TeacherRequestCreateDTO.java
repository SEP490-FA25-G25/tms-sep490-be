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

    private Long teacherId;

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    @NotNull(message = "Request type is required")
    private TeacherRequestType requestType;

    private Long replacementTeacherId;

    private LocalDate newDate;
    private Long newTimeSlotId;

    private Long newResourceId;

    @NotBlank(message = "Reason is required")
    private String reason;
}

