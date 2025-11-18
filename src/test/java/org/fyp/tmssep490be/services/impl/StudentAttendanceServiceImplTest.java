package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class StudentAttendanceServiceImplTest {

    @Autowired
    private StudentAttendanceServiceImpl service;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    private static final LocalDate CLASS_START = LocalDate.of(2025, 1, 10);
    private static final LocalDate CLASS_END = LocalDate.of(2025, 3, 30);

    private Session buildSession(Long id, String classCode) {
        return buildSession(id, classCode, LocalDate.now(), org.fyp.tmssep490be.entities.enums.SessionStatus.PLANNED);
    }

    private Session buildSession(Long id, String classCode, LocalDate date, org.fyp.tmssep490be.entities.enums.SessionStatus status) {
        Session s = new Session();
        s.setId(id);
        s.setDate(date);
        s.setStatus(status);

        ClassEntity ce = new ClassEntity();
        ce.setId(100L);
        ce.setCode(classCode);
        ce.setName("Class " + classCode);
        ce.setStartDate(CLASS_START);
        ce.setActualEndDate(CLASS_END);

        Course c = new Course();
        c.setId(1L);
        c.setCode("COURSE-1");
        c.setName("Course Name");

        ce.setCourse(c);
        s.setClassEntity(ce);
        return s;
    }

    private StudentSession buildSS(long studentId, Session s, AttendanceStatus st) {
        StudentSession ss = new StudentSession();
        Student stu = new Student();
        stu.setId(studentId);
        stu.setStudentCode("STD-" + studentId);
        UserAccount ua = new UserAccount();
        ua.setFullName("Student " + studentId);
        stu.setUserAccount(ua);
        ss.setStudent(stu);
        ss.setSession(s);
        ss.setAttendanceStatus(st);
        return ss;
    }

    @Test
    void getOverview_groupsByClass_andAggregates() {
        when(studentSessionRepository.findAllByStudentId(anyLong()))
                .thenReturn(List.of(
                        buildSS(1, buildSession(1L, "CLS-A"), AttendanceStatus.PRESENT),
                        buildSS(1, buildSession(2L, "CLS-A"), AttendanceStatus.ABSENT),
                        buildSS(1, buildSession(3L, "CLS-A"), AttendanceStatus.PLANNED)
                ));

        StudentAttendanceOverviewResponseDTO dto = service.getOverview(1L);
        assertThat(dto.getClasses()).hasSize(1);
        var item = dto.getClasses().get(0);
        assertThat(item.getClassCode()).isEqualTo("CLS-A");
        assertThat(item.getClassName()).isEqualTo("Class CLS-A");
        assertThat(item.getStartDate()).isEqualTo(CLASS_START);
        assertThat(item.getActualEndDate()).isEqualTo(CLASS_END);
        assertThat(item.getTotalSessions()).isEqualTo(3);
        assertThat(item.getAttended()).isEqualTo(1);
        assertThat(item.getAbsent()).isEqualTo(1);
        assertThat(item.getUpcoming()).isEqualTo(1);
    }

    @Test
    void getReport_computesSummary_andMapsSessions() {
        Session s1 = buildSession(10L, "CLS-A");
        Session s2 = buildSession(11L, "CLS-A");
        when(studentSessionRepository.findByStudentIdAndClassEntityId(anyLong(), anyLong()))
                .thenReturn(List.of(
                        buildSS(1, s1, AttendanceStatus.PRESENT),
                        buildSS(1, s2, AttendanceStatus.ABSENT)
                ));

        StudentAttendanceReportResponseDTO dto = service.getReport(1L, 100L);
        assertThat(dto.getClassId()).isEqualTo(100L);
        assertThat(dto.getClassName()).isEqualTo("Class CLS-A");
        assertThat(dto.getSummary().getTotalSessions()).isEqualTo(2);
        assertThat(dto.getSummary().getAttended()).isEqualTo(1);
        assertThat(dto.getSummary().getAbsent()).isEqualTo(1);
        assertThat(dto.getSessions()).hasSize(2);
    }

    @Test
    void getOverview_ignoresCancelledSessions() {
        Session activeSession = buildSession(5L, "CLS-B");
        Session cancelledSession = buildSession(
                6L,
                "CLS-B",
                LocalDate.now().minusDays(1),
                org.fyp.tmssep490be.entities.enums.SessionStatus.CANCELLED
        );

        when(studentSessionRepository.findAllByStudentId(anyLong()))
                .thenReturn(List.of(
                        buildSS(1, activeSession, AttendanceStatus.PRESENT),
                        buildSS(1, cancelledSession, AttendanceStatus.ABSENT)
                ));

        StudentAttendanceOverviewResponseDTO dto = service.getOverview(2L);

        assertThat(dto.getClasses()).hasSize(1);
        var item = dto.getClasses().get(0);
        assertThat(item.getTotalSessions()).isEqualTo(1);
        assertThat(item.getAttended()).isEqualTo(1);
        assertThat(item.getAbsent()).isZero();
    }

    @Test
    void getReport_marksPastPlannedSessionsAsAbsent() {
        Session pastPlanned = buildSession(
                20L,
                "CLS-C",
                LocalDate.now().minusDays(3),
                org.fyp.tmssep490be.entities.enums.SessionStatus.PLANNED
        );

        when(studentSessionRepository.findByStudentIdAndClassEntityId(anyLong(), anyLong()))
                .thenReturn(List.of(buildSS(1, pastPlanned, AttendanceStatus.PLANNED)));

        StudentAttendanceReportResponseDTO dto = service.getReport(3L, 100L);

        assertThat(dto.getSummary().getTotalSessions()).isEqualTo(1);
        assertThat(dto.getSummary().getAbsent()).isEqualTo(1);
        assertThat(dto.getSummary().getUpcoming()).isZero();
        assertThat(dto.getSessions()).hasSize(1);
        assertThat(dto.getSessions().get(0).getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
    }
}



