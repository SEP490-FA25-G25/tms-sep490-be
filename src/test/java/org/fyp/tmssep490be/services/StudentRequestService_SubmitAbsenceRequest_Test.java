package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.AbsenceRequestDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
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
class StudentRequestService_SubmitAbsenceRequest_Test {

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
    private UserAccount user;

    @BeforeEach
    void setup() {
        dto = new AbsenceRequestDTO();
        dto.setCurrentClassId(10L);
        dto.setTargetSessionId(100L);
        dto.setRequestReason("Sick");
        dto.setNote("Need rest");

        student = Student.builder()
                .id(1L)
                .userAccount(UserAccount.builder().id(99L).build())
                .build();

        clazz = ClassEntity.builder().id(10L).build();

        session = Session.builder()
                .id(100L)
                .date(LocalDate.now().plusDays(3))
                .status(SessionStatus.PLANNED)
                .classEntity(clazz)
                .build();

        user = UserAccount.builder().id(99L).build();
    }

    // =============================================================
    // TC1 — studentRepository không tìm thấy student → exception
    // =============================================================
    @Test
    void TC1_studentNotFound() {
        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAbsenceRequest(99L, dto));
    }

    // =============================================================
    // TC2 — sessionRepository không tìm thấy session
    // =============================================================
    @Test
    void TC2_sessionNotFound() {
        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.submitAbsenceRequest(99L, dto));
    }

    // =============================================================
    // TC3 — session.classId != dto.currentClassId → SESSION_CLASS_MISMATCH
    // =============================================================
    @Test
    void TC3_sessionClassMismatch() {
        session.setClassEntity(ClassEntity.builder().id(999L).build());

        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitAbsenceRequest(99L, dto));

        assertEquals("SESSION_CLASS_MISMATCH", ex.getErrorCode());
    }

    // =============================================================
    // TC4 — session.date < today → PAST_SESSION
    // =============================================================
    @Test
    void TC4_pastSession() {
        session.setDate(LocalDate.now().minusDays(1));

        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitAbsenceRequest(99L, dto));

        assertEquals("PAST_SESSION", ex.getErrorCode());
    }

    // =============================================================
    // TC5 — session.status != PLANNED → INVALID_SESSION_STATUS
    // =============================================================
    @Test
    void TC5_invalidStatus() {
        session.setStatus(SessionStatus.CANCELLED);

        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitAbsenceRequest(99L, dto));

        assertEquals("INVALID_SESSION_STATUS", ex.getErrorCode());
    }

    // =============================================================
    // TC6 — enrollment = null → NOT_ENROLLED
    // =============================================================
    @Test
    void TC6_notEnrolled() {
        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(1L, 10L, EnrollmentStatus.ENROLLED))
                .thenReturn(null);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitAbsenceRequest(99L, dto));

        assertEquals("NOT_ENROLLED", ex.getErrorCode());
    }

    // =============================================================
    // TC7 — studentSessionRepository.findById empty → SESSION_NOT_ASSIGNED
    // =============================================================
    @Test
    void TC7_sessionNotAssigned() {
        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(any(), any(), any()))
                .thenReturn(Enrollment.builder().id(23L).build());
        when(studentSessionRepository.findById(any())).thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitAbsenceRequest(99L, dto));

        assertEquals("SESSION_NOT_ASSIGNED", ex.getErrorCode());
    }

    // =============================================================
    // TC8 — hasDuplicateRequest = true → DUPLICATE_REQUEST
    // =============================================================
    @Test
    void TC8_duplicateRequest() {
        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(any(), any(), any()))
                .thenReturn(Enrollment.builder().id(23L).build());
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(new StudentSession()));

        doReturn(true).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.submitAbsenceRequest(99L, dto));

        assertEquals("DUPLICATE_REQUEST", ex.getErrorCode());
    }

    // =============================================================
    // TC9 — daysUntil < ABSENCE_LEAD_TIME (warning allowed) → still SUCCESS
    // =============================================================
    @Test
    void TC9_leadTimeWarningButSuccess() {
        session.setDate(LocalDate.now().plusDays(0)); // lead time insufficient

        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(any(), any(), any()))
                .thenReturn(Enrollment.builder().id(23L).build());

        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        doReturn(false).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        // absence rate normal
        doReturn(5.0).when(service).calculateAbsenceRate(1L, 10L);

        when(classRepository.findById(10L)).thenReturn(Optional.of(clazz));
        when(userAccountRepository.findById(99L)).thenReturn(Optional.of(user));

        when(studentRequestRepository.save(any())).thenAnswer(inv -> {
            StudentRequest r = inv.getArgument(0);
            r.setId(321L);
            return r;
        });

        StudentRequestResponseDTO res = service.submitAbsenceRequest(99L, dto);
        assertEquals(321L, res.getId());
    }

    // =============================================================
    // TC10 — absenceRate > threshold → warning only, SUCCESS
    // =============================================================
    @Test
    void TC10_absenceRateHighButSuccess() {

        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(any(), any(), any()))
                .thenReturn(Enrollment.builder().id(23L).build());

        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        doReturn(false).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        // high absence rate
        doReturn(90.0).when(service).calculateAbsenceRate(1L, 10L);

        when(classRepository.findById(10L)).thenReturn(Optional.of(clazz));
        when(userAccountRepository.findById(99L)).thenReturn(Optional.of(user));

        when(studentRequestRepository.save(any())).thenAnswer(inv -> {
            StudentRequest r = inv.getArgument(0);
            r.setId(777L);
            return r;
        });

        StudentRequestResponseDTO res = service.submitAbsenceRequest(99L, dto);
        assertEquals(777L, res.getId());
    }

    // =============================================================
    // TC11 — FULL SUCCESS → return DTO
    // =============================================================
    @Test
    void TC11_fullSuccess() {

        when(studentRepository.findByUserAccountId(99L)).thenReturn(Optional.of(student));
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        when(enrollmentRepository.findByStudentIdAndClassIdAndStatus(any(), any(), any()))
                .thenReturn(Enrollment.builder().id(23L).build());

        when(studentSessionRepository.findById(any()))
                .thenReturn(Optional.of(new StudentSession()));

        doReturn(false).when(service)
                .hasDuplicateRequest(1L, 100L, StudentRequestType.ABSENCE);

        doReturn(5.0).when(service).calculateAbsenceRate(1L, 10L);

        when(classRepository.findById(10L)).thenReturn(Optional.of(clazz));
        when(userAccountRepository.findById(99L)).thenReturn(Optional.of(user));

        when(studentRequestRepository.save(any())).thenAnswer(inv -> {
            StudentRequest r = inv.getArgument(0);
            r.setId(999L);
            return r;
        });

        StudentRequestResponseDTO res = service.submitAbsenceRequest(99L, dto);

        assertNotNull(res);
        assertEquals(999L, res.getId());
        assertEquals("ABSENCE", res.getRequestType());
        assertEquals("PENDING", res.getStatus());
    }
}
