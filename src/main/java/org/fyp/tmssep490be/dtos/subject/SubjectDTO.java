package org.fyp.tmssep490be.dtos.subject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDTO {
    private Long id;
    private String name;
    private String code;
    private String status;
    private String requesterName;
    private String approvalStatus;
    private String rejectionReason;
    private java.time.OffsetDateTime submittedAt;
    private java.time.OffsetDateTime decidedAt;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private java.time.LocalDate effectiveDate;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime updatedAt;
}
