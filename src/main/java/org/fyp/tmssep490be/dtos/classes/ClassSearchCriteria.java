package org.fyp.tmssep490be.dtos.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.util.List;

/**
 * Criteria for flexible class search (used by AA for transfer options)
 * Supports filtering by course, branch, modality, time slot, and capacity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSearchCriteria {
    /**
     * Course ID - ALWAYS required (transfer within same course only)
     */
    private Long courseId;

    /**
     * Exclude this class ID from results (current class)
     */
    private Long excludeClassId;

    /**
     * Filter by branch ID (optional - for branch change)
     */
    private Long branchId;

    /**
     * Filter by modality (optional - for modality change)
     */
    private Modality modality;

    /**
     * Exclude this time slot ID (for schedule-only change)
     */
    private Long excludeTimeSlotId;

    /**
     * Filter by class statuses (default: SCHEDULED, ONGOING)
     */
    private List<ClassStatus> statuses;

    /**
     * Only return classes with available capacity
     */
    private Boolean hasCapacity;
}
