package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherAssessmentDTO;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherStudentScoreDTO;
import org.fyp.tmssep490be.dtos.teachergrade.ScoreInputDTO;
import org.fyp.tmssep490be.dtos.teachergrade.BatchScoreInputDTO;
import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.Score;
import org.fyp.tmssep490be.entities.SubjectAssessment;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.AssessmentRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.ScoreRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeacherGradeService {

    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final ClassRepository classRepository;
    private final ScoreRepository scoreRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    // Kiểm tra giáo viên có được phân công lớp hay không (dựa trên teaching slot còn hiệu lực)
    private void assertTeacherOwnsClass(Long teacherId, Long classId) {
        boolean owns = teachingSlotRepository.existsByTeacherIdAndClassEntityId(teacherId, classId);
        if (!owns) {
            throw new CustomException(ErrorCode.FORBIDDEN, "Teacher is not assigned to this class");
        }
    }

    public List<TeacherAssessmentDTO> getClassAssessments(Long teacherId, Long classId, String filter) {
        // Đảm bảo lớp tồn tại và giáo viên có quyền truy cập
        classRepository.findById(classId).orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        assertTeacherOwnsClass(teacherId, classId);

        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);
        int totalStudents = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);

        // Tính trước số bài đã chấm cho từng assessment (đếm student có gradedAt != null)
        Map<Long, Long> gradedCountMap = assessments.stream()
                .collect(Collectors.toMap(
                        Assessment::getId,
                        a -> {
                            Set<Score> scores = a.getScores();
                            if (scores == null || scores.isEmpty()) return 0L;
                            return scores.stream()
                                    .filter(s -> s.getGradedAt() != null)
                                    .map(s -> s.getStudent().getId())
                                    .distinct()
                                    .count();
                        }
                ));

        OffsetDateTime now = OffsetDateTime.now();
        return assessments.stream()
                .map(a -> {
                    SubjectAssessment sa = a.getSubjectAssessment();
                    Long gradedCount = gradedCountMap.getOrDefault(a.getId(), 0L);
                    // Lọc theo filter: upcoming, graded, overdue (mặc định all)
                    if (filter != null && !"all".equalsIgnoreCase(filter)) {
                        boolean hasGraded = gradedCount > 0;
                        boolean isUpcoming = a.getScheduledDate() != null && a.getScheduledDate().isAfter(now);
                        boolean isOverdue = a.getScheduledDate() != null && a.getScheduledDate().isBefore(now) && !hasGraded;
                        boolean include = switch (filter.toLowerCase()) {
                            case "upcoming" -> isUpcoming && !hasGraded;
                            case "graded" -> hasGraded;
                            case "overdue" -> isOverdue;
                            default -> true;
                        };
                        if (!include) return null;
                    }

                    return TeacherAssessmentDTO.builder()
                            .id(a.getId())
                            .classId(classId)
                            .courseAssessmentId(sa != null ? sa.getId() : null)
                            .name(sa != null ? sa.getName() : "Assessment " + a.getId())
                            .description(sa != null ? sa.getDescription() : null)
                            .kind(sa != null && sa.getKind() != null ? sa.getKind().name() : null)
                            .maxScore(sa != null && sa.getMaxScore() != null ? sa.getMaxScore().doubleValue() : null)
                            .durationMinutes(sa != null ? sa.getDurationMinutes() : null)
                            .scheduledDate(a.getScheduledDate())
                            .actualDate(a.getActualDate())
                            .gradedCount(gradedCount.intValue())
                            .totalStudents(totalStudents)
                            .allGraded(totalStudents > 0 && gradedCount == totalStudents)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TeacherAssessmentDTO::getScheduledDate).reversed())
                .collect(Collectors.toList());
    }

    // Kiểm tra giáo viên sở hữu assessment thông qua lớp
    private Assessment assertTeacherOwnsAssessment(Long teacherId, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ASSESSMENT_NOT_FOUND));
        Long classId = assessment.getClassEntity().getId();
        assertTeacherOwnsClass(teacherId, classId);
        return assessment;
    }

    // Lấy điểm của toàn bộ học viên trong một bài kiểm tra
    public List<TeacherStudentScoreDTO> getAssessmentScores(Long teacherId, Long assessmentId) {
        Assessment assessment = assertTeacherOwnsAssessment(teacherId, assessmentId);
        Long classId = assessment.getClassEntity().getId();
        SubjectAssessment sa = assessment.getSubjectAssessment();
        double maxScore = sa != null && sa.getMaxScore() != null ? sa.getMaxScore().doubleValue() : 100d;

        // Lấy danh sách học viên đang ENROLLED
        List<org.fyp.tmssep490be.entities.Enrollment> enrollments =
                enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);

        // Map score theo studentId để tránh N+1
        Map<Long, Score> scoreMap = assessment.getScores().stream()
                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

        return enrollments.stream()
                .map(e -> {
                    Long studentId = e.getStudent().getId();
                    Score sc = scoreMap.get(studentId);
                    Double scoreValue = sc != null && sc.getScore() != null ? sc.getScore().doubleValue() : null;
                    Double scorePercent = (scoreValue != null && maxScore > 0)
                            ? (scoreValue * 100d / maxScore)
                            : null;
                    String gradedByName = sc != null && sc.getGradedBy() != null
                            ? sc.getGradedBy().getUserAccount().getFullName()
                            : null;

                    return TeacherStudentScoreDTO.builder()
                            .scoreId(sc != null ? sc.getId() : null)
                            .studentId(studentId)
                            .studentCode(e.getStudent().getStudentCode())
                            .studentName(e.getStudent().getUserAccount().getFullName())
                            .score(scoreValue)
                            .feedback(sc != null ? sc.getFeedback() : null)
                            .gradedBy(gradedByName)
                            .gradedAt(sc != null ? sc.getGradedAt() : null)
                            .maxScore(maxScore)
                            .scorePercentage(scorePercent)
                            .isGraded(sc != null && sc.getGradedAt() != null)
                            .build();
                })
                .sorted(Comparator.comparing(TeacherStudentScoreDTO::getStudentCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    // Lấy điểm của một học viên trong một bài kiểm tra
    public TeacherStudentScoreDTO getStudentScore(Long teacherId, Long assessmentId, Long studentId) {
        Assessment assessment = assertTeacherOwnsAssessment(teacherId, assessmentId);
        SubjectAssessment sa = assessment.getSubjectAssessment();
        double maxScore = sa != null && sa.getMaxScore() != null ? sa.getMaxScore().doubleValue() : 100d;

        // Kiểm tra enrollment
        boolean enrolled = enrollmentRepository.countByClassIdAndStatus(assessment.getClassEntity().getId(), EnrollmentStatus.ENROLLED) > 0
                && enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(
                studentId,
                assessment.getClassEntity().getId(),
                List.of(EnrollmentStatus.ENROLLED));
        if (!enrolled) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Student is not enrolled in this class");
        }

        Score sc = scoreRepository.findByStudentIdAndAssessmentId(studentId, assessmentId).orElse(null);
        Double scoreValue = sc != null && sc.getScore() != null ? sc.getScore().doubleValue() : null;
        Double scorePercent = (scoreValue != null && maxScore > 0) ? (scoreValue * 100d / maxScore) : null;
        String gradedByName = sc != null && sc.getGradedBy() != null
                ? sc.getGradedBy().getUserAccount().getFullName()
                : null;

        return TeacherStudentScoreDTO.builder()
                .scoreId(sc != null ? sc.getId() : null)
                .studentId(studentId)
                .studentCode(sc != null ? sc.getStudent().getStudentCode() : null)
                .studentName(sc != null ? sc.getStudent().getUserAccount().getFullName() : null)
                .score(scoreValue)
                .feedback(sc != null ? sc.getFeedback() : null)
                .gradedBy(gradedByName)
                .gradedAt(sc != null ? sc.getGradedAt() : null)
                .maxScore(maxScore)
                .scorePercentage(scorePercent)
                .isGraded(sc != null && sc.getGradedAt() != null)
                .build();
    }

    // Lưu/ cập nhật điểm cho một học viên
    @Transactional // cần ghi DB
    public TeacherStudentScoreDTO saveOrUpdateScore(Long teacherId, Long assessmentId, ScoreInputDTO scoreInput) {
        Assessment assessment = assertTeacherOwnsAssessment(teacherId, assessmentId);
        SubjectAssessment sa = assessment.getSubjectAssessment();
        double maxScore = sa != null && sa.getMaxScore() != null ? sa.getMaxScore().doubleValue() : 100d;

        var student = studentRepository.findById(scoreInput.getStudentId())
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        // Kiểm tra enrollment ENROLLED
        boolean enrolled = enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(
                student.getId(), assessment.getClassEntity().getId(), List.of(EnrollmentStatus.ENROLLED));
        if (!enrolled) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Student is not enrolled in this class");
        }

        // Validate điểm không vượt quá maxScore
        if (scoreInput.getScore() != null && scoreInput.getScore() > maxScore) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Score exceeds maxScore");
        }

        Score score = scoreRepository.findByStudentIdAndAssessmentId(student.getId(), assessmentId)
                .orElseGet(() -> Score.builder()
                        .assessment(assessment)
                        .student(student)
                        .build());

        OffsetDateTime now = OffsetDateTime.now();

        score.setScore(scoreInput.getScore() != null ? java.math.BigDecimal.valueOf(scoreInput.getScore()) : null);
        score.setFeedback(scoreInput.getFeedback());
        score.setGradedBy(teacherRepository.findById(teacherId).orElse(null));
        score.setGradedAt(now);

        // Set timestamps thủ công vì entity chưa bật auditing
        if (score.getId() == null) {
            score.setCreatedAt(now);
        }
        score.setUpdatedAt(now);

        Score saved = scoreRepository.save(score);

        Double scorePercent = (saved.getScore() != null && maxScore > 0)
                ? saved.getScore().doubleValue() * 100d / maxScore
                : null;
        String gradedByName = saved.getGradedBy() != null
                ? saved.getGradedBy().getUserAccount().getFullName()
                : null;

        return TeacherStudentScoreDTO.builder()
                .scoreId(saved.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentName(student.getUserAccount().getFullName())
                .score(saved.getScore() != null ? saved.getScore().doubleValue() : null)
                .feedback(saved.getFeedback())
                .gradedBy(gradedByName)
                .gradedAt(saved.getGradedAt())
                .maxScore(maxScore)
                .scorePercentage(scorePercent)
                .isGraded(saved.getGradedAt() != null)
                .build();
    }

    // Lưu/cập nhật điểm hàng loạt
    @Transactional // cần ghi DB
    public List<TeacherStudentScoreDTO> batchSaveOrUpdateScores(Long teacherId, Long assessmentId, BatchScoreInputDTO batchInput) {
        if (batchInput == null || batchInput.getScores() == null || batchInput.getScores().isEmpty()) {
            return List.of();
        }
        return batchInput.getScores().stream()
                .map(s -> saveOrUpdateScore(teacherId, assessmentId, s))
                .collect(Collectors.toList());
    }
}

