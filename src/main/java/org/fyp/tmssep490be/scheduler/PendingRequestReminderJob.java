package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.EmailService;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

// Job: Pending Request Reminder
// Nhắc nhở Academic Affairs về các yêu cầu (student/teacher) ở trạng thái PENDING quá nhiều ngày.
//
// - Tìm các StudentRequest/TeacherRequest có status = PENDING và submittedAt < now - reminderThresholdDays
// - Gửi notification kiểu REQUEST cho tất cả user role ACADEMIC_AFFAIR
// - Gửi email tổng hợp cho từng Academic staff
//
// Lịch chạy mặc định: 9:00 sáng các ngày trong tuần (có thể override bằng cấu hình).
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
    private final EmailService emailService;

    @Value("${tms.scheduler.jobs.pending-request-reminder.reminder-threshold-days:3}")
    private int reminderThresholdDays;

    // Gửi nhắc nhở cho Academic Affairs về các yêu cầu chờ duyệt quá lâu.
    @Scheduled(cron = "${tms.scheduler.jobs.pending-request-reminder.cron:0 0 9 * * MON-FRI}")
    @Transactional
    public void sendPendingRequestReminders() {
        String jobName = "PendingRequestReminderJob";
        logJobStart(jobName);

        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(reminderThresholdDays);

            // Tìm các student request PENDING quá hạn
            List<StudentRequest> oldStudentRequests = studentRequestRepository
                    .findByStatusAndSubmittedAtBefore(RequestStatus.PENDING, cutoffDate);

            // Tìm các teacher request PENDING quá hạn (lọc trong memory để tái sử dụng query sẵn có)
            List<TeacherRequest> oldTeacherRequests = teacherRequestRepository
                    .findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING)
                    .stream()
                    .filter(request -> request.getSubmittedAt() != null
                            && request.getSubmittedAt().isBefore(cutoffDate))
                    .toList();

            // Lấy danh sách Academic Affairs
            List<UserAccount> academicAffairsUsers = userAccountRepository.findUsersByRole("ACADEMIC_AFFAIR");

            if (academicAffairsUsers.isEmpty()) {
                logJobWarning("Không tìm thấy tài khoản Academic Affairs nào để gửi nhắc nhở");
                logJobEnd(jobName, "No Academic Affairs users found");
                return;
            }

            if (oldStudentRequests.isEmpty() && oldTeacherRequests.isEmpty()) {
                logJobEnd(jobName, "No old pending student/teacher requests found");
                return;
            }

            logJobInfo(String.format(
                    "Found %d student requests and %d teacher requests older than %d days",
                    oldStudentRequests.size(), oldTeacherRequests.size(), reminderThresholdDays));

            int notificationsSent = 0;

            // Gửi notification cho từng Academic Affairs
            for (UserAccount aaUser : academicAffairsUsers) {
                try {
                    // Nhắc nhở yêu cầu học viên nếu có
                    if (!oldStudentRequests.isEmpty()) {
                        notificationService.createNotification(
                                aaUser.getId(),
                                NotificationType.REQUEST,
                                "Nhắc nhở: Yêu cầu học viên chờ duyệt",
                                String.format(
                                        "Có %d yêu cầu học viên đang ở trạng thái PENDING quá %d ngày. Vui lòng xem xét và xử lý.",
                                        oldStudentRequests.size(), reminderThresholdDays
                                )
                        );
                        notificationsSent++;
                    }

                    // Nhắc nhở yêu cầu giáo viên nếu có
                    if (!oldTeacherRequests.isEmpty()) {
                        notificationService.createNotification(
                                aaUser.getId(),
                                NotificationType.REQUEST,
                                "Nhắc nhở: Yêu cầu giáo viên chờ duyệt",
                                String.format(
                                        "Có %d yêu cầu giáo viên đang ở trạng thái PENDING quá %d ngày. Vui lòng xem xét và xử lý.",
                                        oldTeacherRequests.size(), reminderThresholdDays
                                )
                        );
                        notificationsSent++;
                    }
                } catch (Exception e) {
                    logJobWarning(String.format(
                            "Không thể gửi notification nhắc nhở cho Academic Affairs user %d: %s",
                            aaUser.getId(), e.getMessage()));
                }
            }

            // Gửi email tổng hợp cho Academic Affairs
            sendPendingRequestEmails(
                    academicAffairsUsers,
                    oldStudentRequests.size(),
                    oldTeacherRequests.size(),
                    cutoffDate
            );

            logJobEnd(jobName,
                    String.format(
                            "Sent %d notifications to %d AA users for %d student + %d teacher requests",
                            notificationsSent, academicAffairsUsers.size(),
                            oldStudentRequests.size(), oldTeacherRequests.size()
                    )
            );

        } catch (Exception e) {
            logJobError(jobName, e);
            throw e;
        }
    }

    private void sendPendingRequestEmails(
            List<UserAccount> academicAffairsUsers,
            int studentRequestCount,
            int teacherRequestCount,
            OffsetDateTime cutoffDate
    ) {
        try {
            int totalPending = studentRequestCount + teacherRequestCount;
            if (totalPending == 0) {
                return;
            }

            String oldestDateStr = cutoffDate.toLocalDate().toString();
            int emailsSent = 0;

            for (UserAccount aaUser : academicAffairsUsers) {
                try {
                    emailService.sendPendingRequestReminderAsync(
                            aaUser.getEmail(),
                            totalPending,
                            oldestDateStr
                    );
                    emailsSent++;
                    logJobInfo("Đã gửi email nhắc nhở pending request tới: " + aaUser.getEmail());
                } catch (Exception e) {
                    logJobError("PendingRequestReminderJob - send email to " + aaUser.getEmail(), e);
                }
            }

            logJobInfo(String.format("Pending request reminder emails sent: %d", emailsSent));
        } catch (Exception e) {
            logJobError("PendingRequestReminderJob - sendPendingRequestEmails", e);
        }
    }
}


