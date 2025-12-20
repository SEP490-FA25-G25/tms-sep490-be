package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherAssessmentDTO;
import org.fyp.tmssep490be.dtos.teachergrade.TeacherStudentScoreDTO;
import org.fyp.tmssep490be.dtos.teachergrade.ScoreInputDTO;
import org.fyp.tmssep490be.dtos.teachergrade.BatchScoreInputDTO;
import org.fyp.tmssep490be.dtos.teachergrade.GradebookDTO;
import org.fyp.tmssep490be.dtos.teachergrade.GradebookAssessmentDTO;
import org.fyp.tmssep490be.dtos.teachergrade.GradebookStudentDTO;
import org.fyp.tmssep490be.dtos.teachergrade.GradebookStudentScoreDTO;
import org.fyp.tmssep490be.dtos.teachergrade.ClassGradesSummaryDTO;
import org.fyp.tmssep490be.dtos.teachergrade.StudentGradeSummaryDTO;
import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Score;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.SubjectAssessment;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.NotificationType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.AssessmentRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.ScoreRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        private final StudentSessionRepository studentSessionRepository;
        private final NotificationService notificationService;
        private final EmailService emailService;

        // Tính % điểm trên thang 100
        private Double calculatePercentage(Double score, double maxScore) {
                if (score == null || maxScore <= 0)
                        return null;
                return (score * 100d / maxScore);
        }

        // Tính nhãn bucket phân bố điểm
        private String bucketLabel(double percentage) {
                if (percentage < 40)
                        return "0-39";
                if (percentage < 60)
                        return "40-59";
                if (percentage < 80)
                        return "60-79";
                return "80-100";
        }

        // Kiểm tra giáo viên có được phân công lớp hay không (dựa trên teaching slot
        // còn hiệu lực)
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

                // Tính trước số bài đã chấm cho từng assessment (đếm student có gradedAt !=
                // null)
                Map<Long, Long> gradedCountMap = assessments.stream()
                                .collect(Collectors.toMap(
                                                Assessment::getId,
                                                a -> {
                                                        Set<Score> scores = a.getScores();
                                                        if (scores == null || scores.isEmpty())
                                                                return 0L;
                                                        return scores.stream()
                                                                        .filter(s -> s.getGradedAt() != null)
                                                                        .map(s -> s.getStudent().getId())
                                                                        .distinct()
                                                                        .count();
                                                }));

                OffsetDateTime now = OffsetDateTime.now();
                return assessments.stream()
                                .map(a -> {
                                        SubjectAssessment sa = a.getSubjectAssessment();
                                        Long gradedCount = gradedCountMap.getOrDefault(a.getId(), 0L);
                                        // Lọc theo filter: upcoming, graded, overdue (mặc định all)
                                        if (filter != null && !"all".equalsIgnoreCase(filter)) {
                                                boolean hasGraded = gradedCount > 0;
                                                boolean isUpcoming = a.getScheduledDate() != null
                                                                && a.getScheduledDate().isAfter(now);
                                                boolean isOverdue = a.getScheduledDate() != null
                                                                && a.getScheduledDate().isBefore(now)
                                                                && !hasGraded;
                                                boolean include = switch (filter.toLowerCase()) {
                                                        case "upcoming" -> isUpcoming && !hasGraded;
                                                        case "graded" -> hasGraded;
                                                        case "overdue" -> isOverdue;
                                                        default -> true;
                                                };
                                                if (!include)
                                                        return null;
                                        }

                                        return TeacherAssessmentDTO.builder()
                                                        .id(a.getId())
                                                        .classId(classId)
                                                        .subjectAssessmentId(sa != null ? sa.getId() : null)
                                                        .name(sa != null ? sa.getName() : "Assessment " + a.getId())
                                                        .description(sa != null ? sa.getDescription() : null)
                                                        .kind(sa != null && sa.getKind() != null ? sa.getKind().name()
                                                                        : null)
                                                        .maxScore(sa != null && sa.getMaxScore() != null
                                                                        ? sa.getMaxScore().doubleValue()
                                                                        : null)
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

        // Lấy gradebook (ma trận điểm) của lớp
        public GradebookDTO getClassGradebook(Long teacherId, Long classId) {
                classRepository.findById(classId).orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
                assertTeacherOwnsClass(teacherId, classId);

                // Assessments
                List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId)
                                .stream()
                                .sorted(Comparator.comparing(Assessment::getScheduledDate))
                                .toList();

                List<GradebookAssessmentDTO> assessmentDTOs = assessments.stream()
                                .map(a -> {
                                        SubjectAssessment sa = a.getSubjectAssessment();
                                        return GradebookAssessmentDTO.builder()
                                                        .id(a.getId())
                                                        .name(sa != null ? sa.getName() : "Assessment " + a.getId())
                                                        .kind(sa != null && sa.getKind() != null ? sa.getKind().name()
                                                                        : null)
                                                        .maxScore(sa != null && sa.getMaxScore() != null
                                                                        ? sa.getMaxScore().doubleValue()
                                                                        : null)
                                                        .durationMinutes(sa != null ? sa.getDurationMinutes() : null)
                                                        .scheduledDate(a.getScheduledDate())
                                                        .build();
                                })
                                .toList();

                // Enrollments
                List<org.fyp.tmssep490be.entities.Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(
                                classId,
                                EnrollmentStatus.ENROLLED);

                // Precompute score map: assessmentId -> (studentId -> score)
                Map<Long, Map<Long, Score>> assessmentScoreMap = assessments.stream()
                                .collect(Collectors.toMap(
                                                Assessment::getId,
                                                a -> {
                                                        Set<Score> scores = a.getScores();
                                                        Stream<Score> scoreStream = scores != null ? scores.stream()
                                                                        : Stream.empty();
                                                        return scoreStream.collect(Collectors.toMap(
                                                                        s -> s.getStudent().getId(),
                                                                        s -> s,
                                                                        (s1, s2) -> s1));
                                                }));

                List<GradebookStudentDTO> students = enrollments.stream()
                                .map(e -> {
                                        Long studentId = e.getStudent().getId();
                                        List<GradebookStudentScoreDTO> scores = assessments.stream()
                                                        .map(a -> {
                                                                Map<Long, Score> m = assessmentScoreMap
                                                                                .getOrDefault(a.getId(), Map.of());
                                                                Score sc = m.get(studentId);
                                                                SubjectAssessment sa = a.getSubjectAssessment();
                                                                double maxScore = sa != null && sa.getMaxScore() != null
                                                                                ? sa.getMaxScore().doubleValue()
                                                                                : 100d;
                                                                Double scoreValue = sc != null && sc.getScore() != null
                                                                                ? sc.getScore().doubleValue()
                                                                                : null;
                                                                Double percent = calculatePercentage(scoreValue,
                                                                                maxScore);
                                                                String gradedByName = sc != null
                                                                                && sc.getGradedBy() != null
                                                                                                ? sc.getGradedBy()
                                                                                                                .getUserAccount()
                                                                                                                .getFullName()
                                                                                                : null;
                                                                return GradebookStudentScoreDTO.builder()
                                                                                .assessmentId(a.getId())
                                                                                .score(scoreValue)
                                                                                .scorePercentage(percent)
                                                                                .feedback(sc != null ? sc.getFeedback()
                                                                                                : null)
                                                                                .gradedBy(gradedByName)
                                                                                .gradedAt(sc != null ? sc.getGradedAt()
                                                                                                : null)
                                                                                .maxScore(maxScore)
                                                                                .build();
                                                        })
                                                        .toList();

                                        // Average & counts
                                        List<Double> percents = scores.stream()
                                                        .map(GradebookStudentScoreDTO::getScorePercentage)
                                                        .filter(Objects::nonNull)
                                                        .toList();
                                        Double avg = percents.isEmpty() ? null
                                                        : percents.stream().mapToDouble(Double::doubleValue).average()
                                                                        .orElse(0);
                                        int gradedCount = (int) percents.size();

                                        // Chuyên cần: tính ngay, không cần chờ môn học kết thúc
                                        // Lấy enrollment để xác định timeline (joinSessionId, leftSessionId)
                                        org.fyp.tmssep490be.entities.Enrollment enrollment = e;
                                        
                                        List<StudentSession> studentSessions = studentSessionRepository
                                                        .findByStudentIdAndClassEntityId(studentId, classId)
                                                        .stream()
                                                        .filter(ss -> !Boolean.TRUE.equals(ss.getIsMakeup())) // Bỏ qua học bù
                                                        .filter(ss -> {
                                                                // Filter CANCELLED sessions
                                                                Session session = ss.getSession();
                                                                if (session == null || session.getStatus() == org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED) {
                                                                        return false;
                                                                }
                                                                
                                                                // Filter theo enrollment timeline
                                                                Long sessionId = session.getId();
                                                                Long joinId = enrollment.getJoinSessionId();
                                                                Long leftId = enrollment.getLeftSessionId();
                                                                
                                                                if (enrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
                                                                        return leftId == null || sessionId <= leftId;
                                                                } else if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                                                                        return joinId == null || sessionId >= joinId;
                                                                } else if (enrollment.getStatus() == org.fyp.tmssep490be.entities.enums.EnrollmentStatus.COMPLETED) {
                                                                        return joinId == null || sessionId >= joinId;
                                                                }
                                                                return true;
                                                        })
                                                        .toList();

                                        // Map thông tin học bù
                                        // Key: originalSessionId, Value: Boolean (true = có PRESENT, false = chỉ có ABSENT)
                                        List<Long> sessionIds = studentSessions.stream()
                                                        .map(ss -> ss.getSession().getId())
                                                        .distinct()
                                                        .toList();
                                        Map<Long, Boolean> makeupCompletedMap = new HashMap<>();
                                        Set<Long> makeupAttemptedSessions = new HashSet<>();
                                        if (!sessionIds.isEmpty()) {
                                                studentSessionRepository
                                                                .findMakeupSessionsByOriginalSessionIds(sessionIds)
                                                                .stream()
                                                                .filter(ss -> ss.getStudent().getId().equals(studentId))
                                                                .forEach(ss -> {
                                                                        Session originalSession = ss
                                                                                        .getOriginalSession();
                                                                        if (originalSession != null && originalSession
                                                                                        .getId() != null) {
                                                                                Long origId = originalSession.getId();
                                                                                makeupAttemptedSessions.add(origId);
                                                                                makeupCompletedMap.merge(
                                                                                                origId,
                                                                                                ss.getAttendanceStatus() == AttendanceStatus.PRESENT,
                                                                                                (oldVal, newVal) -> oldVal
                                                                                                                || newVal);
                                                                        }
                                                                });
                                        }

                                        LocalDateTime now = LocalDateTime.now();
                                        long present = 0;      // Số buổi có mặt (display)
                                        long ratePresent = 0;  // Tử số
                                        long rateTotal = 0;    // Mẫu số

                                        for (StudentSession ss : studentSessions) {
                                                AttendanceStatus status = ss.getAttendanceStatus();
                                                Session session = ss.getSession();

                                                if (status == AttendanceStatus.PRESENT) {
                                                        present++;
                                                        ratePresent++;
                                                        rateTotal++;
                                                } else if (status == AttendanceStatus.ABSENT) {
                                                        rateTotal++;
                                                } else if (status == AttendanceStatus.EXCUSED) {
                                                        Long sessionId = session.getId();
                                                        boolean hasMakeupAttempt = makeupAttemptedSessions.contains(sessionId);
                                                        boolean hasMakeupCompleted = makeupCompletedMap.getOrDefault(sessionId, false);

                                                        if (hasMakeupCompleted) {
                                                                // EXCUSED + makeup PRESENT -> tính như PRESENT
                                                                ratePresent++;
                                                                rateTotal++;
                                                        } else if (hasMakeupAttempt) {
                                                                // EXCUSED + makeup ABSENT -> tính vào mẫu số
                                                                rateTotal++;
                                                        } else {
                                                                // Check deadline
                                                                LocalDate sessionDate = session.getDate();
                                                                LocalDateTime sessionEndDateTime;
                                                                if (session.getTimeSlotTemplate() != null
                                                                                && session.getTimeSlotTemplate().getEndTime() != null) {
                                                                        java.time.LocalTime endTime = session.getTimeSlotTemplate().getEndTime();
                                                                        sessionEndDateTime = LocalDateTime.of(sessionDate, endTime);
                                                                } else {
                                                                        sessionEndDateTime = LocalDateTime.of(sessionDate, java.time.LocalTime.MAX);
                                                                }

                                                                if (now.isAfter(sessionEndDateTime)) {
                                                                        // Qua deadline + không makeup -> tính vào mẫu số
                                                                        rateTotal++;
                                                                }
                                                        }
                                                }
                                        }

                                        Double attendanceRate = rateTotal > 0 ? (ratePresent * 100d / rateTotal) : null;
                                        Double attendanceScore = attendanceRate;

                                        return GradebookStudentDTO.builder()
                                                        .studentId(studentId)
                                                        .studentCode(e.getStudent().getStudentCode())
                                                        .studentName(e.getStudent().getUserAccount().getFullName())
                                                        .scores(scores)
                                                        .averageScore(avg)
                                                        .gradedCount(gradedCount)
                                                        .totalAssessments(assessments.size())
                                                        .attendedSessions((int) present)
                                                        .totalSessions((int) rateTotal)
                                                        .attendanceRate(attendanceRate)
                                                        .attendanceScore(attendanceScore)
                                                        .attendanceFinalized(true)
                                                        .build();
                                })
                                .sorted(Comparator.comparing(GradebookStudentDTO::getStudentCode,
                                                Comparator.nullsLast(String::compareTo)))
                                .toList();

                var classInfo = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                return GradebookDTO.builder()
                                .classId(classId)
                                .className(classInfo.getName())
                                .classCode(classInfo.getCode())
                                .assessments(assessmentDTOs)
                                .students(students)
                                .build();
        }

        // Lấy tổng quan điểm số (summary)
        public ClassGradesSummaryDTO getClassGradesSummary(Long teacherId, Long classId) {
                classRepository.findById(classId).orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
                assertTeacherOwnsClass(teacherId, classId);

                List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);
                List<org.fyp.tmssep490be.entities.Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(
                                classId,
                                EnrollmentStatus.ENROLLED);

                if (assessments.isEmpty() || enrollments.isEmpty()) {
                        return ClassGradesSummaryDTO.builder()
                                        .classId(classId)
                                        .className(classRepository.findById(classId).map(c -> c.getName()).orElse(null))
                                        .classCode(classRepository.findById(classId).map(c -> c.getCode()).orElse(null))
                                        .totalAssessments(assessments.size())
                                        .totalStudents(enrollments.size())
                                        .scoreDistribution(Map.of())
                                        .topStudents(List.of())
                                        .bottomStudents(List.of())
                                        .build();
                }

                // Tính normalized scores (thang 100) cho từng score
                List<Double> allPercents = assessments.stream()
                                .flatMap(a -> a.getScores().stream().map(sc -> {
                                        SubjectAssessment sa = a.getSubjectAssessment();
                                        double maxScore = sa != null && sa.getMaxScore() != null
                                                        ? sa.getMaxScore().doubleValue()
                                                        : 100d;
                                        Double scoreValue = sc.getScore() != null ? sc.getScore().doubleValue() : null;
                                        return calculatePercentage(scoreValue, maxScore);
                                }))
                                .filter(Objects::nonNull)
                                .toList();

                Double avg = allPercents.isEmpty() ? null
                                : allPercents.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                Double max = allPercents.isEmpty() ? null
                                : allPercents.stream().mapToDouble(Double::doubleValue).max().orElse(0);
                Double min = allPercents.isEmpty() ? null
                                : allPercents.stream().mapToDouble(Double::doubleValue).min().orElse(0);

                // Phân bố điểm theo bucket
                Map<String, Integer> distribution = allPercents.stream()
                                .collect(Collectors.groupingBy(this::bucketLabel,
                                                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

                // Tính average theo học viên
                Map<Long, List<Double>> studentPercents = assessments.stream()
                                .flatMap(a -> a.getScores().stream().map(sc -> {
                                        SubjectAssessment sa = a.getSubjectAssessment();
                                        double maxScore = sa != null && sa.getMaxScore() != null
                                                        ? sa.getMaxScore().doubleValue()
                                                        : 100d;
                                        Double scoreValue = sc.getScore() != null ? sc.getScore().doubleValue() : null;
                                        Double percent = calculatePercentage(scoreValue, maxScore);
                                        return percent != null ? Map.entry(sc.getStudent().getId(), percent) : null;
                                }))
                                .filter(Objects::nonNull)
                                .collect(Collectors.groupingBy(
                                                Map.Entry::getKey,
                                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

                List<StudentGradeSummaryDTO> studentSummaries = enrollments.stream()
                                .map(e -> {
                                        Long sid = e.getStudent().getId();
                                        List<Double> percents = studentPercents.getOrDefault(sid, List.of());
                                        Double sAvg = percents.isEmpty() ? null
                                                        : percents.stream().mapToDouble(Double::doubleValue).average()
                                                                        .orElse(0);
                                        int gradedCount = percents.size();
                                        return StudentGradeSummaryDTO.builder()
                                                        .studentId(sid)
                                                        .studentCode(e.getStudent().getStudentCode())
                                                        .studentName(e.getStudent().getUserAccount().getFullName())
                                                        .averageScore(sAvg)
                                                        .gradedCount(gradedCount)
                                                        .build();
                                })
                                .sorted(Comparator.comparing(StudentGradeSummaryDTO::getStudentCode,
                                                Comparator.nullsLast(String::compareTo)))
                                .toList();

                // Top/bottom theo averageScore
                List<StudentGradeSummaryDTO> sortedByAvg = studentSummaries.stream()
                                .filter(s -> s.getAverageScore() != null)
                                .sorted(Comparator.comparing(StudentGradeSummaryDTO::getAverageScore).reversed())
                                .toList();
                List<StudentGradeSummaryDTO> top5 = sortedByAvg.stream().limit(5).toList();
                List<StudentGradeSummaryDTO> bottom5 = sortedByAvg.isEmpty()
                                ? List.of()
                                : sortedByAvg.stream()
                                                .sorted(Comparator.comparing(StudentGradeSummaryDTO::getAverageScore))
                                                .limit(5)
                                                .toList();

                var classInfo = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                return ClassGradesSummaryDTO.builder()
                                .classId(classId)
                                .className(classInfo.getName())
                                .classCode(classInfo.getCode())
                                .totalAssessments(assessments.size())
                                .totalStudents(enrollments.size())
                                .averageScore(avg)
                                .highestScore(max)
                                .lowestScore(min)
                                .scoreDistribution(distribution)
                                .topStudents(top5)
                                .bottomStudents(bottom5)
                                .build();
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
                List<org.fyp.tmssep490be.entities.Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(
                                classId,
                                EnrollmentStatus.ENROLLED);

                // Map score theo studentId để tránh N+1
                Map<Long, Score> scoreMap = assessment.getScores().stream()
                                .collect(Collectors.toMap(s -> s.getStudent().getId(), s -> s));

                return enrollments.stream()
                                .map(e -> {
                                        Long studentId = e.getStudent().getId();
                                        Score sc = scoreMap.get(studentId);
                                        Double scoreValue = sc != null && sc.getScore() != null
                                                        ? sc.getScore().doubleValue()
                                                        : null;
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
                                .sorted(Comparator.comparing(TeacherStudentScoreDTO::getStudentCode,
                                                Comparator.nullsLast(String::compareTo)))
                                .collect(Collectors.toList());
        }

        // Lấy điểm của một học viên trong một bài kiểm tra
        public TeacherStudentScoreDTO getStudentScore(Long teacherId, Long assessmentId, Long studentId) {
                Assessment assessment = assertTeacherOwnsAssessment(teacherId, assessmentId);
                SubjectAssessment sa = assessment.getSubjectAssessment();
                double maxScore = sa != null && sa.getMaxScore() != null ? sa.getMaxScore().doubleValue() : 100d;

                // Kiểm tra enrollment
                boolean enrolled = enrollmentRepository.countByClassIdAndStatus(assessment.getClassEntity().getId(),
                                EnrollmentStatus.ENROLLED) > 0
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
                                student.getId(), assessment.getClassEntity().getId(),
                                List.of(EnrollmentStatus.ENROLLED));
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

                score.setScore(scoreInput.getScore() != null ? java.math.BigDecimal.valueOf(scoreInput.getScore())
                                : null);
                score.setFeedback(scoreInput.getFeedback());
                score.setGradedBy(teacherRepository.findById(teacherId).orElse(null));
                score.setGradedAt(now);

                // Set timestamps thủ công vì entity chưa bật auditing
                if (score.getId() == null) {
                        score.setCreatedAt(now);
                }
                score.setUpdatedAt(now);

                Score saved = scoreRepository.save(score);

                // Gửi thông báo cho học sinh
                try {
                        sendGradeNotificationToStudent(saved, assessment, maxScore);
                } catch (Exception e) {
                        // Log error but don't block grade save
                }

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

        // Helper: Gửi thông báo khi có điểm mới
        private void sendGradeNotificationToStudent(Score score, Assessment assessment, double maxScore) {
                if (score.getStudent() == null || score.getScore() == null) {
                        return;
                }

                Long studentId = score.getStudent().getId();
                String studentName = score.getStudent().getUserAccount().getFullName();
                String className = assessment.getClassEntity().getName();
                String assessmentName = assessment.getSubjectAssessment().getName();
                String teacherName = score.getGradedBy() != null
                                ? score.getGradedBy().getUserAccount().getFullName()
                                : "Giáo viên";

                double scoreValue = score.getScore().doubleValue();
                double scorePercent = (maxScore > 0) ? (scoreValue * 100.0 / maxScore) : 0;

                // Internal notification
                String notificationTitle = "Điểm số mới";
                String notificationMessage = String.format(
                                "Điểm %s của lớp %s đã được chấm: %.2f/%.2f (%.1f%%)",
                                assessmentName, className, scoreValue, maxScore, scorePercent);

                notificationService.createNotification(
                                studentId,
                                NotificationType.NOTIFICATION,
                                notificationTitle,
                                notificationMessage);

                // Email notification
                String gradedAt = score.getGradedAt() != null ? score.getGradedAt().toString() : "";
                emailService.sendGradeUpdatedAsync(
                                score.getStudent().getUserAccount().getEmail(),
                                studentName,
                                className,
                                assessmentName,
                                "N/A", // phaseName - not used in current system
                                teacherName,
                                String.format("%.2f", scoreValue),
                                String.format("%.2f", maxScore),
                                score.getFeedback() != null ? score.getFeedback() : "",
                                gradedAt);
        }

        // Lưu/cập nhật điểm hàng loạt
        @Transactional // cần ghi DB
        public List<TeacherStudentScoreDTO> batchSaveOrUpdateScores(Long teacherId, Long assessmentId,
                        BatchScoreInputDTO batchInput) {
                if (batchInput == null || batchInput.getScores() == null || batchInput.getScores().isEmpty()) {
                        return List.of();
                }
                return batchInput.getScores().stream()
                                .map(s -> saveOrUpdateScore(teacherId, assessmentId, s))
                                .collect(Collectors.toList());
        }

        // Cập nhật ngày kiểm tra dự kiến và thực tế
        @Transactional
        public TeacherAssessmentDTO updateAssessmentDates(Long teacherId, Long assessmentId,
                        OffsetDateTime scheduledDate, OffsetDateTime actualDate) {
                Assessment assessment = assertTeacherOwnsAssessment(teacherId, assessmentId);
                ClassEntity classEntity = assessment.getClassEntity();

                // Validate date range: must be within class start date and end date + 10 days
                LocalDate classStartDate = classEntity.getStartDate();
                LocalDate classEndDate = classEntity.getPlannedEndDate();

                if (classStartDate == null) {
                        throw new CustomException(ErrorCode.INVALID_INPUT, "Lớp học chưa có ngày bắt đầu");
                }

                // End date with 10 days buffer, fallback to start + 90 days if no end date
                LocalDate maxDate = (classEndDate != null ? classEndDate : classStartDate.plusDays(90)).plusDays(10);

                OffsetDateTime minDateTime = classStartDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
                OffsetDateTime maxDateTime = maxDate.atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC);

                if (scheduledDate != null) {
                        if (scheduledDate.isBefore(minDateTime) || scheduledDate.isAfter(maxDateTime)) {
                                throw new CustomException(ErrorCode.INVALID_INPUT,
                                                String.format("Ngày dự kiến phải trong khoảng %s đến %s",
                                                                classStartDate, maxDate));
                        }
                        assessment.setScheduledDate(scheduledDate);
                }
                if (actualDate != null) {
                        if (actualDate.isBefore(minDateTime) || actualDate.isAfter(maxDateTime)) {
                                throw new CustomException(ErrorCode.INVALID_INPUT,
                                                String.format("Ngày thực tế phải trong khoảng %s đến %s",
                                                                classStartDate, maxDate));
                        }
                        assessment.setActualDate(actualDate);
                }
                assessment.setUpdatedAt(OffsetDateTime.now());

                Assessment saved = assessmentRepository.save(assessment);

                SubjectAssessment sa = saved.getSubjectAssessment();
                Long classId = saved.getClassEntity().getId();
                int totalStudents = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);

                // Count graded students
                Set<Score> scores = saved.getScores();
                long gradedCount = (scores != null) ? scores.stream()
                                .filter(s -> s.getGradedAt() != null)
                                .map(s -> s.getStudent().getId())
                                .distinct()
                                .count() : 0L;

                log.info("Updated assessment {} dates: scheduledDate={}, actualDate={}",
                                assessmentId, saved.getScheduledDate(), saved.getActualDate());

                return TeacherAssessmentDTO.builder()
                                .id(saved.getId())
                                .classId(classId)
                                .subjectAssessmentId(sa != null ? sa.getId() : null)
                                .name(sa != null ? sa.getName() : "Assessment " + saved.getId())
                                .description(sa != null ? sa.getDescription() : null)
                                .kind(sa != null && sa.getKind() != null ? sa.getKind().name() : null)
                                .maxScore(sa != null && sa.getMaxScore() != null ? sa.getMaxScore().doubleValue()
                                                : null)
                                .durationMinutes(sa != null ? sa.getDurationMinutes() : null)
                                .scheduledDate(saved.getScheduledDate())
                                .actualDate(saved.getActualDate())
                                .gradedCount((int) gradedCount)
                                .totalStudents(totalStudents)
                                .allGraded(totalStudents > 0 && gradedCount == totalStudents)
                                .build();
        }
}
