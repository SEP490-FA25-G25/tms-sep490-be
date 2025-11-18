package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.schedule.WeeklyScheduleResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.StudentScheduleService;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for weekly schedule location improvements.
 * Verifies that specific room names are shown instead of generic branch names.
 * Uses unit tests to avoid database connection issues.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentScheduleService Weekly Location Tests")
class StudentScheduleServiceWeeklyLocationTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentSessionRepository studentSessionRepository;

    @InjectMocks
    private StudentScheduleServiceImpl studentScheduleService;

    private Student testStudent;
    private List<StudentSession> testStudentSessions;
    private LocalDate testWeekStart;
    private LocalDate testWeekEnd;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // 1. Create test week (Monday to Sunday)
        testWeekStart = LocalDate.of(2025, 11, 10); // Monday
        testWeekEnd = testWeekStart.plusDays(6); // Sunday

        // 2. Create UserAccount and Student
        UserAccount testUserAccount = TestDataBuilder.buildUserAccount()
                .id(1L)
                .fullName("Test Student")
                .email("test.student@example.com")
                .build();

        testStudent = TestDataBuilder.buildStudent()
                .studentCode("ST001")
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
                .modality(org.fyp.tmssep490be.entities.enums.Modality.OFFLINE) // Set to OFFLINE for branch name fallback
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

        // 5. Create Resources
        Resource roomResource = Resource.builder()
                .id(1L)
                .branch(testBranch)
                .resourceType(ResourceType.ROOM)
                .code("HN01-R101")
                .name("Ha Noi Room 101")
                .description("Spacious classroom")
                .capacity(20)
                .build();

        Resource virtualResource = Resource.builder()
                .id(2L)
                .branch(testBranch)
                .resourceType(ResourceType.VIRTUAL)
                .code("ZOOM-01")
                .name("https://zoom.us/j/123456789")
                .description("Main Zoom room")
                .meetingUrl("https://zoom.us/j/123456789")
                .build();

        // 6. Create Sessions and StudentSessions with different resources
        testStudentSessions = new ArrayList<>();

        // Session 1: Monday with Room resource
        Session session1 = TestDataBuilder.buildSession()
                .classEntity(testClass)
                .date(testWeekStart) // Monday
                .status(SessionStatus.PLANNED)
                .build();
        session1.setId(1001L);
        session1.setTimeSlotTemplate(testTimeSlot);

        // Add room resource to session 1
        SessionResource roomSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(1001L, 1L))
                .session(session1)
                .resource(roomResource)
                .build();
        session1.setSessionResources(new HashSet<>(Arrays.asList(roomSessionResource)));

        StudentSession studentSession1 = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), session1.getId()))
                .student(testStudent)
                .session(session1)
                .attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                .isMakeup(false)
                .build();

        // Session 2: Wednesday with Virtual resource
        Session session2 = TestDataBuilder.buildSession()
                .classEntity(testClass)
                .date(testWeekStart.plusDays(2)) // Wednesday
                .status(SessionStatus.PLANNED)
                .build();
        session2.setId(1002L);
        session2.setTimeSlotTemplate(testTimeSlot);

        // Add virtual resource to session 2
        SessionResource virtualSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(1002L, 2L))
                .session(session2)
                .resource(virtualResource)
                .build();
        session2.setSessionResources(new HashSet<>(Arrays.asList(virtualSessionResource)));

        StudentSession studentSession2 = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), session2.getId()))
                .student(testStudent)
                .session(session2)
                .attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                .isMakeup(false)
                .build();

        // Session 3: Friday with no resources (should fallback to branch name)
        Session session3 = TestDataBuilder.buildSession()
                .classEntity(testClass)
                .date(testWeekStart.plusDays(4)) // Friday
                .status(SessionStatus.PLANNED)
                .build();
        session3.setId(1003L);
        session3.setTimeSlotTemplate(testTimeSlot);
        session3.setSessionResources(new HashSet<>()); // No resources

        StudentSession studentSession3 = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), session3.getId()))
                .student(testStudent)
                .session(session3)
                .attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                .isMakeup(false)
                .build();

        testStudentSessions.add(studentSession1);
        testStudentSessions.add(studentSession2);
        testStudentSessions.add(studentSession3);
    }

    @Test
    @DisplayName("Should show specific room names in weekly schedule location")
    void shouldShowSpecificRoomNamesInWeeklySchedule() {
        // Arrange
        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(testStudentSessions);

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSchedule()).hasSize(7); // All 7 days of week

        // Check Monday - should show room name
        List<org.fyp.tmssep490be.dtos.schedule.SessionSummaryDTO> mondaySessions = result.getSchedule().get(DayOfWeek.MONDAY);
        assertThat(mondaySessions).hasSize(1);
        assertThat(mondaySessions.get(0).getLocation()).isEqualTo("Ha Noi Room 101"); // Specific room name

        // Check Wednesday - should show "Online" for virtual resources
        List<org.fyp.tmssep490be.dtos.schedule.SessionSummaryDTO> wednesdaySessions = result.getSchedule().get(DayOfWeek.WEDNESDAY);
        assertThat(wednesdaySessions).hasSize(1);
        assertThat(wednesdaySessions.get(0).getLocation()).isEqualTo("Online"); // Virtual resource

        // Check Friday - should fallback to branch name when no resources
        List<org.fyp.tmssep490be.dtos.schedule.SessionSummaryDTO> fridaySessions = result.getSchedule().get(DayOfWeek.FRIDAY);
        assertThat(fridaySessions).hasSize(1);
        assertThat(fridaySessions.get(0).getLocation()).isEqualTo("Test Branch"); // Fallback to branch name

        // Check other days should be empty
        assertThat(result.getSchedule().get(DayOfWeek.TUESDAY)).isEmpty();
        assertThat(result.getSchedule().get(DayOfWeek.THURSDAY)).isEmpty();
        assertThat(result.getSchedule().get(DayOfWeek.SATURDAY)).isEmpty();
        assertThat(result.getSchedule().get(DayOfWeek.SUNDAY)).isEmpty();

        verify(studentRepository).findById(100L);
        verify(studentSessionRepository).findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd);
    }

    @Test
    @DisplayName("Should handle empty weekly schedule with no resource conflicts")
    void shouldHandleEmptyWeeklyScheduleWithNoResourceConflicts() {
        // Arrange
        when(studentRepository.findById(100L)).thenReturn(Optional.of(testStudent));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd))
                .thenReturn(new ArrayList<>());

        // Act
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(100L, testWeekStart);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTimeSlots()).isEmpty();
        assertThat(result.getSchedule()).hasSize(7); // All 7 days exist but are empty

        verify(studentRepository).findById(100L);
        verify(studentSessionRepository).findWeeklyScheduleByStudentId(100L, testWeekStart, testWeekEnd);
    }
}