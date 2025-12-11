package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

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

    // Các trường bổ sung cho edit mode
    private Long branchId;
    private String branchName;
    private Long subjectId;
    private String subjectName;
    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private List<Short> scheduleDays;
    private Integer maxCapacity;

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
