package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
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
class StudentAttendanceService_GetOverviewTest {

    @InjectMocks
    private StudentAttendanceService service;

    @Mock
    private StudentSessionRepository studentSessionRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    /** Utility: Mock Enrollment */
    private Enrollment mockEnrollment(Long classId, ClassStatus status, EnrollmentStatus enrollmentStatus) {
        Subject subject = new Subject();
        subject.setId(100L);
        subject.setCode("SUB100");
        subject.setName("Subject 100");

        ClassEntity cls = new ClassEntity();
        cls.setId(classId);
        cls.setCode("C" + classId);
        cls.setName("Class " + classId);
        cls.setSubject(subject);
        cls.setStatus(status);

        Enrollment e = new Enrollment();
        e.setClassEntity(cls);
        e.setStatus(enrollmentStatus);

        return e;
    }

    /** Utility: Mock StudentSession */
    private StudentSession mockSession(Long ssId, Long classId, AttendanceStatus status, LocalDate date) {
        ClassEntity cls = new ClassEntity();
        cls.setId(classId);

        Session s = new Session();
        s.setId(ssId);
        s.setClassEntity(cls);
        s.setStatus(SessionStatus.DONE);
        s.setDate(date);

        StudentSession ss = new StudentSession();
        ss.setSession(s);
        ss.setAttendanceStatus(status);
        ss.setIsTransferredOut(false);

        return ss;
    }

    // ============================================================
    //                      TEST CASES
    // ============================================================

    /** TC1: No enrollment → return empty list */
    @Test
    void getOverview_noEnrollment_returnsEmpty() {
        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of());

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of());

        var result = service.getOverview(1L);

        assertTrue(result.getClasses().isEmpty());
    }

    /** TC2: Sorting priority correct (ONGOING → SCHEDULED → COMPLETED) */
    @Test
    void getOverview_sortOrder_correct() {
        Enrollment e1 = mockEnrollment(1L, ClassStatus.COMPLETED, EnrollmentStatus.COMPLETED);
        Enrollment e2 = mockEnrollment(2L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);
        Enrollment e3 = mockEnrollment(3L, ClassStatus.SCHEDULED, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e1, e2, e3));

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of());

        var list = service.getOverview(1L).getClasses();

        assertEquals(2L, list.get(0).getClassId()); // ONGOING
        assertEquals(3L, list.get(1).getClassId()); // SCHEDULED
        assertEquals(1L, list.get(2).getClassId()); // COMPLETED
    }

    /** TC3: Counting PRESENT, ABSENT, EXCUSED, PLANNED */
    @Test
    void getOverview_countAttendance_correct() {
        Enrollment e = mockEnrollment(10L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        List<StudentSession> sessions = List.of(
                mockSession(1L, 10L, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1)),
                mockSession(2L, 10L, AttendanceStatus.ABSENT, LocalDate.now().minusDays(1)),
                mockSession(3L, 10L, AttendanceStatus.EXCUSED, LocalDate.now().minusDays(1)),
                mockSession(4L, 10L, AttendanceStatus.PLANNED, LocalDate.now().plusDays(1))
        );

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(sessions);

        var dto = service.getOverview(1L).getClasses().get(0);

        assertEquals(4, dto.getTotalSessions());
        assertEquals(1, dto.getAttended());
        assertEquals(1, dto.getAbsent());
        assertEquals(1, dto.getExcused());
        assertEquals(1, dto.getUpcoming());
    }

    /** TC4: Session is transferred out → MUST be ignored */
    @Test
    void getOverview_transferredOut_ignored() {
        Enrollment e = mockEnrollment(20L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        StudentSession ss = mockSession(1L, 20L, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1));
        ss.setIsTransferredOut(true);

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of(ss));

        var dto = service.getOverview(1L).getClasses().get(0);

        assertEquals(0, dto.getTotalSessions());
        assertEquals(0, dto.getAttended());
    }

    /** TC5: CANCELLED session → MUST be ignored */
    @Test
    void getOverview_cancelledSession_ignored() {
        Enrollment e = mockEnrollment(30L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        StudentSession ss = mockSession(1L, 30L, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1));
        ss.getSession().setStatus(SessionStatus.CANCELLED);

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of(ss));

        var dto = service.getOverview(1L).getClasses().get(0);

        assertEquals(0, dto.getTotalSessions());
    }

    /** TC6: Missing attendance (null) + date in past => auto ABSENT */
    @Test
    void getOverview_nullAttendance_pastDate_autoAbsent() {
        Enrollment e = mockEnrollment(40L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        StudentSession ss = mockSession(1L, 40L, null, LocalDate.now().minusDays(1)); // null attendance

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of(ss));

        var dto = service.getOverview(1L).getClasses().get(0);

        assertEquals(1, dto.getAbsent());
    }

    /** TC7: PLANNED today → NOT absent */
    @Test
    void getOverview_plannedToday_notAbsence() {
        Enrollment e = mockEnrollment(50L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        StudentSession ss = mockSession(1L, 50L, AttendanceStatus.PLANNED, LocalDate.now());

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of(ss));

        var dto = service.getOverview(1L).getClasses().get(0);

        assertEquals(0, dto.getAbsent());
        assertEquals(1, dto.getUpcoming());
    }

    /** TC8: Multiple classes with mixed sessions → each class counted separately */
    @Test
    void getOverview_multiClass_sessionsSeparatedCorrectly() {

        Enrollment e1 = mockEnrollment(60L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);
        Enrollment e2 = mockEnrollment(61L, ClassStatus.ONGOING, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e1, e2));

        List<StudentSession> sessions = List.of(
                mockSession(1L, 60L, AttendanceStatus.PRESENT, LocalDate.now().minusDays(1)),
                mockSession(2L, 61L, AttendanceStatus.ABSENT, LocalDate.now().minusDays(1))
        );

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(sessions);

        var result = service.getOverview(1L).getClasses();

        var class60 = result.stream().filter(c -> c.getClassId() == 60L).findFirst().get();
        var class61 = result.stream().filter(c -> c.getClassId() == 61L).findFirst().get();

        assertEquals(1, class60.getAttended());
        assertEquals(1, class61.getAbsent());
    }
}
