package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSaveRequestDTO {
    private List<AttendanceRecordDTO> records;
}

