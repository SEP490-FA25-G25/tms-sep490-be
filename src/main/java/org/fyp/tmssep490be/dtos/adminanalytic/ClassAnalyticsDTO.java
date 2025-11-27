package org.fyp.tmssep490be.dtos.adminanalytic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Class analytics data for Admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassAnalyticsDTO {
    private Long totalClasses;
    private Long activeClasses;
    private Long completedClasses;
    private Long cancelledClasses;
    private Map<String, Long> classesByStatus; // Status -> count
    private Double averageEnrollmentRate; // Percentage
    private Long totalEnrollments;
}

