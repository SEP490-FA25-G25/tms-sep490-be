package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentportal.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentPortalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentPortalServiceImpl implements StudentPortalService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentRepository studentRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;

    @Override
    public Page<StudentClassDTO> getStudentClasses(
            Long studentId,
            List<String> statusFilters,
            List<Long> branchFilters,
            List<Long> courseFilters,
            List<String> modalityFilters,
            Pageable pageable
    ) {
        log.info("Getting classes for student: {} with filters", studentId);

        // Validate student exists
        if (!studentRepository.existsById(studentId)) {
            throw new CustomException(ErrorCode.STUDENT_NOT_FOUND);
        }

        // Get enrollments with dynamic status filters
        List<EnrollmentStatus> enrollmentStatuses;
        if (statusFilters != null && !statusFilters.isEmpty()) {
            enrollmentStatuses = statusFilters.stream()
                    .filter(status -> status != null && !status.trim().isEmpty())
                    .map(status -> {
                        try {
                            return EnrollmentStatus.valueOf(status.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn("Invalid enrollment status: {}, skipping", status);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            // When no status filter provided, return ALL enrollment statuses
            enrollmentStatuses = Arrays.asList(EnrollmentStatus.values());
        }

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatusIn(studentId, enrollmentStatuses);

        // Filter by class status if provided
        List<Enrollment> filteredEnrollments = enrollments.stream()
                .filter(enrollment -> {
                    ClassEntity classEntity = enrollment.getClassEntity();

                    // Status filter
                    if (statusFilters != null && !statusFilters.isEmpty()) {
                        String classStatus = classEntity.getStatus().toString();
                        if (!statusFilters.contains(classStatus)) {
                            return false;
                        }
                    }

                    // Branch filter
                    if (branchFilters != null && !branchFilters.isEmpty()) {
                        if (!branchFilters.contains(classEntity.getBranch().getId())) {
                            return false;
                        }
                    }

                    // Course filter
                    if (courseFilters != null && !courseFilters.isEmpty()) {
                        if (!courseFilters.contains(classEntity.getCourse().getId())) {
                            return false;
                        }
                    }

                    // Modality filter
                    if (modalityFilters != null && !modalityFilters.isEmpty()) {
                        String classModality = classEntity.getModality().toString();
                        if (!modalityFilters.contains(classModality)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Convert to DTOs
        List<StudentClassDTO> studentClassDTOs = filteredEnrollments.stream()
                .map(this::convertToStudentClassDTO)
                .collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), studentClassDTOs.size());
        List<StudentClassDTO> pagedContent = studentClassDTOs.subList(start, end);

        return new PageImpl<>(pagedContent, pageable, studentClassDTOs.size());
    }

    @Override
    public ClassDetailDTO getClassDetail(Long classId) {
        log.info("Getting class detail for class: {}", classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        return convertToClassDetailDTO(classEntity);
    }

    @Override
    public ClassSessionsResponseDTO getClassSessions(Long classId, Long studentId) {
        log.info("Getting sessions for class: {} and student: {}", classId, studentId);

        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate student enrollment
        if (!enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(studentId, classId, Arrays.asList(EnrollmentStatus.ENROLLED))) {
            throw new CustomException(ErrorCode.STUDENT_NOT_ENROLLED_IN_CLASS);
        }

        // Get all sessions for the class
        List<Session> allSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId);
        // Loại bỏ các buổi đã bị hủy để thống nhất với báo cáo điểm danh
        List<Session> activeSessions = allSessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .collect(Collectors.toList());

        // Separate upcoming and past sessions
        LocalDate today = LocalDate.now();
        List<Session> upcomingSessions = activeSessions.stream()
                .filter(session -> !session.getDate().isBefore(today) && session.getStatus() == SessionStatus.PLANNED)
                .collect(Collectors.toList());

        List<Session> pastSessions = activeSessions.stream()
                .filter(session -> session.getDate().isBefore(today) || session.getStatus() != SessionStatus.PLANNED)
                .collect(Collectors.toList());

        // Get student sessions for attendance data
        List<StudentSession> studentSessions = studentSessionRepository.findAllByStudentId(studentId)
                .stream()
                .filter(ss -> ss.getSession().getClassEntity().getId().equals(classId))
                .filter(ss -> ss.getSession().getStatus() != SessionStatus.CANCELLED)
                .collect(Collectors.toList());

        // Convert to DTOs
        List<SessionDTO> upcomingSessionDTOs = upcomingSessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());

        List<SessionDTO> pastSessionDTOs = pastSessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());

        List<StudentSessionDTO> studentSessionDTOs = studentSessions.stream()
                .map(ss -> convertToStudentSessionDTOWithDisplayStatus(ss))
                .collect(Collectors.toList());

        return ClassSessionsResponseDTO.builder()
                .upcomingSessions(upcomingSessionDTOs)
                .pastSessions(pastSessionDTOs)
                .studentSessions(studentSessionDTOs)
                .build();
    }

    @Override
    public List<AssessmentDTO> getClassAssessments(Long classId) {
        log.info("Getting assessments for class: {}", classId);

        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
        }

        // Get assessments for the class
        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);

        return assessments.stream()
                .map(this::convertToAssessmentDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentAssessmentScoreDTO> getStudentAssessmentScores(Long classId, Long studentId) {
        log.info("Getting assessment scores for student: {} in class: {}", studentId, classId);

        // Validate class and student enrollment
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        if (!enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(studentId, classId, Arrays.asList(EnrollmentStatus.ENROLLED))) {
            throw new CustomException(ErrorCode.STUDENT_NOT_ENROLLED_IN_CLASS);
        }

        // Get assessments for the class
        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classId);

        // Get scores for the student
        List<StudentAssessmentScoreDTO> scores = new ArrayList<>();
        for (Assessment assessment : assessments) {
            Optional<Score> scoreOpt = scoreRepository.findByStudentIdAndAssessmentId(studentId, assessment.getId());

            StudentAssessmentScoreDTO scoreDTO = StudentAssessmentScoreDTO.builder()
                    .assessmentId(assessment.getId())
                    .studentId(studentId)
                    .maxScore(assessment.getCourseAssessment().getMaxScore())
                    .isSubmitted(scoreOpt.isPresent())
                    .createdAt(assessment.getCreatedAt())
                    .build();

            if (scoreOpt.isPresent()) {
                Score score = scoreOpt.get();
                scoreDTO.setScore(score.getScore());
                scoreDTO.setFeedback(score.getFeedback());
                scoreDTO.setGradedBy(score.getGradedBy() != null ? score.getGradedBy().getUserAccount().getFullName() : null);
                scoreDTO.setGradedAt(score.getGradedAt());
                scoreDTO.setIsGraded(score.getGradedAt() != null);

                if (score.getScore() != null) {
                    scoreDTO.setScorePercentage(
                            score.getScore().divide(assessment.getCourseAssessment().getMaxScore(), 4, RoundingMode.HALF_UP)
                                    .multiply(new BigDecimal("100"))
                    );
                }
            }

            scores.add(scoreDTO);
        }

        return scores;
    }

    @Override
    public List<ClassmateDTO> getClassmates(Long classId) {
        log.info("Getting classmates for class: {}", classId);

        // Validate class exists
        if (!classRepository.existsById(classId)) {
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
        }

        // Get enrolled students
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);

        return enrollments.stream()
                .map(this::convertToClassmateDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StudentTranscriptDTO> getStudentTranscript(Long studentId) {
        log.info("Getting transcript for student: {}", studentId);

        // Validate student exists
        if (!studentRepository.existsById(studentId)) {
            throw new CustomException(ErrorCode.STUDENT_NOT_FOUND);
        }

        // Get all enrollments for the student with status ENROLLED or COMPLETED classes
        List<EnrollmentStatus> enrollmentStatuses = Arrays.asList(
                EnrollmentStatus.ENROLLED,
                EnrollmentStatus.COMPLETED
        );
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndStatusIn(studentId, enrollmentStatuses);

        return enrollments.stream()
                .map(this::convertToStudentTranscriptDTO)
                .collect(Collectors.toList());
    }

    // Helper methods for converting entities to DTOs

    private StudentClassDTO convertToStudentClassDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();
        Student student = enrollment.getStudent();

        // Get instructor names from teaching slots with proper lazy loading handling
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByClassEntityIdAndStatus(classEntity.getId(), TeachingSlotStatus.SCHEDULED);
        List<String> instructorNames = teachingSlots.stream()
                .filter(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null)
                .map(teachingSlot -> teachingSlot.getTeacher().getUserAccount().getFullName())
                .filter(name -> name != null && !name.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // Calculate session statistics (only what's needed for progress bar)
        List<Session> allSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classEntity.getId());
        int totalSessions = allSessions.size();

        int completedSessions = (int) allSessions.stream()
                .filter(session -> session.getDate().isBefore(LocalDate.now()) || session.getStatus() != SessionStatus.PLANNED)
                .count();

        return StudentClassDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseId(classEntity.getCourse().getId())
                .courseName(classEntity.getCourse().getName())
                .courseCode(classEntity.getCourse().getCode())
                .branchId(classEntity.getBranch().getId())
                .branchName(classEntity.getBranch().getName())
                .modality(classEntity.getModality().toString())
                .status(classEntity.getStatus().toString())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .enrollmentId(enrollment.getId())
                .enrollmentDate(enrollment.getEnrolledAt())
                .enrollmentStatus(enrollment.getStatus().toString())
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .instructorNames(instructorNames)
                .scheduleSummary(generateScheduleSummary(classEntity))
                .build();
    }

    private ClassDetailDTO convertToClassDetailDTO(ClassEntity classEntity) {
        // Get teachers from teaching slots with proper lazy loading handling
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByClassEntityIdAndStatus(classEntity.getId(), TeachingSlotStatus.SCHEDULED);

        // Group by teacher and process each teacher
        Map<Long, List<TeachingSlot>> slotsByTeacher = teachingSlots.stream()
                .filter(ts -> ts.getTeacher() != null)
                .collect(Collectors.groupingBy(ts -> ts.getTeacher().getId()));

        // Calculate max sessions for any teacher to determine primary instructor
        int maxSessionsPerTeacher = slotsByTeacher.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        List<ClassDetailDTO.TeacherSummary> teachers = slotsByTeacher.values().stream()
                .map(slotsForTeacher -> {
                    Teacher teacher = slotsForTeacher.get(0).getTeacher();
                    boolean isPrimary = maxSessionsPerTeacher > 0 &&
                            slotsForTeacher.size() > maxSessionsPerTeacher / 2; // Simple heuristic for primary instructor

                    // Safe access to user account with null checks
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

                    return ClassDetailDTO.TeacherSummary.builder()
                            .teacherId(teacher.getId())
                            .teacherName(teacherName)
                            .teacherEmail(teacherEmail)
                            .isPrimaryInstructor(isPrimary)
                            .build();
                })
                .collect(Collectors.toList());

        // Get enrollment summary
        int totalEnrolled = enrollmentRepository.countByClassIdAndStatus(classEntity.getId(), EnrollmentStatus.ENROLLED);

        ClassDetailDTO.EnrollmentSummary enrollmentSummary = ClassDetailDTO.EnrollmentSummary.builder()
                .totalEnrolled(totalEnrolled)
                .maxCapacity(classEntity.getMaxCapacity())
                .build();

        // Get next session only (optimization: previously fetched up to 5 sessions)
        SessionDTO nextSession = sessionRepository.findUpcomingSessions(classEntity.getId(), Pageable.ofSize(1))
                .stream()
                .findFirst()
                .map(this::convertToSessionDTO)
                .orElse(null);

        return ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .course(ClassDetailDTO.CourseInfo.builder()
                        .id(classEntity.getCourse().getId())
                        .name(classEntity.getCourse().getName())
                        .code(classEntity.getCourse().getCode())
                        .build())
                .branch(ClassDetailDTO.BranchInfo.builder()
                        .id(classEntity.getBranch().getId())
                        .name(classEntity.getBranch().getName())
                        .address(classEntity.getBranch().getAddress())
                        .build())
                .modality(classEntity.getModality().toString())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .actualEndDate(classEntity.getActualEndDate())
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus().toString())
                .teachers(teachers)
                .scheduleSummary(generateScheduleSummary(classEntity))
                .enrollmentSummary(enrollmentSummary)
                .nextSession(nextSession)
                .build();
    }

    private SessionDTO convertToSessionDTO(Session session) {
        // Get teachers from teaching slots with proper lazy loading handling
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByClassEntityIdAndStatus(
                session.getClassEntity().getId(), TeachingSlotStatus.SCHEDULED);

        List<String> teachers = teachingSlots.stream()
                .filter(ts -> ts.getSession().getId().equals(session.getId())
                        && ts.getTeacher() != null
                        && ts.getTeacher().getUserAccount() != null
                        && ts.getTeacher().getUserAccount().getFullName() != null
                        && !ts.getTeacher().getUserAccount().getFullName().trim().isEmpty())
                .map(ts -> ts.getTeacher().getUserAccount().getFullName())
                .collect(Collectors.toList());

        return SessionDTO.builder()
                .id(session.getId())
                .classId(session.getClassEntity().getId())
                .date(session.getDate().toString())
                .type(session.getType().toString())
                .status(session.getStatus().toString())
                .room(null) // Session doesn't have direct room relationship
                .teacherNote(session.getTeacherNote())
                .startTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                .endTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                .teachers(teachers)
                .recordedAt(session.getUpdatedAt())
                .build();
    }

    private StudentSessionDTO convertToStudentSessionDTO(StudentSession studentSession) {
        return convertToStudentSessionDTOWithDisplayStatus(studentSession);
    }

    private StudentSessionDTO convertToStudentSessionDTOWithDisplayStatus(StudentSession studentSession) {
        AttendanceStatus displayStatus = resolveDisplayStatusForStudent(studentSession);
        return StudentSessionDTO.builder()
                .sessionId(studentSession.getSession().getId())
                .studentId(studentSession.getStudent().getId())
                .attendanceStatus(displayStatus != null ? displayStatus.toString() : "PLANNED")
                .homeworkStatus(studentSession.getHomeworkStatus() != null ? studentSession.getHomeworkStatus().toString() : "NO_HOMEWORK")
                .isMakeup(studentSession.getIsMakeup())
                .makeupSessionId(studentSession.getMakeupSession() != null ? studentSession.getMakeupSession().getId() : null)
                .originalSessionId(studentSession.getOriginalSession() != null ? studentSession.getOriginalSession().getId() : null)
                .isTransferredOut(studentSession.getIsTransferredOut())
                .note(studentSession.getNote())
                .recordedAt(studentSession.getRecordedAt())
                .build();
    }

    private AssessmentDTO convertToAssessmentDTO(Assessment assessment) {
        String teacherName = null;
        // Get teacher name from teaching slots (simplified approach)
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByClassEntityIdAndStatus(
                assessment.getClassEntity().getId(), TeachingSlotStatus.SCHEDULED);
        if (!teachingSlots.isEmpty()) {
            teacherName = teachingSlots.get(0).getTeacher().getUserAccount().getFullName();
        }

        return AssessmentDTO.builder()
                .id(assessment.getId())
                .classId(assessment.getClassEntity().getId())
                .courseAssessmentId(assessment.getCourseAssessment().getId())
                .name(assessment.getCourseAssessment().getName())
                .description(assessment.getCourseAssessment().getDescription())
                .kind(assessment.getCourseAssessment().getKind().toString())
                .maxScore(assessment.getCourseAssessment().getMaxScore())
                .durationMinutes(assessment.getCourseAssessment().getDurationMinutes())
                .scheduledDate(assessment.getScheduledDate())
                .actualDate(assessment.getActualDate())
                .teacherName(teacherName)
                .build();
    }

    private ClassmateDTO convertToClassmateDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        UserAccount userAccount = student.getUserAccount();

        // Calculate attendance rate for classmate
        List<StudentSession> studentSessions = studentSessionRepository.findAllByStudentId(student.getId())
                .stream()
                .filter(ss -> ss.getSession().getClassEntity().getId().equals(enrollment.getClassId()))
                .collect(Collectors.toList());

        BigDecimal attendanceRate = BigDecimal.ZERO;
        if (!studentSessions.isEmpty()) {
            long attendedCount = studentSessions.stream()
                    .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                    .count();
            attendanceRate = new BigDecimal(attendedCount)
                    .divide(new BigDecimal(studentSessions.size()), 2, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        return ClassmateDTO.builder()
                .studentId(student.getId())
                .fullName(userAccount.getFullName())
                .avatar(null) // UserAccount doesn't have avatarUrl field
                .email(userAccount.getEmail()) // Consider privacy implications
                .studentCode(student.getStudentCode())
                .enrollmentId(enrollment.getId())
                .enrollmentDate(enrollment.getEnrolledAt())
                .enrollmentStatus(enrollment.getStatus().toString())
                .attendanceRate(attendanceRate)
                .build();
    }

    private String generateScheduleSummary(ClassEntity classEntity) {
        // This is a simplified implementation
        // In a real scenario, you'd want to analyze the actual session schedule
        // For now, return a placeholder
        return "Lịch học tạm thời"; // Placeholder
    }

    private StudentTranscriptDTO convertToStudentTranscriptDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();
        Student student = enrollment.getStudent();

        // Get primary teacher name
        String primaryTeacherName = teachingSlotRepository.findByClassEntityIdAndStatus(
                classEntity.getId(), TeachingSlotStatus.SCHEDULED)
                .stream()
                .collect(Collectors.groupingBy(ts -> ts.getTeacher().getId()))
                .values()
                .stream()
                .max(Comparator.comparingInt(List::size))
                .map(teachingSlots -> teachingSlots.get(0).getTeacher().getUserAccount().getFullName())
                .orElse("Chưa phân công");

        // Calculate session statistics
        List<Session> allSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classEntity.getId());
        List<Session> activeSessions = allSessions.stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .collect(Collectors.toList());

        int totalSessions = activeSessions.size();
        int completedSessions = (int) activeSessions.stream()
                .filter(session -> session.getDate().isBefore(LocalDate.now()) || session.getStatus() != SessionStatus.PLANNED)
                .count();

        // Get assessments and scores for this student in this class
        List<Assessment> assessments = assessmentRepository.findByClassEntityId(classEntity.getId());
        List<Score> scores = scoreRepository.findByStudentIdAndClassId(student.getId(), classEntity.getId());

        // Build component scores map
        Map<String, BigDecimal> componentScores = new HashMap<>();
        for (Score score : scores) {
            if (score.getAssessment() != null && score.getAssessment().getCourseAssessment() != null) {
                String assessmentName = score.getAssessment().getCourseAssessment().getName();
                componentScores.put(assessmentName, score.getScore());
            }
        }

        // Calculate average score
        BigDecimal averageScore = null;
        if (!componentScores.isEmpty()) {
            BigDecimal sum = componentScores.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageScore = sum.divide(new BigDecimal(componentScores.size()), 2, RoundingMode.HALF_UP);
        }

        // Determine completed date
        LocalDate completedDate = null;
        if (classEntity.getStatus() == ClassStatus.COMPLETED && classEntity.getActualEndDate() != null) {
            completedDate = classEntity.getActualEndDate();
        }

        return StudentTranscriptDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .teacherName(primaryTeacherName)
                .status(classEntity.getStatus().toString())
                .averageScore(averageScore)
                .componentScores(componentScores)
                .completedDate(completedDate)
                .totalSessions(totalSessions)
                .completedSessions(completedSessions)
                .build();
    }

    /**
     * Hiển thị trạng thái cho học viên:
     * - Nếu ngày session < hôm nay và status là null hoặc PLANNED → ABSENT
     * - Ngược lại dùng status thực tế (PRESENT / ABSENT / PLANNED)
     */
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
}
