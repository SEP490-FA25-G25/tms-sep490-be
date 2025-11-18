package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.schedule.SessionDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
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

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for StudentScheduleService student status mapping fix.
 *
 * CRITICAL BUG FIX VERIFICATION:
 * - Student attendanceStatus should come from StudentSession.attendanceStatus (NOT CourseSession)
 * - Student homeworkStatus should come from StudentSession.homeworkStatus (NOT CourseSession)
 * - Student homeworkDescription should come from StudentSession.note (NOT CourseSession.studentTask)
 *
 * BEFORE FIX: homeworkDescription was incorrectly mapped from courseSession.getStudentTask()
 * AFTER FIX: homeworkDescription is correctly mapped from studentSession.getNote()
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StudentScheduleService Student Status Mapping Tests")
class StudentScheduleServiceStudentStatusMappingTest {

    @Autowired
    private StudentScheduleService studentScheduleService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    private Student testStudent;
    private StudentSession testStudentSession;
    private Session testSession;
    private CourseSession testCourseSession;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // 1. Create UserAccount and Student
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

        // 2. Create Center, Branch, Course, Class
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

        // 3. Create TimeSlotTemplate
        TimeSlotTemplate testTimeSlot = TimeSlotTemplate.builder()
                .id(1L)
                .branch(testBranch)
                .name("Morning Slot")
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(11, 0))
                .build();

        // 4. Create CourseSession with curriculum content (should NOT appear in student status)
        testCourseSession = CourseSession.builder()
                .id(1L)
                .topic("IELTS Introduction")
                .studentTask("Complete the introduction exercise - This is CURRICULUM content")
                .build();

        // 5. Create Session
        testSession = TestDataBuilder.buildSession()
                .classEntity(testClass)
                .date(java.time.LocalDate.of(2024, 1, 15))
                .status(SessionStatus.PLANNED)
                .build();
        testSession.setId(1001L);
        testSession.setCourseSession(testCourseSession);
        testSession.setTimeSlotTemplate(testTimeSlot);
    }

    @Test
    @DisplayName("Should map student status fields from StudentSession entity - NOT from CourseSession")
    void shouldMapStudentStatusFromStudentSessionNotCourseSession() {
        // Arrange - Set StudentSession-specific data
        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PLANNED)
                .homeworkStatus(HomeworkStatus.INCOMPLETE)
                .note("Student was absent for previous session - this is STUDENT-SPECIFIC note")
                .isMakeup(false)
                .build();

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert - StudentStatus should contain StudentSession data, NOT CourseSession data
        assertThat(result.getStudentStatus()).isNotNull();

        // CRITICAL VERIFICATION 1: attendanceStatus from StudentSession
        assertThat(result.getStudentStatus().getAttendanceStatus())
                .isEqualTo(AttendanceStatus.PLANNED);

        // CRITICAL VERIFICATION 2: homeworkStatus from StudentSession
        assertThat(result.getStudentStatus().getHomeworkStatus())
                .isEqualTo(HomeworkStatus.INCOMPLETE);

        // CRITICAL VERIFICATION 3: homeworkDescription from StudentSession.note (NOT from CourseSession.studentTask)
        assertThat(result.getStudentStatus().getHomeworkDescription())
                .isEqualTo("Student was absent for previous session - this is STUDENT-SPECIFIC note");

        // CRITICAL VERIFICATION 4: Ensure CourseSession curriculum data does NOT leak into student status
        assertThat(result.getStudentStatus().getHomeworkDescription())
                .isNotEqualTo("Complete the introduction exercise - This is CURRICULUM content");

        // Additional verification: SessionInfo should still contain course curriculum data (this is correct)
        assertThat(result.getSessionInfo().getDescription())
                .isEqualTo("Complete the introduction exercise - This is CURRICULUM content");

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle different attendance and homework status combinations")
    void shouldHandleDifferentStatusCombinations() {
        // Test Case 1: PRESENT + COMPLETED
        testStudentSession = createStudentSessionWithStatus(
                AttendanceStatus.PRESENT,
                HomeworkStatus.COMPLETED,
                "Student completed all homework on time"
        );

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        assertThat(result.getStudentStatus().getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getStudentStatus().getHomeworkStatus()).isEqualTo(HomeworkStatus.COMPLETED);
        assertThat(result.getStudentStatus().getHomeworkDescription()).isEqualTo("Student completed all homework on time");

        // Test Case 2: ABSENT + INCOMPLETE
        testStudentSession = createStudentSessionWithStatus(
                AttendanceStatus.ABSENT,
                HomeworkStatus.INCOMPLETE,
                "Student was absent and needs to catch up on missed work"
        );

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        result = studentScheduleService.getSessionDetail(100L, 1001L);

        assertThat(result.getStudentStatus().getAttendanceStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.getStudentStatus().getHomeworkStatus()).isEqualTo(HomeworkStatus.INCOMPLETE);
        assertThat(result.getStudentStatus().getHomeworkDescription()).isEqualTo("Student was absent and needs to catch up on missed work");

        // Test Case 3: PRESENT + NO_HOMEWORK
        testStudentSession = createStudentSessionWithStatus(
                AttendanceStatus.PRESENT,
                HomeworkStatus.NO_HOMEWORK,
                "No homework assigned for this session"
        );

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        result = studentScheduleService.getSessionDetail(100L, 1001L);

        assertThat(result.getStudentStatus().getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getStudentStatus().getHomeworkStatus()).isEqualTo(HomeworkStatus.NO_HOMEWORK);
        assertThat(result.getStudentStatus().getHomeworkDescription()).isEqualTo("No homework assigned for this session");

        verify(studentSessionRepository, times(3)).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle null values in student status fields")
    void shouldHandleNullValuesInStudentStatusFields() {
        // Arrange - StudentSession with null homeworkStatus and note
        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PLANNED)
                .homeworkStatus(null) // Null homework status
                .note(null) // Null note
                .isMakeup(false)
                .build();

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result.getStudentStatus()).isNotNull();
        assertThat(result.getStudentStatus().getAttendanceStatus()).isEqualTo(AttendanceStatus.PLANNED);
        assertThat(result.getStudentStatus().getHomeworkStatus()).isNull(); // Should handle null gracefully
        assertThat(result.getStudentStatus().getHomeworkDescription()).isNull(); // Should handle null gracefully
        assertThat(result.getStudentStatus().getHomeworkDueDate()).isNull(); // Always null in current schema

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should ensure StudentSession data is isolated from CourseSession curriculum data")
    void shouldEnsureStudentSessionDataIsolationFromCourseSession() {
        // Arrange - Create CourseSession with curriculum content
        testCourseSession.setStudentTask("CURRICULUM: Complete Chapter 1 exercises and read pages 10-25");

        // Create StudentSession with different student-specific data
        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PRESENT)
                .homeworkStatus(HomeworkStatus.INCOMPLETE)
                .note("STUDENT NOTE: Student struggled with pronunciation practice")
                .isMakeup(false)
                .build();

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert - Student status should contain ONLY student-specific data
        assertThat(result.getStudentStatus().getHomeworkDescription())
                .isEqualTo("STUDENT NOTE: Student struggled with pronunciation practice");

        // CRITICAL: Student status should NOT contain curriculum data
        assertThat(result.getStudentStatus().getHomeworkDescription())
                .doesNotContain("CURRICULUM:")
                .doesNotContain("Complete Chapter 1 exercises")
                .doesNotContain("pages 10-25");

        // Assert - Session info should contain curriculum data (this is correct separation)
        assertThat(result.getSessionInfo().getDescription())
                .isEqualTo("CURRICULUM: Complete Chapter 1 exercises and read pages 10-25");

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle long student notes and special characters")
    void shouldHandleLongStudentNotesAndSpecialCharacters() {
        // Arrange - Create StudentSession with long note containing special characters
        String longNoteWithSpecialChars = "Student showed excellent progress! 📚 " +
                "Homework: Review Unit 1 vocabulary (15 words). " +
                "Practice pronunciation of 'th' sound. " +
                "Next week: Quiz on present perfect tense. " +
                "Parent feedback: Very happy with improvement. " +
                "Special needs: Extra time for reading exercises.";

        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PRESENT)
                .homeworkStatus(HomeworkStatus.COMPLETED)
                .note(longNoteWithSpecialChars)
                .isMakeup(false)
                .build();

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result.getStudentStatus().getHomeworkDescription())
                .isEqualTo(longNoteWithSpecialChars);
        assertThat(result.getStudentStatus().getHomeworkDescription()).contains("📚"); // Emoji support
        assertThat(result.getStudentStatus().getHomeworkDescription()).contains("Parent feedback:");
        assertThat(result.getStudentStatus().getHomeworkDescription()).contains("Special needs:");

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should verify homeworkDueDate is always null (not implemented in current schema)")
    void shouldVerifyHomeworkDueDateIsAlwaysNull() {
        // Arrange - Test all status combinations
        HomeworkStatus[] homeworkStatuses = {
                HomeworkStatus.COMPLETED,
                HomeworkStatus.INCOMPLETE,
                HomeworkStatus.NO_HOMEWORK
        };

        for (HomeworkStatus status : homeworkStatuses) {
            testStudentSession = createStudentSessionWithStatus(
                    AttendanceStatus.PRESENT,
                    status,
                    "Test note for " + status
            );

            when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                    .thenReturn(Optional.of(testStudentSession));

            // Act
            SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

            // Assert
            assertThat(result.getStudentStatus().getHomeworkDueDate()).isNull();
            assertThat(result.getStudentStatus().getHomeworkStatus()).isEqualTo(status);
        }

        verify(studentSessionRepository, times(3)).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle makeup session status mapping")
    void shouldHandleMakeupSessionStatusMapping() {
        // Arrange - Create makeup session
        Session originalSession = TestDataBuilder.buildSession()
                .date(java.time.LocalDate.of(2024, 1, 10))
                .status(SessionStatus.DONE)
                .build();
        originalSession.setId(2001L);

        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PRESENT)
                .homeworkStatus(HomeworkStatus.COMPLETED)
                .note("Makeup session - student attended and completed all work")
                .isMakeup(true)
                .originalSession(originalSession)
                .build();

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert - Student status should still be mapped correctly even for makeup sessions
        assertThat(result.getStudentStatus().getAttendanceStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getStudentStatus().getHomeworkStatus()).isEqualTo(HomeworkStatus.COMPLETED);
        assertThat(result.getStudentStatus().getHomeworkDescription())
                .isEqualTo("Makeup session - student attended and completed all work");

        // Assert - Makeup info should be present
        assertThat(result.getMakeupInfo()).isNotNull();
        assertThat(result.getMakeupInfo().getIsMakeup()).isTrue();
        assertThat(result.getMakeupInfo().getOriginalSessionId()).isEqualTo(2001L);

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    /**
     * Helper method to create StudentSession with specific status values
     */
    private StudentSession createStudentSessionWithStatus(
            AttendanceStatus attendanceStatus,
            HomeworkStatus homeworkStatus,
            String note) {
        return StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(attendanceStatus)
                .homeworkStatus(homeworkStatus)
                .note(note)
                .isMakeup(false)
                .build();
    }

    /**
     * Test to explicitly verify the BUG FIX: homeworkDescription no longer comes from CourseSession.studentTask
     */
    @Test
    @DisplayName("CRITICAL: Verify bug fix - homeworkDescription should come from StudentSession.note NOT CourseSession.studentTask")
    void verifyBugFix_HomeworkDescriptionFromStudentSessionNotCourseSession() {
        // Arrange - Set different values in CourseSession vs StudentSession
        testCourseSession.setStudentTask("COURSE CURRICULUM: Complete textbook pages 10-15");

        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(AttendanceStatus.PRESENT)
                .homeworkStatus(HomeworkStatus.INCOMPLETE)
                .note("STUDENT STATUS: Needs help with pronunciation")
                .isMakeup(false)
                .build();

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // CRITICAL VERIFICATION - Before fix: homeworkDescription would be "COURSE CURRICULUM: Complete textbook pages 10-15"
        // After fix: homeworkDescription should be "STUDENT STATUS: Needs help with pronunciation"

        assertThat(result.getStudentStatus().getHomeworkDescription())
                .as("homeworkDescription should come from StudentSession.note")
                .isEqualTo("STUDENT STATUS: Needs help with pronunciation");

        assertThat(result.getStudentStatus().getHomeworkDescription())
                .as("homeworkDescription should NOT contain CourseSession data")
                .doesNotContain("COURSE CURRICULUM:")
                .doesNotContain("textbook pages 10-15");

        // Verify that CourseSession data is still available in SessionInfo (correct location)
        assertThat(result.getSessionInfo().getDescription())
                .as("CourseSession data should be in SessionInfo")
                .isEqualTo("COURSE CURRICULUM: Complete textbook pages 10-15");

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }
}