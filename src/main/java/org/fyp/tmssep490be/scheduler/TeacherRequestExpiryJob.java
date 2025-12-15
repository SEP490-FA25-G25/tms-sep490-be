package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically expire old teacher requests.
 *
 * Functionality:
 * - Expires PENDING teacher requests after configurable days (default: 7 days)
 * - Expires WAITING_CONFIRM replacement requests after configurable days (default: 3 days)
 * - Updates status to CANCELLED and adds expiry note
 *
 * Runs daily at 3:30 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.teacher-request-expiry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TeacherRequestExpiryJob extends BaseScheduledJob {

    private final TeacherRequestRepository teacherRequestRepository;

    @Value("${tms.scheduler.jobs.teacher-request-expiry.pending-expiry-days:7}")
    private int pendingExpiryDays;

    @Value("${tms.scheduler.jobs.teacher-request-expiry.waiting-confirm-expiry-days:3}")
    private int waitingConfirmExpiryDays;

    @Scheduled(cron = "${tms.scheduler.jobs.teacher-request-expiry.cron:0 30 3 * * ?}")
    @Transactional
    public void expireOldTeacherRequests() {
        try {
            logJobStart("TeacherRequestExpiry");

            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime pendingCutoffDate = now.minusDays(pendingExpiryDays);
            OffsetDateTime waitingConfirmCutoffDate = now.minusDays(waitingConfirmExpiryDays);

            // Expire PENDING requests
            List<TeacherRequest> expiredPendingRequests = teacherRequestRepository
                .findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING)
                .stream()
                .filter(request -> request.getSubmittedAt() != null 
                    && request.getSubmittedAt().isBefore(pendingCutoffDate))
                .toList();

            int expiredPendingCount = 0;
            for (TeacherRequest request : expiredPendingRequests) {
                request.setStatus(RequestStatus.CANCELLED);
                String expiryNote = String.format("Tự động hủy do quá %d ngày không xử lý (hệ thống)", pendingExpiryDays);
                String existingNote = request.getNote();
                if (existingNote != null && !existingNote.isEmpty()) {
                    request.setNote(existingNote + "\n" + expiryNote);
                } else {
                    request.setNote(expiryNote);
                }
                teacherRequestRepository.save(request);
                expiredPendingCount++;
            }

            // Expire WAITING_CONFIRM requests (replacement teacher didn't confirm in time)
            List<TeacherRequest> expiredWaitingConfirmRequests = teacherRequestRepository
                .findByStatusOrderBySubmittedAtDesc(RequestStatus.WAITING_CONFIRM)
                .stream()
                .filter(request -> request.getSubmittedAt() != null 
                    && request.getSubmittedAt().isBefore(waitingConfirmCutoffDate))
                .toList();

            int expiredWaitingConfirmCount = 0;
            for (TeacherRequest request : expiredWaitingConfirmRequests) {
                request.setStatus(RequestStatus.CANCELLED);
                String expiryNote = String.format("Tự động hủy do giáo viên thay thế không xác nhận trong %d ngày (hệ thống)", waitingConfirmExpiryDays);
                String existingNote = request.getNote();
                if (existingNote != null && !existingNote.isEmpty()) {
                    request.setNote(existingNote + "\n" + expiryNote);
                } else {
                    request.setNote(expiryNote);
                }
                teacherRequestRepository.save(request);
                expiredWaitingConfirmCount++;
            }

            int totalExpired = expiredPendingCount + expiredWaitingConfirmCount;
            if (totalExpired > 0) {
                logJobInfo(String.format("Expired %d PENDING requests", expiredPendingCount));
                logJobInfo(String.format("Expired %d WAITING_CONFIRM requests", expiredWaitingConfirmCount));
            }

            logJobEnd("TeacherRequestExpiry", totalExpired);

        } catch (Exception e) {
            logJobError("TeacherRequestExpiry", e);
            throw e;
        }
    }
}

