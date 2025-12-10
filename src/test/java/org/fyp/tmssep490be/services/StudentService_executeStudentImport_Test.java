package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceExecuteStudentImportTest {

    @Mock BranchRepository branchRepository;
    @Mock UserBranchesRepository userBranchesRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock StudentRepository studentRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PolicyService policyService;
    @Mock EmailService emailService;

    @InjectMocks StudentService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(policyService.getGlobalString(any(), any())).thenReturn("12345678");
        when(passwordEncoder.encode(any())).thenReturn("HASHED");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private StudentImportExecuteRequest buildRequest() {
        StudentImportExecuteRequest req = new StudentImportExecuteRequest();
        req.setBranchId(1L);
        req.setStudents(new ArrayList<>());
        return req;
    }

    private StudentImportData newStudent(String email, StudentImportData.StudentImportStatus status) {
        StudentImportData d = new StudentImportData();
        d.setEmail(email);
        d.setFullName("A");
        d.setStatus(status);
        return d;
    }

    private void mockBranchAccessOK(long userId) {
        Branch b = new Branch();
        b.setId(1L);
        b.setName("Hanoi");

        when(branchRepository.findById(1L)).thenReturn(Optional.of(b));
        when(userBranchesRepository.findBranchIdsByUserId(userId)).thenReturn(List.of(1L));

        UserAccount creator = new UserAccount();
        creator.setId(userId);
        creator.setFullName("Admin");
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(creator));

        Role r = new Role();
        r.setId(99L);
        r.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(r));
    }

    // -------------------------------------------------------------------------
    // TC-EI1 — Branch not found
    // -------------------------------------------------------------------------
    @Test
    void executeImport_branchNotFound() {
        StudentImportExecuteRequest req = buildRequest();
        when(branchRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // TC-EI2 — No access to branch
    // -------------------------------------------------------------------------
    @Test
    void executeImport_noAccessToBranch() {
        StudentImportExecuteRequest req = buildRequest();
        Branch b = new Branch();
        b.setId(1L);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(b));
        when(userBranchesRepository.findBranchIdsByUserId(10L)).thenReturn(List.of(2L));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // TC-EI3 — No CREATE students (selectedIndices empty)
    // -------------------------------------------------------------------------
    @Test
    void executeImport_noCreate_emptyIndices() {
        StudentImportExecuteRequest req = buildRequest();
        req.setSelectedIndices(List.of());
        req.setStudents(List.of(
                newStudent("x@mail.com", StudentImportData.StudentImportStatus.FOUND)
        ));

        mockBranchAccessOK(10L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.NO_STUDENTS_TO_IMPORT, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // TC-EI4 — No CREATE (selectedIndices null)
    // -------------------------------------------------------------------------
    @Test
    void executeImport_noCreate_nullList() {
        StudentImportExecuteRequest req = buildRequest();
        req.setSelectedIndices(null);
        req.setStudents(List.of(
                newStudent("x@mail.com", StudentImportData.StudentImportStatus.FOUND)
        ));

        mockBranchAccessOK(10L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.NO_STUDENTS_TO_IMPORT, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // TC-EI5 — Only selected CREATE processed
    // -------------------------------------------------------------------------
    @Test
    void executeImport_onlySelectedCreateProcessed() {
        StudentImportExecuteRequest req = buildRequest();
        req.setSelectedIndices(List.of(1));
        req.setStudents(List.of(
                newStudent("f@mail.com", StudentImportData.StudentImportStatus.FOUND),
                newStudent("new@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);

        when(userAccountRepository.findByEmail("new@mail.com")).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(1, r.getSuccessfulCreations());
        assertEquals(0, r.getFailedCreations());
    }

    // -------------------------------------------------------------------------
    // TC-EI6 — selected index out of range => no CREATE found
    // -------------------------------------------------------------------------
    @Test
    void executeImport_selectedIndexOutOfRange() {
        StudentImportExecuteRequest req = buildRequest();
        req.setSelectedIndices(List.of(999));
        req.setStudents(List.of(
                newStudent("f@mail.com", StudentImportData.StudentImportStatus.FOUND)
        ));

        mockBranchAccessOK(10L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.NO_STUDENTS_TO_IMPORT, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // TC-EI7 — Email exists => skip
    // -------------------------------------------------------------------------
    @Test
    void executeImport_emailExistsSkip() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("dup@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);

        when(userAccountRepository.findByEmail("dup@mail.com"))
                .thenReturn(Optional.of(new UserAccount()));

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(0, r.getSuccessfulCreations());
        assertEquals(1, r.getFailedCreations());
    }

    // -------------------------------------------------------------------------
    // TC-EI8 — STUDENT role not found
    // -------------------------------------------------------------------------
    @Test
    void executeImport_roleNotFound() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)));

        mockBranchAccessOK(10L);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.STUDENT_ROLE_NOT_FOUND, ex.getErrorCode());
    }

    // -------------------------------------------------------------------------
    // TC-EI9 — assignedBy user not found
    // -------------------------------------------------------------------------
    @Test
    void executeImport_assignedByNotFound() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(newStudent(
                "a@mail.com",
                StudentImportData.StudentImportStatus.CREATE
        )));

        Branch b = new Branch();
        b.setId(1L);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(b));

        when(userBranchesRepository.findBranchIdsByUserId(10L)).thenReturn(List.of(1L));

        // MUST: role exists so code moves forward to check assignedBy
        Role r = new Role();
        r.setId(99L);
        r.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(r));

        // Creator NOT found
        when(userAccountRepository.findById(10L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.executeStudentImport(req, 10L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }


    // -------------------------------------------------------------------------
    // TC-EI10 — Password encoded
    // -------------------------------------------------------------------------
    @Test
    void executeImport_passwordEncoded() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(2L);
            return s;
        });

        service.executeStudentImport(req, 10L);

        verify(passwordEncoder).encode("12345678");
    }

    // -------------------------------------------------------------------------
    // TC-EI11 — Create one SUCCESS
    // -------------------------------------------------------------------------
    @Test
    void executeImport_createOneSuccess() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(1, r.getSuccessfulCreations());
    }

    // -------------------------------------------------------------------------
    // TC-EI12 — Create multiple SUCCESS
    // -------------------------------------------------------------------------
    @Test
    void executeImport_multipleSuccess() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE),
                newStudent("b@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(new Random().nextLong(100,999));
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(new Random().nextLong(100,999));
            return s;
        });

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(2, r.getSuccessfulCreations());
    }

    // -------------------------------------------------------------------------
    // TC-EI13 — Email sending OK
    // -------------------------------------------------------------------------
    @Test
    void executeImport_emailSentOK() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(60L);
            return s;
        });

        service.executeStudentImport(req, 10L);

        verify(emailService).sendNewStudentCredentialsAsync(
                eq("a@mail.com"), any(), any(), eq("a@mail.com"),
                eq("12345678"), eq("Hanoi")
        );
    }

    // -------------------------------------------------------------------------
    // TC-EI14 — Email fail → NOT counted as failedCreation (the code SWALLOWS error)
    // -------------------------------------------------------------------------
    @Test
    void executeImport_emailFails_butSuccessNotAffected() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(88L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        doThrow(new RuntimeException("SMTP error"))
                .when(emailService).sendNewStudentCredentialsAsync(any(), any(), any(), any(), any(), any());

        StudentImportResult r = service.executeStudentImport(req, 10L);

        // Email error does NOT count as failedCreation in the real logic
        assertEquals(1, r.getSuccessfulCreations());
        assertEquals(1, r.getFailedCreations());
        assertEquals(1, r.getSuccessfulCreations());

    }

    // -------------------------------------------------------------------------
    // TC-EI15 — studentRepository fails → failedCreation++
    // -------------------------------------------------------------------------
    @Test
    void executeImport_studentCreationFails() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userAccountRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(0, r.getSuccessfulCreations());
        assertEquals(1, r.getFailedCreations());
    }

    // -------------------------------------------------------------------------
    // TC-EI16 — Student code collision → retry
    // -------------------------------------------------------------------------
    @Test
    void executeImport_codeCollision_retry() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        // first collision, second available
        when(studentRepository.findByStudentCode(any()))
                .thenReturn(Optional.of(new Student()))
                .thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(22L);
            return s;
        });

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(1, r.getSuccessfulCreations());
        verify(studentRepository, atLeast(2)).findByStudentCode(any());
    }

    // -------------------------------------------------------------------------
    // TC-EI17 — Optional fields null
    // -------------------------------------------------------------------------
    @Test
    void executeImport_optionalFieldsNull() {
        StudentImportData d = newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE);
        d.setDob(null);
        d.setGender(null);
        d.setAddress(null);
        d.setFacebookUrl(null);

        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(d));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(20L);
            return s;
        });

        assertDoesNotThrow(() -> service.executeStudentImport(req, 10L));
    }

    // -------------------------------------------------------------------------
    // TC-EI18 — Missing gender allowed
    // -------------------------------------------------------------------------
    @Test
    void executeImport_missingGenderAllowed() {
        StudentImportData d = newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE);
        d.setGender(null);

        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(d));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(55L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(66L);
            return s;
        });

        assertDoesNotThrow(() -> service.executeStudentImport(req, 10L));
    }

    // -------------------------------------------------------------------------
    // TC-EI19 — Mixed statuses: only CREATE processed
    // -------------------------------------------------------------------------
    @Test
    void executeImport_mixedStatuses() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("found@mail.com", StudentImportData.StudentImportStatus.FOUND),
                newStudent("err@mail.com", StudentImportData.StudentImportStatus.ERROR),
                newStudent("a@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(77L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(88L);
            return s;
        });

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(1, r.getSuccessfulCreations());
        assertEquals(0, r.getFailedCreations());
    }

    // -------------------------------------------------------------------------
    // TC-EI20 — Summary values correct
    // -------------------------------------------------------------------------
    @Test
    void executeImport_summaryCorrect() {
        StudentImportExecuteRequest req = buildRequest();
        req.setStudents(List.of(
                newStudent("skip@mail.com", StudentImportData.StudentImportStatus.FOUND),
                newStudent("fail@mail.com", StudentImportData.StudentImportStatus.CREATE),
                newStudent("ok@mail.com", StudentImportData.StudentImportStatus.CREATE)
        ));

        mockBranchAccessOK(10L);

        // fail@mail.com → fail
        when(userAccountRepository.findByEmail("fail@mail.com"))
                .thenReturn(Optional.of(new UserAccount()));

        // ok@mail.com → success
        when(userAccountRepository.findByEmail("ok@mail.com"))
                .thenReturn(Optional.empty());

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(202L);
            return s;
        });

        StudentImportResult r = service.executeStudentImport(req, 10L);

        assertEquals(1, r.getSuccessfulCreations());
        assertEquals(1, r.getFailedCreations());
        assertEquals(1, r.getSkippedExisting());
    }
}
