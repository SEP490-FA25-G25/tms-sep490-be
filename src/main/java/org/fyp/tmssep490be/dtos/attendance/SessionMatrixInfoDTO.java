package org.fyp.tmssep490be.dtos.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMatrixInfoDTO {
    private Long sessionId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
}

