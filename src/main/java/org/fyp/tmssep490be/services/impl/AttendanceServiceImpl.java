package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.attendance.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.AttendanceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private static final List<TeachingSlotStatus> OWNERSHIP_STATUSES = List.of(
            TeachingSlotStatus.SCHEDULED,
            TeachingSlotStatus.SUBSTITUTED
    );

    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
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
                            .courseCode(session.getClassEntity().getCourse().getCode())
                            .courseName(session.getClassEntity().getCourse().getName())
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

    @Override
    public StudentsAttendanceResponseDTO getSessionStudents(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Find previous session in the same class to get homework assignment
        Session previousSession = findPreviousSession(session);

        // Get all enrolled students in the class (consistent with matrix API)
        Long classId = session.getClassEntity().getId();
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        
        // Get existing StudentSession records for this session
        List<StudentSession> existingStudentSessions = studentSessionRepository.findBySessionId(sessionId);
        Map<Long, StudentSession> studentSessionMap = existingStudentSessions.stream()
                .collect(Collectors.toMap(ss -> ss.getStudent().getId(), ss -> ss));

        // Build list of all enrolled students with their attendance status
        List<StudentAttendanceDTO> students = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            StudentSession studentSession = studentSessionMap.get(studentId);
            
            if (studentSession != null) {
                // Student has StudentSession record - use it
                students.add(toStudentAttendanceDTO(studentSession, previousSession));
            } else {
                // Student doesn't have StudentSession record yet - create DTO with default values
                boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                        (session.getDate().equals(LocalDate.now()) && 
                         session.getStatus() == SessionStatus.PLANNED);
                
                AttendanceStatus defaultStatus = isFutureSession 
                        ? AttendanceStatus.PLANNED 
                        : AttendanceStatus.ABSENT;
                
                // Determine homework status based on previous session
                Boolean hasPreviousHomework = false;
                HomeworkStatus homeworkStatus = null;
                if (previousSession != null) {
                    CourseSession previousCourseSession = previousSession.getCourseSession();
                    if (previousCourseSession != null && 
                        previousCourseSession.getStudentTask() != null && 
                        !previousCourseSession.getStudentTask().trim().isEmpty()) {
                        hasPreviousHomework = true;
                        homeworkStatus = null; // Allow teacher to choose COMPLETED/INCOMPLETE
                    } else {
                        hasPreviousHomework = false;
                        homeworkStatus = HomeworkStatus.NO_HOMEWORK;
                    }
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

        // Build summary from all students (including those without StudentSession records)
        List<StudentSession> allStudentSessions = new ArrayList<>(existingStudentSessions);
        AttendanceSummaryDTO summary = buildSummary(allStudentSessions);

        return StudentsAttendanceResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .courseCode(session.getClassEntity().getCourse().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate().getName())
                .summary(summary)
                .students(students)
                .build();
    }

    @Override
    @Transactional
    public AttendanceSaveResponseDTO saveAttendance(Long teacherId, Long sessionId, AttendanceSaveRequestDTO request) {
        assertOwnership(teacherId, sessionId);
        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new CustomException(ErrorCode.ATTENDANCE_RECORDS_EMPTY);
        }

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getStatus() == SessionStatus.DONE) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }

        // Find previous session to validate homework assignment
        Session previousSession = findPreviousSession(session);
        boolean hasPreviousHomework = previousSession != null 
                && previousSession.getCourseSession() != null
                && previousSession.getCourseSession().getStudentTask() != null
                && !previousSession.getCourseSession().getStudentTask().trim().isEmpty();

        OffsetDateTime now = OffsetDateTime.now();
        for (AttendanceRecordDTO record : request.getRecords()) {
            StudentSession.StudentSessionId id = new StudentSession.StudentSessionId(record.getStudentId(), sessionId);
            StudentSession studentSession = studentSessionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Student is not part of this session"));
            studentSession.setAttendanceStatus(record.getAttendanceStatus());
            
            // Validate homework status: only allow COMPLETED/INCOMPLETE if previous session has homework
            // Allow NO_HOMEWORK if previous session has no homework
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

    @Override
    @Transactional
    public MarkAllResponseDTO markAllPresent(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getStatus() == SessionStatus.DONE) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        // Do not persist changes here. Only propose the summary as if all are PRESENT.
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

    @Override
    @Transactional
    public MarkAllResponseDTO markAllAbsent(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getStatus() == SessionStatus.DONE) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        // Do not persist changes here. Only propose the summary as if all are ABSENT.
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

    @Override
    public SessionReportResponseDTO getSessionReport(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        
        // Force load related entities
        if (session.getTimeSlotTemplate() != null) {
            session.getTimeSlotTemplate().getStartTime(); // Force load
            session.getTimeSlotTemplate().getEndTime(); // Force load
        }
        if (session.getCourseSession() != null) {
            session.getCourseSession().getTopic(); // Force load
        }
        
        // Get teacher name from teaching slot
        String teacherName = getTeacherNameForSession(sessionId);
        
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        return SessionReportResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .courseCode(session.getClassEntity().getCourse().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : null)
                .sessionStartTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                .sessionEndTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                .sessionTopic(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .summary(summary)
                .build();
    }

    @Override
    @Transactional
    public SessionReportResponseDTO submitSessionReport(Long teacherId, Long sessionId, SessionReportSubmitDTO request) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getStatus() == SessionStatus.DONE) {
            throw new CustomException(ErrorCode.SESSION_ALREADY_DONE);
        }
        session.setTeacherNote(request.getTeacherNote());
        // Mark session as DONE upon report submission
        session.setStatus(SessionStatus.DONE);

        // Force load related entities
        if (session.getTimeSlotTemplate() != null) {
            session.getTimeSlotTemplate().getStartTime(); // Force load
            session.getTimeSlotTemplate().getEndTime(); // Force load
        }
        if (session.getCourseSession() != null) {
            session.getCourseSession().getTopic(); // Force load
        }
        
        // Get teacher name from teaching slot
        String teacherName = getTeacherNameForSession(sessionId);

        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        return SessionReportResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .courseCode(session.getClassEntity().getCourse().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getName() : null)
                .sessionStartTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime() : null)
                .sessionEndTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime() : null)
                .sessionTopic(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .summary(summary)
                .build();
    }

    @Override
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

        // Get all enrolled students first (consistent with Get Session Students API)
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        Set<Long> enrolledStudentIds = enrollments.stream()
                .map(Enrollment::getStudentId)
                .collect(Collectors.toSet());

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();
        Map<Long, List<StudentSession>> sessionStudentMap = studentSessionRepository.findBySessionIds(sessionIds)
                .stream()
                .filter(ss -> enrolledStudentIds.contains(ss.getStudent().getId())) // Only include enrolled students
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        Map<Long, StudentAttendanceMatrixDTO.StudentAttendanceMatrixDTOBuilder> rowBuilders = new LinkedHashMap<>();
        Map<Long, Map<Long, StudentAttendanceMatrixDTO.Cell.CellBuilder>> cellBuilders = new HashMap<>();

        // Initialize row builders for all enrolled students
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            rowBuilders.put(studentId, StudentAttendanceMatrixDTO.builder()
                    .studentId(studentId)
                    .studentCode(enrollment.getStudent().getStudentCode())
                    .fullName(enrollment.getStudent().getUserAccount().getFullName())
                    .cells(new ArrayList<>()));
            cellBuilders.put(studentId, new HashMap<>());
        }

        // Process StudentSession records for enrolled students only
        for (Session session : sessions) {
            List<StudentSession> studentSessions = sessionStudentMap.getOrDefault(session.getId(), List.of());
            for (StudentSession ss : studentSessions) {
                Long studentId = ss.getStudent().getId();
                
                // Only process if student is enrolled (should already be filtered, but double-check)
                if (!enrolledStudentIds.contains(studentId)) {
                    continue;
                }

                // Determine attendance status for matrix display
                AttendanceStatus displayStatus = resolveMatrixDisplayStatus(ss, session);

                cellBuilders.get(studentId)
                        .put(session.getId(), StudentAttendanceMatrixDTO.Cell.builder()
                                .sessionId(session.getId())
                                .attendanceStatus(displayStatus)
                                .makeup(Boolean.TRUE.equals(ss.getIsMakeup())));
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
                                    // Session đã có StudentSession record (đã điểm danh hoặc đã tạo record)
                                    return cellBuilder.build();
                                } else {
                                    // Session chưa có StudentSession record
                                    // Kiểm tra xem session đã diễn ra chưa
                                    boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                                            (session.getDate().equals(LocalDate.now()) && 
                                             session.getStatus() == SessionStatus.PLANNED);
                                    
                                    AttendanceStatus displayStatus = isFutureSession 
                                            ? AttendanceStatus.PLANNED  // Session chưa diễn ra
                                            : AttendanceStatus.ABSENT; // Session đã diễn ra nhưng chưa điểm danh
                                    
                                    return StudentAttendanceMatrixDTO.Cell.builder()
                                            .sessionId(session.getId())
                                            .attendanceStatus(displayStatus)
                                            .makeup(false)
                                            .build();
                                }
                            })
                            .toList();
                    
                    // Tính tỷ lệ chuyên cần của học viên (chỉ tính PRESENT và ABSENT, không tính PLANNED)
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
        
        // Tính tỷ lệ chuyên cần của cả lớp từ danh sách học viên đã tính sẵn
        // Đảm bảo tính nhất quán với tỷ lệ của từng học viên (cùng nguồn dữ liệu)
        double classAttendanceRate = studentDtos.stream()
                .filter(student -> student.getAttendanceRate() != null)
                .mapToDouble(StudentAttendanceMatrixDTO::getAttendanceRate)
                .average()
                .orElse(0.0);

        return AttendanceMatrixDTO.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getCourse() != null ? classEntity.getCourse().getName() : null)
                .attendanceRate(classAttendanceRate)
                .sessions(sessionDtos)
                .students(studentDtos)
                .build();
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
        
        // Determine homework status based on previous session
        // If homework status is already set (by teacher), use it
        // Otherwise, check if previous session has homework assignment
        HomeworkStatus homeworkStatus = studentSession.getHomeworkStatus();
        Boolean hasPreviousHomework = false;
        
        if (previousSession != null) {
            CourseSession previousCourseSession = previousSession.getCourseSession();
            if (previousCourseSession != null && 
                previousCourseSession.getStudentTask() != null && 
                !previousCourseSession.getStudentTask().trim().isEmpty()) {
                hasPreviousHomework = true;
                // If homework status is not set yet, keep it null so frontend can show "not checked" state
                if (homeworkStatus == null) {
                    homeworkStatus = null; // Allows teacher to choose COMPLETED/INCOMPLETE
                }
            } else {
                hasPreviousHomework = false;
                // If homework status is not set yet, set to NO_HOMEWORK
                if (homeworkStatus == null) {
                    homeworkStatus = HomeworkStatus.NO_HOMEWORK;
                }
            }
        } else {
            // No previous session (first session of class) - no homework to check
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

    /**
     * Resolve attendance status for matrix display
     * If session hasn't happened yet (future date or today with PLANNED status), return PLANNED
     * Otherwise, use the actual attendance status from StudentSession
     */
    private AttendanceStatus resolveMatrixDisplayStatus(StudentSession studentSession, Session session) {
        AttendanceStatus status = studentSession.getAttendanceStatus();
        
        // If status is null or PLANNED, check if session has happened
        if (status == null || status == AttendanceStatus.PLANNED) {
            boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                    (session.getDate().equals(LocalDate.now()) && 
                     session.getStatus() == SessionStatus.PLANNED);
            
            return isFutureSession ? AttendanceStatus.PLANNED : AttendanceStatus.ABSENT;
        }
        
        // Return actual status (PRESENT or ABSENT)
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

    /**
     * Find the previous session in the same class before the current session
     * Used to get homework assignment from previous session
     */
    private Session findPreviousSession(Session currentSession) {
        List<Session> previousSessions = sessionRepository.findPreviousSessionsByClassIdAndDate(
                currentSession.getClassEntity().getId(),
                currentSession.getDate()
        );
        if (previousSessions.isEmpty()) {
            return null;
        }
        // Get the most recent previous session (first in the list since it's ordered DESC)
        Session previousSession = previousSessions.get(0);
        // Force load courseSession to check for homework assignment
        if (previousSession.getCourseSession() != null) {
            previousSession.getCourseSession().getStudentTask(); // Force load
        }
        return previousSession;
    }

    /**
     * Get teacher name for a session from teaching slot
     */
    private String getTeacherNameForSession(Long sessionId) {
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findBySessionIdWithTeacher(sessionId);
        if (teachingSlots.isEmpty()) {
            return null;
        }
        // Get the first scheduled/substituted teacher
        TeachingSlot teachingSlot = teachingSlots.get(0);
        if (teachingSlot.getTeacher() != null && teachingSlot.getTeacher().getUserAccount() != null) {
            return teachingSlot.getTeacher().getUserAccount().getFullName();
        }
        return null;
    }

    @Override
    public List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId) {
        List<ClassEntity> classes = teachingSlotRepository.findDistinctClassesByTeacherId(teacherId);
        return classes.stream()
                .map(this::mapToTeacherClassListItemDTO)
                .collect(Collectors.toList());
    }

    private TeacherClassListItemDTO mapToTeacherClassListItemDTO(ClassEntity classEntity) {
        // Count sessions excluding CANCELLED (consistent with attendance rate calculation)
        long totalSessions = sessionRepository.countByClassEntityIdExcludingCancelled(classEntity.getId());
        double attendanceRate = calculateClassAttendanceRate(classEntity.getId());
        return TeacherClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .courseName(classEntity.getCourse() != null ? classEntity.getCourse().getName() : null)
                .courseCode(classEntity.getCourse() != null ? classEntity.getCourse().getCode() : null)
                .branchName(classEntity.getBranch() != null ? classEntity.getBranch().getName() : null)
                .branchCode(classEntity.getBranch() != null ? classEntity.getBranch().getCode() : null)
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus())
                .totalSessions((int) totalSessions)
                .attendanceRate(attendanceRate)
                .build();
    }

    /**
     * Calculate attendance rate for a student based on their cells
     * Formula: PRESENT sessions / (PRESENT + ABSENT sessions) [excluding PLANNED]
     */
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
            // PLANNED status is excluded from calculation
        }
        
        int total = present + absent;
        return total > 0 ? (double) present / total : 0.0;
    }

    /**
     * Calculate average attendance rate for all enrolled students in a class
     * Formula: (Sum of all students' attendance rates) / (Number of enrolled students)
     * Each student's rate = PRESENT sessions / (PRESENT + ABSENT sessions) [excluding PLANNED]
     * Only counts sessions that are not CANCELLED, consistent with matrix calculation
     */
    private double calculateClassAttendanceRate(Long classId) {
        // Get all non-CANCELLED sessions for the class (same as in getClassAttendanceMatrix)
        List<Session> sessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId).stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .toList();
        
        if (sessions.isEmpty()) {
            return 0.0;
        }

        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(
                classId, EnrollmentStatus.ENROLLED);
        
        if (enrollments.isEmpty()) {
            return 0.0;
        }

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();
        Set<Long> enrolledStudentIds = enrollments.stream()
                .map(Enrollment::getStudentId)
                .collect(Collectors.toSet());

        // Get all StudentSessions for enrolled students and these sessions
        Map<Long, List<StudentSession>> sessionStudentMap = studentSessionRepository.findBySessionIds(sessionIds)
                .stream()
                .filter(ss -> enrolledStudentIds.contains(ss.getStudent().getId()))
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        double totalRate = 0.0;
        int studentCount = 0;

        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            
            // Count PRESENT and ABSENT only from non-CANCELLED sessions
            int present = 0;
            int absent = 0;
            
            for (Session session : sessions) {
                List<StudentSession> studentSessions = sessionStudentMap.getOrDefault(session.getId(), List.of());
                StudentSession studentSession = studentSessions.stream()
                        .filter(ss -> ss.getStudent().getId().equals(studentId))
                        .findFirst()
                        .orElse(null);
                
                AttendanceStatus displayStatus;
                if (studentSession != null) {
                    // Use same logic as matrix to resolve display status
                    displayStatus = resolveMatrixDisplayStatus(studentSession, session);
                } else {
                    // No StudentSession record - check if session has passed
                    boolean isFutureSession = session.getDate().isAfter(LocalDate.now()) ||
                            (session.getDate().equals(LocalDate.now()) && session.getStatus() == SessionStatus.PLANNED);
                    displayStatus = isFutureSession ? AttendanceStatus.PLANNED : AttendanceStatus.ABSENT;
                }
                
                // Count only PRESENT and ABSENT (exclude PLANNED)
                if (displayStatus == AttendanceStatus.PRESENT) {
                    present++;
                } else if (displayStatus == AttendanceStatus.ABSENT) {
                    absent++;
                }
                // PLANNED is excluded
            }

            // Calculate rate for this student
            int total = present + absent;
            if (total > 0) {
                double studentRate = (double) present / total;
                totalRate += studentRate;
                studentCount++;
            }
        }

        return studentCount == 0 ? 0.0 : totalRate / studentCount;
    }
}

