package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private List<String> errors;
    private List<String> warnings;

    // Helper methods
    public boolean isValid() {
        return valid != null && valid;
    }

    public boolean canSubmit() {
        return canSubmit != null && canSubmit;
    }

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
        private Boolean hasMultipleTeachersPerSkillGroup;
        private Boolean startDateInPast;
        private Boolean hasValidationErrors;
        private Boolean hasValidationWarnings;

        // Additional helper methods
        public boolean isTimeSlotsComplete() {
            return allSessionsHaveTimeSlots != null && allSessionsHaveTimeSlots;
        }

        public boolean isResourcesComplete() {
            return allSessionsHaveResources != null && allSessionsHaveResources;
        }

        public boolean isTeachersComplete() {
            return allSessionsHaveTeachers != null && allSessionsHaveTeachers;
        }

        public boolean isFullyComplete() {
            return isTimeSlotsComplete() && isResourcesComplete() && isTeachersComplete();
        }
    }
}