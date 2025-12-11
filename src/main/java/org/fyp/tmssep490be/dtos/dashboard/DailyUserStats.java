package org.fyp.tmssep490be.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO cho thống kê User theo ngày
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyUserStats {

    private LocalDate date;
    private Long count;
}
