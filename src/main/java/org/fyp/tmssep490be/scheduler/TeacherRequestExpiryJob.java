package org.fyp.tmssep490be.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically cancel expired teacher requests.
 *
 * Functionality:
 * - Cancels PENDING teacher requests after configurable days (default: 7 days) OR when session startTime has passed
 * - Cancels WAITING_CONFIRM replacement requests after configurable days (default: 7 days) OR when session startTime has passed
 * - Updates status to CANCELLED and adds expiry note
 *
 * Runs daily at 3:30 AM (configurable via application.yml)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.teacher-request-expiry",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class TeacherRequestExpiryJob extends BaseScheduledJob {

    private final TeacherRequestRepository teacherRequestRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${tms.scheduler.jobs.teacher-request-expiry.pending-expiry-days:7}")
    private int pendingExpiryDays;

    @Value("${tms.scheduler.jobs.teacher-request-expiry.waiting-confirm-expiry-days:7}")
    private int waitingConfirmExpiryDays;

    @PostConstruct
    public void expireOnStartup() {
        log.info("Server startup: Checking for expired teacher requests that need to be cancelled");
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    expireOldTeacherRequests();
                } catch (Exception e) {
                    log.error("Error expiring teacher requests on startup", e);
                    status.setRollbackOnly();
                }
            });
        } catch (Exception e) {
            log.error("Error expiring teacher requests on startup: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${tms.scheduler.jobs.teacher-request-expiry.cron:0 30 3 * * ?}")
    @Transactional
    public void expireOldTeacherRequests() {
        try {
            logJobStart("TeacherRequestExpiry");

            OffsetDateTime now = OffsetDateTime.now();
            LocalDateTime nowDateTime = LocalDateTime.now();
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
                
                // Check 2: Session startTime đã qua (ưu tiên hơn)
                LocalDateTime sessionStartDateTime = getSessionStartDateTimeForExpiry(request);
                if (sessionStartDateTime != null && sessionStartDateTime.isBefore(nowDateTime)) {
                    shouldExpire = true;
                    long hoursPassed = java.time.temporal.ChronoUnit.HOURS.between(sessionStartDateTime, nowDateTime);
                    long daysPassed = hoursPassed / 24;
                    if (daysPassed > 0) {
                        expiryReason = String.format("Tự động hủy do buổi học đã bắt đầu từ %d ngày trước (hệ thống)", daysPassed);
                    } else {
                        expiryReason = String.format("Tự động hủy do buổi học đã bắt đầu từ %d giờ trước (hệ thống)", hoursPassed);
                    }
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
                
                // Check 2: Session startTime đã qua (ưu tiên hơn)
                LocalDateTime sessionStartDateTime = getSessionStartDateTimeForExpiry(request);
                if (sessionStartDateTime != null && sessionStartDateTime.isBefore(nowDateTime)) {
                    shouldExpire = true;
                    long hoursPassed = java.time.temporal.ChronoUnit.HOURS.between(sessionStartDateTime, nowDateTime);
                    long daysPassed = hoursPassed / 24;
                    if (daysPassed > 0) {
                        expiryReason = String.format("Tự động hủy do buổi học đã bắt đầu từ %d ngày trước (hệ thống)", daysPassed);
                    } else {
                        expiryReason = String.format("Tự động hủy do buổi học đã bắt đầu từ %d giờ trước (hệ thống)", hoursPassed);
                    }
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
     * Lấy session startDateTime để check expiry.
     * Với RESCHEDULE: lấy startDateTime sớm hơn giữa session gốc và newSession
     * Với các loại khác: lấy session.date + session.startTime
     */
    private LocalDateTime getSessionStartDateTimeForExpiry(TeacherRequest request) {
        LocalDateTime originalStartDateTime = null;
        LocalDateTime newStartDateTime = null;

        // Lấy startDateTime của session gốc
        if (request.getSession() != null) {
            originalStartDateTime = getSessionStartDateTime(request.getSession());
        }

        // Với RESCHEDULE, lấy startDateTime của session mới
        if (request.getRequestType() == TeacherRequestType.RESCHEDULE) {
            if (request.getNewSession() != null) {
                newStartDateTime = getSessionStartDateTime(request.getNewSession());
            } else if (request.getNewDate() != null && request.getNewTimeSlot() != null) {
                // Nếu chưa có newSession nhưng có newDate và newTimeSlot
                LocalTime startTime = request.getNewTimeSlot().getStartTime();
                if (startTime != null) {
                    newStartDateTime = LocalDateTime.of(request.getNewDate(), startTime);
                }
            }
        }

        // Nếu có cả 2 startDateTime, lấy cái sớm hơn
        if (originalStartDateTime != null && newStartDateTime != null) {
            return originalStartDateTime.isBefore(newStartDateTime) ? originalStartDateTime : newStartDateTime;
        }

        // Nếu chỉ có 1 trong 2, trả về startDateTime đó
        return originalStartDateTime != null ? originalStartDateTime : newStartDateTime;
    }

    /**
     * Lấy LocalDateTime từ Session (date + startTime)
     */
    private LocalDateTime getSessionStartDateTime(org.fyp.tmssep490be.entities.Session session) {
        if (session == null || session.getDate() == null) {
            return null;
        }
        
        LocalTime startTime = null;
        if (session.getTimeSlotTemplate() != null) {
            startTime = session.getTimeSlotTemplate().getStartTime();
        }
        
        // Nếu không có startTime, dùng 00:00:00
        if (startTime == null) {
            startTime = LocalTime.MIN;
        }
        
        return LocalDateTime.of(session.getDate(), startTime);
    }
}

