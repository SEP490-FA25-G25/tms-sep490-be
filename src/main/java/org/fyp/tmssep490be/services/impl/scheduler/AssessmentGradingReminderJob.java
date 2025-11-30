package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.NotificationPriority;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.repositories.AssessmentRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.NotificationRepository;
import org.fyp.tmssep490be.repositories.ScoreRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled job to remind teachers about assessment grading deadlines
 * 
 * Checks for:
 * 1. Assessments with deadline approaching (X days before deadline)
 * 2. Assessments with deadline passed (overdue)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.assessment-grading-reminder",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class AssessmentGradingReminderJob extends BaseScheduledJob {

    private final AssessmentRepository assessmentRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScoreRepository scoreRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Value("${tms.scheduler.jobs.assessment-grading-reminder.grading-deadline-days:7}")
    private int gradingDeadlineDays; // Days after scheduledDate to grade

    @Value("${tms.scheduler.jobs.assessment-grading-reminder.warning-days-before-deadline:3}")
    private int warningDaysBeforeDeadline;

    /**
     * Main scheduled method to check and send grading reminders
     * Runs daily at 8:00 AM by default
     */
    @Scheduled(cron = "${tms.scheduler.jobs.assessment-grading-reminder.cron:0 0 8 * * ?}")
    @Transactional
    public void checkAndSendGradingReminders() {
        logJobStart("AssessmentGradingReminderJob");

        try {
            OffsetDateTime now = OffsetDateTime.now();
            int notificationsSent = 0;

            // 1. Check assessments with deadline approaching
            notificationsSent += checkAssessmentsDueSoon(now);

            // 2. Check assessments with deadline passed
            notificationsSent += checkAssessmentsOverdue(now);

            logJobEnd("AssessmentGradingReminderJob",
                String.format("Sent %d grading reminder notifications", notificationsSent));

        } catch (Exception e) {
            logJobError("AssessmentGradingReminderJob", e);
            throw e; // Re-throw to prevent silent failures
        }
    }

    /**
     * Check assessments with deadline approaching (X days before deadline)
     */
    private int checkAssessmentsDueSoon(OffsetDateTime now) {
        OffsetDateTime deadlineStart = now.plusDays(warningDaysBeforeDeadline);
        OffsetDateTime deadlineEnd = deadlineStart.plusDays(1);

        // Find assessments where scheduledDate + gradingDeadlineDays falls within warning window
        List<Assessment> assessments = assessmentRepository.findAll().stream()
            .filter(assessment -> {
                OffsetDateTime gradingDeadline = assessment.getScheduledDate()
                    .plusDays(gradingDeadlineDays);
                return !gradingDeadline.isBefore(deadlineStart) && 
                       !gradingDeadline.isAfter(deadlineEnd) &&
                       !isAllGraded(assessment);
            })
            .collect(Collectors.toList());

        int notificationsSent = 0;
        for (Assessment assessment : assessments) {
            OffsetDateTime gradingDeadline = assessment.getScheduledDate()
                .plusDays(gradingDeadlineDays);
            
            // Get teachers for this assessment's class
            Set<Long> teacherIds = getTeacherIdsForClass(assessment.getClassEntity().getId());

            for (Long teacherId : teacherIds) {
                // Check if notification already exists
                if (!hasNotificationForAssessment(teacherId, assessment.getId(), "GRADING_DUE_SOON")) {
                    String assessmentName = assessment.getCourseAssessment() != null
                        ? assessment.getCourseAssessment().getName()
                        : "Bài tập " + assessment.getId();
                    String className = assessment.getClassEntity().getName();
                    String formattedDeadline = gradingDeadline.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    notificationService.createNotificationFromRequest(
                        org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                            .recipientId(teacherId)
                            .type(NotificationType.ASSIGNMENT_DEADLINE)
                            .title("Nhắc nhở: Deadline chấm bài tập sắp đến")
                            .message(String.format("Deadline chấm bài tập %s cho lớp %s còn %d ngày (hạn: %s).",
                                assessmentName, className, warningDaysBeforeDeadline, formattedDeadline))
                            .priority(NotificationPriority.HIGH)
                            .referenceType("ASSESSMENT")
                            .referenceId(assessment.getId())
                            .actionUrl(String.format("/teacher/classes/%d/grades?assessmentId=%d",
                                assessment.getClassEntity().getId(), assessment.getId()))
                            .metadata(String.format("{\"reminderType\":\"GRADING_DUE_SOON\",\"assessmentId\":%d}", 
                                assessment.getId()))
                            .build()
                    );

                    notificationsSent++;
                    logJobInfo(String.format("Sent grading due soon notification to teacher %d for assessment %d",
                        teacherId, assessment.getId()));
                }
            }
        }

        return notificationsSent;
    }

    /**
     * Check assessments with deadline passed (overdue)
     */
    private int checkAssessmentsOverdue(OffsetDateTime now) {
        // Find assessments where scheduledDate + gradingDeadlineDays has passed
        List<Assessment> assessments = assessmentRepository.findAll().stream()
            .filter(assessment -> {
                OffsetDateTime gradingDeadline = assessment.getScheduledDate()
                    .plusDays(gradingDeadlineDays);
                return gradingDeadline.isBefore(now) && !isAllGraded(assessment);
            })
            .collect(Collectors.toList());

        int notificationsSent = 0;
        for (Assessment assessment : assessments) {
            OffsetDateTime gradingDeadline = assessment.getScheduledDate()
                .plusDays(gradingDeadlineDays);
            
            // Get teachers for this assessment's class
            Set<Long> teacherIds = getTeacherIdsForClass(assessment.getClassEntity().getId());

            for (Long teacherId : teacherIds) {
                // Check if notification already exists (only send once per day)
                if (!hasNotificationForAssessment(teacherId, assessment.getId(), "GRADING_OVERDUE")) {
                    String assessmentName = assessment.getCourseAssessment() != null
                        ? assessment.getCourseAssessment().getName()
                        : "Bài tập " + assessment.getId();
                    String className = assessment.getClassEntity().getName();
                    String formattedDeadline = gradingDeadline.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                    notificationService.createNotificationFromRequest(
                        org.fyp.tmssep490be.dtos.notification.NotificationRequestDTO.builder()
                            .recipientId(teacherId)
                            .type(NotificationType.ASSIGNMENT_DEADLINE)
                            .title("Nhắc nhở: Deadline chấm bài tập đã quá hạn")
                            .message(String.format("Deadline chấm bài tập %s cho lớp %s đã quá hạn (hạn: %s).",
                                assessmentName, className, formattedDeadline))
                            .priority(NotificationPriority.URGENT)
                            .referenceType("ASSESSMENT")
                            .referenceId(assessment.getId())
                            .actionUrl(String.format("/teacher/classes/%d/grades?assessmentId=%d",
                                assessment.getClassEntity().getId(), assessment.getId()))
                            .metadata(String.format("{\"reminderType\":\"GRADING_OVERDUE\",\"assessmentId\":%d}", 
                                assessment.getId()))
                            .build()
                    );

                    notificationsSent++;
                    logJobInfo(String.format("Sent grading overdue notification to teacher %d for assessment %d",
                        teacherId, assessment.getId()));
                }
            }
        }

        return notificationsSent;
    }

    /**
     * Check if all students have been graded for an assessment
     */
    private boolean isAllGraded(Assessment assessment) {
        Long classId = assessment.getClassEntity().getId();
        int totalStudents = enrollmentRepository.countByClassIdAndStatus(
            classId, org.fyp.tmssep490be.entities.enums.EnrollmentStatus.ENROLLED);
        
        if (totalStudents == 0) {
            return true; // No students to grade
        }

        long gradedCount = scoreRepository.countByAssessmentIdAndGradedAtIsNotNull(assessment.getId());
        return gradedCount >= totalStudents;
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
     * Check if notification already exists for this assessment and teacher
     */
    private boolean hasNotificationForAssessment(Long teacherId, Long assessmentId, String reminderType) {
        return notificationRepository.existsByRecipientIdAndReferenceTypeAndReferenceIdAndMetadataContaining(
            teacherId, "ASSESSMENT", assessmentId, reminderType);
    }
}

