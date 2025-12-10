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

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceCreateStudentTest {

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

    @InjectMocks StudentService studentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(studentRepository.findByStudentCode(any())).thenReturn(Optional.empty());
        when(policyService.getGlobalString(any(), any())).thenReturn("12345678");
        when(passwordEncoder.encode(any())).thenReturn("hashed_pw");
    }

    // --------------------------------------------------------------------
    // Helper: create valid request
    // --------------------------------------------------------------------
    private CreateStudentRequest baseRequest() {
        CreateStudentRequest req = new CreateStudentRequest();
        req.setEmail("new@fpt.com");
        req.setFullName("John Smith");
        req.setPhone("0901234567");
        req.setBranchId(1L);
        req.setSkillAssessments(new ArrayList<>());
        return req;
    }

    private void mockCommonSuccess(Long currentUserId) {
        // Branch accessible
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Hanoi Campus");
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(List.of(1L));

        // Creator exists
        UserAccount creator = new UserAccount();
        creator.setId(currentUserId);
        creator.setFullName("Admin");
        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.of(creator));

        // Save user
        UserAccount savedUser = new UserAccount();
        savedUser.setId(100L);
        savedUser.setEmail("new@fpt.com");
        savedUser.setFullName("John Smith");
        when(userAccountRepository.save(any())).thenReturn(savedUser);

        // Save student
        Student savedStudent = new Student();
        savedStudent.setId(200L);
        savedStudent.setStudentCode("ST1JOHN123");
        when(studentRepository.save(any())).thenReturn(savedStudent);

        // Role
        Role role = new Role();
        role.setId(10L);
        role.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT"))
                .thenReturn(Optional.of(role));
    }

    // --------------------------------------------------------------------
    // A — VALIDATION
    // --------------------------------------------------------------------

    // TC01 — Email exists
    @Test
    void createStudent_emailExists() {
        CreateStudentRequest req = baseRequest();
        when(userAccountRepository.findByEmail("new@fpt.com"))
                .thenReturn(Optional.of(new UserAccount()));

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    // TC02 — Phone exists
    @Test
    void createStudent_phoneExists() {
        CreateStudentRequest req = baseRequest();
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userAccountRepository.existsByPhone(req.getPhone()))
                .thenReturn(true);

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.USER_PHONE_ALREADY_EXISTS, ex.getErrorCode());
    }

    // TC03 — Branch not found
    @Test
    void createStudent_branchNotFound() {
        CreateStudentRequest req = baseRequest();
        when(branchRepository.findById(1L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.BRANCH_NOT_FOUND, ex.getErrorCode());
    }

    // TC04 — Branch access denied
    @Test
    void createStudent_branchAccessDenied() {
        CreateStudentRequest req = baseRequest();
        when(branchRepository.findById(1L)).thenReturn(Optional.of(new Branch()));
        when(userBranchesRepository.findBranchIdsByUserId(10L))
                .thenReturn(List.of(2L)); // no access

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // --------------------------------------------------------------------
    // B — SKILL ASSESSMENT VALIDATION
    // --------------------------------------------------------------------

    // TC05 — Level not found
    @Test
    void createStudent_levelNotFound() {
        CreateStudentRequest req = baseRequest();

        SkillAssessmentInput sai = new SkillAssessmentInput();
        sai.setLevelId(999L);
        sai.setSkill(Skill.LISTENING);
        req.setSkillAssessments(List.of(sai));

        mockCommonSuccess(10L);

        when(levelRepository.existsById(999L)).thenReturn(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.LEVEL_NOT_FOUND, ex.getErrorCode());
    }

    // TC06 — Assessor not found
    @Test
    void createStudent_assessorNotFound() {
        CreateStudentRequest req = baseRequest();

        SkillAssessmentInput sai = new SkillAssessmentInput();
        sai.setLevelId(1L);
        sai.setSkill(Skill.LISTENING);
        sai.setAssessedByUserId(999L);
        req.setSkillAssessments(List.of(sai));

        mockCommonSuccess(10L);
        when(levelRepository.existsById(1L)).thenReturn(true);
        when(levelRepository.findById(1L)).thenReturn(Optional.of(new Level()));

        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // TC07 — Assessor = null → fallback to currentUser
    @Test
    void createStudent_assessorFallbackToCurrentUser() {
        CreateStudentRequest req = baseRequest();

        SkillAssessmentInput sai = new SkillAssessmentInput();
        sai.setSkill(Skill.READING);
        sai.setLevelId(1L);
        req.setSkillAssessments(List.of(sai));

        mockCommonSuccess(10L);
        when(levelRepository.existsById(1L)).thenReturn(true);
        when(levelRepository.findById(1L)).thenReturn(Optional.of(new Level()));

        CreateStudentResponse res = studentService.createStudent(req, 10L);

        assertEquals(1, res.getSkillAssessmentsCreated());
        verify(replacementSkillAssessmentRepository, times(1)).save(any());
    }

    // TC08 — Create multiple assessments
    @Test
    void createStudent_multipleAssessments() {
        CreateStudentRequest req = baseRequest();

        SkillAssessmentInput s1 = new SkillAssessmentInput();
        s1.setLevelId(1L);
        s1.setSkill(Skill.LISTENING);

        SkillAssessmentInput s2 = new SkillAssessmentInput();
        s2.setLevelId(1L);
        s2.setSkill(Skill.SPEAKING);

        req.setSkillAssessments(List.of(s1, s2));

        mockCommonSuccess(10L);
        when(levelRepository.existsById(any())).thenReturn(true);
        when(levelRepository.findById(any())).thenReturn(Optional.of(new Level()));

        CreateStudentResponse res = studentService.createStudent(req, 10L);

        assertEquals(2, res.getSkillAssessmentsCreated());
        verify(replacementSkillAssessmentRepository, times(2)).save(any());
    }

    // --------------------------------------------------------------------
    // C — ROLE + CREATOR VALIDATION
    // --------------------------------------------------------------------

    // TC09 — Role STUDENT not found
    @Test
    void createStudent_roleNotFound() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        when(roleRepository.findByCode("STUDENT"))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, 10L));

        assertEquals(ErrorCode.STUDENT_ROLE_NOT_FOUND, ex.getErrorCode());
    }

    // TC10 — Creator not found
    @Test
    void createStudent_creatorNotFound() {
        CreateStudentRequest req = baseRequest();
        Long currentUserId = 10L;

        // Mock email không tồn tại
        when(userAccountRepository.findByEmail(req.getEmail())).thenReturn(Optional.empty());
        when(userAccountRepository.existsByPhone(req.getPhone())).thenReturn(false);

        // Mock branch & access ok
        Branch branch = new Branch();
        branch.setId(req.getBranchId());
        when(branchRepository.findById(req.getBranchId())).thenReturn(Optional.of(branch));
        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(List.of(req.getBranchId()));

        // Mock policy + password
        when(policyService.getGlobalString(any(), any())).thenReturn("12345678");
        when(passwordEncoder.encode(any())).thenReturn("HASHED");

        // ❗ MUST return non-null savedUser
        UserAccount savedUser = new UserAccount();
        savedUser.setId(999L);
        savedUser.setEmail(req.getEmail());
        savedUser.setFullName(req.getFullName());
        when(userAccountRepository.save(any())).thenReturn(savedUser);

        // Mock student save
        Student savedStudent = new Student();
        savedStudent.setId(200L);
        savedStudent.setStudentCode("ST1ABC123");
        when(studentRepository.save(any())).thenReturn(savedStudent);

        // Mock role found
        Role role = new Role();
        role.setId(5L);
        role.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(role));

        // ❗ THIS is the target: Creator not found
        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> studentService.createStudent(req, currentUserId));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }


    // --------------------------------------------------------------------
    // D — CREATION SUCCESS
    // --------------------------------------------------------------------

    // TC11 — Password encoded
    @Test
    void createStudent_passwordEncoded() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        studentService.createStudent(req, 10L);

        verify(passwordEncoder, times(1)).encode("12345678");
    }

    // TC12 — Student created with non-null code
    @Test
    void createStudent_studentCodeGenerated() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        CreateStudentResponse res = studentService.createStudent(req, 10L);

        assertNotNull(res.getStudentCode());
    }

    // --------------------------------------------------------------------
    // E — ASSIGN ROLE & BRANCH
    // --------------------------------------------------------------------

    // TC13 — Assign role success
    @Test
    void createStudent_assignRole() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        studentService.createStudent(req, 10L);

        verify(userRoleRepository, times(1)).save(any());
    }

    // TC14 — Assign branch success
    @Test
    void createStudent_assignBranch() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        studentService.createStudent(req, 10L);

        verify(userBranchesRepository, times(1)).save(any());
    }

    // --------------------------------------------------------------------
    // F — EMAIL NOTIFICATION
    // --------------------------------------------------------------------

    // TC15 — Email sent
    @Test
    void createStudent_emailSent() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        studentService.createStudent(req, 10L);

        verify(emailService, times(1))
                .sendNewStudentCredentialsAsync(
                        eq("new@fpt.com"),
                        eq("John Smith"),
                        any(),
                        eq("new@fpt.com"),
                        eq("12345678"),
                        eq("Hanoi Campus")
                );
    }

    // --------------------------------------------------------------------
    // G — OPTIONAL NULL FIELDS
    // --------------------------------------------------------------------

    // TC16 — Null optional fields should not break
    @Test
    void createStudent_optionalFieldsNull() {
        CreateStudentRequest req = baseRequest();
        req.setFacebookUrl(null);
        req.setAvatarUrl(null);
        req.setAddress(null);
        req.setGender(null);
        req.setDob(null);

        mockCommonSuccess(10L);

        assertDoesNotThrow(() -> studentService.createStudent(req, 10L));
    }

    // TC17 — fullName null → fallback email
    @Test
    void createStudent_fullNameNull_fallbackEmail() {
        CreateStudentRequest req = baseRequest();
        req.setFullName(null);

        mockCommonSuccess(10L);

        when(studentRepository.save(any())).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(200L);
            return s;
        });

        CreateStudentResponse res = studentService.createStudent(req, 10L);

        assertTrue(res.getStudentCode().contains("NEW"));   // now PASS
    }


    // --------------------------------------------------------------------
    // H — STUDENT CODE COLLISION
    // --------------------------------------------------------------------

    // TC18 — retry when student code collision happens
    @Test
    void createStudent_studentCodeCollision() {
        CreateStudentRequest req = baseRequest();
        mockCommonSuccess(10L);

        when(studentRepository.findByStudentCode(any()))
                .thenReturn(Optional.of(new Student()))   // first collision
                .thenReturn(Optional.empty());            // second OK

        CreateStudentResponse res = studentService.createStudent(req, 10L);

        assertNotNull(res.getStudentCode());
        verify(studentRepository, atLeast(2)).findByStudentCode(any());
    }
}
