package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentPortalService_GetClassSessionsTest {

    @InjectMocks
    private StudentPortalService service;

    @Mock private ClassRepository classRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private StudentSessionRepository studentSessionRepository;

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private ScoreRepository scoreRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeachingSlotRepository teachingSlotRepository;

    @Test
    void classNotFound_throwsException() {
        when(classRepository.existsById(99L)).thenReturn(false);

        assertThrows(CustomException.class, () ->
                service.getClassSessions(99L, 1L)
        );
    }

    @Test
    void studentNotEnrolled_throwsException() {
        when(classRepository.existsById(10L)).thenReturn(true);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L))
                .thenReturn(null);

        assertThrows(CustomException.class, () ->
                service.getClassSessions(10L, 1L)
        );
    }

    @Test
    void splitPastAndUpcomingSessions_success() {
        when(classRepository.existsById(10L)).thenReturn(true);

        Enrollment e = new Enrollment();
        e.setStatus(EnrollmentStatus.ENROLLED);
        when(enrollmentRepository.findByStudentIdAndClassId(1L, 10L))
                .thenReturn(e);

        ClassEntity cls = new ClassEntity();

        Session past = new Session();
        past.setId(1L);
        past.setDate(LocalDate.now().minusDays(1));
        past.setStatus(SessionStatus.DONE);
        past.setClassEntity(cls);

        Session upcoming = new Session();
        upcoming.setId(2L);
        upcoming.setDate(LocalDate.now().plusDays(1));
        upcoming.setStatus(SessionStatus.PLANNED);
        upcoming.setClassEntity(cls);

        when(sessionRepository.findAllByClassIdOrderByDateAndTime(10L))
                .thenReturn(List.of(past, upcoming));

        when(studentSessionRepository.findAllByStudentId(1L))
                .thenReturn(List.of());

        var result = service.getClassSessions(10L, 1L);

        assertEquals(1, result.getPastSessions().size());
        assertEquals(1, result.getUpcomingSessions().size());
    }
}
