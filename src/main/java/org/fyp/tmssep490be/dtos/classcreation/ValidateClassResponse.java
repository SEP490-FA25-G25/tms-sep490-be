package org.fyp.tmssep490be.dtos.classcreation;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateClassResponse {

    private Boolean valid;
    private Boolean canSubmit;
    private Long classId;
    private String message;
    private ValidationChecks checks;
    @Builder.Default
    private List<String> errors = List.of();
    @Builder.Default
    private List<String> warnings = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationChecks {
        private Long totalSessions;
        private Long sessionsWithTimeSlots;
        private Long sessionsWithResources;
        private Long sessionsWithTeachers;
        private Long sessionsWithoutTimeSlots;
        private Long sessionsWithoutResources;
        private Long sessionsWithoutTeachers;
        private Integer completionPercentage;
        private Boolean allSessionsHaveTimeSlots;
        private Boolean allSessionsHaveResources;
        private Boolean allSessionsHaveTeachers;
    }
}
