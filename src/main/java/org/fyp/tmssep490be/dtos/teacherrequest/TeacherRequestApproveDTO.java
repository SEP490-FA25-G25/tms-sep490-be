package org.fyp.tmssep490be.dtos.teacherrequest;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for Staff to approve/reject Teacher Request
 * Staff can override Teacher's choices when approving
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestApproveDTO {

    // For REPLACEMENT - Staff can override replacement teacher
    private Long replacementTeacherId;

    // For RESCHEDULE - Staff can override date/slot/resource
    private LocalDate newDate;
    private Long newTimeSlotId;
    private Long newResourceId; // Also used for MODALITY_CHANGE

    // Staff note
    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}



