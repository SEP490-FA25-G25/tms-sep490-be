package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.studentrequest.MakeupRequestDTO;
import org.fyp.tmssep490be.dtos.studentrequest.StudentRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.DuplicateRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simplified unit tests for StudentRequestServiceImpl - Makeup Request methods.
 * Tests core business logic without complex entity setup.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StudentRequestService - Makeup Request Core Tests")
class MakeupRequestServiceTest {

    @Autowired
    private StudentRequestService studentRequestService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    @MockitoBean
    private StudentRequestRepository studentRequestRepository;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    private Student testStudent;
    private UserAccount testUser;
    private Session targetSession;
    private Session makeupSession;
    private StudentSession targetStudentSession;
    private ClassEntity testClass;
    private MakeupRequestDTO testMakeupRequest;

    @BeforeEach
    void setUp() {
        // Create CourseSession (REQUIRED for business logic)
        CourseSession courseSession = new CourseSession();
        courseSession.setId(1L);
        courseSession.setSequenceNo(1);
        courseSession.setTopic("Introduction to Programming");

        // Create test entities with proper relationships
        testStudent = new Student();
        testStudent.setId(1L);

        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("john.doe@example.com");

        testClass = new ClassEntity();
        testClass.setId(1L);
        testClass.setMaxCapacity(30);

        // Create target session with CourseSession
        targetSession = new Session();
        targetSession.setId(1L);
        targetSession.setClassEntity(testClass);
        targetSession.setCourseSession(courseSession);
        targetSession.setDate(LocalDate.now().minusWeeks(2)); // 2 weeks ago - within 4-week window
        targetSession.setStatus(SessionStatus.PLANNED);

        // Create makeup session with SAME CourseSession (CRITICAL for validation)
        makeupSession = new Session();
        makeupSession.setId(2L);
        makeupSession.setClassEntity(testClass);
        makeupSession.setCourseSession(courseSession); // MUST be same course session
        makeupSession.setDate(LocalDate.now().plusWeeks(1)); // Next week - future
        makeupSession.setStatus(SessionStatus.PLANNED);

        targetStudentSession = new StudentSession();
        targetStudentSession.setId(new StudentSession.StudentSessionId(testStudent.getId(), targetSession.getId()));
        targetStudentSession.setStudent(testStudent);
        targetStudentSession.setSession(targetSession);
        targetStudentSession.setAttendanceStatus(AttendanceStatus.ABSENT);

        testMakeupRequest = new MakeupRequestDTO();
        testMakeupRequest.setCurrentClassId(1L);
        testMakeupRequest.setTargetSessionId(targetSession.getId());
        testMakeupRequest.setMakeupSessionId(makeupSession.getId());
        testMakeupRequest.setRequestReason("Medical reasons with documentation");
        testMakeupRequest.setNote("Same time slot preferred");
    }

    // ========== Basic Happy Path Tests ==========

    @Test
    @DisplayName("Should submit makeup request with valid data")
    void shouldSubmitMakeupRequestWithValidData() {
        // Arrange
        StudentRequest savedRequest = new StudentRequest();
        savedRequest.setId(1L);
        savedRequest.setStudent(testStudent);
        savedRequest.setRequestType(StudentRequestType.MAKEUP);
        savedRequest.setStatus(RequestStatus.PENDING);
        savedRequest.setRequestReason(testMakeupRequest.getRequestReason());
        savedRequest.setSubmittedBy(testUser);

        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(targetStudentSession));
        when(sessionRepository.findById(makeupSession.getId())).thenReturn(Optional.of(makeupSession));
        when(studentSessionRepository.countBySessionId(makeupSession.getId())).thenReturn(15L); // Not full
        when(sessionRepository.findSessionsForStudentByDate(anyLong(), any(LocalDate.class))).thenReturn(List.of());
        when(studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                anyLong(), anyLong(), any(StudentRequestType.class), anyList()))
                .thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRequestRepository.save(any(StudentRequest.class))).thenReturn(savedRequest);

        // Act
        StudentRequestResponseDTO result = studentRequestService.submitMakeupRequest(1L, testMakeupRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRequestType()).isEqualTo("MAKEUP");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getRequestReason()).isEqualTo(testMakeupRequest.getRequestReason());

        verify(studentRequestRepository).save(any(StudentRequest.class));
    }

    @Test
    @DisplayName("Should auto-approve on-behalf makeup request")
    void shouldAutoApproveOnBehalfMakeupRequest() {
        // Arrange
        Long aaUserId = 2L;
        UserAccount aaUser = new UserAccount();
        aaUser.setId(aaUserId);
        aaUser.setEmail("aa.staff@example.com");

        testMakeupRequest.setStudentId(1L);

        StudentRequest savedRequest = new StudentRequest();
        savedRequest.setId(1L);
        savedRequest.setStudent(testStudent);
        savedRequest.setRequestType(StudentRequestType.MAKEUP);
        savedRequest.setStatus(RequestStatus.APPROVED); // Auto-approved
        savedRequest.setSubmittedBy(aaUser);
        savedRequest.setDecidedBy(aaUser);
        savedRequest.setTargetSession(targetSession); // Set targetSession
        savedRequest.setMakeupSession(makeupSession); // CRITICAL: Set makeupSession
        savedRequest.setCurrentClass(testClass); // CRITICAL: Set currentClass for executeMakeupApproval

        when(userAccountRepository.findById(aaUserId)).thenReturn(Optional.of(aaUser));
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(targetStudentSession));
        when(sessionRepository.findById(makeupSession.getId())).thenReturn(Optional.of(makeupSession));
        when(studentSessionRepository.countBySessionId(makeupSession.getId())).thenReturn(15L);
        when(sessionRepository.findSessionsForStudentByDate(anyLong(), any(LocalDate.class))).thenReturn(List.of());
        when(studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                anyLong(), anyLong(), any(StudentRequestType.class), anyList()))
                .thenReturn(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRequestRepository.save(any(StudentRequest.class))).thenReturn(savedRequest);

        // Act
        StudentRequestResponseDTO result = studentRequestService.submitMakeupRequestOnBehalf(aaUserId, testMakeupRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("APPROVED");

        verify(studentRequestRepository).save(any(StudentRequest.class));
    }

    // ========== Business Rule Validation Tests ==========

    @Test
    @DisplayName("Should throw exception when student not found")
    void shouldThrowExceptionWhenStudentNotFound() {
        // Arrange
        when(studentRepository.findByUserAccountId(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequest(999L, testMakeupRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Student not found for user ID: 999");

        verify(studentRepository).findByUserAccountId(999L);
        verifyNoMoreInteractions(studentRequestRepository);
    }

    @Test
    @DisplayName("Should throw exception when target session is not ABSENT")
    void shouldThrowExceptionWhenTargetSessionNotAbsent() {
        // Arrange
        targetStudentSession.setAttendanceStatus(AttendanceStatus.PRESENT); // Not ABSENT

        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(targetStudentSession));

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequest(1L, testMakeupRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Can only makeup absent sessions");

        verify(studentRepository).findByUserAccountId(1L);
        verify(userAccountRepository).findById(1L);
        verify(studentSessionRepository).findById(any());
        verifyNoMoreInteractions(studentRequestRepository);
    }

    @Test
    @DisplayName("Should throw exception when makeup session is full")
    void shouldThrowExceptionWhenMakeupSessionIsFull() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(targetStudentSession));
        when(sessionRepository.findById(makeupSession.getId())).thenReturn(Optional.of(makeupSession));
        when(studentSessionRepository.countBySessionId(makeupSession.getId())).thenReturn(30L); // Full capacity

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequest(1L, testMakeupRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Makeup session is full");

        verify(studentRepository).findByUserAccountId(1L);
        verify(userAccountRepository).findById(1L);
        verify(studentSessionRepository).findById(any());
        verify(sessionRepository).findById(makeupSession.getId());
        verify(studentSessionRepository).countBySessionId(makeupSession.getId());
        verifyNoMoreInteractions(studentRequestRepository);
    }

    @Test
    @DisplayName("Should throw exception when duplicate makeup request exists")
    void shouldThrowExceptionWhenDuplicateMakeupRequestExists() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(targetStudentSession));
        when(sessionRepository.findById(makeupSession.getId())).thenReturn(Optional.of(makeupSession));
        when(studentSessionRepository.countBySessionId(makeupSession.getId())).thenReturn(15L);
        when(sessionRepository.findSessionsForStudentByDate(anyLong(), any(LocalDate.class))).thenReturn(List.of());
        when(studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                anyLong(), anyLong(), any(StudentRequestType.class), anyList()))
                .thenReturn(true); // Duplicate exists!

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequest(1L, testMakeupRequest))
                .isInstanceOf(DuplicateRequestException.class)
                .hasMessageContaining("Duplicate makeup request for this session");

        verify(studentRepository).findByUserAccountId(1L);
        verify(userAccountRepository).findById(1L);
        verify(studentSessionRepository).findById(any());
        verify(sessionRepository).findById(makeupSession.getId());
        verify(studentSessionRepository).countBySessionId(makeupSession.getId());
        verify(sessionRepository).findSessionsForStudentByDate(anyLong(), any(LocalDate.class));
        verify(studentRequestRepository).existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                anyLong(), anyLong(), any(StudentRequestType.class), anyList());
        verifyNoMoreInteractions(studentRequestRepository);
    }

    @Test
    @DisplayName("Should throw exception when student ID missing for on-behalf request")
    void shouldThrowExceptionWhenStudentIdMissingForOnBehalf() {
        // Arrange
        testMakeupRequest.setStudentId(null);

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequestOnBehalf(2L, testMakeupRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Student ID is required for on-behalf requests");

        verifyNoInteractions(userAccountRepository);
    }

    @Test
    @DisplayName("Should throw exception when AA user not found for on-behalf request")
    void shouldThrowExceptionWhenAAUserNotFoundForOnBehalf() {
        // Arrange
        testMakeupRequest.setStudentId(1L);

        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequestOnBehalf(999L, testMakeupRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userAccountRepository).findById(999L);
        verifyNoMoreInteractions(userAccountRepository);
    }

    @Test
    @DisplayName("Should throw exception when makeup session not found")
    void shouldThrowExceptionWhenMakeupSessionNotFound() {
        // Arrange
        when(studentRepository.findByUserAccountId(1L)).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(studentSessionRepository.findById(any())).thenReturn(Optional.of(targetStudentSession));
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        // Change request to use non-existent session
        testMakeupRequest.setMakeupSessionId(999L);

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitMakeupRequest(1L, testMakeupRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Makeup session not found");

        verify(studentRepository).findByUserAccountId(1L);
        verify(userAccountRepository).findById(1L);
        verify(studentSessionRepository).findById(any());
        verify(sessionRepository).findById(999L);
        verifyNoMoreInteractions(studentRequestRepository);
    }
}