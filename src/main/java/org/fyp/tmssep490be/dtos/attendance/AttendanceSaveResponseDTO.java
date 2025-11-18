package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceSaveResponseDTO {
    private Long sessionId;
    private AttendanceSummaryDTO summary;
}


