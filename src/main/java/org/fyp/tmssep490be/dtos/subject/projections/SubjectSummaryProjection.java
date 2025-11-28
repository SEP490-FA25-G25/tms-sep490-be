package org.fyp.tmssep490be.dtos.subject.projections;

import org.fyp.tmssep490be.entities.enums.SubjectStatus;

import java.time.OffsetDateTime;

public interface SubjectSummaryProjection {
    Long getId();
    String getCode();
    String getName();
    String getDescription();
    SubjectStatus getStatus();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getUpdatedAt();
    String getOwnerName();
    Long getLevelCount();
    Long getCourseCount();
    Long getPendingCourseCount();
    Long getApprovedCourseCount();
    Long getDraftCourseCount();
}

