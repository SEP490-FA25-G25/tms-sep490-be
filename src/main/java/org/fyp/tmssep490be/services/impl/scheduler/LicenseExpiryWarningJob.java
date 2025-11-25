package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.fyp.tmssep490be.services.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to monitor VIRTUAL resource license expiry dates.
 *
 * Functionality:
 * - Monitors VIRTUAL resources (Zoom, Google Meet, etc.) with license expiry dates
 * - Three-tier alert system:
 *   1. WARNING: 30 days before expiry (configurable)
 *   2. URGENT: 7 days before expiry (configurable)
 *   3. CRITICAL: Expired today or past due
 * - Logs warnings for admin attention
 * - Ready for future notification service integration
 *
 * Runs daily at 8:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.license-expiry-warning",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class LicenseExpiryWarningJob extends BaseScheduledJob {

    private final ResourceRepository resourceRepository;
    private final CenterRepository centerRepository;
    private final EmailService emailService;

    @Value("${tms.scheduler.jobs.license-expiry-warning.warning-days:30}")
    private int warningDays;

    @Value("${tms.scheduler.jobs.license-expiry-warning.urgent-days:7}")
    private int urgentDays;

    /**
     * Monitor VIRTUAL resource license expiry and log warnings.
     * Runs daily at 8:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.license-expiry-warning.cron:0 0 8 * * ?}")
    @Transactional(readOnly = true)
    public void monitorLicenseExpiry() {
        try {
            logJobStart("LicenseExpiryWarning");

            LocalDate today = LocalDate.now();
            LocalDate warningDate = today.plusDays(warningDays);
            LocalDate urgentDate = today.plusDays(urgentDays);

            logJobInfo(String.format("Monitoring VIRTUAL resources with license expiry dates (warning: %d days, urgent: %d days)",
                warningDays, urgentDays));

            // Find all VIRTUAL resources with expiry dates
            List<Resource> virtualResources = resourceRepository
                .findByResourceTypeAndExpiryDateIsNotNull(ResourceType.VIRTUAL);

            if (virtualResources.isEmpty()) {
                logJobEnd("LicenseExpiryWarning", "No VIRTUAL resources with expiry dates found");
                return;
            }

            int criticalCount = 0;
            int urgentCount = 0;
            int warningCount = 0;
            int okCount = 0;

            for (Resource resource : virtualResources) {
                LocalDate expiryDate = resource.getExpiryDate();
                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);

                if (daysUntilExpiry < 0) {
                    // CRITICAL: Already expired
                    criticalCount++;
                    log.error("║ CRITICAL: Resource '{}' (ID: {}) EXPIRED {} days ago (expiry: {})",
                        resource.getName(), resource.getId(), Math.abs(daysUntilExpiry), expiryDate);
                    log.error("║   License Type: {}, Email: {}", resource.getLicenseType(), resource.getAccountEmail());

                } else if (daysUntilExpiry == 0) {
                    // CRITICAL: Expires today
                    criticalCount++;
                    log.error("║ CRITICAL: Resource '{}' (ID: {}) EXPIRES TODAY ({})",
                        resource.getName(), resource.getId(), expiryDate);
                    log.error("║   License Type: {}, Email: {}", resource.getLicenseType(), resource.getAccountEmail());

                } else if (daysUntilExpiry <= urgentDays) {
                    // URGENT: Expires within urgent window
                    urgentCount++;
                    log.warn("║ URGENT: Resource '{}' (ID: {}) expires in {} days (expiry: {})",
                        resource.getName(), resource.getId(), daysUntilExpiry, expiryDate);
                    log.warn("║   License Type: {}, Email: {}", resource.getLicenseType(), resource.getAccountEmail());

                } else if (daysUntilExpiry <= warningDays) {
                    // WARNING: Expires within warning window
                    warningCount++;
                    logJobWarning(String.format("Resource '%s' (ID: %d) expires in %d days (expiry: %s)",
                        resource.getName(), resource.getId(), daysUntilExpiry, expiryDate));
                    logJobInfo(String.format("  License Type: %s, Email: %s",
                        resource.getLicenseType(), resource.getAccountEmail()));

                } else {
                    // OK: More than warning days away
                    okCount++;
                }
            }

            // Summary
            String summary = String.format(
                "Total: %d | CRITICAL: %d | URGENT: %d | WARNING: %d | OK: %d",
                virtualResources.size(), criticalCount, urgentCount, warningCount, okCount
            );

            if (criticalCount > 0 || urgentCount > 0) {
                log.error("╔═══════════════════════════════════════════════════════════════╗");
                log.error("║ ACTION REQUIRED: {} resources need immediate attention", (criticalCount + urgentCount));
                log.error("╚═══════════════════════════════════════════════════════════════╝");

                // Send email notifications for critical and urgent resources
                sendLicenseExpiryEmails(virtualResources, today, urgentDate);
            }

            logJobEnd("LicenseExpiryWarning", summary);

        } catch (Exception e) {
            logJobError("LicenseExpiryWarning", e);
            throw e;
        }
    }

    /**
     * Send email notifications for license expiry warnings
     */
    private void sendLicenseExpiryEmails(List<Resource> resources, LocalDate today, LocalDate urgentDate) {
        try {
            logJobInfo("Sending license expiry email notifications...");

            int emailsSent = 0;
            for (Resource resource : resources) {
                LocalDate expiryDate = resource.getExpiryDate();
                long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);

                // Send email for critical and urgent resources
                if (daysUntilExpiry <= urgentDays) {
                    try {
                        String centerName = "TMS";
                        if (resource.getBranch() != null) {
                            centerName = resource.getBranch().getName();
                        }

                        emailService.sendLicenseExpiryWarningAsync(
                            "admin@tms.edu.vn", // Should be configured or fetched from admins
                            resource.getName(),
                            expiryDate.toString(),
                            centerName
                        );
                        emailsSent++;
                        logJobInfo("Sent license expiry warning email for resource: " + resource.getName());
                    } catch (Exception e) {
                        logJobError("Failed to send license expiry email for resource " + resource.getName(), e);
                    }
                }
            }

            logJobInfo(String.format("License expiry email notifications sent: %d", emailsSent));

        } catch (Exception e) {
            logJobError("Failed to send license expiry emails", e);
        }
    }
}
