package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassResponse {

    private Long classId;
    private String code;
    private String name;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private OffsetDateTime createdAt;
    private SessionGenerationSummary sessionSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionGenerationSummary {
        private int sessionsGenerated;
        private int totalSessionsInSubject;
        private String subjectCode;
        private String subjectName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Short[] scheduleDays;
    }
}
