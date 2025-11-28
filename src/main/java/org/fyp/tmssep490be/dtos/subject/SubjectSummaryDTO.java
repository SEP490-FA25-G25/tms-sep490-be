package org.fyp.tmssep490be.dtos.subject;

import lombok.Builder;
import lombok.Data;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;

import java.time.OffsetDateTime;

@Data
@Builder
public class SubjectSummaryDTO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private SubjectStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String ownerName;
    private long levelCount;
    private long courseCount;
    private long pendingCourseCount;
    private long approvedCourseCount;
    private long draftCourseCount;
}

