package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_CalculateAbsenceRate_Test {

    @Mock private StudentSessionRepository studentSessionRepository;

    @Spy
    @InjectMocks
    private StudentRequestService service;

    private final Long studentId = 1L;
    private final Long classId = 10L;

    // Helper function tạo StudentSession
    private StudentSession ss(LocalDate date, AttendanceStatus attendanceStatus, SessionStatus status) {
        return StudentSession.builder()
                .attendanceStatus(attendanceStatus)
                .session(Session.builder()
                        .date(date)
                        .status(status)
                        .build())
                .build();
    }

    // ---------------------------------------------------------------
    // TC1 — Không có session nào → return 0.0
    // ---------------------------------------------------------------
    @Test
    void calculateAbsenceRate_noSessions() {
        when(studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId))
                .thenReturn(List.of());

        double rate = service.calculateAbsenceRate(studentId, classId);

        assertEquals(0.0, rate);
    }

    // ---------------------------------------------------------------
    // TC2 — Không có buổi nào đã diễn ra (tất cả ở tương lai)
    // ---------------------------------------------------------------
    @Test
    void calculateAbsenceRate_noPastSessions() {
        LocalDate future = LocalDate.now().plusDays(1);

        List<StudentSession> sessions = List.of(
                ss(future, AttendanceStatus.PRESENT, SessionStatus.PLANNED)
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId))
                .thenReturn(sessions);

        double rate = service.calculateAbsenceRate(studentId, classId);

        assertEquals(0.0, rate);
    }

    // ---------------------------------------------------------------
    // TC3 — Có buổi học nhưng không có ABSENT → rate = 0.0
    // ---------------------------------------------------------------
    @Test
    void calculateAbsenceRate_noAbsences() {
        LocalDate past = LocalDate.now().minusDays(1);

        List<StudentSession> sessions = List.of(
                ss(past, AttendanceStatus.PRESENT, SessionStatus.DONE),
                ss(past, AttendanceStatus.EXCUSED, SessionStatus.DONE)
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId))
                .thenReturn(sessions);

        double rate = service.calculateAbsenceRate(studentId, classId);

        assertEquals(0.0, rate);
    }

    // ---------------------------------------------------------------
    // TC4 — Có ABSENT → tính đúng %
    // ---------------------------------------------------------------
    @Test
    void calculateAbsenceRate_withAbsence() {
        LocalDate past = LocalDate.now().minusDays(1);

        List<StudentSession> sessions = List.of(
                ss(past, AttendanceStatus.ABSENT, SessionStatus.DONE),
                ss(past, AttendanceStatus.ABSENT, SessionStatus.DONE),
                ss(past, AttendanceStatus.PRESENT, SessionStatus.DONE),
                ss(past, AttendanceStatus.EXCUSED, SessionStatus.DONE)
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId))
                .thenReturn(sessions);

        double rate = service.calculateAbsenceRate(studentId, classId);

        assertEquals(50.0, rate);   // 2 ABSENT / 4 buổi
    }

    // ---------------------------------------------------------------
    // TC5 — CANCELLED session bị bỏ qua
    // ---------------------------------------------------------------
    @Test
    void calculateAbsenceRate_cancelledIgnored() {
        LocalDate past = LocalDate.now().minusDays(1);

        List<StudentSession> sessions = List.of(
                ss(past, AttendanceStatus.ABSENT, SessionStatus.DONE),
                ss(past, AttendanceStatus.PRESENT, SessionStatus.DONE),

                // CANCELLED → không tính vào mẫu
                ss(past, AttendanceStatus.ABSENT, SessionStatus.CANCELLED)
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId))
                .thenReturn(sessions);

        double rate = service.calculateAbsenceRate(studentId, classId);

        assertEquals(50.0, rate); // chỉ 2 buổi DONE
    }
}
