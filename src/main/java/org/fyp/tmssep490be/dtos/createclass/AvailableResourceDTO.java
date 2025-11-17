package org.fyp.tmssep490be.dtos.createclass;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.ResourceType;

/**
 * DTO for available resources in STEP 4 of Create Class workflow (ResourceOption)
 * <p>
 * Represents a resource (room/online account) that is available for assignment
 * based on branch, capacity, and time slot availability.
 * Matches frontend ResourceOption interface.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Available resource option for class session assignment (ResourceOption)")
public class AvailableResourceDTO {

    @Schema(description = "Resource ID", example = "6")
    private Long id;

    @Schema(description = "Resource code", example = "HN01-R203")
    private String code;

    @Schema(description = "Resource name", example = "Room 203")
    private String name;

    @Schema(description = "Display name for UI (code + name)", example = "HN01-R203 - Room 203")
    private String displayName;

    @Schema(description = "Resource type (ROOM or VIRTUAL for frontend compatibility)", example = "ROOM")
    private String resourceType;

    @Schema(description = "Resource capacity", example = "30")
    private Integer capacity;

    @Schema(description = "Availability rate (0-100%)", example = "95.5")
    private Double availabilityRate;

    @Schema(description = "Number of conflicts for this resource at this time slot", example = "0")
    private Integer conflictCount;

    @Schema(description = "Total sessions for this day of week", example = "12")
    private Integer totalSessions;

    @Schema(description = "Is this resource recommended for the class", example = "true")
    private Boolean isRecommended;

    /**
     * Factory method to create DTO from entity with metadata
     */
    public static AvailableResourceDTO fromEntity(
            org.fyp.tmssep490be.entities.Resource resource,
            Integer conflictCount,
            Integer totalSessions) {
        
        // Map backend enum to frontend string format
        String frontendResourceType = mapResourceType(resource.getResourceType());
        
        // Calculate availability rate
        double availabilityRate = totalSessions > 0 
                ? ((totalSessions - conflictCount) * 100.0 / totalSessions) 
                : 100.0;
        
        // Recommended if no conflicts and capacity is adequate
        boolean isRecommended = conflictCount == 0 && availabilityRate >= 90.0;
        
        return AvailableResourceDTO.builder()
                .id(resource.getId())
                .code(resource.getCode())
                .name(resource.getName())
                .displayName(resource.getCode() + " - " + resource.getName())
                .resourceType(frontendResourceType)
                .capacity(resource.getCapacity())
                .availabilityRate(availabilityRate)
                .conflictCount(conflictCount)
                .totalSessions(totalSessions)
                .isRecommended(isRecommended)
                .build();
    }

    /**
     * Lightweight factory used for Quick Fix suggestions where conflict statistics
     * are not required.
     */
    public static AvailableResourceDTO basic(org.fyp.tmssep490be.entities.Resource resource) {
        return AvailableResourceDTO.builder()
                .id(resource.getId())
                .code(resource.getCode())
                .name(resource.getName())
                .displayName(resource.getCode() + " - " + resource.getName())
                .resourceType(mapResourceType(resource.getResourceType()))
                .capacity(resource.getCapacity())
                .availabilityRate(null)
                .conflictCount(null)
                .totalSessions(null)
                .isRecommended(null)
                .build();
    }

    /**
     * Map backend ResourceType enum to frontend string format
     */
    private static String mapResourceType(ResourceType resourceType) {
        return switch (resourceType) {
            case VIRTUAL -> "VIRTUAL"; // Frontend expects 'ONLINE_ACCOUNT' but uses 'VIRTUAL'
            case ROOM -> "ROOM";
            default -> resourceType.name();
        };
    }
}
