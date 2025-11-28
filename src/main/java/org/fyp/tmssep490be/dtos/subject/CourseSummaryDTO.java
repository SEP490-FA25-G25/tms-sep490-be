package org.fyp.tmssep490be.dtos.subject;

import lombok.Builder;
import lombok.Data;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.CourseStatus;

import java.time.OffsetDateTime;

@Data
@Builder
public class CourseSummaryDTO {
    private Long id;
    private String code;
    private String name;
    private CourseStatus status;
    private ApprovalStatus approvalStatus;
    private String levelName;
    private String scoreScale;
    private OffsetDateTime createdAt;
    private OffsetDateTime decidedAt;
}

