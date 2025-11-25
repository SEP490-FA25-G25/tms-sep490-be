package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
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
 * - Expires PENDING TeacherRequests after configurable days (default: 7 days)
 * - Updates status to CANCELLED and adds expiry note
 *
 * Runs daily at 3:00 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.request-expiry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RequestExpiryJob extends BaseScheduledJob {

    private final StudentRequestRepository studentRequestRepository;
    private final TeacherRequestRepository teacherRequestRepository;

    @Value("${tms.scheduler.jobs.request-expiry.expiry-days:7}")
    private int expiryDays;

    /**
     * Expire old PENDING student requests after configured days.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.request-expiry.cron:0 0 3 * * ?}")
    @Transactional
    public void expireOldStudentRequests() {
        try {
            logJobStart("RequestExpiry: StudentRequests");

            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(expiryDays);
            logJobInfo(String.format("Expiring PENDING student requests submitted before %s (%d days ago)",
                cutoffDate, expiryDays));

            List<StudentRequest> expiredRequests = studentRequestRepository
                .findByStatusAndSubmittedAtBefore(RequestStatus.PENDING, cutoffDate);

            if (expiredRequests.isEmpty()) {
                logJobEnd("RequestExpiry: StudentRequests", "No requests to expire");
                return;
            }

            int expiredCount = 0;
            for (StudentRequest request : expiredRequests) {
                logJobInfo(String.format("Expiring student request ID: %d (submitted at: %s, type: %s)",
                    request.getId(), request.getSubmittedAt(), request.getRequestType()));

                request.setStatus(RequestStatus.CANCELLED);

                // Add expiry note
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

            logJobEnd("RequestExpiry: StudentRequests", expiredCount);

        } catch (Exception e) {
            logJobError("RequestExpiry: StudentRequests", e);
            throw e;
        }
    }

    /**
     * Expire old PENDING teacher requests after configured days.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "${tms.scheduler.jobs.request-expiry.cron:0 0 3 * * ?}")
    @Transactional
    public void expireOldTeacherRequests() {
        try {
            logJobStart("RequestExpiry: TeacherRequests");

            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(expiryDays);
            logJobInfo(String.format("Expiring PENDING teacher requests submitted before %s (%d days ago)",
                cutoffDate, expiryDays));

            List<TeacherRequest> expiredRequests = teacherRequestRepository
                .findByStatusAndSubmittedAtBefore(RequestStatus.PENDING, cutoffDate);

            if (expiredRequests.isEmpty()) {
                logJobEnd("RequestExpiry: TeacherRequests", "No requests to expire");
                return;
            }

            int expiredCount = 0;
            for (TeacherRequest request : expiredRequests) {
                logJobInfo(String.format("Expiring teacher request ID: %d (submitted at: %s, type: %s)",
                    request.getId(), request.getSubmittedAt(), request.getRequestType()));

                request.setStatus(RequestStatus.CANCELLED);

                // Add expiry note
                String expiryNote = String.format("Tự động hủy do quá %d ngày không xử lý (hệ thống)", expiryDays);
                String existingNote = request.getNote();
                if (existingNote != null && !existingNote.isEmpty()) {
                    request.setNote(existingNote + "\n" + expiryNote);
                } else {
                    request.setNote(expiryNote);
                }

                teacherRequestRepository.save(request);
                expiredCount++;
            }

            logJobEnd("RequestExpiry: TeacherRequests", expiredCount);

        } catch (Exception e) {
            logJobError("RequestExpiry: TeacherRequests", e);
            throw e;
        }
    }
}
