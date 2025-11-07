package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.ValidateClassResponse;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.services.ValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationServiceImpl implements ValidationService {

    private final SessionRepository sessionRepository;
    private final ClassRepository classRepository;

    @Override
    public ValidateClassResponse validateClassComplete(Long classId) {
        log.info("Starting validation for class ID: {}", classId);

        try {
            // Validate class exists
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            // Get session counts
            long totalSessions = sessionRepository.countByClassEntityId(classId);
            if (totalSessions == 0) {
                return createEmptyClassResponse(classId);
            }

            long sessionsWithTimeSlots = sessionRepository.countSessionsWithTimeSlots(classId);
            long sessionsWithoutTimeSlots = sessionRepository.countSessionsWithoutTimeSlots(classId);
            long sessionsWithResources = sessionRepository.countSessionsWithResources(classId);
            long sessionsWithoutResources = sessionRepository.countSessionsWithoutResources(classId);
            long sessionsWithTeachers = sessionRepository.countSessionsWithTeachers(classId);
            long sessionsWithoutTeachers = sessionRepository.countSessionsWithoutTeachers(classId);

            // Build validation checks
            ValidateClassResponse.ValidationChecks validationChecks = ValidateClassResponse.ValidationChecks.builder()
                    .totalSessions(totalSessions)
                    .sessionsWithTimeSlots(sessionsWithTimeSlots)
                    .sessionsWithResources(sessionsWithResources)
                    .sessionsWithTeachers(sessionsWithTeachers)
                    .sessionsWithoutTimeSlots(sessionsWithoutTimeSlots)
                    .sessionsWithoutResources(sessionsWithoutResources)
                    .sessionsWithoutTeachers(sessionsWithoutTeachers)
                    .allSessionsHaveTimeSlots(sessionsWithoutTimeSlots == 0)
                    .allSessionsHaveResources(sessionsWithoutResources == 0)
                    .allSessionsHaveTeachers(sessionsWithoutTeachers == 0)
                    .startDateInPast(classEntity.getStartDate().isBefore(LocalDate.now()))
                    .build();

            // Calculate completion percentage
            int completionPercentage = calculateCompletionPercentage(
                    sessionsWithTimeSlots, sessionsWithResources, sessionsWithTeachers, totalSessions);
            validationChecks.setCompletionPercentage(completionPercentage);

            // Check for multiple teachers per skill group (simplified check for now)
            boolean hasMultipleTeachersPerSkillGroup = checkMultipleTeachersPerSkillGroup(classId);
            validationChecks.setHasMultipleTeachersPerSkillGroup(hasMultipleTeachersPerSkillGroup);

            // Build error and warning lists
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            // Check 1: All sessions have timeslot assignment
            if (sessionsWithoutTimeSlots > 0) {
                errors.add(String.format("%d sessions missing time slot assignments", sessionsWithoutTimeSlots));
            }

            // Check 2: All sessions have resource assignment
            if (sessionsWithoutResources > 0) {
                errors.add(String.format("%d sessions missing resource assignments", sessionsWithoutResources));
            }

            // Check 3: All sessions have teacher assignment
            if (sessionsWithoutTeachers > 0) {
                errors.add(String.format("%d sessions missing teacher assignments", sessionsWithoutTeachers));
            }

            // Warning: Multiple teachers per skill group
            if (hasMultipleTeachersPerSkillGroup) {
                warnings.add("Multiple teachers assigned to the same skill group");
            }

            // Warning: Start date in past
            if (classEntity.getStartDate().isBefore(LocalDate.now())) {
                warnings.add("Class start date is in the past");
            }

            // Determine if class can be submitted
            boolean canSubmit = errors.isEmpty();
            boolean isValid = canSubmit && warnings.isEmpty();

            validationChecks.setHasValidationErrors(!errors.isEmpty());
            validationChecks.setHasValidationWarnings(!warnings.isEmpty());

            // Build response message
            String message = buildValidationMessage(isValid, canSubmit, completionPercentage);

            ValidateClassResponse response = ValidateClassResponse.builder()
                    .valid(isValid)
                    .canSubmit(canSubmit)
                    .classId(classId)
                    .message(message)
                    .checks(validationChecks)
                    .errors(errors)
                    .warnings(warnings)
                    .build();

            log.info("Validation completed for class ID: {}. Valid: {}, CanSubmit: {}",
                    classId, isValid, canSubmit);

            return response;

        } catch (Exception e) {
            log.error("Error validating class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private ValidateClassResponse createEmptyClassResponse(Long classId) {
        ValidateClassResponse.ValidationChecks checks = ValidateClassResponse.ValidationChecks.builder()
                .totalSessions(0L)
                .sessionsWithTimeSlots(0L)
                .sessionsWithResources(0L)
                .sessionsWithTeachers(0L)
                .sessionsWithoutTimeSlots(0L)
                .sessionsWithoutResources(0L)
                .sessionsWithoutTeachers(0L)
                .completionPercentage(0)
                .allSessionsHaveTimeSlots(false)
                .allSessionsHaveResources(false)
                .allSessionsHaveTeachers(false)
                .hasMultipleTeachersPerSkillGroup(false)
                .startDateInPast(false)
                .hasValidationErrors(true)
                .hasValidationWarnings(false)
                .build();

        List<String> errors = List.of("Class has no sessions generated");

        return ValidateClassResponse.builder()
                .valid(false)
                .canSubmit(false)
                .classId(classId)
                .message("Class has no sessions to validate")
                .checks(checks)
                .errors(errors)
                .warnings(new ArrayList<>())
                .build();
    }

    private int calculateCompletionPercentage(long withTimeSlots, long withResources, long withTeachers, long total) {
        if (total == 0) return 0;

        int timeSlotPercentage = (int) ((withTimeSlots * 100) / total);
        int resourcePercentage = (int) ((withResources * 100) / total);
        int teacherPercentage = (int) ((withTeachers * 100) / total);

        return (timeSlotPercentage + resourcePercentage + teacherPercentage) / 3;
    }

    private boolean checkMultipleTeachersPerSkillGroup(Long classId) {
        // Simplified implementation - in real scenario, this would check
        // if multiple teachers are assigned to sessions requiring the same skill
        // For now, return false as this is a complex business rule
        return false;
    }

    private String buildValidationMessage(boolean isValid, boolean canSubmit, int completionPercentage) {
        if (isValid) {
            return String.format("Class is fully validated and ready for submission (%d%% complete)", completionPercentage);
        } else if (canSubmit) {
            return String.format("Class can be submitted but has warnings (%d%% complete)", completionPercentage);
        } else {
            return String.format("Class is not ready for submission (%d%% complete)", completionPercentage);
        }
    }
}