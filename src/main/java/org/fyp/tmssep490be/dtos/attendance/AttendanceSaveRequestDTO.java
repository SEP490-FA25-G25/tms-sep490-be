package org.fyp.tmssep490be.dtos.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AttendanceSaveRequestDTO {

    @NotEmpty
    @Valid
    private List<AttendanceRecordDTO> records;
}


