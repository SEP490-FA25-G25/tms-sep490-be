package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Service layer tests for StudentScheduleService.
 * Uses modern Spring Boot 3.5.7 @SpringBootTest with @MockitoBean pattern.
 * Tests business logic in Spring context with proper dependency injection.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StudentScheduleService Unit Tests")
class StudentScheduleServiceImplTest {

    @Autowired
    private StudentScheduleService studentScheduleService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    private Student testStudent;
    private List<StudentSession> testStudentSessions;
    private LocalDate testWeekStart;
    private LocalDate testWeekEnd;

    @BeforeEach
    void setUp() {
        // Create test data
        setupTestData();
    }

    private void setupTestData() {
        // 1. Create test week (Monday to Sunday)
        testWeekStart = LocalDate.of(2025, 11, 10); // Monday
        testWeekEnd = testWeekStart.plusDays(6); // Sunday

        // 2. Create UserAccount and Student
        UserAccount testUserAccount = TestDataBuilder.buildUserAccount()
                .id(1L)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .build();

        testStudent = TestDataBuilder.buildStudent()
                .studentCode("ST100")
                .userAccount(testUserAccount)
                .build();
        testStudent.setId(100L);

        // 3. Create Center, Branch, Course, Class
        Center testCenter = TestDataBuilder.buildCenter()
                .code("CENTER01")
                .name("Test Center")
                .build();
        testCenter.setId(1L);

        Branch testBranch = TestDataBuilder.buildBranch()
                .code("BRANCH01")
                .name("Test Branch")
                .center(testCenter)
                .build();
        testBranch.setId(1L);

        Subject testSubject = TestDataBuilder.buildSubject()
                .code("ENG")
                .name("English")
                .build();
        testSubject.setId(1L);

        Level testLevel = TestDataBuilder.buildLevel()
                .code("A1")
                .name("Beginner")
                .subject(testSubject)
                .build();
        testLevel.setId(1L);

        Course testCourse = TestDataBuilder.buildCourse()
                .code("ENG-A1-2024")
                .name("English A1 Course")
                .subject(testSubject)
                .level(testLevel)
                .build();
        testCourse.setId(1L);

        ClassEntity testClass = TestDataBuilder.buildClassEntity()
                .code("CLASS001")
                .name("English A1 Class")
                .course(testCourse)
                .branch(testBranch)
                .build();
        testClass.setId(1L);

        // 4. Create TimeSlotTemplate
        TimeSlotTemplate testTimeSlot = TimeSlotTemplate.builder()
                .id(1L)
                .branch(testBranch)
                .name("Morning Slot")
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(11, 0))
                .build();

        // 5. Create Sessions and StudentSessions
        testStudentSessions = new ArrayList<>();

        // Session 1: Monday
        Session session1 = TestDataBuilder.buildSession()
                .classEntity(testClass)
                .date(testWeekStart) // Monday
                .status(SessionStatus.PLANNED)
                .build();
        session1.setId(1001L);
        session1.setTimeSlotTemplate(testTimeSlot);

        StudentSession studentSession1 = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), session1.getId()))
                .student(testStudent)
                .session(session1)
                .attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                .isMakeup(false)
                .build();

        testStudentSessions.add(studentSession1);
    }

    @Test
    @DisplayName("Should get weekly schedule successfully")
    void shouldGetWeeklyScheduleSuccessfully() {
        // Arrange
        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(testStudentSessions);

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(100L);
        assertThat(result.getStudentName()).isEqualTo("John Doe");
        assertThat(result.getWeekStart()).isEqualTo(testWeekStart);
        assertThat(result.getWeekEnd()).isEqualTo(testWeekEnd);

        // Check schedule by day
        assertThat(result.getSchedule()).hasSize(7); // All 7 days of week
        assertThat(result.getSchedule().get(DayOfWeek.MONDAY)).hasSize(1);
        assertThat(result.getSchedule().get(DayOfWeek.TUESDAY)).isEmpty();

        verify(studentRepository).findById(100L);
        verify(studentSessionRepository).findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd);
    }

    @Test
    @DisplayName("Should throw exception when weekStart is not Monday")
    void shouldThrowExceptionWhenWeekStartIsNotMonday() {
        // Arrange
        LocalDate invalidWeekStart = testWeekStart.plusDays(1); // Tuesday

        // Act & Assert
        assertThatThrownBy(() -> studentScheduleService.getWeeklySchedule(100L, invalidWeekStart))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_REQUEST.getMessage());

        verify(studentRepository, never()).findById(any());
        verify(studentSessionRepository, never()).findWeeklyScheduleByStudentId(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowExceptionWhenStudentNotFound() {
        // Arrange
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentScheduleService.getWeeklySchedule(999L, testWeekStart))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.STUDENT_NOT_FOUND.getMessage());

        verify(studentRepository).findById(999L);
        verify(studentSessionRepository, never()).findWeeklyScheduleByStudentId(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle empty weekly schedule")
    void shouldHandleEmptyWeeklySchedule() {
        // Arrange
        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(new ArrayList<>());

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStudentId()).isEqualTo(100L);
        assertThat(result.getTimeSlots()).isEmpty();
        assertThat(result.getSchedule()).hasSize(7); // All 7 days exist but are empty
        assertThat(result.getSchedule().get(DayOfWeek.MONDAY)).isEmpty();
        assertThat(result.getSchedule().get(DayOfWeek.TUESDAY)).isEmpty();

        verify(studentRepository).findById(100L);
        verify(studentSessionRepository).findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd);
    }

    @Test
    @DisplayName("Should get current week start successfully")
    void shouldGetCurrentWeekStartSuccessfully() {
        // Act
        LocalDate result = studentScheduleService.getCurrentWeekStart();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }
}