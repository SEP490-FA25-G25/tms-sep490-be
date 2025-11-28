package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.qa.SessionDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.SessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;

    @Override
    @Transactional(readOnly = true)
    public SessionDetailDTO getSessionDetail(Long sessionId, Long userId) {
        log.info("Getting session detail for sessionId={}", sessionId);

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
        double attendanceRate = totalStudents > 0 ? (double) presentCount / totalStudents * 100 : 0.0;

        long homeworkCompletedCount = studentSessions.stream()
                .filter(ss -> "COMPLETED".equals(ss.getHomeworkStatus().name()))
                .count();
        double homeworkCompletionRate = totalStudents > 0 ? (double) homeworkCompletedCount / totalStudents * 100 : 0.0;

        SessionDetailDTO.AttendanceStats stats = SessionDetailDTO.AttendanceStats.builder()
                .totalStudents(totalStudents)
                .presentCount((int) presentCount)
                .absentCount((int) absentCount)
                .attendanceRate(attendanceRate)
                .homeworkCompletedCount((int) homeworkCompletedCount)
                .homeworkCompletionRate(homeworkCompletionRate)
                .build();

        // Map student attendance
        List<SessionDetailDTO.StudentAttendanceDTO> students = studentSessions.stream()
                .map(ss -> {
                    return SessionDetailDTO.StudentAttendanceDTO.builder()
                            .studentId(ss.getStudent().getId())
                            .studentCode(ss.getStudent().getUserAccount().getEmail())
                            .studentName(ss.getStudent().getUserAccount().getFullName())
                            .attendanceStatus(ss.getAttendanceStatus().name())
                            .homeworkStatus(ss.getHomeworkStatus() != null ? ss.getHomeworkStatus().name() : null)
                            .isMakeup(ss.getIsMakeup() != null ? ss.getIsMakeup() : false)
                            .note(ss.getNote())
                            .build();
                })
                .collect(Collectors.toList());

        // Get CLOs covered (if courseSession exists)
        List<SessionDetailDTO.CLOInfo> clos = List.of();
        if (session.getCourseSession() != null && session.getCourseSession().getCourseSessionCLOMappings() != null) {
            clos = session.getCourseSession().getCourseSessionCLOMappings().stream()
                    .map(mapping -> SessionDetailDTO.CLOInfo.builder()
                            .cloId(mapping.getClo().getId())
                            .cloCode(mapping.getClo().getCode())
                            .description(mapping.getClo().getDescription())
                            .build())
                    .collect(Collectors.toList());
        }

        // Get teacher name
        String teacherName = null;
        if (session.getTeachingSlots() != null && !session.getTeachingSlots().isEmpty()) {
            TeachingSlot firstSlot = session.getTeachingSlots().iterator().next();
            if (firstSlot.getTeacher() != null && firstSlot.getTeacher().getUserAccount() != null) {
                teacherName = firstSlot.getTeacher().getUserAccount().getFullName();
            }
        }

        return SessionDetailDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlot(session.getTimeSlotTemplate() != null ?
                        session.getTimeSlotTemplate().getStartTime() + " - " + session.getTimeSlotTemplate().getEndTime() : null)
                .topic(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .studentTask(session.getCourseSession() != null ? session.getCourseSession().getStudentTask() : null)
                .status(session.getStatus().name())
                .teacherName(teacherName)
                .teacherNote(session.getTeacherNote())
                .attendanceStats(stats)
                .students(students)
                .closCovered(clos)
                .build();
    }
}
