package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.attendance.AttendanceSaveRequestDTO;
import org.fyp.tmssep490be.dtos.attendance.MarkAllResponseDTO;
import org.fyp.tmssep490be.dtos.attendance.SessionReportResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AttendanceServiceImplTest {

    @Autowired
    private AttendanceServiceImpl attendanceService;

    @MockitoBean
    private TeachingSlotRepository teachingSlotRepository;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    private Session buildSession(Long id) {
        Session s = new Session();
        s.setId(id);
        s.setDate(LocalDate.now());
        s.setStatus(SessionStatus.PLANNED);
        TimeSlotTemplate tst = new TimeSlotTemplate();
        tst.setName("Morning");
        tst.setStartTime(java.time.LocalTime.of(8, 0));
        tst.setEndTime(java.time.LocalTime.of(10, 0));
        s.setTimeSlotTemplate(tst);
        ClassEntity ce = new ClassEntity();
        ce.setId(999L);
        ce.setCode("CLS-999");
        Course c = new Course();
        c.setCode("COURSE-1");
        c.setName("Course Name");
        ce.setCourse(c);
        s.setClassEntity(ce);
        return s;
    }

    private StudentSession buildStudentSession(long studentId, Session session) {
        StudentSession ss = new StudentSession();
        Student student = new Student();
        student.setId(studentId);
        student.setStudentCode("STD-" + studentId);
        UserAccount ua = new UserAccount();
        ua.setFullName("Student " + studentId);
        student.setUserAccount(ua);
        ss.setStudent(student);
        ss.setSession(session);
        ss.setAttendanceStatus(AttendanceStatus.PLANNED);
        return ss;
    }

    @Test
    void markAllPresent_returnsPreviewSummary_withoutPersisting() {
        Long sessionId = 10L;
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(true);
        when(studentSessionRepository.findBySessionId(sessionId))
                .thenReturn(List.of(
                        buildStudentSession(1, buildSession(sessionId)),
                        buildStudentSession(2, buildSession(sessionId)),
                        buildStudentSession(3, buildSession(sessionId))
                ));

        MarkAllResponseDTO res = attendanceService.markAllPresent(123L, sessionId);
        assertThat(res.getSessionId()).isEqualTo(sessionId);
        assertThat(res.getSummary().getTotalStudents()).isEqualTo(3);
        assertThat(res.getSummary().getPresentCount()).isEqualTo(3);
        assertThat(res.getSummary().getAbsentCount()).isEqualTo(0);
    }

    @Test
    void markAllAbsent_returnsPreviewSummary_withoutPersisting() {
        Long sessionId = 11L;
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(true);
        when(studentSessionRepository.findBySessionId(sessionId))
                .thenReturn(List.of(
                        buildStudentSession(1, buildSession(sessionId)),
                        buildStudentSession(2, buildSession(sessionId))
                ));

        MarkAllResponseDTO res = attendanceService.markAllAbsent(123L, sessionId);
        assertThat(res.getSessionId()).isEqualTo(sessionId);
        assertThat(res.getSummary().getTotalStudents()).isEqualTo(2);
        assertThat(res.getSummary().getPresentCount()).isEqualTo(0);
        assertThat(res.getSummary().getAbsentCount()).isEqualTo(2);
    }

    @Test
    void saveAttendance_emptyRecords_throwsIllegalArgument() {
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(true);
        AttendanceSaveRequestDTO req = AttendanceSaveRequestDTO.builder().build();
        assertThatThrownBy(() -> attendanceService.saveAttendance(1L, 99L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void submitSessionReport_setsStatusDone_andReturnsSummary() {
        Long sessionId = 20L;
        Session session = buildSession(sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(studentSessionRepository.findBySessionId(sessionId))
                .thenReturn(List.of(
                        buildStudentSession(1, session),
                        buildStudentSession(2, session)
                ));

        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(true);
        SessionReportResponseDTO res = attendanceService.submitSessionReport(5L, sessionId,
                org.fyp.tmssep490be.dtos.attendance.SessionReportSubmitDTO.builder().teacherNote("Note").build());

        assertThat(session.getStatus()).isEqualTo(SessionStatus.DONE);
        assertThat(res.getSummary().getTotalStudents()).isEqualTo(2);
    }
}


