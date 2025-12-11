package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.enrollment.EnrollExistingStudentsRequest;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import jakarta.persistence.EntityNotFoundException;
import org.fyp.tmssep490be.dtos.enrollment.EnrollmentResult;

@ExtendWith(MockitoExtension.class)
class EnrollmentService_EnrollExistingStudents_Test {

    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private ClassRepository classRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private StudentSessionRepository studentSessionRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private ExcelParserService excelParserService;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;
    @Mock private ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private StudentService studentService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Spy
    @InjectMocks
    private EnrollmentService service;

    // --------------------------------------------------------------
    // Helper
    // --------------------------------------------------------------
    private ClassEntity mockClass(Long id, int capacity) {
        ClassEntity c = new ClassEntity();
        c.setId(id);
        c.setApprovalStatus(ApprovalStatus.APPROVED);
        c.setStatus(ClassStatus.SCHEDULED);
        c.setMaxCapacity(capacity);
        return c;
    }

    // --------------------------------------------------------------
    // TEST 1: Class not found
    // --------------------------------------------------------------
    @Test
    void test_classNotFound() {
        when(classRepository.findById(10L)).thenReturn(Optional.empty());

        EnrollExistingStudentsRequest req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(List.of(1L));

        assertThrows(EntityNotFoundException.class,
                () -> service.enrollExistingStudents(req, 999L));
    }

    // --------------------------------------------------------------
    // TEST 2: Capacity exceeded without override
    // --------------------------------------------------------------
    @Test
    void test_capacityExceeded_noOverride() {
        ClassEntity c = mockClass(10L, 1); // max capacity = 1
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        when(enrollmentRepository.countByClassIdAndStatus(eq(10L), any()))
                .thenReturn(0);

        when(studentRepository.findAllById(anyList()))
                .thenReturn(List.of(new Student(), new Student()));

        EnrollExistingStudentsRequest req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(List.of(1L, 2L)); // 2 > 1 -> exceeded
        req.setOverrideCapacity(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.enrollExistingStudents(req, 999L));

        assertEquals(ErrorCode.CLASS_CAPACITY_EXCEEDED, ex.getErrorCode());
    }

    // --------------------------------------------------------------
    // TEST 3: Override but missing reason → FAIL
    // --------------------------------------------------------------
    @Test
    void test_override_missingReason() {
        ClassEntity c = mockClass(10L, 1);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        when(enrollmentRepository.countByClassIdAndStatus(eq(10L), any()))
                .thenReturn(0);

        when(studentRepository.findAllById(anyList()))
                .thenReturn(List.of(new Student(), new Student()));

        EnrollExistingStudentsRequest req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(List.of(1L, 2L));
        req.setOverrideCapacity(true);
        req.setOverrideReason(null); // missing

        CustomException ex = assertThrows(CustomException.class,
                () -> service.enrollExistingStudents(req, 999L));

        assertEquals(ErrorCode.OVERRIDE_REASON_REQUIRED, ex.getErrorCode());
    }

    // --------------------------------------------------------------
    // TEST 4: Success with override (FULL PASS)
    // --------------------------------------------------------------
    @Test
    void test_success_override() {
        ClassEntity c = mockClass(10L, 1);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        when(enrollmentRepository.countByClassIdAndStatus(eq(10L), any()))
                .thenReturn(0);

        when(studentRepository.findAllById(anyList()))
                .thenReturn(List.of(new Student(), new Student()));

        // MOCK enrollStudents() – IMPORTANT!!!
        EnrollmentResult fakeResult = EnrollmentResult.builder()
                .enrolledCount(2)
                .sessionsGeneratedPerStudent(5)
                .totalStudentSessionsCreated(10)
                .build();

        doReturn(fakeResult)
                .when(service)
                .enrollStudents(
                        anyLong(),
                        anyList(),
                        anyLong(),
                        anyBoolean(),
                        any()
                );

        EnrollExistingStudentsRequest req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(List.of(1L, 2L));
        req.setOverrideCapacity(true);
        req.setOverrideReason("Capacity override accepted by admin.");

        EnrollmentResult result = service.enrollExistingStudents(req, 999L);

        assertEquals(2, result.getEnrolledCount());
    }

    // --------------------------------------------------------------
    // TEST 5: Success WITHOUT override
    // --------------------------------------------------------------
    @Test
    void test_success_noOverride() {
        ClassEntity c = mockClass(10L, 5);
        when(classRepository.findById(10L)).thenReturn(Optional.of(c));

        when(enrollmentRepository.countByClassIdAndStatus(eq(10L), any()))
                .thenReturn(1);

        when(studentRepository.findAllById(anyList()))
                .thenReturn(List.of(new Student(), new Student()));

        EnrollmentResult fakeResult = EnrollmentResult.builder()
                .enrolledCount(2)
                .build();

        doReturn(fakeResult)
                .when(service)
                .enrollStudents(
                        anyLong(),
                        anyList(),
                        anyLong(),
                        anyBoolean(),
                        any()
                );

        EnrollExistingStudentsRequest req = new EnrollExistingStudentsRequest();
        req.setClassId(10L);
        req.setStudentIds(List.of(1L, 2L));
        req.setOverrideCapacity(false);

        EnrollmentResult result = service.enrollExistingStudents(req, 100L);

        assertEquals(2, result.getEnrolledCount());
    }
}
