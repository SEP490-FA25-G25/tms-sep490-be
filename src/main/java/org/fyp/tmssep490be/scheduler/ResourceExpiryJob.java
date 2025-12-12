package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled job to automatically handle resource expiry.
 *
 * Functionality:
 * - Disables resources (sets status to INACTIVE) when expiryDate has passed
 * - Logs warnings for resources expiring soon (configurable days, default: 30 days)
 *
 * Runs daily at 4:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.resource-expiry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ResourceExpiryJob extends BaseScheduledJob {

    private final ResourceRepository resourceRepository;

    @Value("${tms.scheduler.jobs.resource-expiry.warning-days:30}")
    private int warningDays;

    @Scheduled(cron = "${tms.scheduler.jobs.resource-expiry.cron:0 0 4 * * ?}")
    @Transactional
    public void handleResourceExpiry() {
        try {
            logJobStart("ResourceExpiry");

            LocalDate today = LocalDate.now();
            LocalDate warningThreshold = today.plusDays(warningDays);
            int disabledCount = 0;
            int warningCount = 0;

            // Find all active resources with expiry date
            List<Resource> allResources = resourceRepository.findAll();
            List<Resource> resourcesWithExpiry = allResources.stream()
                .filter(r -> r.getExpiryDate() != null && r.getStatus() == ResourceStatus.ACTIVE)
                .toList();

            for (Resource resource : resourcesWithExpiry) {
                LocalDate expiryDate = resource.getExpiryDate();

                // Disable expired resources
                if (expiryDate.isBefore(today)) {
                    resource.setStatus(ResourceStatus.INACTIVE);
                    resourceRepository.save(resource);
                    logJobInfo(String.format("Disabled expired resource '%s' (ID: %d, code: %s, expiryDate: %s)",
                        resource.getName(), resource.getId(), resource.getCode(), expiryDate));
                    disabledCount++;
                }
                // Warn about resources expiring soon
                else if (!expiryDate.isAfter(warningThreshold) && !expiryDate.isBefore(today)) {
                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
                    logJobWarning(String.format("Resource '%s' (ID: %d, code: %s) will expire in %d days (expiryDate: %s)",
                        resource.getName(), resource.getId(), resource.getCode(), daysUntilExpiry, expiryDate));
                    warningCount++;
                }
            }

            if (disabledCount > 0 || warningCount > 0) {
                logJobInfo(String.format("Disabled %d expired resources", disabledCount));
                logJobInfo(String.format("Found %d resources expiring soon (within %d days)", warningCount, warningDays));
            }

            logJobEnd("ResourceExpiry", disabledCount);

        } catch (Exception e) {
            logJobError("ResourceExpiry", e);
            throw e;
        }
    }
}

