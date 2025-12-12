package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentportal.AssessmentDTO;
import org.fyp.tmssep490be.dtos.studentportal.ClassSessionsResponseDTO;
import org.fyp.tmssep490be.dtos.studentportal.SessionDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentAssessmentScoreDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentClassDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentSessionDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentTranscriptDTO;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.Assessment;
import org.fyp.tmssep490be.entities.Score;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;
import org.fyp.tmssep490be.entities.enums.Modality;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

    public Page<StudentClassDTO> getStudentClasses(
            Long studentId,
            List<String> enrollmentStatusFilters,
            List<String> classStatusFilters,
            List<Long> branchFilters,
            List<Long> subjectFilters, // Note: called courseId in API but refers to subject in new schema
            List<String> modalityFilters,
            Pageable pageable
    ) {
        log.info("Getting classes for student: {} with filters", studentId);

        // Validate student exists
        if (!studentRepository.existsById(studentId)) {
            throw new CustomException(ErrorCode.STUDENT_NOT_FOUND);
        }

        List<EnrollmentStatus> enrollmentStatuses = resolveEnrollmentStatuses(enrollmentStatusFilters);
        List<ClassStatus> classStatuses = resolveClassStatuses(classStatusFilters);
        Set<Modality> modalities = resolveModalities(modalityFilters);

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatusIn(studentId, enrollmentStatuses);

        List<Enrollment> filteredEnrollments = enrollments.stream()
                .filter(enrollment -> {
                    ClassEntity classEntity = enrollment.getClassEntity();

                    // Class status filter
                    boolean classStatusMatch = classStatuses.isEmpty() || classStatuses.contains(classEntity.getStatus());
                    if (!classStatusMatch) return false;

                    // Branch filter
                    if (branchFilters != null && !branchFilters.isEmpty()) {
                        if (!branchFilters.contains(classEntity.getBranch().getId())) {
                            return false;
                        }
                    }

                    // Subject filter (frontend sends as courseId)
                    if (subjectFilters != null && !subjectFilters.isEmpty()) {
                        if (classEntity.getSubject() == null || !subjectFilters.contains(classEntity.getSubject().getId())) {
                            return false;
                        }
                    }

                    // Modality filter
                    if (!modalities.isEmpty() && !modalities.contains(classEntity.getModality())) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Convert to DTOs
        List<StudentClassDTO> studentClassDTOs = filteredEnrollments.stream()
                .map(this::convertToStudentClassDTO)
                .collect(Collectors.toList());

        // Apply pagination manually (since we're filtering in memory)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), studentClassDTOs.size());
        
        // Handle case where start is beyond list size
        if (start >= studentClassDTOs.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, studentClassDTOs.size());
        }
        
        List<StudentClassDTO> pagedContent = studentClassDTOs.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, studentClassDTOs.size());
    }

    public ClassSessionsResponseDTO getClassSessions(Long classId, Long studentId) {
        log.info("Getting sessions for class: {} and student: {}", classId, studentId);

        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
        }

        // Get enrollment to check status and timeline (allow ENROLLED, COMPLETED, TRANSFERRED for history)
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndClassId(studentId, classId);
        if (enrollment == null) {
            throw new CustomException(ErrorCode.STUDENT_NOT_ENROLLED_IN_CLASS);
        }

        // Get all sessions for the class
        List<Session> allSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId);
        
        // Filter sessions by enrollment timeline
        List<Session> relevantSessions = allSessions.stream()
                .filter(session -> {
                    if (session.getStatus() == SessionStatus.CANCELLED) {
                        return false;
                    }
                    
                    Long sessionId = session.getId();
                    Long joinId = enrollment.getJoinSessionId();
                    Long leftId = enrollment.getLeftSessionId();
                    
                    // Filter by enrollment timeline
                    if (enrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
                        // Only sessions <= leftSessionId
                        return leftId == null || sessionId <= leftId;
                    } else if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                        // Only sessions >= joinSessionId
                        return joinId == null || sessionId >= joinId;
                    } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                        // Sessions from join to end
                        return joinId == null || sessionId >= joinId;
                    }
                    
                    return true;
                })
                .toList();

        // Separate upcoming and past sessions
        LocalDate today = LocalDate.now();
        List<Session> upcomingSessions = relevantSessions.stream()
                .filter(session -> !session.getDate().isBefore(today) && session.getStatus() == SessionStatus.PLANNED)
                .toList();

        List<Session> pastSessions = relevantSessions.stream()
                .filter(session -> session.getDate().isBefore(today) || session.getStatus() != SessionStatus.PLANNED)
                .toList();

        // Get student sessions for attendance data - filter by enrollment timeline and isTransferredOut
        List<StudentSession> studentSessions = studentSessionRepository.findAllByStudentId(studentId)
                .stream()
                .filter(ss -> ss.getSession().getClassEntity().getId().equals(classId))
                .filter(ss -> ss.getSession().getStatus() != SessionStatus.CANCELLED)
                .filter(ss -> !Boolean.TRUE.equals(ss.getIsTransferredOut()))  // Filter transferred out sessions
                .filter(ss -> {
                    Long sessionId = ss.getSession().getId();
                    Long joinId = enrollment.getJoinSessionId();
                    Long leftId = enrollment.getLeftSessionId();
                    
                    // Filter by enrollment timeline
                    if (enrollment.getStatus() == EnrollmentStatus.TRANSFERRED) {
                        return leftId == null || sessionId <= leftId;
                    } else if (enrollment.getStatus() == EnrollmentStatus.ENROLLED) {
                        return joinId == null || sessionId >= joinId;
                    } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
                        return joinId == null || sessionId >= joinId;
                    }
                    
                    return true;
                })
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
                .enrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                .leftAt(enrollment.getLeftAt() != null ? enrollment.getLeftAt().toLocalDate() : null)
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

        public org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO getClassDetail(Long classId) {
                log.info("Getting class detail for class: {}", classId);

                ClassEntity classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

                return convertToClassDetailDTO(classEntity);
        }

        public List<org.fyp.tmssep490be.dtos.studentportal.ClassmateDTO> getClassmates(Long classId) {
                log.info("Getting classmates for class: {}", classId);

                if (!classRepository.existsById(classId)) {
                        throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
                }

                List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);

                return enrollments.stream()
                                .map(this::convertToClassmateDTO)
                                .collect(Collectors.toList());
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
                .skill(subjectAssessment != null && subjectAssessment.getSkills() != null && !subjectAssessment.getSkills().isEmpty()
                        ? subjectAssessment.getSkills().get(0).name()
                        : null)
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

    private StudentClassDTO convertToStudentClassDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();

        // Calculate session progress
        LocalDate today = LocalDate.now();
        List<Session> allSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classEntity.getId());
        
        // Exclude cancelled sessions
        List<Session> activeSessions = allSessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .collect(Collectors.toList());

        int totalSessions = activeSessions.size();
        int completedSessions = (int) activeSessions.stream()
                .filter(session -> 
                    (session.getDate() != null && session.getDate().isBefore(today)) 
                    || session.getStatus() == SessionStatus.DONE)
                .count();

        return StudentClassDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .subjectId(classEntity.getSubject() != null ? classEntity.getSubject().getId() : null)
                .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .subjectCode(classEntity.getSubject() != null ? classEntity.getSubject().getCode() : null)
                .branchId(classEntity.getBranch().getId())
                .branchName(classEntity.getBranch().getName())
                .branchAddress(classEntity.getBranch().getAddress())
                .modality(classEntity.getModality() != null ? classEntity.getModality().name() : null)
                .status(classEntity.getStatus() != null ? classEntity.getStatus().name() : null)
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .actualEndDate(classEntity.getActualEndDate())
                .enrollmentId(enrollment.getId())
                .enrollmentDate(enrollment.getEnrolledAt())
                .enrollmentStatus(enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .scheduleSummary(generateScheduleSummary(classEntity))
                .scheduleDetails(generateScheduleDetails(classEntity))
                .build();
    }

    private List<StudentClassDTO.ScheduleDetailDTO> generateScheduleDetails(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return List.of();
        }

        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classEntity.getId());
        if (sessions.isEmpty()) {
            return List.of();
        }

        // Group sessions by day of week to get time slots
        Map<Integer, String> dayTimeSlots = new HashMap<>();
        for (Session session : sessions) {
            if (session.getTimeSlotTemplate() != null) {
                int dayOfWeek = session.getDate().getDayOfWeek().getValue();
                String timeSlot = String.format("%s-%s", 
                    session.getTimeSlotTemplate().getStartTime(), 
                    session.getTimeSlotTemplate().getEndTime());
                dayTimeSlots.putIfAbsent(dayOfWeek, timeSlot);
            }
        }

        return Arrays.stream(classEntity.getScheduleDays())
            .sorted()
            .map(day -> {
                DayOfWeek dayOfWeek = DayOfWeek.of(day);
                String dayName = switch (dayOfWeek) {
                    case MONDAY -> "Thứ 2";
                    case TUESDAY -> "Thứ 3";
                    case WEDNESDAY -> "Thứ 4";
                    case THURSDAY -> "Thứ 5";
                    case FRIDAY -> "Thứ 6";
                    case SATURDAY -> "Thứ 7";
                    case SUNDAY -> "Chủ nhật";
                };

                String[] times = dayTimeSlots.getOrDefault(day, "07:00-08:30").split("-");
                return StudentClassDTO.ScheduleDetailDTO.builder()
                    .day(dayName)
                    .startTime(times.length > 0 ? times[0] : "07:00")
                    .endTime(times.length > 1 ? times[1] : "08:30")
                    .build();
            })
            .collect(Collectors.toList());
    }

    private List<org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.ScheduleDetailDTO> convertScheduleDetailsForClassDetail(
            List<StudentClassDTO.ScheduleDetailDTO> studentClassSchedules) {
        return studentClassSchedules.stream()
            .map(schedule -> org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.ScheduleDetailDTO.builder()
                .day(schedule.getDay())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build())
            .collect(Collectors.toList());
    }

    private String generateScheduleSummary(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return "Chưa có lịch";
        }

        // Convert schedule days (1-7) to Vietnamese day names
        List<String> dayNames = Arrays.stream(classEntity.getScheduleDays())
                .sorted()
                .map(day -> {
                    DayOfWeek dayOfWeek = DayOfWeek.of(day);
                    return switch (dayOfWeek) {
                        case MONDAY -> "Thứ 2";
                        case TUESDAY -> "Thứ 3";
                        case WEDNESDAY -> "Thứ 4";
                        case THURSDAY -> "Thứ 5";
                        case FRIDAY -> "Thứ 6";
                        case SATURDAY -> "Thứ 7";
                        case SUNDAY -> "Chủ nhật";
                    };
                })
                .collect(Collectors.toList());

        String daysString = String.join(", ", dayNames);

        // Add time slot info if available (get from first session)
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classEntity.getId());
        if (!sessions.isEmpty() && sessions.get(0).getTimeSlotTemplate() != null) {
            var timeSlot = sessions.get(0).getTimeSlotTemplate();
            String timeString = String.format("%s-%s", 
                timeSlot.getStartTime(), 
                timeSlot.getEndTime());
            return String.format("%s | %s", daysString, timeString);
        }

        return daysString;
    }

    private List<EnrollmentStatus> resolveEnrollmentStatuses(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            // Default: show all relevant enrollments including transferred (UI will distinguish with badges)
            return Arrays.asList(EnrollmentStatus.ENROLLED, EnrollmentStatus.COMPLETED, EnrollmentStatus.TRANSFERRED);
        }

        return filters.stream()
                .map(filter -> {
                    try {
                        return EnrollmentStatus.valueOf(filter.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid enrollment status filter: {}", filter);
                        return null;
                    }
                })
                .filter(status -> status != null)
                .collect(Collectors.toList());
    }

    private List<ClassStatus> resolveClassStatuses(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            // Default: return all class statuses
            return Arrays.asList(ClassStatus.values());
        }

        return filters.stream()
                .map(filter -> {
                    try {
                        return ClassStatus.valueOf(filter.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid class status filter: {}", filter);
                        return null;
                    }
                })
                .filter(status -> status != null)
                .collect(Collectors.toList());
    }

    private Set<Modality> resolveModalities(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            // Default: return empty set (no filter)
            return new HashSet<>();
        }

        return filters.stream()
                .map(filter -> {
                    try {
                        return Modality.valueOf(filter.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid modality filter: {}", filter);
                        return null;
                    }
                })
                .filter(modality -> modality != null)
                .collect(Collectors.toSet());
    }

    private org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO convertToClassDetailDTO(ClassEntity classEntity) {
        // Get teachers from teaching slots
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByClassEntityIdAndStatus(
                classEntity.getId(), TeachingSlotStatus.SCHEDULED);

        Map<Long, List<TeachingSlot>> slotsByTeacher = teachingSlots.stream()
                .filter(ts -> ts.getTeacher() != null)
                .collect(Collectors.groupingBy(ts -> ts.getTeacher().getId()));

        int maxSessionsPerTeacher = slotsByTeacher.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        List<org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.TeacherSummary> teachers = slotsByTeacher.values().stream()
                .map(slotsForTeacher -> {
                    Teacher teacher = slotsForTeacher.get(0).getTeacher();
                    boolean isPrimary = maxSessionsPerTeacher > 0 &&
                            slotsForTeacher.size() > maxSessionsPerTeacher / 2;

                    String teacherName = "N/A";
                    String teacherEmail = "N/A";
                    if (teacher != null && teacher.getUserAccount() != null) {
                        teacherName = teacher.getUserAccount().getFullName();
                        teacherEmail = teacher.getUserAccount().getEmail();
                        if (teacherName == null || teacherName.trim().isEmpty()) {
                            teacherName = "N/A";
                        }
                        if (teacherEmail == null || teacherEmail.trim().isEmpty()) {
                            teacherEmail = "N/A";
                        }
                    }

                    return org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.TeacherSummary.builder()
                            .teacherId(teacher.getId())
                            .teacherName(teacherName)
                            .teacherEmail(teacherEmail)
                            .isPrimaryInstructor(isPrimary)
                            .build();
                })
                .collect(Collectors.toList());

        // Get enrollment summary
        int totalEnrolled = enrollmentRepository.countByClassIdAndStatus(classEntity.getId(), EnrollmentStatus.ENROLLED);

        org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.EnrollmentSummary enrollmentSummary = 
                org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.EnrollmentSummary.builder()
                .totalEnrolled(totalEnrolled)
                .maxCapacity(classEntity.getMaxCapacity())
                .build();

        // Get next session
        LocalDate today = LocalDate.now();
        SessionDTO nextSession = sessionRepository.findByClassEntityIdOrderByDateAsc(classEntity.getId())
                .stream()
                .filter(s -> s.getStatus() == SessionStatus.PLANNED && !s.getDate().isBefore(today))
                .findFirst()
                .map(this::convertToSessionDTO)
                .orElse(null);

        // Build subject info (new schema: Subject has Curriculum and Level)
        Subject subject = classEntity.getSubject();
        org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.CurriculumInfo curriculumInfo = null;
        org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.LevelInfo levelInfo = null;

        if (subject.getLevel() != null) {
            Level level = subject.getLevel();
            levelInfo = org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.LevelInfo.builder()
                    .id(level.getId())
                    .code(level.getCode())
                    .name(level.getName())
                    .build();
        }

        if (subject.getCurriculum() != null) {
            Curriculum curriculum = subject.getCurriculum();
            curriculumInfo = org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.CurriculumInfo.builder()
                    .id(curriculum.getId())
                    .code(curriculum.getCode())
                    .name(curriculum.getName())
                    .build();
        }

        org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.SubjectInfo subjectInfo = 
                org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.SubjectInfo.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .description(subject.getDescription())
                .totalHours(subject.getTotalHours())
                .numberOfSessions(subject.getNumberOfSessions())
                .hoursPerSession(subject.getHoursPerSession())
                .prerequisites(subject.getPrerequisites())
                .targetAudience(subject.getTargetAudience())
                .curriculum(curriculumInfo)
                .level(levelInfo)
                .build();

        // Convert scheduleDays from Short[] to List<Integer>
        List<Integer> scheduleDays = classEntity.getScheduleDays() != null ?
                Arrays.stream(classEntity.getScheduleDays())
                        .map(Short::intValue)
                        .collect(Collectors.toList()) : List.of();

        return org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .subject(subjectInfo)
                .branch(org.fyp.tmssep490be.dtos.studentportal.ClassDetailDTO.BranchInfo.builder()
                        .id(classEntity.getBranch().getId())
                        .name(classEntity.getBranch().getName())
                        .address(classEntity.getBranch().getAddress())
                        .build())
                .modality(classEntity.getModality().toString())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .actualEndDate(classEntity.getActualEndDate())
                .scheduleDays(scheduleDays)
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus().toString())
                .teachers(teachers)
                .scheduleSummary(generateScheduleSummary(classEntity))
                .scheduleDetails(convertScheduleDetailsForClassDetail(generateScheduleDetails(classEntity)))
                .enrollmentSummary(enrollmentSummary)
                .nextSession(nextSession)
                .build();
    }

    private org.fyp.tmssep490be.dtos.studentportal.ClassmateDTO convertToClassmateDTO(Enrollment enrollment) {
        var student = enrollment.getStudent();
        var userAccount = student.getUserAccount();

        // Calculate attendance rate
        BigDecimal attendanceRate = calculateAttendanceRate(enrollment.getClassEntity().getId(), student.getId());

        return org.fyp.tmssep490be.dtos.studentportal.ClassmateDTO.builder()
                .studentId(student.getId())
                .fullName(userAccount.getFullName())
                .avatar(userAccount.getAvatarUrl())
                .email(userAccount.getEmail())
                .studentCode(student.getStudentCode())
                .enrollmentId(enrollment.getId())
                .enrollmentDate(enrollment.getEnrolledAt())
                .enrollmentStatus(enrollment.getStatus().toString())
                .attendanceRate(attendanceRate)
                .build();
    }

    private BigDecimal calculateAttendanceRate(Long classId, Long studentId) {
        List<Session> completedSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId).stream()
                .filter(session -> session.getStatus() == SessionStatus.DONE)
                .toList();

        if (completedSessions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long presentCount = studentSessionRepository.findAllByStudentId(studentId).stream()
                .filter(ss -> ss.getSession().getClassEntity().getId().equals(classId))
                .filter(ss -> ss.getSession().getStatus() == SessionStatus.DONE)
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();

        return BigDecimal.valueOf(presentCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(completedSessions.size()), 1, RoundingMode.HALF_UP);
    }
}
