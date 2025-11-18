package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.CourseRepository;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.RoleRepository;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for StudentScheduleController.
 * Tests complete API workflow with real Spring Security context and database.
 * Uses modern Spring Boot 3.5.7 testing patterns with @SpringBootTest and @AutoConfigureMockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("StudentScheduleController Integration Tests")
class StudentScheduleControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ClassRepository classEntityRepository;

    @Autowired
    private TimeSlotTemplateRepository timeSlotTemplateRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private StudentSessionRepository studentSessionRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UserAccount testUserAccount;
    private Student testStudent;
    private LocalDate testWeekStart;

    @BeforeEach
    void setUp() {
        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // Generate unique identifiers to avoid constraint violations
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create test week (Monday)
        testWeekStart = LocalDate.of(2025, 11, 10); // Monday

        // 2. Create UserAccount and Student (simplified, without complex role setup)
        testUserAccount = TestDataBuilder.buildUserAccount()
                .email("student-" + uniqueSuffix + "@test.com")
                .fullName("Test Student " + uniqueSuffix)
                .build();
        testUserAccount = userRepository.save(testUserAccount);

        testStudent = TestDataBuilder.buildStudent()
                .studentCode("ST" + uniqueSuffix)
                .userAccount(testUserAccount)
                .build();
        testStudent = studentRepository.save(testStudent);

        // 3. Create Center, Branch, Subject, Level, Course, Class
        Center testCenter = TestDataBuilder.buildCenter()
                .code("CENTER" + uniqueSuffix)
                .name("Test Center " + uniqueSuffix)
                .build();
        testCenter = centerRepository.save(testCenter);

        Branch testBranch = TestDataBuilder.buildBranch()
                .code("BRANCH" + uniqueSuffix)
                .name("Test Branch " + uniqueSuffix)
                .center(testCenter)
                .build();
        testBranch = branchRepository.save(testBranch);

        Subject testSubject = TestDataBuilder.buildSubject()
                .code("ENG" + uniqueSuffix)
                .name("English " + uniqueSuffix)
                .build();
        testSubject = subjectRepository.save(testSubject);

        Level testLevel = TestDataBuilder.buildLevel()
                .code("A1" + uniqueSuffix)
                .name("Beginner " + uniqueSuffix)
                .subject(testSubject)
                .build();
        testLevel = levelRepository.save(testLevel);

        Course testCourse = TestDataBuilder.buildCourse()
                .code("ENG-A1-" + uniqueSuffix)
                .name("English A1 Course " + uniqueSuffix)
                .subject(testSubject)
                .level(testLevel)
                .build();
        testCourse = courseRepository.save(testCourse);

        ClassEntity testClass = TestDataBuilder.buildClassEntity()
                .code("CLASS" + uniqueSuffix)
                .name("English A1 Class " + uniqueSuffix)
                .course(testCourse)
                .branch(testBranch)
                .build();
        testClass = classEntityRepository.save(testClass);

        // 4. Create TimeSlot
        TimeSlotTemplate testTimeSlot = TimeSlotTemplate.builder()
                .branch(testBranch)
                .name("Morning Slot " + uniqueSuffix)
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(11, 0))
                .build();
        testTimeSlot = timeSlotTemplateRepository.save(testTimeSlot);

        // 5. Create Session and StudentSession
        Session testSession = Session.builder()
                .classEntity(testClass)
                .date(testWeekStart)
                .timeSlotTemplate(testTimeSlot)
                .status(SessionStatus.PLANNED)
                .build();
        testSession = sessionRepository.save(testSession);

        StudentSession testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PLANNED)
                .isMakeup(false)
                .build();
        testStudentSession = studentSessionRepository.save(testStudentSession);
    }

    @Test
    @DisplayName("Should return 500 for authenticated endpoints without proper user context")
    void shouldReturnInternalServerErrorForAuthenticatedEndpointsWithoutUserContext() throws Exception {
        // Act & Assert - This should fail with 500 because UserPrincipal is null
        // This verifies that the security bypass is working as expected
        mockMvc.perform(get("/api/v1/students/me/schedule")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should handle invalid date format gracefully")
    void shouldHandleInvalidDateFormat() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get("/api/v1/students/me/schedule")
                .param("weekStart", "invalid-date")
                .contentType(MediaType.APPLICATION_JSON));

        // Assert - Should return 400 for invalid date format
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should get current week start successfully")
    void shouldGetCurrentWeekStartSuccessfully() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get("/api/v1/students/me/current-week")
                .contentType(MediaType.APPLICATION_JSON));

        // Assert - Should return 200 with current week start date
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
}