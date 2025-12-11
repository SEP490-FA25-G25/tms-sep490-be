package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.teacher.TeacherProfileDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private TeachingSlotRepository teachingSlotRepository;

    @InjectMocks
    private TeacherService teacherService;

    private Teacher teacher;
    private UserAccount user;

    @BeforeEach
    void setup() {

        user = UserAccount.builder()
                .id(10L)
                .email("teacher@mail.com")
                .fullName("John Doe")
                .phone("098888")
                .facebookUrl("fb.com/john")
                .gender(Gender.MALE)
                .dob(LocalDate.of(1990, 1, 1))
                .address("HN")
                .passwordHash("hash")
                .status(UserStatus.ACTIVE)
                .lastLoginAt(OffsetDateTime.now())
                .build();

        // Branch
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Hanoi Campus");

        UserBranches ub = UserBranches.builder()
                .id(new UserBranches.UserBranchesId(10L, 1L))
                .branch(branch)
                .userAccount(user)
                .assignedAt(OffsetDateTime.now())
                .build();

        user.getUserBranches().add(ub);

        // Teacher
        teacher = Teacher.builder()
                .id(5L)
                .userAccount(user)
                .employeeCode("GV001")
                .build();
    }

    // =======================================================================================
    // 1. TEACHER NOT FOUND
    // =======================================================================================
    @Test
    void testTeacherNotFound() {
        when(teacherRepository.findByUserAccountId(10L))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> teacherService.getMyProfile(10L));

        assertEquals(ErrorCode.TEACHER_NOT_FOUND, ex.getErrorCode());
    }

    // =======================================================================================
    // 2. SUCCESS CASE — FULL MAPPING
    // =======================================================================================
    @Test
    void testSuccessMapping() {

        ClassEntity c1 = new ClassEntity();
        c1.setId(100L);
        c1.setCode("C001");
        c1.setName("Math Class");
        c1.setStatus(ClassStatus.ONGOING);
        c1.setStartDate(LocalDate.of(2025, 1, 1));
        c1.setPlannedEndDate(LocalDate.of(2025, 3, 1));
        c1.setCreatedAt(OffsetDateTime.now().minusDays(5));

        Subject s = new Subject();
        s.setName("Math");
        c1.setSubject(s);

        Branch b = new Branch();
        b.setName("Hanoi Campus");
        c1.setBranch(b);

        ClassEntity c2 = new ClassEntity();
        c2.setId(101L);
        c2.setCode("C002");
        c2.setName("English");
        c2.setStatus(ClassStatus.COMPLETED);
        c2.setStartDate(LocalDate.of(2024, 10, 1));
        c2.setPlannedEndDate(LocalDate.of(2024, 12, 1));
        c2.setCreatedAt(OffsetDateTime.now().minusMonths(2));

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(List.of(c1, c2));

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertEquals(2, dto.getTotalClasses());
        assertEquals(1, dto.getActiveClasses());
        assertEquals(1, dto.getCompletedClasses());
        assertEquals(LocalDate.of(2024, 10, 1), dto.getFirstClassDate());

        // Sorting: ongoing first
        assertEquals("C001", dto.getClasses().get(0).getClassCode());
        assertEquals("C002", dto.getClasses().get(1).getClassCode());
    }

    // =======================================================================================
    // 3. NO BRANCH
    // =======================================================================================
    @Test
    void testNoBranch() {
        user.getUserBranches().clear();

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(Collections.emptyList());

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertNull(dto.getBranchId());
        assertNull(dto.getBranchName());
    }

    // =======================================================================================
    // 4. SORTING STATUS (ONGOING → SCHEDULED → COMPLETED)
    // =======================================================================================
    @Test
    void testSortingByStatus() {
        ClassEntity c1 = new ClassEntity();
        c1.setId(1L);
        c1.setStatus(ClassStatus.SCHEDULED);
        c1.setStartDate(LocalDate.of(2025, 2, 1));
        c1.setCreatedAt(OffsetDateTime.now());

        ClassEntity c2 = new ClassEntity();
        c2.setId(2L);
        c2.setStatus(ClassStatus.ONGOING);
        c2.setStartDate(LocalDate.of(2025, 3, 1));
        c2.setCreatedAt(OffsetDateTime.now());

        ClassEntity c3 = new ClassEntity();
        c3.setId(3L);
        c3.setStatus(ClassStatus.COMPLETED);
        c3.setStartDate(LocalDate.of(2024, 1, 1));
        c3.setCreatedAt(OffsetDateTime.now());

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(List.of(c1, c2, c3));

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        List<Long> sorted = dto.getClasses().stream().map(x -> x.getClassId()).toList();
        assertEquals(List.of(2L, 1L, 3L), sorted);
    }

    // =======================================================================================
    // 5. createdAt null → assignedAt fallback
    // =======================================================================================
    @Test
    void testAssignedAtFallback() {
        ClassEntity c = new ClassEntity();
        c.setId(1L);
        c.setStatus(ClassStatus.ONGOING);
        c.setStartDate(LocalDate.now());
        c.setCreatedAt(null);

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(List.of(c));

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertNotNull(dto.getClasses().get(0).getAssignedAt());
    }

    // =======================================================================================
    // 6. subject null + branch null
    // =======================================================================================
    @Test
    void testSubjectBranchNull() {

        ClassEntity c = new ClassEntity();
        c.setId(1L);
        c.setStatus(ClassStatus.ONGOING);
        c.setStartDate(LocalDate.now());
        c.setCreatedAt(OffsetDateTime.now());
        c.setSubject(null);
        c.setBranch(null);

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(List.of(c));

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertNull(dto.getClasses().get(0).getSubjectName());
        assertNull(dto.getClasses().get(0).getBranchName());
    }

    // =======================================================================================
    // 7. firstClassDate null (all startDate null)
    // =======================================================================================
    @Test
    void testFirstClassDateNull() {
        ClassEntity c = new ClassEntity();
        c.setId(1L);
        c.setStatus(ClassStatus.ONGOING);
        c.setStartDate(null);
        c.setCreatedAt(OffsetDateTime.now());

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(List.of(c));

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertNull(dto.getFirstClassDate());
    }

    // =======================================================================================
    // 8. gender null mapping
    // =======================================================================================
    @Test
    void testGenderNull() {
        user.setGender(null);

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(Collections.emptyList());

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertNull(dto.getGender());
    }

    // =======================================================================================
    // 9. facebookUrl null mapping
    // =======================================================================================
    @Test
    void testFacebookNull() {
        user.setFacebookUrl(null);

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(Collections.emptyList());

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertNull(dto.getFacebookUrl());
    }

    // =======================================================================================
    // 10. Status COMPLETED branch mapping (for coverage)
    // =======================================================================================
    @Test
    void testCompletedStatusMapping() {

        ClassEntity c = new ClassEntity();
        c.setId(1L);
        c.setStatus(ClassStatus.COMPLETED);
        c.setStartDate(LocalDate.of(2024, 1, 1));
        c.setCreatedAt(OffsetDateTime.now());

        when(teacherRepository.findByUserAccountId(10L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findDistinctClassesByTeacherId(5L))
                .thenReturn(List.of(c));

        TeacherProfileDTO dto = teacherService.getMyProfile(10L);

        assertEquals(1, dto.getCompletedClasses());
        assertEquals("COMPLETED", dto.getClasses().get(0).getStatus());
    }
}
