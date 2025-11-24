package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.fyp.tmssep490be.entities.enums.SessionStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Information about makeup session context")
public class MakeupInfoDTO {

    @Schema(description = "Whether this is a makeup session", example = "true")
    private Boolean isMakeup;

    @Schema(description = "ID of the original session that was replaced", example = "1001")
    private Long originalSessionId;

    @Schema(description = "Date of the original session", example = "2025-10-28")
    private LocalDate originalDate;

    @Schema(description = "Start time of the original session", example = "15:30")
    private String originalStartTime;

    @Schema(description = "End time of the original session", example = "17:30")
    private String originalEndTime;

    @Schema(description = "Status of the original session", example = "CANCELLED")
    private SessionStatus originalStatus;

    @Schema(description = "Reason for makeup session", example = "Teacher unavailable")
    private String reason;

    @Schema(description = "Date when makeup is scheduled", example = "2025-11-04")
    private LocalDate makeupDate;
}