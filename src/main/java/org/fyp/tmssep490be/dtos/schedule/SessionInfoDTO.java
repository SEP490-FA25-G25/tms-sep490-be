package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.SessionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Session content information")
public class SessionInfoDTO {

    @Schema(description = "Session topic", example = "Introduction to IELTS")
    private String topic;

    @Schema(description = "Session description", example = "Overview of IELTS exam structure and scoring")
    private String description;

    @Schema(description = "Session type", example = "CLASS")
    private SessionType sessionType;

    @Schema(description = "Session status", example = "PLANNED")
    private SessionStatus sessionStatus;

    @Schema(description = "Physical location", example = "Room 301")
    private String location;

    @Schema(description = "Online meeting link", example = "https://zoom.us/j/123456789")
    private String onlineLink;
}