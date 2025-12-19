package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled job to automatically expire PENDING requests based on session deadlines.
 *
 * Functionality:
 * - ABSENCE: Cancel if target session date has passed
 * - MAKEUP: Cancel if makeup session date has passed
 * 
 * Note: TRANSFER requests are NOT handled here because:
 * - Students cannot self-submit transfer requests
 * - Only AA can create transfer requests on-behalf (auto-approved immediately)
 * - Therefore, transfer requests never stay in PENDING status
 *
 * Logic: Requests should be cancelled when the relevant session deadline expires,
 * NOT based on submission date.
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

    @Scheduled(cron = "${tms.scheduler.jobs.request-expiry.cron:0 0 3 * * ?}")
    @Transactional
    public void expireOldStudentRequests() {
        try {
            LocalDate today = LocalDate.now();

            // Find all PENDING requests
            List<StudentRequest> pendingRequests = studentRequestRepository
                .findByStatus(RequestStatus.PENDING, org.springframework.data.domain.Sort.unsorted());

            if (pendingRequests.isEmpty()) {
                log.info("RequestExpiryJob: No pending requests to process");
                return;
            }

            int expiredCount = 0;
            int absenceExpired = 0;
            int makeupExpired = 0;

            for (StudentRequest request : pendingRequests) {
                boolean shouldExpire = false;
                String expiryReason = "";

                // Determine expiry based on request type and relevant session deadline
                // Note: TRANSFER requests are not handled because they're always auto-approved by AA
                if (request.getRequestType() == StudentRequestType.ABSENCE) {
                    // ABSENCE: Cancel if target session has passed
                    Session targetSession = request.getTargetSession();
                    if (targetSession != null && targetSession.getDate() != null) {
                        if (targetSession.getDate().isBefore(today)) {
                            shouldExpire = true;
                            expiryReason = String.format("Buổi học đã qua (%s) - tự động hủy đơn xin nghỉ", 
                                targetSession.getDate());
                            absenceExpired++;
                        }
                    }

                } else if (request.getRequestType() == StudentRequestType.MAKEUP) {
                    // MAKEUP: Cancel if makeup session has passed
                    Session makeupSession = request.getMakeupSession();
                    if (makeupSession != null && makeupSession.getDate() != null) {
                        if (makeupSession.getDate().isBefore(today)) {
                            shouldExpire = true;
                            expiryReason = String.format("Buổi học bù đã qua (%s) - tự động hủy đơn học bù", 
                                makeupSession.getDate());
                            makeupExpired++;
                        }
                    }
                }

                // Cancel the request if expired
                if (shouldExpire) {
                    request.setStatus(RequestStatus.CANCELLED);
                    
                    String existingNote = request.getNote();
                    if (existingNote != null && !existingNote.isEmpty()) {
                        request.setNote(existingNote + "\n---\n" + expiryReason);
                    } else {
                        request.setNote(expiryReason);
                    }

                    studentRequestRepository.save(request);
                    expiredCount++;
                    
                    log.debug("Expired request {}: {} - {}", 
                        request.getId(), request.getRequestType(), expiryReason);
                }
            }

            if (expiredCount > 0) {
                log.info("RequestExpiryJob: Cancelled {} expired requests " +
                        "(ABSENCE: {}, MAKEUP: {})", 
                        expiredCount, absenceExpired, makeupExpired);
            } else {
                log.info("RequestExpiryJob: No expired requests found");
            }

        } catch (Exception e) {
            logJobError("RequestExpiry: StudentRequests", e);
            throw e;
        }
    }
}
