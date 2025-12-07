package org.fyp.tmssep490be.dtos.teacherclass;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudentDTO {

    private Long id; //Student enrollment ID
    private Long studentId;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String branchName;

    private OffsetDateTime enrolledAt;
    private String enrolledBy;
    private Long enrolledById;
    private EnrollmentStatus status;

    private Long joinSessionId;
    private String joinSessionDate;

    private Boolean capacityOverride;
    private String overrideReason;
}
