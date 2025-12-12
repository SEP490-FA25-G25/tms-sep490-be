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
    private String code;
    private String name;
    private String status;
    private String approvalStatus;
    private String requesterName;
    private String rejectionReason;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private LocalDate effectiveDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer numberOfSessions;
    private String subjectName;
    private String levelName;
}
