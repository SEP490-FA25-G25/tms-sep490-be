package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.entities.enums.RegistrationStatus;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.TeacherClassRegistrationRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Job: Unassigned Class Alert
 * 
 * Alerts Academic Affairs about classes that:
 * 1. Are approved (SCHEDULED status)
 * 2. Have no assigned teacher
 * 3. Registration has closed (registrationCloseDate has passed)
 * OR
 * 4. Are in emergency state (startDate within 2 days, no registration opened)
 * 
 * Schedule: Every morning at 8:00 AM (configurable)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "tms.scheduler.jobs.unassigned-class-alert", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UnassignedClassAlertJob extends BaseScheduledJob {

    private final ClassRepository classRepository;
    private final TeacherClassRegistrationRepository registrationRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    @Value("${tms.scheduler.jobs.unassigned-class-alert.emergency-days:2}")
    private int emergencyDays;

    @Scheduled(cron = "${tms.scheduler.jobs.unassigned-class-alert.cron:0 0 8 * * ?}")
    @Transactional(readOnly = true)
    public void checkUnassignedClasses() {
        String jobName = "UnassignedClassAlertJob";
        logJobStart(jobName);

        try {
            OffsetDateTime now = OffsetDateTime.now();
            LocalDate today = LocalDate.now();

            // Find classes that are SCHEDULED, APPROVED, have no teacher assigned
            List<ClassEntity> unassignedClasses = classRepository.findAll().stream()
                    .filter(c -> c.getStatus() == ClassStatus.SCHEDULED)
                    .filter(c -> c.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .filter(c -> c.getAssignedTeacher() == null)
                    .filter(c -> c.getStartDate() != null && !today.isAfter(c.getStartDate())) // Not started yet
                    .collect(Collectors.toList());

            if (unassignedClasses.isEmpty()) {
                logJobEnd(jobName, "No unassigned classes found");
                return;
            }

            // Categorize classes
            List<ClassEntity> expiredRegistrationClasses = unassignedClasses.stream()
                    .filter(c -> c.getRegistrationCloseDate() != null && now.isAfter(c.getRegistrationCloseDate()))
                    .toList();

            List<ClassEntity> emergencyClasses = unassignedClasses.stream()
                    .filter(c -> {
                        // Emergency: startDate within emergencyDays, no registration opened
                        if (c.getRegistrationCloseDate() != null) {
                            return false; // Registration was opened, not emergency
                        }
                        LocalDate latestCloseDate = c.getStartDate().minusDays(emergencyDays);
                        return !today.isBefore(latestCloseDate);
                    })
                    .toList();

            if (expiredRegistrationClasses.isEmpty() && emergencyClasses.isEmpty()) {
                logJobEnd(jobName, "No urgent unassigned classes found");
                return;
            }

            logJobInfo(String.format(
                    "Found %d expired registration classes and %d emergency classes",
                    expiredRegistrationClasses.size(), emergencyClasses.size()));

            // Group classes by branch
            Map<Long, List<ClassEntity>> expiredByBranch = expiredRegistrationClasses.stream()
                    .collect(Collectors.groupingBy(c -> c.getBranch().getId()));

            Map<Long, List<ClassEntity>> emergencyByBranch = emergencyClasses.stream()
                    .collect(Collectors.groupingBy(c -> c.getBranch().getId()));

            int notificationsSent = 0;

            // Send notifications to AA users per branch
            for (Long branchId : expiredByBranch.keySet()) {
                List<ClassEntity> branchClasses = expiredByBranch.get(branchId);
                List<UserAccount> aaUsers = userAccountRepository.findByRoleCodeAndBranches(
                        "ACADEMIC_AFFAIR", List.of(branchId));

                if (aaUsers.isEmpty()) {
                    logJobWarning("No AA users found for branch " + branchId);
                    continue;
                }

                // Build class list with pending registration count
                String classListStr = branchClasses.stream()
                        .limit(5)
                        .map(c -> {
                            long pendingCount = registrationRepository.countByClassEntityIdAndStatus(
                                    c.getId(), RegistrationStatus.PENDING);
                            if (pendingCount > 0) {
                                return String.format("• %s (%s) - %d GV đã đăng ký",
                                        c.getName(), c.getCode(), pendingCount);
                            } else {
                                return String.format("• %s (%s) - Không có đăng ký",
                                        c.getName(), c.getCode());
                            }
                        })
                        .collect(Collectors.joining("\n"));

                if (branchClasses.size() > 5) {
                    classListStr += String.format("\n... và %d lớp khác", branchClasses.size() - 5);
                }

                // Count how many classes have pending registrations
                long classesWithRegistrations = branchClasses.stream()
                        .filter(c -> registrationRepository.countByClassEntityIdAndStatus(
                                c.getId(), RegistrationStatus.PENDING) > 0)
                        .count();

                String title = String.format("⏰ %d lớp hết hạn đăng ký chưa gán GV", branchClasses.size());
                String message;
                if (classesWithRegistrations > 0) {
                    message = String.format(
                            "Các lớp sau đã hết hạn đăng ký:\n%s\n\n" +
                                    "Có %d lớp có GV đăng ký, vui lòng vào 'Chờ gán GV' để duyệt.\n" +
                                    "Các lớp không có đăng ký cần gán GV trực tiếp.",
                            classListStr, classesWithRegistrations);
                } else {
                    message = String.format(
                            "Các lớp sau đã hết hạn đăng ký nhưng không có GV nào đăng ký:\n%s\n\n" +
                                    "Vui lòng vào 'Duyệt đăng ký dạy lớp' → 'Chờ gán GV' để gán GV trực tiếp.",
                            classListStr);
                }

                List<Long> recipientIds = aaUsers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotifications(
                        recipientIds,
                        NotificationType.REQUEST,
                        title,
                        message);

                notificationsSent += aaUsers.size();
            }

            // Send notifications for emergency classes
            for (Long branchId : emergencyByBranch.keySet()) {
                List<ClassEntity> branchClasses = emergencyByBranch.get(branchId);
                List<UserAccount> aaUsers = userAccountRepository.findByRoleCodeAndBranches(
                        "ACADEMIC_AFFAIR", List.of(branchId));

                if (aaUsers.isEmpty()) {
                    logJobWarning("No AA users found for branch " + branchId);
                    continue;
                }

                String classListStr = branchClasses.stream()
                        .limit(5)
                        .map(c -> String.format("• %s (%s) - Khai giảng: %s",
                                c.getName(), c.getCode(), c.getStartDate()))
                        .collect(Collectors.joining("\n"));

                if (branchClasses.size() > 5) {
                    classListStr += String.format("\n... và %d lớp khác", branchClasses.size() - 5);
                }

                String title = String.format("⚠️ %d lớp KHẨN CẤP cần gán GV ngay", branchClasses.size());
                String message = String.format(
                        "Các lớp sau sắp khai giảng nhưng chưa có giáo viên:\n%s\n\n" +
                                "Do không còn đủ thời gian mở đăng ký (tối thiểu %d ngày trước ngày bắt đầu), " +
                                "bạn cần gán giáo viên trực tiếp.\n" +
                                "Vào 'Duyệt đăng ký dạy lớp' → 'Chờ mở đăng ký' để gán GV khẩn cấp.",
                        classListStr, emergencyDays);

                List<Long> recipientIds = aaUsers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotifications(
                        recipientIds,
                        NotificationType.REQUEST,
                        title,
                        message);

                notificationsSent += aaUsers.size();
            }

            logJobEnd(jobName, String.format(
                    "Sent %d notifications for %d expired + %d emergency classes",
                    notificationsSent,
                    expiredRegistrationClasses.size(),
                    emergencyClasses.size()));

        } catch (Exception e) {
            logJobError(jobName, e);
            throw e;
        }
    }
}
