package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentportal.AssessmentDTO;
import org.fyp.tmssep490be.dtos.studentportal.ClassSessionsResponseDTO;
import org.fyp.tmssep490be.dtos.studentportal.SessionDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentAssessmentScoreDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentSessionDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentTranscriptDTO;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.Score;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.AssessmentRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.ScoreRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPortalService {

    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
        private final StudentRepository studentRepository;
        private final TeachingSlotRepository teachingSlotRepository;
        private final AssessmentRepository assessmentRepository;
        private final ScoreRepository scoreRepository;

    public ClassSessionsResponseDTO getClassSessions(Long classId, Long studentId) {
        log.info("Getting sessions for class: {} and student: {}", classId, studentId);

        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
        }

        // Validate student enrollment (allow both ENROLLED and COMPLETED for history access)
        List<EnrollmentStatus> allowedStatuses = Arrays.asList(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);
        if (!enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(studentId, classId, allowedStatuses)) {
            throw new CustomException(ErrorCode.STUDENT_NOT_ENROLLED_IN_CLASS);
        }

        // Get all sessions for the class
        List<Session> allSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId);
        
        // Loại bỏ các buổi đã bị hủy để thống nhất với báo cáo điểm danh
        List<Session> activeSessions = allSessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .toList();

        // Separate upcoming and past sessions
        LocalDate today = LocalDate.now();
        List<Session> upcomingSessions = activeSessions.stream()
                .filter(session -> !session.getDate().isBefore(today) && session.getStatus() == SessionStatus.PLANNED)
                .toList();

        List<Session> pastSessions = activeSessions.stream()
                .filter(session -> session.getDate().isBefore(today) || session.getStatus() != SessionStatus.PLANNED)
                .toList();

        // Get student sessions for attendance data
        List<StudentSession> studentSessions = studentSessionRepository.findAllByStudentId(studentId)
                .stream()
                .filter(ss -> ss.getSession().getClassEntity().getId().equals(classId))
                .filter(ss -> ss.getSession().getStatus() != SessionStatus.CANCELLED)
                .toList();

        // Convert to DTOs
        List<SessionDTO> upcomingSessionDTOs = upcomingSessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());

        List<SessionDTO> pastSessionDTOs = pastSessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());

        List<StudentSessionDTO> studentSessionDTOs = studentSessions.stream()
                .map(this::convertToStudentSessionDTOWithDisplayStatus)
                .collect(Collectors.toList());

        return ClassSessionsResponseDTO.builder()
                .upcomingSessions(upcomingSessionDTOs)
                .pastSessions(pastSessionDTOs)
                .studentSessions(studentSessionDTOs)
                .build();
    }

        public List<StudentTranscriptDTO> getStudentTranscript(Long studentId) {
                log.info("Getting transcript for student: {}", studentId);

                if (!studentRepository.existsById(studentId)) {
                        throw new CustomException(ErrorCode.STUDENT_NOT_FOUND);
                }

                List<EnrollmentStatus> transcriptStatuses = Arrays.asList(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);
                List<Enrollment> enrollments = enrollmentRepository.findByStudentIdWithClassAndCourse(studentId).stream()
                                .filter(enrollment -> transcriptStatuses.contains(enrollment.getStatus()))
                                .toList();

                return enrollments.stream()
                                .map(this::convertToStudentTranscriptDTO)
                                .toList();
        }

        public List<AssessmentDTO> getClassAssessments(Long classId) {
                log.info("Getting assessments for class: {}", classId);

                if (!classRepository.existsById(classId)) {
                        throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
                }

                return assessmentRepository.findByClassEntityId(classId).stream()
                                .map(this::convertToAssessmentDTO)
                                .toList();
        }

        public List<StudentAssessmentScoreDTO> getStudentAssessmentScores(Long classId, Long studentId) {
                log.info("Getting assessment scores for student: {} in class: {}", studentId, classId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                List<EnrollmentStatus> allowedStatuses = Arrays.asList(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED);
                if (!enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(studentId, classEntity.getId(), allowedStatuses)) {
                        throw new CustomException(ErrorCode.STUDENT_NOT_ENROLLED_IN_CLASS);
                }

//            {
//                101 → Score {id: 1, assessmentId: 101, score: 85.0},
//                102 → Score {id: 2, assessmentId: 102, score: null},
//                103 → Score {id: 3, assessmentId: 103, score: 78.5}
//            }
                Map<Long, Score> scoreMap = scoreRepository.findByStudentIdAndClassId(studentId, classId).stream()
                                .collect(Collectors.toMap(score -> score.getAssessment().getId(), score -> score));

                return assessmentRepository.findByClassEntityId(classId).stream()
                                .map(assessment -> convertToStudentAssessmentScoreDTO(assessment, studentId, scoreMap.get(assessment.getId())))
                                .toList();
        }

    private SessionDTO convertToSessionDTO(Session session) {
        // Get teachers list
        List<String> teachers = session.getTeachingSlots().stream()
                .map(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null
                        ? ts.getTeacher().getUserAccount().getFullName()
                        : null)
                .filter(name -> name != null)
                .collect(Collectors.toList());

        // Get room from first resource
        String room = session.getSessionResources().stream()
                .findFirst()
                .map(sr -> sr.getResource() != null ? sr.getResource().getName() : null)
                .orElse(null);

        var timeSlot = session.getTimeSlotTemplate();

        return SessionDTO.builder()
                .id(session.getId())
                .classId(session.getClassEntity().getId())
                .date(session.getDate() != null ? session.getDate().toString() : null)
                .type(null) // Can be set if needed
                .status(session.getStatus() != null ? session.getStatus().name() : null)
                .room(room)
                .teacherNote(null) // Can be set if needed
                .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                .teachers(teachers)
                .recordedAt(session.getCreatedAt())
                .build();
    }

    private StudentSessionDTO convertToStudentSessionDTOWithDisplayStatus(StudentSession ss) {
        // Resolve display status (past sessions with null/PLANNED → ABSENT)
        AttendanceStatus displayStatus = resolveDisplayStatusForStudent(ss);

        return StudentSessionDTO.builder()
                .sessionId(ss.getSession().getId())
                .studentId(ss.getStudent().getId())
                .attendanceStatus(displayStatus != null ? displayStatus.name() : null)
                .homeworkStatus(ss.getHomeworkStatus() != null ? ss.getHomeworkStatus().name() : null)
                .isMakeup(ss.getIsMakeup())
                .makeupSessionId(ss.getMakeupSession() != null ? ss.getMakeupSession().getId() : null)
                .originalSessionId(ss.getOriginalSession() != null ? ss.getOriginalSession().getId() : null)
                .isTransferredOut(ss.getIsTransferredOut())
                .note(ss.getNote())
                .recordedAt(ss.getRecordedAt())
                .build();
    }

    private AttendanceStatus resolveDisplayStatusForStudent(StudentSession studentSession) {
        Session session = studentSession.getSession();
        if (session == null) {
            return studentSession.getAttendanceStatus();
        }
        AttendanceStatus status = studentSession.getAttendanceStatus();
        LocalDate today = LocalDate.now();

        // Qua ngày mà vẫn chưa điểm danh (null hoặc PLANNED) thì coi là ABSENT
        if (session.getDate() != null
                && session.getDate().isBefore(today)
                && (status == null || status == AttendanceStatus.PLANNED)) {
            return AttendanceStatus.ABSENT;
        }

        return status;
    }

    private StudentTranscriptDTO convertToStudentTranscriptDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();

        String primaryTeacherName = teachingSlotRepository.findByClassEntityIdAndStatus(classEntity.getId(), TeachingSlotStatus.SCHEDULED)
                .stream()
                .collect(Collectors.groupingBy(ts -> ts.getTeacher().getId()))
                .values()
                .stream()
                .max(Comparator.comparingInt(List::size))
                .map(slots -> slots.get(0).getTeacher().getUserAccount().getFullName())
                .orElse("Chua phan cong");

        List<Session> sessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classEntity.getId());
        List<Session> activeSessions = sessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .toList();

        int totalSessions = activeSessions.size();
        int completedSessions = (int) activeSessions.stream()
                .filter(session ->
                        (session.getDate() != null && session.getDate().isBefore(LocalDate.now()))
                                || session.getStatus() != SessionStatus.PLANNED)
                .count();

        Map<String, BigDecimal> componentScores = new HashMap<>();
        scoreRepository.findByStudentIdAndClassId(enrollment.getStudentId(), classEntity.getId()).forEach(score -> {
            if (score.getAssessment() != null && score.getAssessment().getSubjectAssessment() != null) {
                String assessmentName = score.getAssessment().getSubjectAssessment().getName();
                componentScores.put(assessmentName, score.getScore());
            }
        });

        BigDecimal averageScore = null;
        if (!componentScores.isEmpty()) {
            BigDecimal sum = componentScores.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageScore = sum.divide(new BigDecimal(componentScores.size()), 2, RoundingMode.HALF_UP);
        }

        return StudentTranscriptDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .teacherName(primaryTeacherName)
                .status(classEntity.getStatus() != null ? classEntity.getStatus().name() : null)
                .averageScore(averageScore)
                .componentScores(componentScores)
                .completedDate(classEntity.getStatus() == ClassStatus.COMPLETED ? classEntity.getActualEndDate() : null)
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .build();
    }

    private AssessmentDTO convertToAssessmentDTO(Assessment assessment) {
        var subjectAssessment = assessment.getSubjectAssessment();

        return AssessmentDTO.builder()
                .id(assessment.getId())
                .name(subjectAssessment != null ? subjectAssessment.getName() : null)
                .kind(subjectAssessment != null && subjectAssessment.getKind() != null ? subjectAssessment.getKind().name() : null)
                .durationMinutes(subjectAssessment != null ? subjectAssessment.getDurationMinutes() : null)
                .description(subjectAssessment != null ? subjectAssessment.getDescription() : null)
                .scheduledDate(assessment.getScheduledDate())
                .maxScore(subjectAssessment != null ? subjectAssessment.getMaxScore() : null)
                .build();
    }

    private StudentAssessmentScoreDTO convertToStudentAssessmentScoreDTO(Assessment assessment, Long studentId, Score score) {
        var subjectAssessment = assessment.getSubjectAssessment();
        BigDecimal maxScore = subjectAssessment != null ? subjectAssessment.getMaxScore() : null;

        BigDecimal scorePercentage = null;
        if (score != null && score.getScore() != null && maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
            scorePercentage = score.getScore()
                    .divide(maxScore, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return StudentAssessmentScoreDTO.builder()
                .assessmentId(assessment.getId())
                .studentId(studentId)
                .score(score != null ? score.getScore() : null)
                .maxScore(maxScore)
                .feedback(score != null ? score.getFeedback() : null)
                .gradedBy(score != null && score.getGradedBy() != null && score.getGradedBy().getUserAccount() != null
                        ? score.getGradedBy().getUserAccount().getFullName()
                        : null)
                .gradedAt(score != null ? score.getGradedAt() : null)
                .isSubmitted(score != null)
                .isGraded(score != null && score.getGradedAt() != null)
                .scorePercentage(scorePercentage)
                .createdAt(assessment.getCreatedAt())
                .build();
    }
}
