package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.config.AbstractRepositoryTest;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer tests for StudentRequestRepository.
 * Tests database queries and entity relationships using TestEntityManager.
 */
@DataJpaTest
@DisplayName("Student Request Repository Tests")
class StudentRequestRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudentRequestRepository studentRequestRepository;

    // Test entities
    private Center testCenter;
    private Branch testBranch;
    private Subject testSubject;
    private Level testLevel;
    private Course testCourse;
    private ClassEntity testClass;
    private Student testStudent;
    private UserAccount testUser;
    private Session testSession;
    private StudentRequest testRequest;

    @BeforeEach
    void setUp() {
        // Create test center
        testCenter = TestDataBuilder.buildCenter()
            .code("TC001")
            .name("Test Center")
            .build();
        entityManager.persistAndFlush(testCenter);

        // Create test branch
        testBranch = TestDataBuilder.buildBranch()
            .center(testCenter)
            .code("BR001")
            .name("Test Branch")
            .build();
        entityManager.persistAndFlush(testBranch);

        // Create test subject
        testSubject = TestDataBuilder.buildSubject()
            .code("ENG")
            .name("English")
            .build();
        entityManager.persistAndFlush(testSubject);

        // Create test level
        testLevel = TestDataBuilder.buildLevel()
            .subject(testSubject)
            .code("A1")
            .name("Beginner")
            .build();
        entityManager.persistAndFlush(testLevel);

        // Create test course
        testCourse = TestDataBuilder.buildCourse()
            .subject(testSubject)
            .level(testLevel)
            .code("ENG-A1-2024")
            .name("English A1 Course")
            .build();
        entityManager.persistAndFlush(testCourse);

        // Create test class
        testClass = TestDataBuilder.buildClassEntity()
            .branch(testBranch)
            .course(testCourse)
            .code("CLASS001")
            .name("Test Class")
            .build();
        entityManager.persistAndFlush(testClass);

        // Create test user account
        testUser = TestDataBuilder.buildUserAccount()
            .email("student@test.com")
            .fullName("Test Student")
            .build();
        entityManager.persistAndFlush(testUser);

        // Create test student
        testStudent = TestDataBuilder.buildStudent()
            .userAccount(testUser)
            .studentCode("ST001")
            .build();
        entityManager.persistAndFlush(testStudent);

        // Create test session
        testSession = TestDataBuilder.buildSession()
            .classEntity(testClass)
            .date(LocalDate.now().plusDays(3))
            .status(SessionStatus.PLANNED)
            .build();
        entityManager.persistAndFlush(testSession);

        // Create test request
        testRequest = StudentRequest.builder()
            .student(testStudent)
            .currentClass(testClass)
            .requestType(StudentRequestType.ABSENCE)
            .targetSession(testSession)
            .requestReason("Valid reason for absence request that meets minimum length")
            .status(RequestStatus.PENDING)
            .submittedBy(testUser)
            .submittedAt(OffsetDateTime.now())
            .build();
        entityManager.persistAndFlush(testRequest);
    }

    @Test
    @DisplayName("Should find requests by student ID and status")
    void shouldFindRequestsByStudentIdAndStatus() {
        // Arrange
        List<RequestStatus> statuses = List.of(RequestStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<StudentRequest> result = studentRequestRepository.findByStudentIdAndStatusIn(
            testStudent.getId(), statuses, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStudent().getId()).isEqualTo(testStudent.getId());
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("Should return empty page when no requests found for student and status")
    void shouldReturnEmptyPageWhenNoRequestsFoundForStudentAndStatus() {
        // Arrange
        List<RequestStatus> statuses = List.of(RequestStatus.APPROVED);
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<StudentRequest> result = studentRequestRepository.findByStudentIdAndStatusIn(
            testStudent.getId(), statuses, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should check for duplicate requests correctly")
    void shouldCheckForDuplicateRequestsCorrectly() {
        // Arrange
        List<RequestStatus> statuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);

        // Act
        boolean exists = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
            testStudent.getId(), testSession.getId(), StudentRequestType.ABSENCE, statuses);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when no duplicate request exists")
    void shouldReturnFalseWhenNoDuplicateRequestExists() {
        // Arrange
        List<RequestStatus> statuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);

        // Act
        boolean exists = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
            testStudent.getId(), testSession.getId(), StudentRequestType.MAKEUP, statuses);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should count requests by status correctly")
    void shouldCountRequestsByStatusCorrectly() {
        // Act
        long count = studentRequestRepository.countByStatus(RequestStatus.PENDING);

        // Assert
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count requests by type and status correctly")
    void shouldCountRequestsByTypeAndStatusCorrectly() {
        // Act
        long count = studentRequestRepository.countByRequestTypeAndStatus(
            StudentRequestType.ABSENCE, RequestStatus.PENDING);

        // Assert
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find all requests by student")
    void shouldFindAllRequestsByStudent() {
        // Act
        List<StudentRequest> requests = studentRequestRepository.findByStudentId(testStudent.getId());

        // Assert
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getStudent().getId()).isEqualTo(testStudent.getId());
    }

    @Test
    @DisplayName("Should check if request exists by ID and student ID")
    void shouldCheckIfRequestExistsByIdAndStudentId() {
        // Act
        boolean exists = studentRequestRepository.existsByIdAndStudentId(
            testRequest.getId(), testStudent.getId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when request does not exist for student")
    void shouldReturnFalseWhenRequestDoesNotExistForStudent() {
        // Act
        boolean exists = studentRequestRepository.existsByIdAndStudentId(
            999L, testStudent.getId());

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should create and retrieve student request with all relationships")
    void shouldCreateAndRetrieveStudentRequestWithAllRelationships() {
        // Arrange - create a new request
        StudentRequest newRequest = StudentRequest.builder()
            .student(testStudent)
            .currentClass(testClass)
            .requestType(StudentRequestType.MAKEUP)
            .targetSession(testSession)
            .requestReason("Another valid reason for request that meets minimum length requirement")
            .status(RequestStatus.PENDING)
            .submittedBy(testUser)
            .submittedAt(OffsetDateTime.now())
            .build();

        // Act
        StudentRequest saved = studentRequestRepository.save(newRequest);
        StudentRequest retrieved = studentRequestRepository.findById(saved.getId()).orElse(null);

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isNotNull();
        assertThat(retrieved.getStudent().getId()).isEqualTo(testStudent.getId());
        assertThat(retrieved.getCurrentClass().getId()).isEqualTo(testClass.getId());
        assertThat(retrieved.getTargetSession().getId()).isEqualTo(testSession.getId());
        assertThat(retrieved.getRequestType()).isEqualTo(StudentRequestType.MAKEUP);
        assertThat(retrieved.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(retrieved.getSubmittedBy().getId()).isEqualTo(testUser.getId());
        assertThat(retrieved.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update request status correctly")
    void shouldUpdateRequestStatusCorrectly() {
        // Arrange
        testRequest.setStatus(RequestStatus.APPROVED);

        // Act
        StudentRequest updated = studentRequestRepository.save(testRequest);
        StudentRequest retrieved = studentRequestRepository.findById(updated.getId()).orElse(null);

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    @DisplayName("Should handle requests with null relationships gracefully")
    void shouldHandleRequestsWithNullRelationshipsGracefully() {
        // Arrange - create request with minimal data
        StudentRequest minimalRequest = StudentRequest.builder()
            .student(testStudent)
            .requestType(StudentRequestType.TRANSFER)
            .requestReason("Valid reason for transfer request")
            .status(RequestStatus.PENDING)
            .submittedBy(testUser)
            .submittedAt(OffsetDateTime.now())
            .build();

        // Act
        StudentRequest saved = studentRequestRepository.save(minimalRequest);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCurrentClass()).isNull();
        assertThat(saved.getTargetSession()).isNull();
    }

    @Test
    @DisplayName("Should find pending requests")
    void shouldFindPendingRequestsWithMultipleFilters() {
        // This test verifies the simplified query structure

        // Act
        Page<StudentRequest> result = studentRequestRepository.findPendingRequestsForAA(
            RequestStatus.PENDING,
            PageRequest.of(0, 10));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    @DisplayName("Should find all requests by status")
    void shouldFindAllRequestsWithAAFiltering() {
        // Act
        Page<StudentRequest> result = studentRequestRepository.findByStatus(
            RequestStatus.PENDING,
            PageRequest.of(0, 10));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getContent().get(0).getRequestType()).isEqualTo(StudentRequestType.ABSENCE);
    }
}