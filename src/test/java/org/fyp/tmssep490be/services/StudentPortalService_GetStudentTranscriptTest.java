package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentPortalService_GetStudentTranscriptTest {

    @InjectMocks
    private StudentPortalService service;

    @Mock private StudentRepository studentRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private TeachingSlotRepository teachingSlotRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private ScoreRepository scoreRepository;

    // Utility to mock ClassEntity+Subject
    private ClassEntity mockClass(Long id, ClassStatus status) {
        Subject s = new Subject();
        s.setId(100L);
        s.setName("Math");
        s.setCode("MTH101");

        ClassEntity cls = new ClassEntity();
        cls.setId(id);
        cls.setStatus(status);
        cls.setSubject(s);
        return cls;
    }

    private Enrollment mockEnrollment(ClassEntity cls, EnrollmentStatus status) {
        Enrollment e = new Enrollment();
        e.setClassEntity(cls);
        e.setStatus(status);
        return e;
    }

    // ---------------------------------------------------
    // TC-STT1 — Student not found
    // ---------------------------------------------------
    @Test
    void TC_STT1_studentNotFound_throwsException() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThrows(CustomException.class,
                () -> service.getStudentTranscript(1L));
    }

    // ---------------------------------------------------
    // TC-STT2 — Only ENROLLED & COMPLETED included
    // ---------------------------------------------------
    @Test
    void TC_STT2_onlyValidStatusesIncluded() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.ONGOING);

        Enrollment e1 = mockEnrollment(cls, EnrollmentStatus.ENROLLED);
        Enrollment e2 = mockEnrollment(cls, EnrollmentStatus.COMPLETED);
        Enrollment e3 = mockEnrollment(cls, EnrollmentStatus.TRANSFERRED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e1, e2, e3));

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of());

        var result = service.getStudentTranscript(1L);

        assertEquals(2, result.size());
    }

    // ---------------------------------------------------
    // TC-STT3 — No sessions found → totalSessions = 0
    // ---------------------------------------------------
    @Test
    void TC_STT3_emptySessionList_safeHandling() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.ONGOING);
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of());

        var dto = service.getStudentTranscript(1L).get(0);

        assertEquals(0, dto.getTotalSessions());
        assertEquals(0, dto.getCompletedSessions());
    }

    // ---------------------------------------------------
    // TC-STT4 — No teaching slots → teacher = "Chua phan cong"
    // ---------------------------------------------------
    @Test
    void TC_STT4_noTeachingSlots_primaryTeacherFallback() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.ONGOING);
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        when(teachingSlotRepository.findByClassEntityIdAndStatus(10L, TeachingSlotStatus.SCHEDULED))
                .thenReturn(List.of());

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of());

        var dto = service.getStudentTranscript(1L).get(0);

        assertEquals("Chua phan cong", dto.getTeacherName());
    }

    // ---------------------------------------------------
    // TC-STT5 — Score list empty → averageScore = null
    // ---------------------------------------------------
    @Test
    void TC_STT5_noScores_averageNull() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.ONGOING);
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of());

        var dto = service.getStudentTranscript(1L).get(0);

        assertNull(dto.getAverageScore());
    }

    // ---------------------------------------------------
    // TC-STT6 — Average score calculation correct
    // ---------------------------------------------------
    @Test
    void TC_STT6_averageScoreCorrect() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.ONGOING);
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        // Mock two scores: 80 + 90 = average 85
        // Mock two scores: 80 + 90 = average 85
        Score sc1 = new Score();
        sc1.setScore(BigDecimal.valueOf(80));

        Score sc2 = new Score();
        sc2.setScore(BigDecimal.valueOf(90));

// DIFFERENT assessment names, otherwise map overwrites!
        SubjectAssessment sa1 = new SubjectAssessment();
        sa1.setName("Quiz");

        SubjectAssessment sa2 = new SubjectAssessment();
        sa2.setName("Assignment");

        Assessment a1 = new Assessment();
        a1.setSubjectAssessment(sa1);
        sc1.setAssessment(a1);

        Assessment a2 = new Assessment();
        a2.setSubjectAssessment(sa2);
        sc2.setAssessment(a2);


        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of(sc1, sc2));

        var dto = service.getStudentTranscript(1L).get(0);

        assertEquals(0, dto.getAverageScore().compareTo(new BigDecimal("85.00")));

    }

    // ---------------------------------------------------
    // TC-STT7 — SubjectAssessment null → safe ignore
    // ---------------------------------------------------
    @Test
    void TC_STT7_subjectAssessmentNull_safeHandling() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.ONGOING);
        Enrollment e = mockEnrollment(cls, EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        Score sc = new Score();
        sc.setScore(BigDecimal.valueOf(70));

        Assessment a = new Assessment();
        a.setSubjectAssessment(null);   // Important case
        sc.setAssessment(a);

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of(sc));

        var dto = service.getStudentTranscript(1L).get(0);

        assertTrue(dto.getComponentScores().isEmpty());
        assertNull(dto.getAverageScore());
    }

    // ---------------------------------------------------
    // TC-STT8 — Completed class → completedDate returned
    // ---------------------------------------------------
    @Test
    void TC_STT8_completedClass_completedDateSet() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L, ClassStatus.COMPLETED);
        cls.setActualEndDate(LocalDate.of(2024, 12, 1));

        Enrollment e = mockEnrollment(cls, EnrollmentStatus.COMPLETED);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e));

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of());

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of());

        var dto = service.getStudentTranscript(1L).get(0);

        assertEquals(LocalDate.of(2024, 12, 1), dto.getCompletedDate());
    }
}
