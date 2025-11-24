package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
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

    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    @MockitoBean
    private TimeSlotTemplateRepository timeSlotTemplateRepository;

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

    @Test
    @DisplayName("Should get time slots from single branch enrollment")
    void shouldGetTimeSlotsFromSingleBranchEnrollment() {
        // Arrange
        Branch branch = TestDataBuilder.buildBranch()
                .code("BRANCH01")
                .name("Test Branch")
                .build();
        branch.setId(1L);

        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .code("CLASS001")
                .name("Test Class")
                .branch(branch)
                .build();
        classEntity.setId(1L);

        Enrollment enrollment = Enrollment.builder()
                .id(1L)
                .studentId(100L)
                .classId(1L)
                .classEntity(classEntity)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        TimeSlotTemplate timeSlot1 = TimeSlotTemplate.builder()
                .id(1L)
                .branch(branch)
                .name("Morning 1")
                .startTime(java.time.LocalTime.of(8, 0))
                .endTime(java.time.LocalTime.of(10, 0))
                .build();

        TimeSlotTemplate timeSlot2 = TimeSlotTemplate.builder()
                .id(2L)
                .branch(branch)
                .name("Morning 2")
                .startTime(java.time.LocalTime.of(10, 0))
                .endTime(java.time.LocalTime.of(12, 0))
                .build();

        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(enrollmentRepository.findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment));
        when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(timeSlot1, timeSlot2));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(new ArrayList<>());

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result.getTimeSlots()).hasSize(2);
        assertThat(result.getTimeSlots().get(0).getName()).isEqualTo("Morning 1");
        assertThat(result.getTimeSlots().get(1).getName()).isEqualTo("Morning 2");

        verify(enrollmentRepository).findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED);
        verify(timeSlotTemplateRepository).findByBranchIdOrderByStartTimeAsc(1L);
    }

    @Test
    @DisplayName("Should union time slots from multiple branch enrollments")
    void shouldUnionTimeSlotsFromMultipleBranches() {
        // Arrange
        Branch branch1 = TestDataBuilder.buildBranch()
                .code("BRANCH01")
                .name("HN Branch")
                .build();
        branch1.setId(1L);

        Branch branch2 = TestDataBuilder.buildBranch()
                .code("BRANCH02")
                .name("SG Branch")
                .build();
        branch2.setId(2L);

        ClassEntity class1 = TestDataBuilder.buildClassEntity()
                .code("CLASS001")
                .name("Test Class 1")
                .branch(branch1)
                .build();
        class1.setId(1L);

        ClassEntity class2 = TestDataBuilder.buildClassEntity()
                .code("CLASS002")
                .name("Test Class 2")
                .branch(branch2)
                .build();
        class2.setId(2L);

        Enrollment enrollment1 = Enrollment.builder()
                .id(1L)
                .studentId(100L)
                .classId(1L)
                .classEntity(class1)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .id(2L)
                .studentId(100L)
                .classId(2L)
                .classEntity(class2)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        // Branch 1 time slots
        TimeSlotTemplate hnMorning = TimeSlotTemplate.builder()
                .id(1L)
                .branch(branch1)
                .name("HN Morning 1")
                .startTime(java.time.LocalTime.of(8, 0))
                .endTime(java.time.LocalTime.of(10, 0))
                .build();

        TimeSlotTemplate hnAfternoon = TimeSlotTemplate.builder()
                .id(2L)
                .branch(branch1)
                .name("HN Afternoon 1")
                .startTime(java.time.LocalTime.of(14, 0))
                .endTime(java.time.LocalTime.of(16, 0))
                .build();

        // Branch 2 time slots (including one with same time range as branch 1)
        TimeSlotTemplate sgMorning = TimeSlotTemplate.builder()
                .id(3L)
                .branch(branch2)
                .name("SG Morning 1")
                .startTime(java.time.LocalTime.of(8, 0))
                .endTime(java.time.LocalTime.of(10, 0))
                .build();

        TimeSlotTemplate sgEvening = TimeSlotTemplate.builder()
                .id(4L)
                .branch(branch2)
                .name("SG Evening 1")
                .startTime(java.time.LocalTime.of(18, 0))
                .endTime(java.time.LocalTime.of(20, 0))
                .build();

        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(enrollmentRepository.findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment1, enrollment2));
        when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(hnMorning, hnAfternoon));
        when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(2L))
                .thenReturn(List.of(sgMorning, sgEvening));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(new ArrayList<>());

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result.getTimeSlots()).hasSize(3); // 4 time slots but 2 with same time range merged into 1

        // Check that time slots are sorted by start time
        assertThat(result.getTimeSlots().get(0).getStartTime()).isEqualTo(java.time.LocalTime.of(8, 0));
        assertThat(result.getTimeSlots().get(1).getStartTime()).isEqualTo(java.time.LocalTime.of(14, 0));
        assertThat(result.getTimeSlots().get(2).getStartTime()).isEqualTo(java.time.LocalTime.of(18, 0));

        // Check that same time range is merged (8:00-10:00 from both branches)
        assertThat(result.getTimeSlots().get(0).getName()).contains("Morning 1");

        verify(enrollmentRepository).findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED);
        verify(timeSlotTemplateRepository).findByBranchIdOrderByStartTimeAsc(1L);
        verify(timeSlotTemplateRepository).findByBranchIdOrderByStartTimeAsc(2L);
    }

    @Test
    @DisplayName("Should merge duplicate time slots with same time range")
    void shouldMergeDuplicateTimeSlotsWithSameTimeRange() {
        // Arrange
        Branch branch1 = TestDataBuilder.buildBranch()
                .code("BRANCH01")
                .name("HN Branch")
                .build();
        branch1.setId(1L);

        Branch branch2 = TestDataBuilder.buildBranch()
                .code("BRANCH02")
                .name("SG Branch")
                .build();
        branch2.setId(2L);

        ClassEntity class1 = TestDataBuilder.buildClassEntity()
                .code("CLASS001")
                .name("Test Class 1")
                .branch(branch1)
                .build();
        class1.setId(1L);

        ClassEntity class2 = TestDataBuilder.buildClassEntity()
                .code("CLASS002")
                .name("Test Class 2")
                .branch(branch2)
                .build();
        class2.setId(2L);

        Enrollment enrollment1 = Enrollment.builder()
                .id(1L)
                .studentId(100L)
                .classId(1L)
                .classEntity(class1)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        Enrollment enrollment2 = Enrollment.builder()
                .id(2L)
                .studentId(100L)
                .classId(2L)
                .classEntity(class2)
                .status(EnrollmentStatus.ENROLLED)
                .build();

        // Both branches have same time range but different names
        TimeSlotTemplate hnMorning = TimeSlotTemplate.builder()
                .id(1L)
                .branch(branch1)
                .name("HN Morning 1")
                .startTime(java.time.LocalTime.of(8, 0))
                .endTime(java.time.LocalTime.of(10, 0))
                .build();

        TimeSlotTemplate sgMorning = TimeSlotTemplate.builder()
                .id(2L)
                .branch(branch2)
                .name("SG Morning 1")
                .startTime(java.time.LocalTime.of(8, 0))
                .endTime(java.time.LocalTime.of(10, 0))
                .build();

        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(enrollmentRepository.findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED))
                .thenReturn(List.of(enrollment1, enrollment2));
        when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(1L))
                .thenReturn(List.of(hnMorning));
        when(timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(2L))
                .thenReturn(List.of(sgMorning));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(new ArrayList<>());

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result.getTimeSlots()).hasSize(1); // Merged into 1
        assertThat(result.getTimeSlots().get(0).getStartTime()).isEqualTo(java.time.LocalTime.of(8, 0));
        assertThat(result.getTimeSlots().get(0).getEndTime()).isEqualTo(java.time.LocalTime.of(10, 0));
        // Name should contain both branch names merged
        assertThat(result.getTimeSlots().get(0).getName()).containsAnyOf("HN Morning 1", "SG Morning 1");

        verify(enrollmentRepository).findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED);
        verify(timeSlotTemplateRepository).findByBranchIdOrderByStartTimeAsc(1L);
        verify(timeSlotTemplateRepository).findByBranchIdOrderByStartTimeAsc(2L);
    }

    @Test
    @DisplayName("Should throw exception when student has no active enrollments")
    void shouldThrowExceptionWhenNoActiveEnrollments() {
        // Arrange
        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(enrollmentRepository.findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED))
                .thenReturn(new ArrayList<>());

        // Act & Assert
        assertThatThrownBy(() -> studentScheduleService.getWeeklySchedule(100L, testWeekStart))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.STUDENT_NOT_ENROLLED_IN_CLASS.getMessage());

        verify(studentRepository).findById(100L);
        verify(enrollmentRepository).findByStudentIdAndStatus(100L, EnrollmentStatus.ENROLLED);
        verify(timeSlotTemplateRepository, never()).findByBranchIdOrderByStartTimeAsc(any());
    }
}