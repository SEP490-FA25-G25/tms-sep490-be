package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentportal.StudentClassDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentPortalService_GetStudentClassesTest {

    @InjectMocks
    private StudentPortalService studentPortalService;

    @Mock private ClassRepository classRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private StudentSessionRepository studentSessionRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeachingSlotRepository teachingSlotRepository;
    @Mock private AssessmentRepository assessmentRepository;
    @Mock private ScoreRepository scoreRepository;

    /** Helper mock class */
    private ClassEntity mockClass(Long id) {
        ClassEntity c = new ClassEntity();
        c.setId(id);
        c.setStatus(ClassStatus.ONGOING); // FIXED – enum hợp lệ

        Branch b = new Branch();
        b.setId(5L);
        c.setBranch(b);

        Subject s = new Subject();
        s.setId(7L);
        c.setSubject(s);

        c.setModality(Modality.ONLINE);
        return c;
    }

    /** TC1 – Student not found */
    @Test
    void studentNotFound_throwsException() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThrows(CustomException.class, () ->
                studentPortalService.getStudentClasses(
                        1L,
                        List.of(), List.of(), List.of(), List.of(), List.of(),
                        PageRequest.of(0, 10)
                )
        );
    }

    /** TC2 – Filters work correctly */
    @Test
    void filtersWorkCorrectly_success() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L);

        Enrollment e = new Enrollment();
        e.setStudentId(1L);
        e.setClassEntity(cls);
        e.setStatus(EnrollmentStatus.ENROLLED);

        when(enrollmentRepository.findByStudentIdAndStatusIn(eq(1L), any()))
                .thenReturn(List.of(e));

        Page<StudentClassDTO> result = studentPortalService.getStudentClasses(
                1L,
                List.of("ENROLLED"),
                List.of("ONGOING"),   // FIXED – enum hợp lệ
                List.of(5L),
                List.of(7L),
                List.of("ONLINE"),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getClassId());
    }

    /** TC3 – Pagination out of range */
    @Test
    void paginationOutOfRange_returnsEmpty() {
        when(studentRepository.existsById(1L)).thenReturn(true);

        ClassEntity cls = mockClass(10L);

        Enrollment e = new Enrollment();
        e.setClassEntity(cls);

        when(enrollmentRepository.findByStudentIdAndStatusIn(eq(1L), any()))
                .thenReturn(List.of(e));

        Page<StudentClassDTO> result = studentPortalService.getStudentClasses(
                1L,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                PageRequest.of(999, 10)
        );

        assertTrue(result.getContent().isEmpty());
    }
}
