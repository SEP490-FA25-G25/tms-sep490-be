package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceTest {

    @Mock StudentRepository studentRepository;
    @Mock UserBranchesRepository userBranchesRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock BranchRepository branchRepository;
    @Mock LevelRepository levelRepository;
    @Mock EmailService emailService;
    @Mock ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PolicyService policyService;
    @Mock ExcelParserService excelParserService;
    @Mock EnrollmentRepository enrollmentRepository;

    @InjectMocks StudentService studentService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        // default mocks to avoid optional null issues
        when(studentRepository.findByStudentCode(any())).thenReturn(Optional.empty());
        when(studentRepository.findByUserAccountId(any())).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    }


    // -----------------------------------------------------------------------
    // 1️⃣ CREATE STUDENT — SUCCESS
    // -----------------------------------------------------------------------
    @Test
    void createStudent_success() {

        CreateStudentRequest req = new CreateStudentRequest();
        req.setEmail("new@student.com");
        req.setFullName("John Smith");
        req.setPhone("0901234567");
        req.setBranchId(1L);
        req.setSkillAssessments(new ArrayList<>()); // no assessments

        Long currentUserId = 99L;

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("FPT Campus");

        UserAccount creator = new UserAccount();
        creator.setId(currentUserId);
        creator.setFullName("Admin");

        // REPO MOCKS
        when(userAccountRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
        when(userAccountRepository.existsByPhone(req.getPhone())).thenReturn(false);

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userBranchesRepository.findBranchIdsByUserId(currentUserId)).thenReturn(List.of(1L));

        when(policyService.getGlobalString(any(), any())).thenReturn("12345678");
        when(passwordEncoder.encode(any())).thenReturn("hashed_pw");

        UserAccount savedUser = new UserAccount();
        savedUser.setId(100L);
        savedUser.setEmail(req.getEmail());
        savedUser.setFullName(req.getFullName());

        when(userAccountRepository.save(any())).thenReturn(savedUser);

        Student savedStudent = new Student();
        savedStudent.setId(200L);
        savedStudent.setStudentCode("ST1JOHN000");

        when(studentRepository.save(any())).thenReturn(savedStudent);

        Role studentRole = new Role();
        studentRole.setId(10L);
        studentRole.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));

        when(userAccountRepository.findById(currentUserId)).thenReturn(Optional.of(creator));

        CreateStudentResponse res =
                studentService.createStudent(req, currentUserId);

        assertEquals(200L, res.getStudentId());
        assertEquals("new@student.com", res.getEmail());
        assertEquals("John Smith", res.getFullName());

        verify(emailService, times(1)).sendNewStudentCredentialsAsync(
                eq("new@student.com"),
                eq("John Smith"),
                any(),
                eq("new@student.com"),
                eq("12345678"),
                eq("FPT Campus")
        );
    }

    // -----------------------------------------------------------------------
    // 2️⃣ CREATE STUDENT — EMAIL DUPLICATE
    // -----------------------------------------------------------------------
    @Test
    void createStudent_emailAlreadyExists() {

        CreateStudentRequest req = new CreateStudentRequest();
        req.setEmail("dup@fpt.edu.vn");
        Long currentUserId = 1L;

        when(userAccountRepository.findByEmail("dup@fpt.edu.vn"))
                .thenReturn(Optional.of(new UserAccount()));

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, currentUserId));

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    // -----------------------------------------------------------------------
    // 3️⃣ CREATE STUDENT — INVALID BRANCH ACCESS
    // -----------------------------------------------------------------------
    @Test
    void createStudent_branchAccessDenied() {

        CreateStudentRequest req = new CreateStudentRequest();
        req.setEmail("new@fpt.com");
        req.setFullName("John");
        req.setBranchId(5L);

        Long currentUserId = 1L;

        when(userAccountRepository.findByEmail(req.getEmail()))
                .thenReturn(Optional.empty());

        when(branchRepository.findById(5L)).thenReturn(Optional.of(new Branch()));

        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(List.of(1L, 2L)); // no branch = 5

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, currentUserId));

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // -----------------------------------------------------------------------
    // 4️⃣ GET STUDENTS — RETURNS PAGE
    // -----------------------------------------------------------------------
    @Test
    void getStudents_success() {
        Pageable pageable = PageRequest.of(0, 10);

        Long userId = 50L;

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Student student = new Student();
        student.setId(10L);

        UserAccount ua = new UserAccount();
        ua.setFullName("Alice");
        ua.setEmail("a@x.com");
        student.setUserAccount(ua);

        Page<Student> page = new PageImpl<>(List.of(student));

        when(studentRepository.findStudentsWithFilters(
                any(), any(), any(), any(), any()))
                .thenReturn(page);

        Page<StudentListItemDTO> result =
                studentService.getStudents(null, "", null, null, pageable, userId);

        assertEquals(1, result.getTotalElements());
        assertEquals("Alice", result.getContent().get(0).getFullName());
    }

    // -----------------------------------------------------------------------
    // 5️⃣ PREVIEW IMPORT — SUCCESS
    // -----------------------------------------------------------------------
    @Test
    void previewImport_success() {

        Long branchId = 1L;
        Long currentUserId = 9L;

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Hanoi Campus");

        MultipartFile mockFile = mock(MultipartFile.class);

        StudentImportData d1 = new StudentImportData();
        d1.setEmail("st1@fpt.com");
        d1.setFullName("Student One");
        d1.setGender(Gender.MALE);

        when(branchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(List.of(1L));

        when(excelParserService.parseStudentImport(mockFile))
                .thenReturn(List.of(d1));

        when(userAccountRepository.findByEmail("st1@fpt.com"))
                .thenReturn(Optional.empty());

        StudentImportPreview preview =
                studentService.previewStudentImport(branchId, mockFile, currentUserId);

        assertEquals(1, preview.getTotalValid());
        assertEquals("Hanoi Campus", preview.getBranchName());
    }

    // -----------------------------------------------------------------------
    // 6️⃣ EXECUTE IMPORT — BASIC SUCCESS
    // -----------------------------------------------------------------------
    @Test
    void executeImport_success() {

        StudentImportData s = new StudentImportData();
        s.setFullName("Bob");
        s.setEmail("b@fpt.com");
        s.setGender(Gender.MALE);
        s.setStatus(StudentImportData.StudentImportStatus.CREATE);

        StudentImportExecuteRequest req = new StudentImportExecuteRequest();
        req.setBranchId(1L);
        req.setStudents(List.of(s));
        req.setSelectedIndices(List.of(0));

        Long currentUserId = 99L;

        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Hanoi");

        when(branchRepository.findById(1L))
                .thenReturn(Optional.of(branch));

        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(List.of(1L));

        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.of(new UserAccount()));

        when(policyService.getGlobalString(any(), any()))
                .thenReturn("12345678");

        Role role = new Role();
        role.setId(5L);
        role.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT"))
                .thenReturn(Optional.of(role));

        UserAccount savedUser = new UserAccount();
        savedUser.setId(10L);
        savedUser.setEmail(s.getEmail());
        savedUser.setFullName("Bob");
        when(userAccountRepository.save(any())).thenReturn(savedUser);

        Student savedStudent = new Student();
        savedStudent.setId(20L);
        savedStudent.setStudentCode("ST1001");
        when(studentRepository.save(any())).thenReturn(savedStudent);

        StudentImportResult result =
                studentService.executeStudentImport(req, currentUserId);

        assertEquals(1, result.getSuccessfulCreations());
        verify(emailService, times(1)).sendNewStudentCredentialsAsync(
                eq("b@fpt.com"),
                eq("Bob"),
                any(),
                eq("b@fpt.com"),
                eq("12345678"),
                eq("Hanoi")
        );
    }
}
