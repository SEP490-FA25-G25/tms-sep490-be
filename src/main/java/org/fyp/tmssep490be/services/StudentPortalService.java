package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentportal.ClassSessionsResponseDTO;
import org.fyp.tmssep490be.dtos.studentportal.SessionDTO;
import org.fyp.tmssep490be.dtos.studentportal.StudentSessionDTO;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentPortalService {

    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final EnrollmentRepository enrollmentRepository;

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
}
