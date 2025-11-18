package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.attendance.*;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    List<SessionTodayDTO> getSessionsForDate(Long teacherId, LocalDate date);

    StudentsAttendanceResponseDTO getSessionStudents(Long teacherId, Long sessionId);

    AttendanceSaveResponseDTO saveAttendance(Long teacherId, Long sessionId, AttendanceSaveRequestDTO request);

    MarkAllResponseDTO markAllPresent(Long teacherId, Long sessionId);

    MarkAllResponseDTO markAllAbsent(Long teacherId, Long sessionId);

    SessionReportResponseDTO getSessionReport(Long teacherId, Long sessionId);

    SessionReportResponseDTO submitSessionReport(Long teacherId, Long sessionId, SessionReportSubmitDTO request);

    AttendanceMatrixDTO getClassAttendanceMatrix(Long teacherId, Long classId);

    List<TeacherClassListItemDTO> getTeacherClasses(Long teacherId);
}


