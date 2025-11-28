package org.fyp.tmssep490be.services.impl.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.StudentFeedbackCreationCandidateDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.CoursePhase;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.StudentFeedback;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
import org.fyp.tmssep490be.services.EmailService;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Job tạo feedback placeholder cho sinh viên sau khi phase kết thúc.
 * Chạy hằng ngày lúc 01:00 (có thể cấu hình).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "tms.scheduler.jobs.student-feedback-creation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class StudentFeedbackCreationJob extends BaseScheduledJob {

    private final StudentFeedbackRepository studentFeedbackRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Scheduled(cron = "${tms.scheduler.jobs.student-feedback-creation.cron:0 0 1 * * ?}")
    @Transactional
    public void generateFeedbackPlaceholders() {
        logJobStart("StudentFeedbackCreationJob");

        LocalDate targetDate = LocalDate.now().minusDays(1);
        List<StudentFeedbackCreationCandidateDTO> candidates =
            studentFeedbackRepository.findFeedbackCreationCandidates(targetDate);

        if (candidates.isEmpty()) {
            logJobEnd("StudentFeedbackCreationJob", "Không có phase kết thúc cần tạo feedback");
            return;
        }

        int created = 0;
        int skipped = 0;

        for (StudentFeedbackCreationCandidateDTO candidate : candidates) {
            try {
                StudentFeedback feedback = StudentFeedback.builder()
                    .student(Student.builder().id(candidate.getStudentId()).build())
                    .classEntity(ClassEntity.builder().id(candidate.getClassId()).build())
                    .phase(CoursePhase.builder().id(candidate.getPhaseId()).build())
                    .isFeedback(false)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

                studentFeedbackRepository.save(feedback);
                created++;

                sendReminderNotification(candidate);
                sendReminderEmail(candidate);

            } catch (DataIntegrityViolationException e) {
                skipped++;
                logJobWarning(String.format(
                    "Bỏ qua do trùng feedback student=%d, class=%d, phase=%d",
                    candidate.getStudentId(), candidate.getClassId(), candidate.getPhaseId()));
            } catch (Exception e) {
                skipped++;
                logJobWarning(String.format(
                    "Lỗi khi tạo feedback cho student=%d, class=%d, phase=%d: %s",
                    candidate.getStudentId(), candidate.getClassId(), candidate.getPhaseId(), e.getMessage()));
            }
        }

        logJobEnd("StudentFeedbackCreationJob",
            String.format("Tạo mới %d feedback, bỏ qua %d (tổng %d ứng viên)",
                created, skipped, candidates.size()));
    }

    private void sendReminderNotification(StudentFeedbackCreationCandidateDTO candidate) {
        String referenceType = "FEEDBACK_REMINDER_" + candidate.getClassId() + "_" + candidate.getPhaseId();

        boolean alreadySent = notificationService.hasUserNotificationForReference(
            candidate.getStudentId(),
            referenceType,
            candidate.getPhaseId()
        );

        if (alreadySent) {
            return;
        }

        String title = "Nhắc nhở: Đánh giá sau phase " + candidate.getPhaseName();
        String message = String.format(
            "Vui lòng hoàn thành phản hồi cho khóa %s - lớp %s. Phản hồi giúp cải thiện chất lượng đào tạo.",
            candidate.getCourseName(),
            candidate.getClassName()
        );

        logJobInfo(String.format(
            "Gửi notification FEEDBACK_REMINDER cho student=%d class=%d phase=%d (refType=%s, refId=%d)",
            candidate.getStudentId(), candidate.getClassId(), candidate.getPhaseId(), referenceType, candidate.getPhaseId()
        ));

        notificationService.createNotificationWithReference(
            candidate.getStudentId(),
            NotificationType.FEEDBACK_REMINDER,
            title,
            message,
            referenceType,
            candidate.getPhaseId()
        );
    }

    private void sendReminderEmail(StudentFeedbackCreationCandidateDTO candidate) {
        if (candidate.getStudentEmail() == null || candidate.getStudentEmail().isBlank()) {
            return;
        }

        emailService.sendFeedbackReminderAsync(
            candidate.getStudentEmail(),
            candidate.getStudentName(),
            candidate.getClassName()
        );
    }
}
