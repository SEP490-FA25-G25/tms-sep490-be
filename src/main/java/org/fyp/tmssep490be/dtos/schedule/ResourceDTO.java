package org.fyp.tmssep490be.dtos.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.fyp.tmssep490be.entities.enums.ResourceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Classroom resource information for session location")
public class ResourceDTO {

    @Schema(description = "Resource ID", example = "1")
    private Long resourceId;

    @Schema(description = "Resource code", example = "HN01-R101")
    private String resourceCode;

    @Schema(description = "Resource name (room name or zoom name)", example = "Ha Noi Room 101")
    private String resourceName;

    @Schema(description = "Resource type", example = "ROOM")
    private ResourceType resourceType;

    @Schema(description = "Resource capacity", example = "20")
    private Integer capacity;

    @Schema(description = "Physical location or online link", example = "Room 101, TMS Ha Noi Branch")
    private String location;

    @Schema(description = "Zoom/online meeting link for virtual sessions", example = "https://zoom.us/j/123456789")
    private String onlineLink;
}