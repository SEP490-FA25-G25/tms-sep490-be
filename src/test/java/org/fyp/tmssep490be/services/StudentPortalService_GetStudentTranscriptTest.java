package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void studentNotFound_throwsException() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThrows(CustomException.class, () ->
                service.getStudentTranscript(1L)
        );
    }

    @Test
    void onlyEnrolledAndCompletedIncluded_success() {

        when(studentRepository.existsById(1L)).thenReturn(true);

        // Mock ClassEntity
        ClassEntity cls = new ClassEntity();
        cls.setId(10L);
        cls.setStatus(ClassStatus.ONGOING);

        // Mock two valid enrollments
        Enrollment e1 = new Enrollment();
        e1.setStatus(EnrollmentStatus.ENROLLED);
        e1.setClassEntity(cls);

        Enrollment e2 = new Enrollment();
        e2.setStatus(EnrollmentStatus.COMPLETED);
        e2.setClassEntity(cls);

        // Mock one invalid enrollment
        Enrollment e3 = new Enrollment();
        e3.setStatus(EnrollmentStatus.TRANSFERRED);
        e3.setClassEntity(cls);

        when(enrollmentRepository.findByStudentIdWithClassAndCourse(1L))
                .thenReturn(List.of(e1, e2, e3));

        // Mock session & score to avoid null when calculating average
        when(sessionRepository.findAllByClassIdOrderByDateAndTime(cls.getId()))
                .thenReturn(List.of());

        when(scoreRepository.findByStudentIdAndClassId(any(), any()))
                .thenReturn(List.of());

        var result = service.getStudentTranscript(1L);

        assertEquals(2, result.size());
    }
}
