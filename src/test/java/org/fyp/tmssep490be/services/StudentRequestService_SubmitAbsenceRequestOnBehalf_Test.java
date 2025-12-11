package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.AbsenceRequestDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentRequestService_SubmitAbsenceRequestOnBehalf_Test {

    @Mock private StudentRepository studentRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentSessionRepository studentSessionRepository;
    @Mock private ClassRepository classRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private StudentRequestRepository studentRequestRepository;

    @Spy
    @InjectMocks
    private StudentRequestService service;

    private AbsenceRequestDTO dto;
    private Student student;
    private Session session;
    private ClassEntity clazz;
    private UserAccount aaUser;

    @BeforeEach
    void setup() {
        dto = new AbsenceRequestDTO();
        dto.setStudentId(1L);
        dto.setCurrentClassId(10L);
        dto.setTargetSessionId(100L);
        dto.setRequestReason("Sick");
        dto.setNote("Created by AA");

        clazz = ClassEntity.builder()
                .id(10L)
                .build();

        student = Student.builder()
                .id(1L)
                .userAccount(UserAccount.builder().id(1000L).fullName("Student A").build())
                .build();

        session = Session.builder()
                .id(100L)
                .date(LocalDate.now().plusDays(3))
                .status(SessionStatus.PLANNED)
                .classEntity(clazz)
                .build();

        aaUser = UserAccount.builder()
                .id(99L)
                .fullName("AA Staff")
                .build();
    }

    // =============================================================
    // TC1 — MISSING_STUDENT_ID
    // =============================================================
    @Test
    void TC1_missingStudentId() {
        dto.setStudentId(null);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("MISSING_STUDENT_ID", ex.getErrorCode());
    }

    // =============================================================
    // TC2 — Student không tồn tại
    // =============================================================
    @Test
    void TC2_studentNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto));
    }

    // =============================================================
    // TC3 — Session không tồn tại
    // =============================================================
    @Test
    void TC3_sessionNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto));
    }

    // =============================================================
    // TC4 — SESSION_CLASS_MISMATCH
    // =============================================================
    @Test
    void TC4_sessionClassMismatch() {
        Session otherClassSession = Session.builder()
                .id(100L)
                .date(LocalDate.now().plusDays(3))
                .status(SessionStatus.PLANNED)
                .classEntity(ClassEntity.builder().id(999L).build())
                .build();

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(otherClassSession));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("SESSION_CLASS_MISMATCH", ex.getErrorCode());
    }

    // =============================================================
    // TC5 — PAST_SESSION
    // =============================================================
    @Test
    void TC5_pastSession() {
        session.setDate(LocalDate.now().minusDays(1));

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("PAST_SESSION", ex.getErrorCode());
    }

    // =============================================================
    // TC6 — INVALID_SESSION_STATUS
    // =============================================================
    @Test
    void TC6_invalidSessionStatus() {
        session.setStatus(SessionStatus.CANCELLED);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("INVALID_SESSION_STATUS", ex.getErrorCode());
    }

    // =============================================================
    // TC7 — NOT_ENROLLED
    // =============================================================
    @Test
    void TC7_notEnrolled() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(
                1L, 10L, EnrollmentStatus.ENROLLED)).thenReturn(null);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("NOT_ENROLLED", ex.getErrorCode());
    }

    // =============================================================
    // TC8 — SESSION_NOT_ASSIGNED
    // =============================================================
    @Test
    void TC8_sessionNotAssigned() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(
                1L, 10L, EnrollmentStatus.ENROLLED))
                .thenReturn(Enrollment.builder().id(200L).build());

        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("SESSION_NOT_ASSIGNED", ex.getErrorCode());
    }

    // =============================================================
    // TC9 — DUPLICATE_REQUEST
    // =============================================================
    @Test
    void TC9_duplicateRequest() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(
                1L, 10L, EnrollmentStatus.ENROLLED))
                .thenReturn(Enrollment.builder().id(200L).build());

        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        doReturn(true).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto)
        );

        assertEquals("DUPLICATE_REQUEST", ex.getErrorCode());
    }

    // =============================================================
    // TC10 — Class not found
    // =============================================================
    @Test
    void TC10_classNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(
                1L, 10L, EnrollmentStatus.ENROLLED))
                .thenReturn(Enrollment.builder().id(200L).build());
        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        doReturn(false).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        when(classRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto));
    }

    // =============================================================
    // TC11 — AA user (decidedBy) not found
    // =============================================================
    @Test
    void TC11_userNotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(
                1L, 10L, EnrollmentStatus.ENROLLED))
                .thenReturn(Enrollment.builder().id(200L).build());
        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        doReturn(false).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        when(classRepository.findById(10L)).thenReturn(Optional.of(clazz));
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAbsenceRequestOnBehalf(99L, dto));
    }

    // =============================================================
    // TC12 — FULL SUCCESS (auto-approve + mark EXCUSED)
    // =============================================================
    @Test
    void TC12_success_autoApprovedAndExcused() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(
                1L, 10L, EnrollmentStatus.ENROLLED))
                .thenReturn(Enrollment.builder().id(200L).build());
        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(StudentSession.builder().build()));

        doReturn(false).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        when(classRepository.findById(10L)).thenReturn(Optional.of(clazz));
        when(userAccountRepository.findById(99L)).thenReturn(Optional.of(aaUser));

        // Gắn ID cho request khi save
        when(studentRequestRepository.save(any()))
                .thenAnswer(inv -> {
                    StudentRequest r = inv.getArgument(0);
                    r.setId(999L);
                    return r;
                });

        // Khi markSessionAsExcused gọi save(StudentSession)
        when(studentSessionRepository.save(any(StudentSession.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        StudentRequestResponseDTO result =
                service.submitAbsenceRequestOnBehalf(99L, dto);

        assertNotNull(result);
        assertEquals(999L, result.getId());
        assertEquals("ABSENCE", result.getRequestType());
        assertEquals("APPROVED", result.getStatus());

        // Verify request được lưu
        verify(studentRequestRepository, times(1)).save(any(StudentRequest.class));
        // Verify session được mark EXCUSED (save StudentSession)
        verify(studentSessionRepository, atLeastOnce()).save(argThat(ss ->
                ss.getAttendanceStatus() != null
        ));
    }
}
