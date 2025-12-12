package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ResourceType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableResourceDTO {

    private Long id;
    private String code;
    private String name;
    private String displayName;
    private String resourceType;
    private Integer capacity;
    private Double availabilityRate;
    private Integer conflictCount;
    private Integer totalSessions;
    private Boolean isRecommended;

    public static AvailableResourceDTO fromEntity(Resource resource, Integer conflictCount, Integer totalSessions) {
        String frontendResourceType = mapResourceType(resource.getResourceType());

        double availabilityRate = totalSessions > 0
                ? ((totalSessions - conflictCount) * 100.0 / totalSessions)
                : 100.0;

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

    public static AvailableResourceDTO basic(Resource resource) {
        return AvailableResourceDTO.builder()
                .id(resource.getId())
                .code(resource.getCode())
                .name(resource.getName())
                .displayName(resource.getCode() + " - " + resource.getName())
                .resourceType(mapResourceType(resource.getResourceType()))
                .capacity(resource.getCapacity())
                .build();
    }

    private static String mapResourceType(ResourceType resourceType) {
        return switch (resourceType) {
            case VIRTUAL -> "VIRTUAL";
            case ROOM -> "ROOM";
            default -> resourceType.name();
        };
    }
}
