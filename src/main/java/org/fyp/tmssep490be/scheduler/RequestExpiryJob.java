package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically expire old PENDING requests.
 *
 * Functionality:
 * - Expires PENDING StudentRequests after configurable days (default: 7 days)
 * - Updates status to CANCELLED and adds expiry note
 *
 * Runs daily at 3:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.request-expiry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RequestExpiryJob extends BaseScheduledJob {

    private final StudentRequestRepository studentRequestRepository;

    @Value("${tms.scheduler.jobs.request-expiry.expiry-days:7}")
    private int expiryDays;

    @Scheduled(cron = "${tms.scheduler.jobs.request-expiry.cron:0 0 3 * * ?}")
    @Transactional
    public void expireOldStudentRequests() {
        try {

            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(expiryDays);

            List<StudentRequest> expiredRequests = studentRequestRepository
                .findByStatusAndSubmittedAtBefore(RequestStatus.PENDING, cutoffDate);

            if (expiredRequests.isEmpty()) {
                return;
            }

            int expiredCount = 0;
            for (StudentRequest request : expiredRequests) {

                request.setStatus(RequestStatus.CANCELLED);

                String expiryNote = String.format("Tự động hủy do quá %d ngày không xử lý (hệ thống)", expiryDays);
                String existingNote = request.getNote();
                if (existingNote != null && !existingNote.isEmpty()) {
                    request.setNote(existingNote + "\n" + expiryNote);
                } else {
                    request.setNote(expiryNote);
                }

                studentRequestRepository.save(request);
                expiredCount++;
            }

        } catch (Exception e) {
            logJobError("RequestExpiry: StudentRequests", e);
            throw e;
        }
    }
}
