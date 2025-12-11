package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.fyp.tmssep490be.dtos.enrollment.EnrollExistingStudentsRequest;

import jakarta.persistence.EntityNotFoundException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentService_ValidateClass_Test {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private StudentSessionRepository studentSessionRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private ExcelParserService excelParserService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private StudentService studentService;

    @InjectMocks
    private EnrollmentService service;

    // --------------------------------------------------------------------
    // Helper
    // --------------------------------------------------------------------
    private ClassEntity mockClass(Long id, ApprovalStatus approval, ClassStatus status) {
        ClassEntity c = new ClassEntity();
        c.setId(id);
        c.setApprovalStatus(approval);
        c.setStatus(status);
        c.setStartDate(LocalDate.now().minusDays(1));
        c.setMaxCapacity(30); // ← FIX: tránh NullPointerException
        return c;
    }


    private void mockPostValidationFlow() {
        // avoid NPE after validateClass()
        when(studentRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(enrollmentRepository.countByClassIdAndStatus(anyLong(), any())).thenReturn(0);

        // mock future sessions list
        Session s = new Session();
        s.setId(999L);
        s.setDate(LocalDate.now().plusDays(1));
        s.setStatus(SessionStatus.PLANNED);

        when(sessionRepository
                .findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                        anyLong(), any(), any()))
                .thenReturn(Collections.singletonList(s));
    }

    // --------------------------------------------------------------------
    // TEST 1: Class NOT FOUND
    // --------------------------------------------------------------------
    @Test
    void testValidateClass_classNotFound() {
        when(classRepository.findById(10L)).thenReturn(Optional.empty());

        var req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(java.util.List.of(1L));

        assertThrows(
                EntityNotFoundException.class,
                () -> service.enrollExistingStudents(req, 100L)
        );
    }

    // --------------------------------------------------------------------
    // TEST 2: Class NOT APPROVED
    // --------------------------------------------------------------------
    @Test
    void testValidateClass_notApproved() {
        ClassEntity c = mockClass(10L, ApprovalStatus.PENDING, ClassStatus.SCHEDULED);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        var req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(java.util.List.of(1L));

        CustomException ex = assertThrows(
                CustomException.class,
                () -> service.enrollExistingStudents(req, 100L)
        );

        assertEquals(ErrorCode.CLASS_NOT_APPROVED, ex.getErrorCode());
    }

    // --------------------------------------------------------------------
    // TEST 3: Class INVALID STATUS
    // --------------------------------------------------------------------
    @Test
    void testValidateClass_invalidStatus() {
        ClassEntity c = mockClass(10L, ApprovalStatus.APPROVED, ClassStatus.CANCELLED);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        var req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(java.util.List.of(1L));

        CustomException ex = assertThrows(
                CustomException.class,
                () -> service.enrollExistingStudents(req, 100L)
        );

        assertEquals(ErrorCode.CLASS_INVALID_STATUS, ex.getErrorCode());
    }

    // --------------------------------------------------------------------
    // TEST 4: VALIDATE OK (SCHEDULED)
    // --------------------------------------------------------------------
    @Test
    void testValidateClass_scheduled_ok() {
        ClassEntity c = mockClass(10L, ApprovalStatus.APPROVED, ClassStatus.SCHEDULED);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        mockPostValidationFlow();

        var req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(Collections.emptyList());

        assertDoesNotThrow(() -> service.enrollExistingStudents(req, 100L));
    }

    // --------------------------------------------------------------------
    // TEST 5: VALIDATE OK (ONGOING)
    // --------------------------------------------------------------------
    @Test
    void testValidateClass_ongoing_ok() {
        ClassEntity c = mockClass(10L, ApprovalStatus.APPROVED, ClassStatus.ONGOING);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        mockPostValidationFlow();

        var req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(Collections.emptyList());

        assertDoesNotThrow(() -> service.enrollExistingStudents(req, 100L));
    }
}
