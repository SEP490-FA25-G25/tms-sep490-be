package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically cancel expired teacher requests.
 *
 * Functionality:
 * - Cancels PENDING teacher requests after configurable days (default: 7 days) OR when session date has passed
 * - Cancels WAITING_CONFIRM replacement requests after configurable days (default: 3 days) OR when session date has passed
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
            LocalDate today = LocalDate.now();
            OffsetDateTime pendingCutoffDate = now.minusDays(pendingExpiryDays);
            OffsetDateTime waitingConfirmCutoffDate = now.minusDays(waitingConfirmExpiryDays);

            // Expire PENDING requests
            List<TeacherRequest> pendingRequests = teacherRequestRepository
                .findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING);

            int expiredPendingCount = 0;
            for (TeacherRequest request : pendingRequests) {
                boolean shouldExpire = false;
                String expiryReason = null;

                // Check 1: Quá thời gian submit (theo config)
                if (request.getSubmittedAt() != null 
                    && request.getSubmittedAt().isBefore(pendingCutoffDate)) {
                    shouldExpire = true;
                    expiryReason = String.format("Tự động hủy do quá %d ngày không xử lý (hệ thống)", pendingExpiryDays);
                }
                
                // Check 2: Session date đã qua (ưu tiên hơn)
                LocalDate sessionDate = getSessionDateForExpiry(request);
                if (sessionDate != null && sessionDate.isBefore(today)) {
                    shouldExpire = true;
                    long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(sessionDate, today);
                    expiryReason = String.format("Tự động hủy do buổi học đã qua %d ngày (hệ thống)", daysPassed);
                }

                if (shouldExpire) {
                    request.setStatus(RequestStatus.CANCELLED);
                    String existingNote = request.getNote();
                    if (existingNote != null && !existingNote.isEmpty()) {
                        request.setNote(existingNote + "\n" + expiryReason);
                    } else {
                        request.setNote(expiryReason);
                    }
                    teacherRequestRepository.save(request);
                    expiredPendingCount++;
                }
            }

            // Expire WAITING_CONFIRM requests (replacement teacher didn't confirm in time)
            List<TeacherRequest> waitingConfirmRequests = teacherRequestRepository
                .findByStatusOrderBySubmittedAtDesc(RequestStatus.WAITING_CONFIRM);

            int expiredWaitingConfirmCount = 0;
            for (TeacherRequest request : waitingConfirmRequests) {
                boolean shouldExpire = false;
                String expiryReason = null;

                // Check 1: Quá thời gian submit (theo config)
                if (request.getSubmittedAt() != null 
                    && request.getSubmittedAt().isBefore(waitingConfirmCutoffDate)) {
                    shouldExpire = true;
                    expiryReason = String.format("Tự động hủy do giáo viên thay thế không xác nhận trong %d ngày (hệ thống)", waitingConfirmExpiryDays);
                }
                
                // Check 2: Session date đã qua (ưu tiên hơn)
                LocalDate sessionDate = getSessionDateForExpiry(request);
                if (sessionDate != null && sessionDate.isBefore(today)) {
                    shouldExpire = true;
                    long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(sessionDate, today);
                    expiryReason = String.format("Tự động hủy do buổi học đã qua %d ngày (hệ thống)", daysPassed);
                }

                if (shouldExpire) {
                    request.setStatus(RequestStatus.CANCELLED);
                    String existingNote = request.getNote();
                    if (existingNote != null && !existingNote.isEmpty()) {
                        request.setNote(existingNote + "\n" + expiryReason);
                    } else {
                        request.setNote(expiryReason);
                    }
                    teacherRequestRepository.save(request);
                    expiredWaitingConfirmCount++;
                }
            }

            int totalExpired = expiredPendingCount + expiredWaitingConfirmCount;
            if (totalExpired > 0) {
                logJobInfo(String.format("Cancelled %d PENDING requests", expiredPendingCount));
                logJobInfo(String.format("Cancelled %d WAITING_CONFIRM requests", expiredWaitingConfirmCount));
            }

            logJobEnd("TeacherRequestExpiry", totalExpired);

        } catch (Exception e) {
            logJobError("TeacherRequestExpiry", e);
            throw e;
        }
    }

    /**
     * Lấy session date để check expiry.
     * Với RESCHEDULE: lấy ngày sớm hơn giữa session gốc và newSession (ngày gần hơn với hôm nay)
     * Với các loại khác: lấy session.date
     */
    private LocalDate getSessionDateForExpiry(TeacherRequest request) {
        LocalDate originalDate = null;
        LocalDate newDate = null;

        // Lấy date của session gốc
        if (request.getSession() != null) {
            originalDate = request.getSession().getDate();
        }

        // Với RESCHEDULE, lấy date của session mới
        if (request.getRequestType() == TeacherRequestType.RESCHEDULE) {
            if (request.getNewSession() != null) {
                newDate = request.getNewSession().getDate();
            } else if (request.getNewDate() != null) {
                newDate = request.getNewDate();
            }
        }

        // Nếu có cả 2 date, lấy ngày sớm hơn (gần hơn với hôm nay)
        if (originalDate != null && newDate != null) {
            return originalDate.isBefore(newDate) ? originalDate : newDate;
        }

        // Nếu chỉ có 1 trong 2, trả về date đó
        return originalDate != null ? originalDate : newDate;
    }
}

