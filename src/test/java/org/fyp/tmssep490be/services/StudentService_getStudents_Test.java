package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.StudentListItemDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.*;
import org.mockito.*;

import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentServiceGetStudentsTest {

    @Mock StudentRepository studentRepository;
    @Mock UserBranchesRepository userBranchesRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock PolicyService policyService;

    @InjectMocks StudentService studentService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    private Pageable pageable() {
        return PageRequest.of(0, 10, Sort.by("studentCode"));
    }

    // ---------------------------------------------------------
    // TC-GS1 — branchIds=null → auto-use user branches
    // ---------------------------------------------------------
    @Test
    void getStudents_branchIdsNull_autoUseUserBranches() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Page<Student> page = new PageImpl<>(new ArrayList<>());
        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(page);

        studentService.getStudents(null, "", null, null, pageable(), userId);

        verify(studentRepository, times(1))
                .findStudentsWithFilters(eq(List.of(1L)), eq(""), eq(null), eq(null), any());
    }

    // ---------------------------------------------------------
    // TC-GS2 — Valid branchIds
    // ---------------------------------------------------------
    @Test
    void getStudents_validBranchIds() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L, 2L, 3L));

        Page<Student> page = new PageImpl<>(new ArrayList<>());
        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(page);

        studentService.getStudents(List.of(1L, 2L), "", null, null, pageable(), userId);

        verify(studentRepository).findStudentsWithFilters(eq(List.of(1L, 2L)), eq(""), eq(null), eq(null), any());
    }

    // ---------------------------------------------------------
    // TC-GS3 — Unauthorized branch requested
    // ---------------------------------------------------------
    @Test
    void getStudents_unauthorizedBranch() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L, 2L));

        CustomException ex = assertThrows(CustomException.class, () ->
                studentService.getStudents(List.of(99L), "", null, null, pageable(), userId)
        );

        assertEquals(ErrorCode.BRANCH_ACCESS_DENIED, ex.getErrorCode());
    }

    // ---------------------------------------------------------
    // TC-GS4 — Invalid sort field → remapped to studentCode
    // ---------------------------------------------------------
    @Test
    void getStudents_sortFieldInvalid_remapToStudentCode() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Pageable badSort = PageRequest.of(0, 10, Sort.by("fullName"));

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        studentService.getStudents(null, "", null, null, badSort, userId);

        // verify remapped sort
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentRepository).findStudentsWithFilters(any(), any(), any(), any(), captor.capture());

        String sorted = captor.getValue().getSort().iterator().next().getProperty();
        assertEquals("studentCode", sorted);
    }

    // ---------------------------------------------------------
    // TC-GS5 — Valid sort field
    // ---------------------------------------------------------
    @Test
    void getStudents_sortFieldValid_noRemap() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Pageable goodSort = PageRequest.of(0, 10, Sort.by("studentCode"));

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        studentService.getStudents(null, "", null, null, goodSort, userId);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(studentRepository).findStudentsWithFilters(any(), any(), any(), any(), captor.capture());

        assertEquals("studentCode", captor.getValue().getSort().iterator().next().getProperty());
    }

    // ---------------------------------------------------------
    // TC-GS6 — Repository returns empty page
    // ---------------------------------------------------------
    @Test
    void getStudents_emptyPage() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<StudentListItemDTO> result =
                studentService.getStudents(null, "", null, null, pageable(), userId);

        assertEquals(0, result.getTotalElements());
    }

    // ---------------------------------------------------------
    // Helper: create Student + UserAccount
    // ---------------------------------------------------------
    private Student mockStudent(long id, String code, String name, String email, Branch branch) {
        UserAccount user = new UserAccount();
        user.setFullName(name);
        user.setEmail(email);
        if (branch != null) {
            UserBranches ub = new UserBranches();
            ub.setBranch(branch);
            user.setUserBranches(Set.of(ub));
        } else {
            user.setUserBranches(new HashSet<>());
        }

        Student s = new Student();
        s.setId(id);
        s.setStudentCode(code);
        s.setUserAccount(user);
        return s;
    }

    // ---------------------------------------------------------
    // TC-GS7 — DTO mapping with full data
    // ---------------------------------------------------------
    @Test
    void getStudents_dtoMapping_fullData() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Branch b = new Branch();
        b.setId(1L);
        b.setName("Hanoi");

        Student st = mockStudent(10L, "ST001", "Alice", "alice@mail.com", b);

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(st)));

        when(enrollmentRepository.countByStudentIdAndStatus(eq(10L), eq(EnrollmentStatus.ENROLLED)))
                .thenReturn(2);

        Enrollment last = new Enrollment();
        last.setEnrolledAt(OffsetDateTime.now());
        when(enrollmentRepository.findLatestEnrollmentByStudent(10L))
                .thenReturn(last);

        when(policyService.getGlobalInt(any(), anyInt())).thenReturn(5);

        Page<StudentListItemDTO> result =
                studentService.getStudents(null, "", null, null, pageable(), userId);

        StudentListItemDTO dto = result.getContent().get(0);
        assertEquals("Alice", dto.getFullName());
        assertEquals("Hanoi", dto.getBranchName());
    }

    // ---------------------------------------------------------
    // TC-GS8 — Student has no branch
    // ---------------------------------------------------------
    @Test
    void getStudents_noBranch() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Student st = mockStudent(10L, "ST001", "Bob", "bob@mail.com", null);

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(st)));

        when(enrollmentRepository.countByStudentIdAndStatus(any(), any()))
                .thenReturn(0);

        when(enrollmentRepository.findLatestEnrollmentByStudent(any()))
                .thenReturn(null);

        when(policyService.getGlobalInt(any(), anyInt())).thenReturn(5);

        Page<StudentListItemDTO> result =
                studentService.getStudents(null, "", null, null, pageable(), userId);

        StudentListItemDTO dto = result.getContent().get(0);
        assertNull(dto.getBranchId());
        assertNull(dto.getBranchName());
    }

    // ---------------------------------------------------------
    // TC-GS9 — Enrollment count = 0
    // ---------------------------------------------------------
    @Test
    void getStudents_enrollmentZero() {
        Long userId = 50L;
        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Branch b = new Branch();
        b.setId(1L);
        b.setName("Campus");

        Student st = mockStudent(10L, "ST001", "Tom", "tom@mail.com", b);

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(st)));

        when(enrollmentRepository.countByStudentIdAndStatus(eq(10L), eq(EnrollmentStatus.ENROLLED)))
                .thenReturn(0);

        when(enrollmentRepository.findLatestEnrollmentByStudent(10L))
                .thenReturn(null);

        when(policyService.getGlobalInt(any(), anyInt())).thenReturn(3);

        StudentListItemDTO dto =
                studentService.getStudents(null, "", null, null, pageable(), userId)
                        .getContent().get(0);

        assertEquals(0L, dto.getActiveEnrollments());
        assertTrue(dto.getCanEnroll());
    }

    // ---------------------------------------------------------
    // TC-GS10 — Enrollment count = max → cannot enroll
    // ---------------------------------------------------------
    @Test
    void getStudents_enrollmentMaxCannotEnroll() {
        Long userId = 50L;

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Branch b = new Branch();
        b.setId(1L);
        b.setName("Campus");

        Student st = mockStudent(10L, "ST001", "Max", "max@mail.com", b);

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(st)));

        when(enrollmentRepository.countByStudentIdAndStatus(eq(10L), eq(EnrollmentStatus.ENROLLED)))
                .thenReturn(3);

        when(policyService.getGlobalInt(any(), anyInt())).thenReturn(3);

        when(enrollmentRepository.findLatestEnrollmentByStudent(10L))
                .thenReturn(null);

        StudentListItemDTO dto =
                studentService.getStudents(null, "", null, null, pageable(), userId)
                        .getContent().get(0);

        assertFalse(dto.getCanEnroll());
    }

    // ---------------------------------------------------------
    // TC-GS11 — Latest enrollment date returned
    // ---------------------------------------------------------
    @Test
    void getStudents_latestEnrollmentDate() {
        Long userId = 50L;

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Branch b = new Branch();
        b.setId(1L);
        b.setName("Campus");

        Student st = mockStudent(10L, "ST001", "Zoe", "zoe@mail.com", b);

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(st)));

        LocalDate date = LocalDate.of(2024, 1, 1);

        Enrollment last = new Enrollment();
        last.setEnrolledAt(OffsetDateTime.of(date.atStartOfDay(), OffsetDateTime.now().getOffset()));

        when(enrollmentRepository.findLatestEnrollmentByStudent(10L))
                .thenReturn(last);

        when(enrollmentRepository.countByStudentIdAndStatus(any(), any()))
                .thenReturn(1);

        when(policyService.getGlobalInt(any(), anyInt())).thenReturn(5);

        StudentListItemDTO dto =
                studentService.getStudents(null, "", null, null, pageable(), userId)
                        .getContent().get(0);

        assertEquals(date, dto.getLastEnrollmentDate());
    }

    // ---------------------------------------------------------
    // TC-GS12 — No enrollment → lastEnrollmentDate = null
    // ---------------------------------------------------------
    @Test
    void getStudents_noEnrollmentDate() {
        Long userId = 50L;

        when(userBranchesRepository.findBranchIdsByUserId(userId))
                .thenReturn(List.of(1L));

        Branch b = new Branch();
        b.setId(1L);
        b.setName("Campus");

        Student st = mockStudent(10L, "ST001", "NoDate", "nodate@mail.com", b);

        when(studentRepository.findStudentsWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(st)));

        when(enrollmentRepository.findLatestEnrollmentByStudent(10L))
                .thenReturn(null);

        when(enrollmentRepository.countByStudentIdAndStatus(any(), any()))
                .thenReturn(0);

        when(policyService.getGlobalInt(any(), anyInt())).thenReturn(5);

        StudentListItemDTO dto =
                studentService.getStudents(null, "", null, null, pageable(), userId)
                        .getContent().get(0);

        assertNull(dto.getLastEnrollmentDate());
    }
}
