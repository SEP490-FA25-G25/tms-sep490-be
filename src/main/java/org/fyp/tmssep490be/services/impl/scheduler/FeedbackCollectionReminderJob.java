package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.FeedbackReminderDTO;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Job 10: Feedback Collection Reminder Job
 * Remind students to provide feedback for completed classes
 * Schedule: Mon/Wed/Fri 10:00 AM
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.feedback-collection-reminder",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class FeedbackCollectionReminderJob extends BaseScheduledJob {

    private final StudentFeedbackRepository studentFeedbackRepository;
    private final NotificationService notificationService;

    /**
     * Main scheduled method to send feedback collection reminders
     * Runs Monday, Wednesday, Friday at 10:00 AM by default
     */
    @Scheduled(cron = "${tms.scheduler.jobs.feedback-collection-reminder.cron:0 0 10 * * MON,WED,FRI}")
    @Transactional
    public void sendFeedbackCollectionReminders() {
        logJobStart("FeedbackCollectionReminderJob");

        try {
            // Find completed classes with students who haven't provided feedback
            List<FeedbackReminderDTO> pendingFeedbacks = studentFeedbackRepository
                .findPendingFeedbackReminders();

            if (pendingFeedbacks.isEmpty()) {
                logJobEnd("FeedbackCollectionReminderJob", "No pending feedback reminders found");
                return;
            }

            logJobInfo(String.format("Found %d students needing feedback reminders", pendingFeedbacks.size()));

            int remindersSent = 0;
            int duplicatesSkipped = 0;

            for (FeedbackReminderDTO reminder : pendingFeedbacks) {
                try {
                    // Check if reminder already sent recently (avoid spam)
                    boolean hasRecentReminder = notificationService.hasUserNotificationForReference(
                        reminder.getStudentId(),
                        "FEEDBACK_REMINDER",
                        reminder.getPhaseId()
                    );

                    if (hasRecentReminder) {
                        duplicatesSkipped++;
                        continue;
                    }

                    // Create feedback reminder notification
                    String reminderMessage = String.format(
                        "Vui lòng chia sẻ phản hồi về %s - %s. Phản hồi của bạn giúp chúng tôi cải thiện chất lượng đào tạo.",
                        reminder.getCourseName(), reminder.getPhaseName()
                    );

                    notificationService.createNotificationWithReference(
                        reminder.getStudentId(),
                        NotificationType.FEEDBACK_REMINDER,
                        "Nhắc nhở: Đánh giá khóa học",
                        reminderMessage,
                        "FEEDBACK_REMINDER",
                        reminder.getPhaseId()
                    );

                    remindersSent++;
                    logJobInfo(String.format("Sent feedback reminder to student %d for phase %d (%s)",
                        reminder.getStudentId(), reminder.getPhaseId(), reminder.getPhaseName()));

                } catch (Exception e) {
                    logJobWarning(String.format("Failed to send feedback reminder to student %d: %s",
                        reminder.getStudentId(), e.getMessage()));
                }
            }

            logJobEnd("FeedbackCollectionReminderJob",
                String.format("Sent %d new reminders, skipped %d duplicates (out of %d total)",
                    remindersSent, duplicatesSkipped, pendingFeedbacks.size()));

        } catch (Exception e) {
            logJobError("FeedbackCollectionReminderJob", e);
            throw e; // Re-throw to prevent silent failures
        }
    }
}