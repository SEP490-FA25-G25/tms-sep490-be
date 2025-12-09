package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;

import java.time.OffsetDateTime;

/**
 * Student information in class context
 * Used in GET /api/v1/classes/{id}/students endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudentDTO {

    private Long id;
    private Long studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String address;
    private String branchName;

    // Enrollment information
    private OffsetDateTime enrolledAt;
    private String enrolledBy;
    private Long enrolledById;
    private EnrollmentStatus status;

    // Session information for mid-course enrollment
    private Long joinSessionId;
    private String joinSessionDate;

    // Capacity override information
    private Boolean capacityOverride;
    private String overrideReason;

    /**
     * Quick status check for UI
     */
    public boolean isActive() {
        return status == EnrollmentStatus.ENROLLED;
    }

    /**
     * Display name for enrolled by
     */
    public String getEnrolledByDisplay() {
        return enrolledBy != null ? enrolledBy : "System";
    }
}
