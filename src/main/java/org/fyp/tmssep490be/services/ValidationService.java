package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classcreation.ValidateClassResponse;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidationService {

    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;


    @Transactional(readOnly = true)
    public ValidateClassResponse validateClassComplete(Long classId) {
        log.info("Validating class completeness for class ID: {}", classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Get session counts
        Long totalSessions = sessionRepository.countByClassEntityId(classId);
        if (totalSessions == null || totalSessions == 0) {
            return ValidateClassResponse.builder()
                    .valid(false)
                    .canSubmit(false)
                    .classId(classId)
                    .message("Không có buổi học nào được khởi tạo")
                    .errors(List.of("Lớp học chưa có buổi học"))
                    .build();
        }

        // Count sessions with time slots and resources
        List<Session> allSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);
        long sessionsWithTimeSlots = allSessions.stream()
                .filter(s -> s.getTimeSlotTemplate() != null)
                .count();
        long sessionsWithResources = allSessions.stream()
                .filter(s -> s.getSessionResources() != null && !s.getSessionResources().isEmpty())
                .count();

        long sessionsWithoutTimeSlots = totalSessions - sessionsWithTimeSlots;
        long sessionsWithoutResources = totalSessions - sessionsWithResources;

        boolean allSessionsHaveTimeSlots = sessionsWithoutTimeSlots == 0;
        boolean allSessionsHaveResources = sessionsWithoutResources == 0;

        // Calculate completion percentage (only time slots and resources)
        long completedChecks = (allSessionsHaveTimeSlots ? 1 : 0) +
                (allSessionsHaveResources ? 1 : 0);
        int completionPercentage = (int) (completedChecks * 100 / 2);

        // Only check time slots and resources (no teacher check)
        boolean valid = allSessionsHaveTimeSlots && allSessionsHaveResources;
        boolean canSubmit = valid;

        List<String> errors = new ArrayList<>();
        if (!allSessionsHaveTimeSlots) {
            errors.add(String.format("Còn %d buổi chưa có khung giờ", sessionsWithoutTimeSlots));
        }
        if (!allSessionsHaveResources) {
            errors.add(String.format("Còn %d buổi chưa có tài nguyên", sessionsWithoutResources));
        }

        String message = valid ? "Lớp học đã sẵn sàng để gửi duyệt"
                : "Lớp học chưa hoàn tất các cấu hình cần thiết";

        return ValidateClassResponse.builder()
                .valid(valid)
                .canSubmit(canSubmit)
                .classId(classId)
                .message(message)
                .checks(ValidateClassResponse.ValidationChecks.builder()
                        .totalSessions(totalSessions)
                        .sessionsWithTimeSlots(sessionsWithTimeSlots)
                        .sessionsWithResources(sessionsWithResources)
                        .sessionsWithoutTimeSlots(sessionsWithoutTimeSlots)
                        .sessionsWithoutResources(sessionsWithoutResources)
                        .completionPercentage(completionPercentage)
                        .allSessionsHaveTimeSlots(allSessionsHaveTimeSlots)
                        .allSessionsHaveResources(allSessionsHaveResources)
                        .build())
                .errors(errors)
                .warnings(List.of())
                .build();
    }
}
