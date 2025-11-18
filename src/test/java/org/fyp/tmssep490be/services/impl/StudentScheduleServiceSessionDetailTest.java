package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.schedule.MaterialDTO;
import org.fyp.tmssep490be.dtos.schedule.ResourceDTO;
import org.fyp.tmssep490be.dtos.schedule.SessionDetailDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.MaterialType;
import org.fyp.tmssep490be.entities.enums.ResourceType;
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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for StudentScheduleService session detail functionality.
 * Tests the critical bug fix where course materials should be returned instead of room names.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StudentScheduleService Session Detail Tests")
class StudentScheduleServiceSessionDetailTest {

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
    private List<CourseMaterial> testCourseMaterials;
    private Resource testRoomResource;
    private Resource testVirtualResource;
    private List<SessionResource> testSessionResources;

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

        // 4. Create CourseSession with materials
        testCourseSession = CourseSession.builder()
                .id(1L)
                .topic("IELTS Introduction")
                .studentTask("Complete the introduction exercise")
                .build();

        // 5. Create Course Materials (the actual learning materials, NOT room resources)
        Set<CourseMaterial> materialSet = new HashSet<>();
        materialSet.add(CourseMaterial.builder()
                .id(1L)
                .title("IELTS Foundation - Course Syllabus")
                .description("The complete syllabus for the course")
                .materialType(MaterialType.PDF)
                .url("/materials/courses/1/syllabus.pdf")
                .uploadedBy(testStudent.getUserAccount())
                .createdAt(OffsetDateTime.of(2024, 1, 14, 17, 0, 0, 0, java.time.ZoneOffset.UTC))
                .build());
        materialSet.add(CourseMaterial.builder()
                .id(2L)
                .title("Session 1 - Listening Slides")
                .description("Presentation slides for the first session")
                .materialType(MaterialType.SLIDE)
                .url("/materials/sessions/1/slides.pptx")
                .uploadedBy(testStudent.getUserAccount())
                .createdAt(OffsetDateTime.of(2024, 1, 15, 10, 30, 0, 0, java.time.ZoneOffset.UTC))
                .build());
        materialSet.add(CourseMaterial.builder()
                .id(3L)
                .title("Phase 1 Vocabulary List")
                .description("Key vocabulary for the first 4 weeks")
                .materialType(MaterialType.DOCUMENT)
                .url("/materials/phases/1/vocab.docx")
                .uploadedBy(testStudent.getUserAccount())
                .createdAt(OffsetDateTime.of(2024, 1, 13, 14, 20, 0, 0, java.time.ZoneOffset.UTC))
                .build());
        testCourseSession.setCourseMaterials(materialSet);
        testCourseMaterials = new ArrayList<>(materialSet);

        // 5.5. Create classroom Resources
        testRoomResource = Resource.builder()
                .id(1L)
                .branch(testBranch)
                .resourceType(ResourceType.ROOM)
                .code("HN01-R101")
                .name("Ha Noi Room 101")
                .description("Spacious classroom with projector")
                .capacity(20)
                .build();

        testVirtualResource = Resource.builder()
                .id(2L)
                .branch(testBranch)
                .resourceType(ResourceType.VIRTUAL)
                .code("ZOOM-01")
                .name("https://zoom.us/j/123456789?pwd=abc123")
                .description("Main Zoom meeting room")
                .meetingUrl("https://zoom.us/j/123456789?pwd=abc123")
                .meetingId("123456789")
                .meetingPasscode("abc123")
                .build();

        // 6. Create Session
        testSession = TestDataBuilder.buildSession()
                .classEntity(testClass)
                .date(java.time.LocalDate.of(2024, 1, 15))
                .status(SessionStatus.PLANNED)
                .build();
        testSession.setId(1001L);
        testSession.setCourseSession(testCourseSession);
        testSession.setTimeSlotTemplate(testTimeSlot);

        // 6.5. Create SessionResources linking the session to resources
        testSessionResources = new ArrayList<>();

        SessionResource roomSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(1001L, 1L))
                .session(testSession)
                .resource(testRoomResource)
                .build();
        testSessionResources.add(roomSessionResource);

        // Set the session resources on the session
        testSession.setSessionResources(new HashSet<>(testSessionResources));

        // 7. Create StudentSession
        testStudentSession = StudentSession.builder()
                .id(new StudentSession.StudentSessionId(testStudent.getId(), testSession.getId()))
                .student(testStudent)
                .session(testSession)
                .attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                .isMakeup(false)
                .build();
    }

    @Test
    @DisplayName("Should get session detail with both learning materials AND classroom resources")
    void shouldGetSessionDetailWithMaterialsAndResources() {
        // Arrange
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert - Basic session details
        assertThat(result).isNotNull();
        assertThat(result.getSessionId()).isEqualTo(1001L);
        assertThat(result.getStudentSessionId()).isEqualTo(1001L);
        assertThat(result.getDate()).isEqualTo(java.time.LocalDate.of(2024, 1, 15));
        assertThat(result.getStartTime()).isEqualTo(java.time.LocalTime.of(9, 0));
        assertThat(result.getEndTime()).isEqualTo(java.time.LocalTime.of(11, 0));

        // Assert - Class information
        assertThat(result.getClassInfo()).isNotNull();
        assertThat(result.getClassInfo().getClassCode()).isEqualTo("CLASS001");
        assertThat(result.getClassInfo().getCourseName()).isEqualTo("English A1 Course");
        assertThat(result.getClassInfo().getBranchName()).isEqualTo("Test Branch");

        // Assert - Session information
        assertThat(result.getSessionInfo()).isNotNull();
        assertThat(result.getSessionInfo().getTopic()).isEqualTo("IELTS Introduction");
        assertThat(result.getSessionInfo().getDescription()).isEqualTo("Complete the introduction exercise");

        // Assert - CRITICAL: Learning materials should be actual course materials
        assertThat(result.getMaterials()).isNotNull();
        assertThat(result.getMaterials()).hasSize(3);

        // Verify materials contain expected learning materials (order not guaranteed due to Set)
        Map<Long, MaterialDTO> materialsById = result.getMaterials().stream()
                .collect(Collectors.toMap(MaterialDTO::getMaterialId, material -> material));

        // Verify Course Syllabus (ID: 1L)
        MaterialDTO syllabus = materialsById.get(1L);
        assertThat(syllabus).isNotNull();
        assertThat(syllabus.getFileName()).isEqualTo("IELTS Foundation - Course Syllabus");
        assertThat(syllabus.getFileUrl()).isEqualTo("/materials/courses/1/syllabus.pdf");
        assertThat(syllabus.getUploadedAt()).isEqualTo(LocalDateTime.of(2024, 1, 14, 17, 0));

        // Verify Session Slides (ID: 2L)
        MaterialDTO slides = materialsById.get(2L);
        assertThat(slides).isNotNull();
        assertThat(slides.getFileName()).isEqualTo("Session 1 - Listening Slides");
        assertThat(slides.getFileUrl()).isEqualTo("/materials/sessions/1/slides.pptx");
        assertThat(slides.getUploadedAt()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30));

        // Verify Vocabulary List (ID: 3L)
        MaterialDTO vocab = materialsById.get(3L);
        assertThat(vocab).isNotNull();
        assertThat(vocab.getFileName()).isEqualTo("Phase 1 Vocabulary List");
        assertThat(vocab.getFileUrl()).isEqualTo("/materials/phases/1/vocab.docx");
        assertThat(vocab.getUploadedAt()).isEqualTo(LocalDateTime.of(2024, 1, 13, 14, 20));

        // Assert - NEW: Classroom resource should be provided separately
        assertThat(result.getClassroomResource()).isNotNull();
        assertThat(result.getClassroomResource().getResourceId()).isEqualTo(1L);
        assertThat(result.getClassroomResource().getResourceCode()).isEqualTo("HN01-R101");
        assertThat(result.getClassroomResource().getResourceName()).isEqualTo("Ha Noi Room 101");
        assertThat(result.getClassroomResource().getResourceType()).isEqualTo(ResourceType.ROOM);
        assertThat(result.getClassroomResource().getCapacity()).isEqualTo(20);
        assertThat(result.getClassroomResource().getLocation()).isEqualTo("Ha Noi Room 101, Test Branch");
        assertThat(result.getClassroomResource().getOnlineLink()).isNull();

        // CRITICAL VERIFICATION: Ensure no room names are returned as materials
        assertThat(result.getMaterials()).noneMatch(material ->
            material.getFileName().equals("Ha Noi Room 101") ||
            material.getFileName().contains("Room") ||
            material.getFileUrl().contains("/api/v1/resources/download")
        );

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle session with no course materials but with classroom resource")
    void shouldHandleSessionWithNoMaterialsButWithResource() {
        // Arrange
        testCourseSession.setCourseMaterials(new HashSet<>()); // Empty materials list
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMaterials()).isNotNull();
        assertThat(result.getMaterials()).isEmpty();

        // But classroom resource should still be provided
        assertThat(result.getClassroomResource()).isNotNull();
        assertThat(result.getClassroomResource().getResourceId()).isEqualTo(1L);
        assertThat(result.getClassroomResource().getResourceName()).isEqualTo("Ha Noi Room 101");

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle session with null CourseSession")
    void shouldHandleSessionWithNullCourseSession() {
        // Arrange
        testSession.setCourseSession(null); // No CourseSession
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMaterials()).isNotNull();
        assertThat(result.getMaterials()).isEmpty();
        assertThat(result.getSessionInfo().getTopic()).isNull();
        assertThat(result.getSessionInfo().getDescription()).isNull();

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should throw exception when student session not found")
    void shouldThrowExceptionWhenStudentSessionNotFound() {
        // Arrange
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 9999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentScheduleService.getSessionDetail(100L, 9999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.STUDENT_SESSION_NOT_FOUND.getMessage());

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 9999L);
    }

    @Test
    @DisplayName("Should verify materials are correctly mapped from CourseMaterial entity")
    void shouldVerifyMaterialsAreMappedFromCourseMaterialEntity() {
        // Arrange
        CourseMaterial specialMaterial = CourseMaterial.builder()
                .id(999L)
                .title("Special Test Material")
                .description("A special material for testing")
                .materialType(MaterialType.VIDEO)
                .url("/materials/special/test-video.mp4")
                .uploadedBy(testStudent.getUserAccount())
                .createdAt(OffsetDateTime.of(2024, 2, 1, 12, 0, 0, 0, java.time.ZoneOffset.UTC))
                .build();

        testCourseSession.setCourseMaterials(new HashSet<>(Arrays.asList(specialMaterial)));
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result.getMaterials()).hasSize(1);
        assertThat(result.getMaterials().get(0).getMaterialId()).isEqualTo(999L);
        assertThat(result.getMaterials().get(0).getFileName()).isEqualTo("Special Test Material");
        assertThat(result.getMaterials().get(0).getFileUrl()).isEqualTo("/materials/special/test-video.mp4");
        assertThat(result.getMaterials().get(0).getUploadedAt()).isEqualTo(LocalDateTime.of(2024, 2, 1, 12, 0));

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle VIRTUAL type resources with zoom links")
    void shouldHandleVirtualResourcesWithZoomLinks() {
        // Arrange - Replace room resource with virtual resource
        SessionResource virtualSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(1001L, 2L))
                .session(testSession)
                .resource(testVirtualResource)
                .build();
        testSession.setSessionResources(new HashSet<>(Arrays.asList(virtualSessionResource)));

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result.getClassroomResource()).isNotNull();
        assertThat(result.getClassroomResource().getResourceId()).isEqualTo(2L);
        assertThat(result.getClassroomResource().getResourceCode()).isEqualTo("ZOOM-01");
        assertThat(result.getClassroomResource().getResourceName()).isEqualTo("https://zoom.us/j/123456789?pwd=abc123");
        assertThat(result.getClassroomResource().getResourceType()).isEqualTo(ResourceType.VIRTUAL);
        assertThat(result.getClassroomResource().getLocation()).isEqualTo("Online");
        assertThat(result.getClassroomResource().getOnlineLink()).isEqualTo("https://zoom.us/j/123456789?pwd=abc123");

        // Learning materials should still be present
        assertThat(result.getMaterials()).hasSize(3);

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle session with no classroom resources")
    void shouldHandleSessionWithNoClassroomResources() {
        // Arrange - Remove all session resources
        testSession.setSessionResources(new HashSet<>());
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getClassroomResource()).isNull();

        // Learning materials should still be present
        assertThat(result.getMaterials()).hasSize(3);

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should handle session with neither materials nor resources")
    void shouldHandleSessionWithNeitherMaterialsNorResources() {
        // Arrange - Remove both materials and resources
        testCourseSession.setCourseMaterials(new HashSet<>());
        testSession.setSessionResources(new HashSet<>());
        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMaterials()).isNotNull();
        assertThat(result.getMaterials()).isEmpty();
        assertThat(result.getClassroomResource()).isNull();

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }

    @Test
    @DisplayName("Should use first resource when multiple resources are assigned")
    void shouldUseFirstResourceWhenMultipleAssigned() {
        // Arrange - Add both room and virtual resources
        SessionResource roomSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(1001L, 1L))
                .session(testSession)
                .resource(testRoomResource)
                .build();

        SessionResource virtualSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(1001L, 2L))
                .session(testSession)
                .resource(testVirtualResource)
                .build();

        testSession.setSessionResources(new HashSet<>(Arrays.asList(roomSessionResource, virtualSessionResource)));

        when(studentSessionRepository.findByStudentIdAndSessionId(100L, 1001L))
                .thenReturn(Optional.of(testStudentSession));

        // Act
        SessionDetailDTO result = studentScheduleService.getSessionDetail(100L, 1001L);

        // Assert - Should return one of the resources (order not guaranteed)
        assertThat(result.getClassroomResource()).isNotNull();
        assertThat(result.getClassroomResource().getResourceId()).isIn(1L, 2L);
        assertThat(result.getClassroomResource().getResourceType()).isIn(ResourceType.ROOM, ResourceType.VIRTUAL);

        verify(studentSessionRepository).findByStudentIdAndSessionId(100L, 1001L);
    }
}