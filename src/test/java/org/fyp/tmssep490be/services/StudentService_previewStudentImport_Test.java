package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServicePreviewImportTest {

    @Mock BranchRepository branchRepository;
    @Mock UserBranchesRepository userBranchesRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock StudentRepository studentRepository;
    @Mock ExcelParserService excelParserService;

    @InjectMocks StudentService studentService;

    MultipartFile fileMock;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        fileMock = mock(MultipartFile.class);
    }

    // Helper: branch OK + access OK
    private void mockBranchAccess(Long branchId, Long userId) {
        Branch b = new Branch();
        b.setId(branchId);
        b.setName("Hanoi Campus");

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(b));
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(branchId));
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI1 — Branch not found
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_branchNotFound() {
        when(branchRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.previewStudentImport(1L, fileMock, 10L));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex.getErrorCode());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI2 — User has no branch access
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_noBranchAccess() {
        Branch b = new Branch(); b.setId(1L);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(b));

        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(2L)); // no access

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.previewStudentImport(1L, fileMock, 10L));

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI3 — Excel file empty
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_excelEmpty() {
        mockBranchAccess(1L, 10L);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of());

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.previewStudentImport(1L, fileMock, 10L));

        assertEquals(ErrorCode.EXCEL_FILE_EMPTY, ex.getErrorCode());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI4 — Missing email
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_missingEmail() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setFullName("A");
        d.setGender(Gender.MALE);
        d.setEmail(null);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.ERROR,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI5 — Missing fullName
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_missingFullName() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setEmail("x@fpt.com");
        d.setGender(Gender.MALE);
        d.setFullName(null);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.ERROR,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI6 — Missing gender
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_missingGender() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setEmail("x@fpt.com");
        d.setFullName("A");
        d.setGender(null);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.ERROR,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI7 — Invalid email format
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_invalidEmail() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setEmail("abc@"); // invalid
        d.setFullName("A");
        d.setGender(Gender.MALE);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.ERROR,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI8 — Duplicate email inside Excel
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_duplicateEmailInExcel() {
        mockBranchAccess(1L, 10L);

        StudentImportData d1 = new StudentImportData();
        d1.setEmail("dup@fpt.com");
        d1.setFullName("A");
        d1.setGender(Gender.MALE);

        StudentImportData d2 = new StudentImportData();
        d2.setEmail("dup@fpt.com");
        d2.setFullName("B");
        d2.setGender(Gender.FEMALE);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d1, d2));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.ERROR,
                preview.getStudents().get(1).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI9 — Email exists → FOUND
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_emailExistsFound() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setEmail("x@fpt.com");
        d.setFullName("A");
        d.setGender(Gender.MALE);

        UserAccount u = new UserAccount();
        u.setId(100L);

        Student st = new Student();
        st.setId(200L);
        st.setStudentCode("ST200");

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        when(userAccountRepository.findByEmail("x@fpt.com"))
                .thenReturn(Optional.of(u));

        when(studentRepository.findByUserAccountId(100L))
                .thenReturn(Optional.of(st));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.FOUND,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI10 — Email belongs to user but no student → CREATE
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_emailExistsButNoStudent() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setEmail("x@fpt.com");
        d.setFullName("A");
        d.setGender(Gender.MALE);

        UserAccount u = new UserAccount();
        u.setId(100L);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        when(userAccountRepository.findByEmail("x@fpt.com"))
                .thenReturn(Optional.of(u));

        when(studentRepository.findByUserAccountId(100L))
                .thenReturn(Optional.empty());

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.CREATE,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI11 — Valid student row → CREATE
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_validRowCreate() {
        mockBranchAccess(1L, 10L);

        StudentImportData d = new StudentImportData();
        d.setEmail("new@fpt.com");
        d.setFullName("A");
        d.setGender(Gender.MALE);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(d));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(StudentImportData.StudentImportStatus.CREATE,
                preview.getStudents().get(0).getStatus());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI12 — Summary counts
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_summaryCounts() {
        mockBranchAccess(1L, 10L);

        StudentImportData found = new StudentImportData();
        found.setEmail("a@fpt.com");
        found.setFullName("A");
        found.setGender(Gender.MALE);

        StudentImportData create1 = new StudentImportData();
        create1.setEmail("b@fpt.com");
        create1.setFullName("B");
        create1.setGender(Gender.MALE);

        StudentImportData create2 = new StudentImportData();
        create2.setEmail("c@fpt.com");
        create2.setFullName("C");
        create2.setGender(Gender.FEMALE);

        StudentImportData err = new StudentImportData();
        err.setEmail(null); // ERROR

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(found, create1, create2, err));

        // FOUND case
        UserAccount u = new UserAccount(); u.setId(100L);
        Student st = new Student(); st.setId(200L); st.setStudentCode("ST200");

        when(userAccountRepository.findByEmail("a@fpt.com"))
                .thenReturn(Optional.of(u));

        when(studentRepository.findByUserAccountId(100L))
                .thenReturn(Optional.of(st));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertEquals(1, preview.getFoundCount());
        assertEquals(2, preview.getCreateCount());
        assertEquals(1, preview.getErrorCount());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI13 — Warning message generated
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_warningsGenerated() {
        mockBranchAccess(1L, 10L);

        StudentImportData found = new StudentImportData();
        found.setEmail("a@fpt.com");
        found.setFullName("A");
        found.setGender(Gender.MALE);

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(found));

        UserAccount u = new UserAccount(); u.setId(100L);
        Student st = new Student(); st.setId(200L);

        when(userAccountRepository.findByEmail("a@fpt.com"))
                .thenReturn(Optional.of(u));

        when(studentRepository.findByUserAccountId(100L))
                .thenReturn(Optional.of(st));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertFalse(preview.getWarnings().isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // TC-PI14 — Error message generated
    // ---------------------------------------------------------------------------------------------
    @Test
    void preview_errorsGenerated() {
        mockBranchAccess(1L, 10L);

        StudentImportData err = new StudentImportData();
        err.setEmail(null); // ERROR

        when(excelParserService.parseStudentImport(fileMock))
                .thenReturn(List.of(err));

        StudentImportPreview preview =
                studentService.previewStudentImport(1L, fileMock, 10L);

        assertFalse(preview.getErrors().isEmpty());
    }
}
