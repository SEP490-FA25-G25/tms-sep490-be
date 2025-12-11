package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;

    @Transactional(readOnly = true)
    public SessionDetailDTO getSessionDetail(Long sessionId, Long userId) {
        log.info("Getting session detail for sessionId={} by userId={}", sessionId, userId);

        Session session = sessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại với ID: " + sessionId));

        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);

        // Calculate attendance stats
        int totalStudents = studentSessions.size();
        long presentCount = studentSessions.stream()
                .filter(ss -> "PRESENT".equals(ss.getAttendanceStatus().name()))
                .count();
        long absentCount = studentSessions.stream()
                .filter(ss -> "ABSENT".equals(ss.getAttendanceStatus().name()))
                .count();

        long homeworkCompletedCount = studentSessions.stream()
                .filter(ss -> ss.getHomeworkStatus() != null && "COMPLETED".equals(ss.getHomeworkStatus().name()))
                .count();
        
        // Check if session has homework (at least one student has homework status != NO_HOMEWORK)
        boolean hasHomework = studentSessions.stream()
                .anyMatch(ss -> ss.getHomeworkStatus() != null && !"NO_HOMEWORK".equals(ss.getHomeworkStatus().name()));

        SessionDetailDTO.AttendanceStats stats = SessionDetailDTO.AttendanceStats.builder()
                .totalStudents(totalStudents)
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .homeworkCompletedCount((int) homeworkCompletedCount)
                .hasHomework(hasHomework)
                .build();

        // Map student attendance
        List<SessionDetailDTO.StudentAttendanceDTO> students = studentSessions.stream()
                .map(ss -> SessionDetailDTO.StudentAttendanceDTO.builder()
                        .studentId(ss.getStudent().getId())
                        .studentCode(ss.getStudent().getUserAccount().getEmail())
                        .studentName(ss.getStudent().getUserAccount().getFullName())
                        .attendanceStatus(ss.getAttendanceStatus().name())
                        .homeworkStatus(ss.getHomeworkStatus() != null ? ss.getHomeworkStatus().name() : null)
                        .isMakeup(ss.getIsMakeup() != null ? ss.getIsMakeup() : false)
                        .note(ss.getNote())
                        .build())
                .collect(Collectors.toList());

        // Get CLOs covered (if subjectSession exists)
        List<SessionDetailDTO.CLOInfo> clos = List.of();
        if (session.getSubjectSession() != null && session.getSubjectSession().getSubjectSessionCLOMappings() != null) {
            clos = session.getSubjectSession().getSubjectSessionCLOMappings().stream()
                    .map(mapping -> SessionDetailDTO.CLOInfo.builder()
                            .cloId(mapping.getClo().getId())
                            .cloCode(mapping.getClo().getCode())
                            .description(mapping.getClo().getDescription())
                            .build())
                    .collect(Collectors.toList());
        }

        // Get teacher name - exclude ON_LEAVE teachers
        String teacherName = null;
        if (session.getTeachingSlots() != null && !session.getTeachingSlots().isEmpty()) {
            TeachingSlot activeSlot = session.getTeachingSlots().stream()
                    .filter(slot -> slot.getStatus() != org.fyp.tmssep490be.entities.enums.TeachingSlotStatus.ON_LEAVE)
                    .findFirst()
                    .orElse(null);
            
            if (activeSlot != null && activeSlot.getTeacher() != null && activeSlot.getTeacher().getUserAccount() != null) {
                teacherName = activeSlot.getTeacher().getUserAccount().getFullName();
            }
        }

        return SessionDetailDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .subjectName(session.getClassEntity().getSubject().getName())
                .date(session.getDate())
                .timeSlot(session.getTimeSlotTemplate() != null ?
                        session.getTimeSlotTemplate().getStartTime() + " - " + session.getTimeSlotTemplate().getEndTime() : null)
                .topic(session.getSubjectSession() != null ? session.getSubjectSession().getTopic() : null)
                .studentTask(session.getSubjectSession() != null ? session.getSubjectSession().getStudentTask() : null)
                .status(session.getStatus().name())
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .attendanceStats(stats)
                .students(students)
                .closCovered(clos)
                .build();
    }
}
