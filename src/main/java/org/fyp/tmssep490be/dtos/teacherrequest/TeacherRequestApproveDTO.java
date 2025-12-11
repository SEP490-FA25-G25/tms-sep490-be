package org.fyp.tmssep490be.dtos.teacherrequest;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestApproveDTO {

    private Long replacementTeacherId;

    private java.time.LocalDate newDate;

    private Long newTimeSlotId;
    
    private Long newResourceId;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}

