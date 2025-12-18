package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.attendance.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private static final List<TeachingSlotStatus> OWNERSHIP_STATUSES = List.of(
            TeachingSlotStatus.SCHEDULED,
            TeachingSlotStatus.SUBSTITUTED
    );

    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    public List<SessionTodayDTO> getSessionsForDate(Long teacherId, LocalDate date) {
        List<TeachingSlot> slots = teachingSlotRepository.findByTeacherIdAndDate(teacherId, date);
        if (slots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> sessionIds = slots.stream()
                .map(slot -> slot.getSession().getId())
                .distinct()
                .toList();

        Map<Long, List<StudentSession>> sessionStudentMap = studentSessionRepository.findBySessionIds(sessionIds)
                .stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        return slots.stream()
                .map(TeachingSlot::getSession)
                .collect(Collectors.toMap(Session::getId, session -> session, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .map(session -> {
                    List<StudentSession> studentSessions = sessionStudentMap.getOrDefault(session.getId(), List.of());
                    AttendanceSummaryDTO summary = buildSummary(studentSessions);
                    boolean submitted = studentSessions.stream()
                            .anyMatch(ss -> ss.getAttendanceStatus() != null && ss.getAttendanceStatus() != AttendanceStatus.PLANNED);
                    return SessionTodayDTO.builder()
                            .sessionId(session.getId())
                            .classId(session.getClassEntity().getId())
                            .classCode(session.getClassEntity().getCode())
                            .className(session.getClassEntity().getName())
                            .subjectCode(session.getClassEntity().getSubject().getCode())
                            .subjectName(session.getClassEntity().getSubject().getName())
                            .date(session.getDate())
                            .startTime(session.getTimeSlotTemplate().getStartTime())
                            .endTime(session.getTimeSlotTemplate().getEndTime())
                            .status(session.getStatus().name())
                            .attendanceSubmitted(submitted)
                            .totalStudents(summary.getTotalStudents())
                            .presentCount(summary.getPresentCount())
                            .absentCount(summary.getAbsentCount())
                            .build();
                })
                .toList();
    }

    public StudentsAttendanceResponseDTO getSessionStudents(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Session previousSession = findPreviousSession(session);

        Long classId = session.getClassEntity().getId();
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);

        List<StudentSession> existingStudentSessions = studentSessionRepository.findBySessionId(sessionId);
        Map<Long, StudentSession> studentSessionMap = existingStudentSessions.stream()
                .collect(Collectors.toMap(ss -> ss.getStudent().getId(), ss -> ss));

        List<StudentAttendanceDTO> students = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            StudentSession studentSession = studentSessionMap.get(studentId);

            if (studentSession != null) {
                students.add(toStudentAttendanceDTO(studentSession, previousSession));
            } else {
                boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                        (session.getDate().equals(LocalDate.now()) &&
                                session.getStatus() == SessionStatus.PLANNED);

                AttendanceStatus defaultStatus = isFutureSession
                        ? AttendanceStatus.PLANNED
                        : AttendanceStatus.ABSENT;

                boolean hasPreviousHomework;
                HomeworkStatus homeworkStatus;
                if (previousSession != null && previousSession.getSubjectSession() != null &&
                        previousSession.getSubjectSession().getStudentTask() != null &&
                        !previousSession.getSubjectSession().getStudentTask().trim().isEmpty()) {
                    hasPreviousHomework = true;
                    homeworkStatus = null;
                } else {
                    hasPreviousHomework = false;
                    homeworkStatus = HomeworkStatus.NO_HOMEWORK;
                }

                students.add(StudentAttendanceDTO.builder()
                        .studentId(studentId)
                        .studentCode(enrollment.getStudent().getStudentCode())
                        .fullName(enrollment.getStudent().getUserAccount().getFullName())
                        .attendanceStatus(defaultStatus)
                        .homeworkStatus(homeworkStatus)
                        .hasPreviousHomework(hasPreviousHomework)
                        .note(null)
                        .makeup(false)
                        .makeupSessionId(null)
                        .build());
            }
        }

        List<StudentSession> allStudentSessions = new ArrayList<>(existingStudentSessions);
        AttendanceSummaryDTO summary = buildSummary(allStudentSessions);

        // Check if session has homework
        boolean hasHomework = session.getSubjectSession() != null &&
                session.getSubjectSession().getStudentTask() != null &&
                !session.getSubjectSession().getStudentTask().trim().isEmpty();

        return StudentsAttendanceResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .subjectCode(session.getClassEntity().getSubject().getCode())
                .subjectName(session.getClassEntity().getSubject().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : null)
                .sessionStartTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                .sessionEndTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                .sessionTopic(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .summary(summary)
                .students(students)
                .hasHomework(hasHomework)
                .build();
    }

    @Transactional
    public AttendanceSaveResponseDTO saveAttendance(Long teacherId, Long sessionId, AttendanceSaveRequestDTO request) {
        assertOwnership(teacherId, sessionId);
        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new CustomException(ErrorCode.ATTENDANCE_RECORDS_EMPTY);
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!canEditAttendance(session)) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }

        Session previousSession = findPreviousSession(session);
        boolean hasPreviousHomework = previousSession != null
                && previousSession.getSubjectSession() != null
                && previousSession.getSubjectSession().getStudentTask() != null
                && !previousSession.getSubjectSession().getStudentTask().trim().isEmpty();

        OffsetDateTime now = OffsetDateTime.now();
        for (AttendanceRecordDTO record : request.getRecords()) {
            StudentSession.StudentSessionId id = new StudentSession.StudentSessionId(record.getStudentId(), sessionId);
            StudentSession studentSession = studentSessionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Student is not part of this session"));
            studentSession.setAttendanceStatus(record.getAttendanceStatus());

            if (record.getHomeworkStatus() != null) {
                if (!hasPreviousHomework && record.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK) {
                    throw new CustomException(ErrorCode.HOMEWORK_STATUS_INVALID_NO_PREVIOUS_HOMEWORK);
                }
                if (hasPreviousHomework && record.getHomeworkStatus() == HomeworkStatus.NO_HOMEWORK) {
                    throw new CustomException(ErrorCode.HOMEWORK_STATUS_INVALID_HAS_PREVIOUS_HOMEWORK);
                }
            }

            studentSession.setHomeworkStatus(record.getHomeworkStatus());
            studentSession.setNote(record.getNote());
            studentSession.setRecordedAt(now);
        }

        List<StudentSession> updatedSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(updatedSessions);

        return AttendanceSaveResponseDTO.builder()
                .sessionId(sessionId)
                .summary(summary)
                .build();
    }

    @Transactional
    public MarkAllResponseDTO markAllPresent(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!canEditAttendance(session)) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                .totalStudents(studentSessions.size())
                .presentCount(studentSessions.size())
                .absentCount(0)
                .build();
        return MarkAllResponseDTO.builder()
                .sessionId(sessionId)
                .summary(summary)
                .build();
    }

    @Transactional
    public MarkAllResponseDTO markAllAbsent(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!canEditAttendance(session)) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                .totalStudents(studentSessions.size())
                .presentCount(0)
                .absentCount(studentSessions.size())
                .build();
        return MarkAllResponseDTO.builder()
                .sessionId(sessionId)
                .summary(summary)
                .build();
    }

    public SessionReportResponseDTO getSessionReport(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getTimeSlotTemplate() != null) {
            session.getTimeSlotTemplate().getStartTime();
            session.getTimeSlotTemplate().getEndTime();
        }
        if (session.getSubjectSession() != null) {
            session.getSubjectSession().getTopic();
        }

        String teacherName = getTeacherNameForSession(sessionId);

        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        return SessionReportResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .subjectCode(session.getClassEntity().getSubject().getCode())
                .subjectName(session.getClassEntity().getSubject().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : null)
                .sessionStartTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                .sessionEndTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                .sessionTopic(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .summary(summary)
                .build();
    }

    @Transactional
    public SessionReportResponseDTO submitSessionReport(Long teacherId, Long sessionId, SessionReportSubmitDTO request) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (!canEditAttendance(session)) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }
        session.setTeacherNote(request.getTeacherNote());
        session.setStatus(SessionStatus.DONE);

        if (session.getTimeSlotTemplate() != null) {
            session.getTimeSlotTemplate().getStartTime();
            session.getTimeSlotTemplate().getEndTime();
        }
        if (session.getSubjectSession() != null) {
            session.getSubjectSession().getTopic();
        }

        String teacherName = getTeacherNameForSession(sessionId);

        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        return SessionReportResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .subjectCode(session.getClassEntity().getSubject().getCode())
                .subjectName(session.getClassEntity().getSubject().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : null)
                .sessionStartTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                .sessionEndTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                .sessionTopic(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .summary(summary)
                .build();
    }

    public AttendanceMatrixDTO getClassAttendanceMatrix(Long teacherId, Long classId) {
        List<Session> sessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId).stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .toList();
        if (sessions.isEmpty()) {
            throw new ResourceNotFoundException("Class has no sessions");
        }

        boolean ownsAtLeastOne = sessions.stream()
                .anyMatch(session -> teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(
                        session.getId(),
                        teacherId,
                        OWNERSHIP_STATUSES
                ));

        if (!ownsAtLeastOne) {
            throw new AccessDeniedException("Teacher does not own this class");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        Set<Long> enrolledStudentIds = enrollments.stream()
                .map(Enrollment::getStudentId)
                .collect(Collectors.toSet());

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();

        // Map các bản ghi điểm danh theo session gốc trong lớp
        Map<Long, List<StudentSession>> sessionStudentMap = studentSessionRepository.findBySessionIds(sessionIds)
                .stream()
                .filter(ss -> enrolledStudentIds.contains(ss.getStudent().getId()))
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        // Map thông tin học bù theo (originalSessionId, studentId) để tính cờ xanh/đỏ
        Map<Long, Map<Long, Boolean>> makeupCompletedMap = new HashMap<>();
        studentSessionRepository.findMakeupSessionsByOriginalSessionIds(sessionIds)
                .forEach(ss -> {
                    Session originalSession = ss.getOriginalSession();
                    if (originalSession == null || ss.getStudent() == null) {
                        return;
                    }
                    Long originalSessionId = originalSession.getId();
                    Long studentId = ss.getStudent().getId();
                    if (originalSessionId == null || studentId == null) {
                        return;
                    }

                    makeupCompletedMap
                            .computeIfAbsent(originalSessionId, k -> new HashMap<>())
                            .merge(
                                    studentId,
                                    ss.getAttendanceStatus() == AttendanceStatus.PRESENT,
                                    (oldVal, newVal) -> oldVal || newVal
                            );
                });

        Map<Long, StudentAttendanceMatrixDTO.StudentAttendanceMatrixDTOBuilder> rowBuilders = new LinkedHashMap<>();
        Map<Long, Map<Long, StudentAttendanceMatrixDTO.Cell.CellBuilder>> cellBuilders = new HashMap<>();

        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            rowBuilders.put(studentId, StudentAttendanceMatrixDTO.builder()
                    .studentId(studentId)
                    .studentCode(enrollment.getStudent().getStudentCode())
                    .fullName(enrollment.getStudent().getUserAccount().getFullName())
                    .cells(new ArrayList<>()));
            cellBuilders.put(studentId, new HashMap<>());
        }

        for (Session session : sessions) {
            List<StudentSession> studentSessions = sessionStudentMap.getOrDefault(session.getId(), List.of());
            for (StudentSession ss : studentSessions) {
                Long studentId = ss.getStudent().getId();
                if (!enrolledStudentIds.contains(studentId)) {
                    continue;
                }

                AttendanceStatus displayStatus = resolveMatrixDisplayStatus(ss, session);

                 boolean isMakeup = Boolean.TRUE.equals(ss.getIsMakeup());

                 // Mặc định không hiển thị chấm
                 boolean hasMakeupCompleted = false;
                 boolean hasMakeupPlanned = false;

                 // Chỉ áp dụng logic chấm cho buổi gốc có phép (E), không phải bản ghi học bù
                 if (!isMakeup && ss.getAttendanceStatus() == AttendanceStatus.EXCUSED) {
                     // Kiểm tra xem đã có buổi học bù PRESENT hay chưa
                     boolean completed = makeupCompletedMap
                             .getOrDefault(session.getId(), Map.of())
                             .getOrDefault(studentId, false);

                     hasMakeupCompleted = completed;

                     // Tính xem đã qua thời gian kết thúc buổi học gốc chưa
                     LocalDate sessionDate = session.getDate();
                     LocalDateTime now = LocalDateTime.now();

                     LocalDateTime sessionEndDateTime;
                     if (session.getTimeSlotTemplate() != null && session.getTimeSlotTemplate().getEndTime() != null) {
                         LocalTime endTime = session.getTimeSlotTemplate().getEndTime();
                         sessionEndDateTime = LocalDateTime.of(sessionDate, endTime);
                     } else {
                         sessionEndDateTime = LocalDateTime.of(sessionDate, LocalTime.MAX);
                     }

                     boolean isAfterSessionEnd = now.isAfter(sessionEndDateTime);

                     // Chỉ tô đỏ khi đã qua giờ kết thúc buổi gốc và chưa có buổi bù PRESENT
                     if (!completed && isAfterSessionEnd) {
                         hasMakeupPlanned = true;
                     }
                 }

                cellBuilders.get(studentId)
                        .put(session.getId(), StudentAttendanceMatrixDTO.Cell.builder()
                                .sessionId(session.getId())
                                .attendanceStatus(displayStatus)
                                .homeworkStatus(ss.getHomeworkStatus())
                                .makeup(isMakeup)
                                .hasMakeupPlanned(hasMakeupPlanned)
                                .hasMakeupCompleted(hasMakeupCompleted));
            }
        }

        List<SessionMatrixInfoDTO> sessionDtos = sessions.stream()
                .map(session -> SessionMatrixInfoDTO.builder()
                        .sessionId(session.getId())
                        .date(session.getDate())
                        .startTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                        .endTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                        .status(session.getStatus().name())
                        .build())
                .toList();

        List<StudentAttendanceMatrixDTO> studentDtos = rowBuilders.values().stream()
                .map(builder -> {
                    Long studentId = builder.build().getStudentId();
                    Map<Long, StudentAttendanceMatrixDTO.Cell.CellBuilder> cellsBySession = cellBuilders.getOrDefault(studentId, Map.of());
                    List<StudentAttendanceMatrixDTO.Cell> cells = sessions.stream()
                            .map(session -> {
                                StudentAttendanceMatrixDTO.Cell.CellBuilder cellBuilder = cellsBySession.get(session.getId());
                                if (cellBuilder != null) {
                                    return cellBuilder.build();
                                } else {
                                    boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                                            (session.getDate().equals(LocalDate.now()) &&
                                                    session.getStatus() == SessionStatus.PLANNED);

                                    AttendanceStatus displayStatus = isFutureSession
                                            ? AttendanceStatus.PLANNED
                                            : AttendanceStatus.ABSENT;

                                    return StudentAttendanceMatrixDTO.Cell.builder()
                                            .sessionId(session.getId())
                                            .attendanceStatus(displayStatus)
                                            .homeworkStatus(null)
                                            .makeup(false)
                                            .hasMakeupPlanned(false)
                                            .hasMakeupCompleted(false)
                                            .build();
                                }
                            })
                            .toList();

                    double studentAttendanceRate = calculateStudentAttendanceRate(cells);

                    return StudentAttendanceMatrixDTO.builder()
                            .studentId(studentId)
                            .studentCode(builder.build().getStudentCode())
                            .fullName(builder.build().getFullName())
                            .attendanceRate(studentAttendanceRate)
                            .cells(cells)
                            .build();
                })
                .sorted(Comparator.comparing(StudentAttendanceMatrixDTO::getStudentCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        ClassEntity classEntity = sessions.get(0).getClassEntity();

        double classAttendanceRate = studentDtos.stream()
                .filter(student -> student.getAttendanceRate() != null)
                .mapToDouble(StudentAttendanceMatrixDTO::getAttendanceRate)
                .average()
                .orElse(0.0);

        return AttendanceMatrixDTO.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .attendanceRate(classAttendanceRate)
                .sessions(sessionDtos)
                .students(studentDtos)
                .build();
    }

    public List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId) {
        List<ClassEntity> classes = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId);
        return classes.stream()
                .map(this::mapToTeacherClassListItemDTO)
                .collect(Collectors.toList());
    }

    private void assertOwnership(Long teacherId, Long sessionId) {
        boolean owns = teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(
                sessionId,
                teacherId,
                OWNERSHIP_STATUSES
        );
        if (!owns) {
            throw new AccessDeniedException("Teacher does not own this session");
        }
    }

    private StudentAttendanceDTO toStudentAttendanceDTO(StudentSession studentSession, Session previousSession) {
        AttendanceStatus status = resolveDisplayStatus(studentSession);
        Session makeupSession = studentSession.getMakeupSession();

        HomeworkStatus homeworkStatus = studentSession.getHomeworkStatus();
        Boolean hasPreviousHomework = false;

        if (previousSession != null) {
            SubjectSession previousSubjectSession = previousSession.getSubjectSession();
            if (previousSubjectSession != null &&
                    previousSubjectSession.getStudentTask() != null &&
                    !previousSubjectSession.getStudentTask().trim().isEmpty()) {
                hasPreviousHomework = true;
                if (homeworkStatus == null) {
                    homeworkStatus = null;
                }
            } else {
                hasPreviousHomework = false;
                if (homeworkStatus == null) {
                    homeworkStatus = HomeworkStatus.NO_HOMEWORK;
                }
            }
        } else {
            hasPreviousHomework = false;
            if (homeworkStatus == null) {
                homeworkStatus = HomeworkStatus.NO_HOMEWORK;
            }
        }

        return StudentAttendanceDTO.builder()
                .studentId(studentSession.getStudent().getId())
                .studentCode(studentSession.getStudent().getStudentCode())
                .fullName(studentSession.getStudent().getUserAccount().getFullName())
                .attendanceStatus(status)
                .homeworkStatus(homeworkStatus)
                .hasPreviousHomework(hasPreviousHomework)
                .note(studentSession.getNote())
                .makeup(Boolean.TRUE.equals(studentSession.getIsMakeup()))
                .makeupSessionId(makeupSession != null ? makeupSession.getId() : null)
                .build();
    }

    private AttendanceStatus resolveDisplayStatus(StudentSession studentSession) {
        AttendanceStatus status = studentSession.getAttendanceStatus();
        if (status == null || status == AttendanceStatus.PLANNED) {
            return AttendanceStatus.ABSENT;
        }
        return status;
    }

    private AttendanceStatus resolveMatrixDisplayStatus(StudentSession studentSession, Session session) {
        AttendanceStatus status = studentSession.getAttendanceStatus();

        if (status == null || status == AttendanceStatus.PLANNED) {
            boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                    (session.getDate().equals(LocalDate.now()) &&
                            session.getStatus() == SessionStatus.PLANNED);

            return isFutureSession ? AttendanceStatus.PLANNED : AttendanceStatus.ABSENT;
        }

        return status;
    }

    private AttendanceSummaryDTO buildSummary(Collection<StudentSession> studentSessions) {
        int total = studentSessions.size();
        int present = 0;
        int absent = 0;
        for (StudentSession ss : studentSessions) {
            AttendanceStatus status = resolveDisplayStatus(ss);
            if (status == AttendanceStatus.PRESENT) {
                present++;
            } else {
                absent++;
            }
        }
        return AttendanceSummaryDTO.builder()
                .totalStudents(total)
                .presentCount(present)
                .absentCount(absent)
                .build();
    }

    private Session findPreviousSession(Session currentSession) {
        List<Session> previousSessions = sessionRepository.findPreviousSessionsByClassIdAndDate(
                currentSession.getClassEntity().getId(),
                currentSession.getDate()
        );
        if (previousSessions.isEmpty()) {
            return null;
        }
        Session previousSession = previousSessions.get(0);
        if (previousSession.getSubjectSession() != null) {
            previousSession.getSubjectSession().getStudentTask();
        }
        return previousSession;
    }

    private String getTeacherNameForSession(Long sessionId) {
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findBySessionIdWithTeacher(sessionId);
        if (teachingSlots.isEmpty()) {
            return null;
        }
        TeachingSlot teachingSlot = teachingSlots.get(0);
        if (teachingSlot.getTeacher() != null && teachingSlot.getTeacher().getUserAccount() != null) {
            return teachingSlot.getTeacher().getUserAccount().getFullName();
        }
        return null;
    }

    private TeacherClassListItemDTO mapToTeacherClassListItemDTO(ClassEntity classEntity) {
        long totalSessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classEntity.getId()).stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .count();
        double attendanceRate = calculateClassAttendanceRate(classEntity.getId());
        return TeacherClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .subjectName(classEntity.getSubject() != null ? classEntity.getSubject().getName() : null)
                .subjectCode(classEntity.getSubject() != null ? classEntity.getSubject().getCode() : null)
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                .branchCode(classEntity.getBranch() != null ? classEntity.getBranch().getCode() : null)
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus().name())
                .totalSessions((int) totalSessions)
                .attendanceRate(attendanceRate)
                .build();
    }

    private double calculateStudentAttendanceRate(List<StudentAttendanceMatrixDTO.Cell> cells) {
        int present = 0;
        int absent = 0;

        for (StudentAttendanceMatrixDTO.Cell cell : cells) {
            AttendanceStatus status = cell.getAttendanceStatus();
            if (status == AttendanceStatus.PRESENT) {
                present++;
            } else if (status == AttendanceStatus.ABSENT) {
                absent++;
            }
        }

        int total = present + absent;
        return total > 0 ? (double) present / total : 0.0;
    }

    private double calculateClassAttendanceRate(Long classId) {
        // Chỉ tính các buổi đã điểm danh (đã học), không tính các buổi chưa học
        List<Session> sessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId).stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .toList();

        if (sessions.isEmpty()) {
            return 0.0;
        }

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();

        // Lấy tất cả bản ghi điểm danh thực sự có trong DB
        List<StudentSession> allStudentSessions = studentSessionRepository.findBySessionIds(sessionIds);

        // Bỏ qua các bản ghi học bù (isMakeup = true) để không làm tăng tỷ lệ của lớp học bù
        List<StudentSession> primarySessions = allStudentSessions.stream()
                .filter(ss -> !Boolean.TRUE.equals(ss.getIsMakeup()))
                .toList();

        // Chỉ tính các bản ghi có status PRESENT hoặc ABSENT (đã điểm danh)
        long totalPresent = primarySessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                .count();
        long totalRecorded = primarySessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT
                        || ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();

        if (totalRecorded == 0) {
            return 0.0;
        }

        return (double) totalPresent / totalRecorded;
    }

    private boolean canEditAttendance(Session session) {
        if (session == null || session.getDate() == null) {
            return false;
        }

        SessionStatus status = session.getStatus();
        
        // Không cho phép điểm danh nếu session bị hủy
        if (status == SessionStatus.CANCELLED) {
            return false;
        }

        LocalDate sessionDate = session.getDate();
        LocalDateTime now = LocalDateTime.now();

        // Tính toán thời gian bắt đầu và kết thúc
        LocalDateTime sessionStartDateTime;
        LocalDateTime sessionEndDateTime;
        
        if (session.getTimeSlotTemplate() != null) {
            if (session.getTimeSlotTemplate().getStartTime() != null) {
                LocalTime startTime = session.getTimeSlotTemplate().getStartTime();
                sessionStartDateTime = LocalDateTime.of(sessionDate, startTime);
            } else {
                sessionStartDateTime = LocalDateTime.of(sessionDate, LocalTime.MIN);
            }
            
            if (session.getTimeSlotTemplate().getEndTime() != null) {
                LocalTime endTime = session.getTimeSlotTemplate().getEndTime();
                sessionEndDateTime = LocalDateTime.of(sessionDate, endTime);
            } else {
                sessionEndDateTime = LocalDateTime.of(sessionDate, LocalTime.MAX);
            }
        } else {
            sessionStartDateTime = LocalDateTime.of(sessionDate, LocalTime.MIN);
            sessionEndDateTime = LocalDateTime.of(sessionDate, LocalTime.MAX);
        }

        // Cho phép điểm danh nếu:
        // 1. Session đang diễn ra (đã đến giờ bắt đầu và chưa kết thúc)
        // 2. Hoặc session đã kết thúc nhưng chưa quá 48 giờ
        boolean isSessionOngoing = !now.isBefore(sessionStartDateTime) && !now.isAfter(sessionEndDateTime);
        boolean isWithin48HoursAfterEnd = now.isAfter(sessionEndDateTime);
        
        if (isSessionOngoing) {
            return true; // Cho phép điểm danh trong khi buổi học đang diễn ra
        }
        
        if (isWithin48HoursAfterEnd) {
            // Cho phép chỉnh sửa trong vòng 48 giờ sau khi kết thúc
            LocalDate deadlineDate = sessionDate.plusDays(2);
            LocalDateTime deadline = LocalDateTime.of(deadlineDate, LocalTime.MAX);
            return !now.isAfter(deadline);
        }

        // Nếu chưa đến giờ bắt đầu, không cho phép điểm danh
        return false;
    }
}

