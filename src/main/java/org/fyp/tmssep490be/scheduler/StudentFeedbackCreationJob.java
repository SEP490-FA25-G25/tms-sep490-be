package org.fyp.tmssep490be.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentfeedback.StudentFeedbackCreationCandidateDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.StudentFeedback;
import org.fyp.tmssep490be.entities.SubjectPhase;
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
import java.util.stream.Collectors;

// Job tạo feedback placeholder cho sinh viên sau khi phase kết thúc. Chạy hằng ngày lúc 01:00
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

    private List<StudentFeedbackCreationCandidateDTO> mapToStudentFeedbackCreationCandidateDTO(List<Object[]> rawData) {
        return rawData.stream()
            .map(row -> new StudentFeedbackCreationCandidateDTO(
                (Long) row[0],          // studentId
                (String) row[1],        // studentName
                (String) row[2],        // studentEmail
                (Long) row[3],          // classId
                (String) row[4],        // classCode
                (String) row[5],        // className
                (Long) row[6],          // phaseId
                (String) row[7],        // phaseName
                (String) row[8],        // subjectName
                row[9] != null ? (LocalDate) row[9] : null  // lastSessionDate
            ))
            .collect(Collectors.toList());
    }

    @Scheduled(cron = "${tms.scheduler.jobs.student-feedback-creation.cron:0 0 1 * * ?}")
    @Transactional
    public void generateFeedbackPlaceholders() {
        logJobStart("StudentFeedbackCreationJob");

        LocalDate targetDate = LocalDate.now().minusDays(1);
        List<StudentFeedbackCreationCandidateDTO> candidates = mapToStudentFeedbackCreationCandidateDTO(
            studentFeedbackRepository.findFeedbackCreationCandidatesRawData(targetDate));

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
                    .phase(SubjectPhase.builder().id(candidate.getPhaseId()).build())
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
        try {
            notificationService.createFeedbackReminderNotification(
                candidate.getStudentId(),
                candidate.getPhaseName(),
                candidate.getSubjectName(),
                candidate.getClassName()
            );
            
            logJobInfo(String.format(
                "Gửi notification FEEDBACK_REMINDER cho student=%d class=%d phase=%d",
                candidate.getStudentId(), candidate.getClassId(), candidate.getPhaseId()
            ));
        } catch (Exception e) {
            logJobWarning(String.format(
                "Lỗi khi gửi notification cho student=%d: %s",
                candidate.getStudentId(), e.getMessage()
            ));
        }
    }

    private void sendReminderEmail(StudentFeedbackCreationCandidateDTO candidate) {
        if (candidate.getStudentEmail() == null || candidate.getStudentEmail().isBlank()) {
            return;
        }

        try {
            emailService.sendFeedbackReminderAsync(
                candidate.getStudentEmail(),
                candidate.getStudentName(),
                candidate.getClassName()
            );
        } catch (Exception e) {
            logJobWarning(String.format(
                "Lỗi khi gửi email cho student=%d: %s",
                candidate.getStudentId(), e.getMessage()
            ));
        }
    }
}
