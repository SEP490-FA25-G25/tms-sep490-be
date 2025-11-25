package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job 7: Pending Request Reminder Job
 * Remind Academic Affairs about pending requests
 * Schedule: Weekdays 9:00 AM
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.pending-request-reminder",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PendingRequestReminderJob extends BaseScheduledJob {

    private final StudentRequestRepository studentRequestRepository;
    private final TeacherRequestRepository teacherRequestRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    @Value("${tms.scheduler.jobs.pending-request-reminder.reminder-threshold-days:3}")
    private int reminderThresholdDays;

    /**
     * Main scheduled method to send pending request reminders
     * Runs weekdays at 9:00 AM by default
     */
    @Scheduled(cron = "${tms.scheduler.jobs.pending-request-reminder.cron:0 0 9 * * MON-FRI}")
    @Transactional
    public void sendPendingRequestReminders() {
        logJobStart("PendingRequestReminderJob");

        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(reminderThresholdDays);

            // Find old pending student requests
            List<StudentRequest> oldStudentRequests = studentRequestRepository
                .findByStatusAndSubmittedAtBefore(RequestStatus.PENDING, cutoffDate);

            // Find old pending teacher requests
            List<TeacherRequest> oldTeacherRequests = teacherRequestRepository
                .findByStatusAndSubmittedAtBefore(RequestStatus.PENDING, cutoffDate);

            // Get Academic Affairs users
            List<UserAccount> academicAffairsUsers = userAccountRepository
                .findUsersByRole("ROLE_ACADEMIC_AFFAIR");

            if (academicAffairsUsers.isEmpty()) {
                logJobWarning("No Academic Affairs users found to send reminders to");
                logJobEnd("PendingRequestReminderJob", "No AA users found");
                return;
            }

            if (oldStudentRequests.isEmpty() && oldTeacherRequests.isEmpty()) {
                logJobEnd("PendingRequestReminderJob", "No old pending requests found");
                return;
            }

            logJobInfo(String.format("Found %d student requests and %d teacher requests older than %d days",
                oldStudentRequests.size(), oldTeacherRequests.size(), reminderThresholdDays));

            int notificationsSent = 0;

            // Send notifications to Academic Affairs staff
            for (UserAccount aaUser : academicAffairsUsers) {
                try {
                    // Send student request reminder if any exist
                    if (!oldStudentRequests.isEmpty()) {
                        notificationService.createNotification(
                            aaUser.getId(),
                            NotificationType.REQUEST_APPROVAL,
                            "Nhắc nhở: Yêu cầu học viên chờ duyệt",
                            String.format("Có %d yêu cầu học viên đang chờ duyệt quá %d ngày. Vui lòng xem xét và xử lý.",
                                oldStudentRequests.size(), reminderThresholdDays)
                        );
                        notificationsSent++;
                    }

                    // Send teacher request reminder if any exist
                    if (!oldTeacherRequests.isEmpty()) {
                        notificationService.createNotification(
                            aaUser.getId(),
                            NotificationType.REQUEST_APPROVAL,
                            "Nhắc nhở: Yêu cầu giáo viên chờ duyệt",
                            String.format("Có %d yêu cầu giáo viên đang chờ duyệt quá %d ngày. Vui lòng xem xét và xử lý.",
                                oldTeacherRequests.size(), reminderThresholdDays)
                        );
                        notificationsSent++;
                    }

                } catch (Exception e) {
                    logJobWarning(String.format("Failed to send notification to AA user %d: %s",
                        aaUser.getId(), e.getMessage()));
                }
            }

            logJobEnd("PendingRequestReminderJob",
                String.format("Sent %d notifications to %d AA users for %d student + %d teacher requests",
                    notificationsSent, academicAffairsUsers.size(), oldStudentRequests.size(), oldTeacherRequests.size()));

        } catch (Exception e) {
            logJobError("PendingRequestReminderJob", e);
            throw e; // Re-throw to prevent silent failures
        }
    }
}