package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentAttendanceService_GetReportTest {

    @InjectMocks
    private StudentAttendanceService service;

    @Mock
    private StudentSessionRepository studentSessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    // ------------------------- UTILITIES -----------------------------

    private Enrollment mockEnrollment(ClassEntity cls, EnrollmentStatus status, Long joinId, Long leftId) {
        Enrollment e = new Enrollment();
        e.setClassEntity(cls);
        e.setStatus(status);
        e.setJoinSessionId(joinId);
        e.setLeftSessionId(leftId);
        return e;
    }

    private StudentSession mockStudentSession(
            Long ssId, ClassEntity cls, AttendanceStatus attendance, LocalDate date) {

        Session session = new Session();
        session.setId(ssId);
        session.setClassEntity(cls);
        session.setDate(date);
        session.setStatus(SessionStatus.DONE);
        session.setTeachingSlots(Set.of());
        session.setSessionResources(Set.of());

        StudentSession ss = new StudentSession();
        ss.setSession(session);
        ss.setAttendanceStatus(attendance);

        return ss;
    }

    private ClassEntity mockClass() {
        Subject subject = new Subject();
        subject.setId(100L);
        subject.setCode("SUB100");
        subject.setName("Subject 100");

        ClassEntity cls = new ClassEntity();
        cls.setId(10L);
        cls.setCode("C10");
        cls.setName("Class 10");
        cls.setSubject(subject);
        cls.setStatus(ClassStatus.ONGOING);

        return cls;
    }

    // --------------------------------------------------------------------
    //                          TEST CASES
    // --------------------------------------------------------------------

    /** TC1 – No enrollment → return empty report */
    @Test
    void getReport_noEnrollment_returnsEmpty() {
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L))
                .thenReturn(null);

        StudentAttendanceReportResponseDTO result = service.getReport(1L, 10L);

        assertEquals(10L, result.getClassId());
        assertEquals(0, result.getSummary().getTotalSessions());
        assertTrue(result.getSessions().isEmpty());
    }

    /** TC2 – Ignoring CANCELLED sessions */
    @Test
    void getReport_ignoreCancelledSessions() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);

        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L))
                .thenReturn(e);

        StudentSession cancelled = mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1));
        cancelled.getSession().setStatus(SessionStatus.CANCELLED);

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(cancelled));

        var result = service.getReport(1L, 10L);

        assertEquals(0, result.getSummary().getTotalSessions());
    }

    /** TC3 – Auto ABSENT when date < today and attendance null or PLANNED */
    @Test
    void getReport_autoAbsent_whenPastAndNoAttendance() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        StudentSession ss = mockStudentSession(1L, cls, null, LocalDate.now().minusDays(1));

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(ss));

        var result = service.getReport(1L, 10L);

        assertEquals(1, result.getSummary().getAbsent());
    }

    /** TC4 – Count PRESENT, ABSENT, EXCUSED, UPCOMING */
    @Test
    void getReport_countCorrect() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        List<StudentSession> sessions = List.of(
                mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1)),
                mockStudentSession(2L, cls, AttendanceStatus.ABSENT, LocalDate.now().minusDays(1)),
                mockStudentSession(3L, cls, AttendanceStatus.EXCUSED, LocalDate.now().minusDays(1)),
                mockStudentSession(4L, cls, AttendanceStatus.PLANNED, LocalDate.now().plusDays(1))
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(sessions);

        var summary = service.getReport(1L, 10L).getSummary();

        assertEquals(4, summary.getTotalSessions());
        assertEquals(1, summary.getAttended());
        assertEquals(1, summary.getAbsent());
        assertEquals(1, summary.getExcused());
        assertEquals(1, summary.getUpcoming());
    }

    /** TC5 – Attendance Rate calculated correctly */
    @Test
    void getReport_attendanceRateCorrect() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        List<StudentSession> sessions = List.of(
                mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1)),
                mockStudentSession(2L, cls, AttendanceStatus.ABSENT, LocalDate.now().minusDays(1))
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(sessions);

        var summary = service.getReport(1L, 10L).getSummary();

        assertEquals(0.5, summary.getAttendanceRate()); // 1/2
    }

    /** TC6 – Verify session DTO mapping (teacherName, classroomName, timeslot) */
    @Test
    void getReport_sessionMappingCorrect() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        // Mock session with details
        Session session = new Session();
        session.setId(1L);
        session.setClassEntity(cls);
        session.setStatus(SessionStatus.DONE);
        session.setDate(LocalDate.now());
        // teaching slot
        Teacher teacher = new Teacher();
        UserAccount ua = new UserAccount();
        ua.setFullName("Teacher A");
        teacher.setUserAccount(ua);
        TeachingSlot ts = new TeachingSlot();
        ts.setTeacher(teacher);
        session.setTeachingSlots(Set.of(ts));

        // classroom
        Resource resource = new Resource();
        resource.setName("Room 101");
        SessionResource sr = new SessionResource();
        sr.setResource(resource);
        session.setSessionResources(Set.of(sr));

        StudentSession ss = new StudentSession();
        ss.setSession(session);
        ss.setAttendanceStatus(AttendanceStatus.PRESENT);

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(ss));

        var item = service.getReport(1L, 10L).getSessions().get(0);

        assertEquals("Room 101", item.getClassroomName());
        assertEquals("Teacher A", item.getTeacherName());
    }

    /** TC7 – enrollment timeline: ENROLLED → only sessions >= joinId */
    @Test
    void getReport_filterTimeline_enrolled_joinOnly() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, 2L, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        StudentSession beforeJoin = mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now());
        StudentSession afterJoin = mockStudentSession(3L, cls, AttendanceStatus.PRESENT, LocalDate.now());

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(beforeJoin, afterJoin));

        var summary = service.getReport(1L, 10L).getSummary();

        assertEquals(1, summary.getTotalSessions());
    }

    /** TC8 – enrollment timeline: TRANSFERRED → only sessions <= leftId */
    @Test
    void getReport_filterTimeline_transferred_onlyBeforeLeft() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.TRANSFERRED, null, 2L);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        StudentSession beforeLeft = mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now());
        StudentSession afterLeft = mockStudentSession(3L, cls, AttendanceStatus.PRESENT, LocalDate.now());

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(beforeLeft, afterLeft));

        var summary = service.getReport(1L, 10L).getSummary();

        assertEquals(1, summary.getTotalSessions());
    }

    /** TC9 – Makeup Session Info mapped correctly */
    @Test
    void getReport_makeupSessionMappingCorrect() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        // Makeup session
        Session makeupSession = new Session();
        makeupSession.setId(99L);
        makeupSession.setClassEntity(cls);
        makeupSession.setDate(LocalDate.now());

        // Base session
        StudentSession ss = mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now());
        ss.setIsMakeup(true);
        ss.setMakeupSession(makeupSession);

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(ss));

        var dto = service.getReport(1L, 10L).getSessions().get(0);

        assertNotNull(dto.getMakeupSessionInfo());
        assertEquals(99L, dto.getMakeupSessionInfo().getSessionId());
    }

    /** TC10 – Active sessions empty → fallback to studentSessions to get class info */
    @Test
    void getReport_fallbackToOriginalSessionsForClassInfo() {
        ClassEntity cls = mockClass();
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED, null, null);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L)).thenReturn(e);

        // session is CANCELLED → activeSessions empty
        StudentSession ss = mockStudentSession(1L, cls, AttendanceStatus.PRESENT, LocalDate.now());
        ss.getSession().setStatus(SessionStatus.CANCELLED);

        when(studentSessionRepository.findByStudentIdAndClassEntityId(1L, 10L))
                .thenReturn(List.of(ss));

        var result = service.getReport(1L, 10L);

        assertEquals("Class 10", result.getClassName());
        assertEquals("SUB100", result.getCourseCode());
    }
}
