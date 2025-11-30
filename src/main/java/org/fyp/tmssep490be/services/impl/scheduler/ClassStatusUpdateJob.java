package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled job to automatically update class status based on dates.
 *
 * Status Transitions:
 * 1. SCHEDULED -> ONGOING: When startDate is reached
 * 2. ONGOING -> COMPLETED: When plannedEndDate is reached
 *
 * Runs daily at 12:30 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.class-status-update",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ClassStatusUpdateJob extends BaseScheduledJob {

    private final ClassRepository classRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final NotificationService notificationService;
    private final org.fyp.tmssep490be.repositories.NotificationRepository notificationRepository;

    @Value("${tms.scheduler.jobs.class-status-update.notify-days-before-start:7}")
    private int notifyDaysBeforeStart;

    @Value("${tms.scheduler.jobs.class-status-update.notify-days-before-end:7}")
    private int notifyDaysBeforeEnd;

    /**
     * Update class status from SCHEDULED to ONGOING when startDate is reached.
     * Runs daily at 12:30 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 30 0 * * ?}")
    @Transactional
    public void updateScheduledToOngoing() {
        try {
            logJobStart("ClassStatusUpdate: SCHEDULED->ONGOING");

            LocalDate today = LocalDate.now();
            logJobInfo("Checking classes with startDate <= " + today + " and status = SCHEDULED");

            List<ClassEntity> scheduledClasses = classRepository
                .findByStartDateBeforeOrEqualAndStatus(today, ClassStatus.SCHEDULED);

            if (scheduledClasses.isEmpty()) {
                logJobEnd("ClassStatusUpdate: SCHEDULED->ONGOING", "No classes to update");
                return;
            }

            int updatedCount = 0;
            for (ClassEntity classEntity : scheduledClasses) {
                logJobInfo(String.format("Updating class %s (ID: %d) from SCHEDULED to ONGOING",
                    classEntity.getCode(), classEntity.getId()));
                classEntity.setStatus(ClassStatus.ONGOING);
                classRepository.save(classEntity);
                
                // Send notification to teachers
                sendNotificationForClassStarted(classEntity);
                
                updatedCount++;
            }

            logJobEnd("ClassStatusUpdate: SCHEDULED->ONGOING", updatedCount);

        } catch (Exception e) {
            logJobError("ClassStatusUpdate: SCHEDULED->ONGOING", e);
            throw e;
        }
    }

    /**
     * Update class status from ONGOING to COMPLETED when plannedEndDate is reached.
     * Runs daily at 12:30 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 30 0 * * ?}")
    @Transactional
    public void updateOngoingToCompleted() {
        try {
            logJobStart("ClassStatusUpdate: ONGOING->COMPLETED");

            LocalDate today = LocalDate.now();
            logJobInfo("Checking classes with plannedEndDate <= " + today + " and status = ONGOING");

            List<ClassEntity> ongoingClasses = classRepository
                .findByPlannedEndDateBeforeOrEqualAndStatus(today, ClassStatus.ONGOING);

            if (ongoingClasses.isEmpty()) {
                logJobEnd("ClassStatusUpdate: ONGOING->COMPLETED", "No classes to update");
                return;
            }

            int updatedCount = 0;
            for (ClassEntity classEntity : ongoingClasses) {
                logJobInfo(String.format("Updating class %s (ID: %d) from ONGOING to COMPLETED",
                    classEntity.getCode(), classEntity.getId()));
                classEntity.setStatus(ClassStatus.COMPLETED);
                classEntity.setActualEndDate(today); // Record actual end date
                classRepository.save(classEntity);
                
                // Send notification to teachers
                sendNotificationForClassEnded(classEntity);
                
                updatedCount++;
            }

            logJobEnd("ClassStatusUpdate: ONGOING->COMPLETED", updatedCount);

        } catch (Exception e) {
            logJobError("ClassStatusUpdate: ONGOING->COMPLETED", e);
            throw e;
        }
    }

    /**
     * Check classes starting soon and send notifications to teachers
     * Runs daily at 12:30 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 30 0 * * ?}")
    @Transactional
    public void checkClassesStartingSoon() {
        try {
            logJobStart("ClassNotification: Starting Soon");

            LocalDate today = LocalDate.now();
            LocalDate checkDate = today.plusDays(notifyDaysBeforeStart);

            List<ClassEntity> classes = classRepository
                .findByStartDateAndStatus(checkDate, ClassStatus.SCHEDULED);

            int notificationsSent = 0;
            for (ClassEntity classEntity : classes) {
                notificationsSent += sendNotificationForClassStartingSoon(classEntity);
            }

            logJobEnd("ClassNotification: Starting Soon", 
                String.format("Sent %d notifications", notificationsSent));

        } catch (Exception e) {
            logJobError("ClassNotification: Starting Soon", e);
            throw e;
        }
    }

    /**
     * Check classes ending soon and send notifications to teachers
     * Runs daily at 12:30 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.class-status-update.cron:0 30 0 * * ?}")
    @Transactional
    public void checkClassesEndingSoon() {
        try {
            logJobStart("ClassNotification: Ending Soon");

            LocalDate today = LocalDate.now();
            LocalDate checkDate = today.plusDays(notifyDaysBeforeEnd);

            List<ClassEntity> classes = classRepository
                .findByPlannedEndDateAndStatus(checkDate, ClassStatus.ONGOING);

            int notificationsSent = 0;
            for (ClassEntity classEntity : classes) {
                notificationsSent += sendNotificationForClassEndingSoon(classEntity);
            }

            logJobEnd("ClassNotification: Ending Soon", 
                String.format("Sent %d notifications", notificationsSent));

        } catch (Exception e) {
            logJobError("ClassNotification: Ending Soon", e);
            throw e;
        }
    }

    /**
     * Send notification to teachers when class starts
     */
    private void sendNotificationForClassStarted(ClassEntity classEntity) {
        try {
            Set<Long> teacherIds = getTeacherIdsForClass(classEntity.getId());
            String className = classEntity.getName() != null ? classEntity.getName() : classEntity.getCode();
            String formattedDate = classEntity.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            for (Long teacherId : teacherIds) {
                notificationService.createNotificationFromRequest(
                    org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                        .recipientId(teacherId)
                        .type(NotificationType.CLASS_REMINDER)
                        .title("Lớp học đã bắt đầu")
                        .message(String.format("Lớp %s đã bắt đầu vào ngày %s.",
                            className, formattedDate))
                        .priority(NotificationPriority.MEDIUM)
                        .referenceType("CLASS")
                        .referenceId(classEntity.getId())
                        .actionUrl(String.format("/teacher/classes/%d", classEntity.getId()))
                        .metadata(String.format("{\"classStatus\":\"ONGOING\",\"classId\":%d}", classEntity.getId()))
                        .build()
                );

                logJobInfo(String.format("Sent class started notification to teacher %d for class %d",
                    teacherId, classEntity.getId()));
            }
        } catch (Exception e) {
            log.error("Failed to send class started notification for class {}: {}", 
                classEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification to teachers when class ends
     */
    private void sendNotificationForClassEnded(ClassEntity classEntity) {
        try {
            Set<Long> teacherIds = getTeacherIdsForClass(classEntity.getId());
            String className = classEntity.getName() != null ? classEntity.getName() : classEntity.getCode();
            String formattedDate = classEntity.getActualEndDate() != null 
                ? classEntity.getActualEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : classEntity.getPlannedEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            for (Long teacherId : teacherIds) {
                notificationService.createNotificationFromRequest(
                    org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                        .recipientId(teacherId)
                        .type(NotificationType.CLASS_REMINDER)
                        .title("Lớp học đã kết thúc")
                        .message(String.format("Lớp %s đã kết thúc vào ngày %s.",
                            className, formattedDate))
                        .priority(NotificationPriority.MEDIUM)
                        .referenceType("CLASS")
                        .referenceId(classEntity.getId())
                        .actionUrl(String.format("/teacher/classes/%d", classEntity.getId()))
                        .metadata(String.format("{\"classStatus\":\"COMPLETED\",\"classId\":%d}", classEntity.getId()))
                        .build()
                );

                logJobInfo(String.format("Sent class ended notification to teacher %d for class %d",
                    teacherId, classEntity.getId()));
            }
        } catch (Exception e) {
            log.error("Failed to send class ended notification for class {}: {}", 
                classEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification to teachers when class is starting soon
     */
    private int sendNotificationForClassStartingSoon(ClassEntity classEntity) {
        try {
            Set<Long> teacherIds = getTeacherIdsForClass(classEntity.getId());
            String className = classEntity.getName() != null ? classEntity.getName() : classEntity.getCode();
            String formattedDate = classEntity.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            int notificationsSent = 0;
            for (Long teacherId : teacherIds) {
                // Check if notification already exists
                if (!hasNotificationForClass(teacherId, classEntity.getId(), "CLASS_STARTING_SOON")) {
                    notificationService.createNotificationFromRequest(
                        org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                            .recipientId(teacherId)
                            .type(NotificationType.CLASS_REMINDER)
                            .title("Lớp học sắp bắt đầu")
                            .message(String.format("Lớp %s sẽ bắt đầu sau %d ngày (ngày %s).",
                                className, notifyDaysBeforeStart, formattedDate))
                            .priority(NotificationPriority.MEDIUM)
                            .referenceType("CLASS")
                            .referenceId(classEntity.getId())
                            .actionUrl(String.format("/teacher/classes/%d", classEntity.getId()))
                            .metadata(String.format("{\"reminderType\":\"CLASS_STARTING_SOON\",\"classId\":%d}", 
                                classEntity.getId()))
                            .build()
                    );

                    notificationsSent++;
                    logJobInfo(String.format("Sent class starting soon notification to teacher %d for class %d",
                        teacherId, classEntity.getId()));
                }
            }
            return notificationsSent;
        } catch (Exception e) {
            log.error("Failed to send class starting soon notification for class {}: {}", 
                classEntity.getId(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Send notification to teachers when class is ending soon
     */
    private int sendNotificationForClassEndingSoon(ClassEntity classEntity) {
        try {
            Set<Long> teacherIds = getTeacherIdsForClass(classEntity.getId());
            String className = classEntity.getName() != null ? classEntity.getName() : classEntity.getCode();
            String formattedDate = classEntity.getPlannedEndDate() != null
                ? classEntity.getPlannedEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "N/A";

            int notificationsSent = 0;
            for (Long teacherId : teacherIds) {
                // Check if notification already exists
                if (!hasNotificationForClass(teacherId, classEntity.getId(), "CLASS_ENDING_SOON")) {
                    notificationService.createNotificationFromRequest(
                        org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                            .recipientId(teacherId)
                            .type(NotificationType.CLASS_REMINDER)
                            .title("Lớp học sắp kết thúc")
                            .message(String.format("Lớp %s sẽ kết thúc sau %d ngày (ngày %s).",
                                className, notifyDaysBeforeEnd, formattedDate))
                            .priority(NotificationPriority.MEDIUM)
                            .referenceType("CLASS")
                            .referenceId(classEntity.getId())
                            .actionUrl(String.format("/teacher/classes/%d", classEntity.getId()))
                            .metadata(String.format("{\"reminderType\":\"CLASS_ENDING_SOON\",\"classId\":%d}", 
                                classEntity.getId()))
                            .build()
                    );

                    notificationsSent++;
                    logJobInfo(String.format("Sent class ending soon notification to teacher %d for class %d",
                        teacherId, classEntity.getId()));
                }
            }
            return notificationsSent;
        } catch (Exception e) {
            log.error("Failed to send class ending soon notification for class {}: {}", 
                classEntity.getId(), e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get all teacher IDs for a class
     */
    private Set<Long> getTeacherIdsForClass(Long classId) {
        List<TeachingSlot> teachingSlots = teachingSlotRepository
            .findBySessionClassIdAndStatus(classId, TeachingSlotStatus.SCHEDULED);
        
        return teachingSlots.stream()
            .map(slot -> slot.getTeacher().getUserAccount().getId())
            .collect(Collectors.toSet());
    }

    /**
     * Check if notification already exists for this class and teacher
     */
    private boolean hasNotificationForClass(Long teacherId, Long classId, String reminderType) {
        return notificationRepository.existsByRecipientIdAndReferenceTypeAndReferenceIdAndMetadataContaining(
            teacherId, "CLASS", classId, reminderType);
    }
}
