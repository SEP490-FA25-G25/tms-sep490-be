package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.TransferEligibilityDTO;
import org.fyp.tmssep490be.dtos.studentrequest.TransferOptionDTO;
import org.fyp.tmssep490be.dtos.studentrequest.TransferRequestDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.impl.StudentRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for Transfer Request functionality
 * Tests cover: eligibility check, transfer options, submission, validation, content gap analysis
 *
 * Following Spring Boot 3.5.7 best practices with @MockitoBean pattern
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Transfer Request Service Tests")
class StudentRequestTransferServiceTest {

    @Autowired
    private StudentRequestServiceImpl studentRequestService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private StudentRequestRepository studentRequestRepository;

    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    private Student testStudent;
    private UserAccount testUser;
    private ClassEntity currentClass;
    private ClassEntity targetClass;
    private Course testCourse;
    private Branch testBranch;
    private Enrollment currentEnrollment;
    private Session effectiveSession;

    @BeforeEach
    void setUp() {
        // Setup test data
        testUser = UserAccount.builder()
                .id(1L)
                .email("student@test.com")
                .fullName("Test Student")
                .build();

        testStudent = Student.builder()
                .id(101L)
                .userAccount(testUser)
                .studentCode("STU001")
                .build();

        testBranch = Branch.builder()
                .id(1L)
                .name("Central Branch")
                .build();

        testCourse = Course.builder()
                .id(10L)
                .subject(Subject.builder().id(1L).name("Chinese").build())
                .level(Level.builder().id(1L).name("A1").build())
                .build();

        currentClass = ClassEntity.builder()
                .id(101L)
                .code("CHN-A1-01")
                .name("Morning Class")
                .course(testCourse)
                .branch(testBranch)
                .modality(Modality.OFFLINE)
                .status(ClassStatus.ONGOING)
                .maxCapacity(20)
                .startDate(LocalDate.now().minusMonths(1))
                .plannedEndDate(LocalDate.now().plusMonths(2))
                .build();

        targetClass = ClassEntity.builder()
                .id(103L)
                .code("CHN-A1-03")
                .name("Evening Class")
                .course(testCourse)
                .branch(testBranch)
                .modality(Modality.OFFLINE)
                .status(ClassStatus.ONGOING)
                .maxCapacity(20)
                .startDate(LocalDate.now().minusMonths(1))
                .plannedEndDate(LocalDate.now().plusMonths(2))
                .build();

        currentEnrollment = Enrollment.builder()
                .id(1001L)
                .studentId(testStudent.getId())
                .classId(currentClass.getId())
                .classEntity(currentClass)
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now().minusMonths(1))
                .build();

        effectiveSession = Session.builder()
                .id(3001L)
                .classEntity(targetClass)
                .date(LocalDate.now().plusDays(7))
                .status(SessionStatus.PLANNED)
                .courseSession(CourseSession.builder()
                        .id(201L)
                        .sequenceNo(13)
                        .topic("Listening Practice")
                        .build())
                .build();
    }

    // ============== TRANSFER ELIGIBILITY TESTS ==============

    @Test
    @DisplayName("Should return eligible when student has active enrollment and no transfers")
    void testGetTransferEligibility_Success() {
        // Arrange
        when(studentRepository.findByUserAccountId(testUser.getId()))
                .thenReturn(Optional.of(testStudent));
        when(enrollmentRepository.findByStudentIdAndStatusIn(testStudent.getId(), List.of(EnrollmentStatus.ENROLLED)))
                .thenReturn(List.of(currentEnrollment));
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatus(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED))
                .thenReturn(0L);

        // Act
        TransferEligibilityDTO result = studentRequestService.getTransferEligibility(testUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isEligibleForTransfer()).isTrue();
        assertThat(result.getCurrentClasses()).hasSize(1);
        assertThat(result.getPolicyInfo().getMaxTransfersPerCourse()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return ineligible when student has no enrollments")
    void testGetTransferEligibility_NoEnrollments() {
        // Arrange
        when(studentRepository.findByUserAccountId(testUser.getId()))
                .thenReturn(Optional.of(testStudent));
        when(enrollmentRepository.findByStudentIdAndStatusIn(testStudent.getId(), List.of(EnrollmentStatus.ENROLLED)))
                .thenReturn(List.of());

        // Act
        TransferEligibilityDTO result = studentRequestService.getTransferEligibility(testUser.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.isEligibleForTransfer()).isFalse();
        assertThat(result.getIneligibilityReason()).contains("No eligible enrollments");
    }

    // ============== TRANSFER OPTIONS TESTS ==============

    @Test
    @DisplayName("Should return transfer options with same branch and modality")
    void testGetTransferOptions_Success() {
        // Arrange
        when(studentRepository.findByUserAccountId(testUser.getId()))
                .thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdWithCourse(currentClass.getId()))
                .thenReturn(Optional.of(currentClass));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(
                testStudent.getId(), currentClass.getId(), List.of(EnrollmentStatus.ENROLLED)))
                .thenReturn(true);
        when(classRepository.findByCourseIdAndStatusIn(testCourse.getId(),
                List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING)))
                .thenReturn(List.of(currentClass, targetClass));
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(sessionRepository.findByClassIdAndStatusIn(anyLong(), any())).thenReturn(List.of());
        when(sessionRepository.findByClassIdAndDateBefore(anyLong(), any())).thenReturn(List.of());

        // Act
        List<TransferOptionDTO> result = studentRequestService.getTransferOptions(testUser.getId(), currentClass.getId());

        // Assert
        assertThat(result).isNotEmpty();
        TransferOptionDTO option = result.get(0);
        assertThat(option.getClassId()).isEqualTo(targetClass.getId());
        assertThat(option.getAvailableSlots()).isEqualTo(5);
        assertThat(option.isCanTransfer()).isTrue();
        assertThat(option.getContentGapAnalysis()).isNotNull();
    }

    // ============== CONTENT GAP ANALYSIS TESTS ==============

    @Test
    @DisplayName("Content gap should be NONE when classes are at same pace")
    void testContentGapAnalysis_None() {
        // Arrange
        Session completedSession = Session.builder()
                .id(2001L)
                .classEntity(currentClass)
                .date(LocalDate.now().minusDays(5))
                .status(SessionStatus.DONE)
                .courseSession(CourseSession.builder().id(201L).sequenceNo(12).topic("Test").build())
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(
                testStudent.getId(), currentClass.getId(), List.of(EnrollmentStatus.ENROLLED)))
                .thenReturn(true);
        when(classRepository.findByCourseIdAndStatusIn(testCourse.getId(),
                List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING)))
                .thenReturn(List.of(targetClass));
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(10);
        when(sessionRepository.findByClassIdAndStatusIn(currentClass.getId(),
                List.of(SessionStatus.DONE, SessionStatus.CANCELLED)))
                .thenReturn(List.of(completedSession));
        when(sessionRepository.findByClassIdAndDateBefore(targetClass.getId(), LocalDate.now()))
                .thenReturn(List.of(completedSession));

        // Act
        List<TransferOptionDTO> result = studentRequestService.getTransferOptions(testUser.getId(), currentClass.getId());

        // Assert
        assertThat(result).isNotEmpty();
        TransferOptionDTO.ContentGapAnalysis gap = result.get(0).getContentGapAnalysis();
        assertThat(gap.getGapLevel()).isEqualTo("NONE");
        assertThat(gap.getMissedSessions()).isEqualTo(0);
    }

    @Test
    @DisplayName("Content gap should be MINOR when 1-2 sessions behind")
    void testContentGapAnalysis_Minor() {
        // Arrange
        CourseSession cs1 = CourseSession.builder().id(201L).sequenceNo(11).topic("Session 11").build();
        CourseSession cs2 = CourseSession.builder().id(202L).sequenceNo(12).topic("Session 12").build();

        Session targetPastSession1 = Session.builder()
                .id(3001L)
                .classEntity(targetClass)
                .date(LocalDate.now().minusDays(10))
                .courseSession(cs1)
                .build();

        Session targetPastSession2 = Session.builder()
                .id(3002L)
                .classEntity(targetClass)
                .date(LocalDate.now().minusDays(5))
                .courseSession(cs2)
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(enrollmentRepository.existsByStudentIdAndClassIdAndStatusIn(
                testStudent.getId(), currentClass.getId(), List.of(EnrollmentStatus.ENROLLED)))
                .thenReturn(true);
        when(classRepository.findByCourseIdAndStatusIn(testCourse.getId(),
                List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING)))
                .thenReturn(List.of(targetClass));
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(10);
        when(sessionRepository.findByClassIdAndStatusIn(currentClass.getId(),
                List.of(SessionStatus.DONE, SessionStatus.CANCELLED)))
                .thenReturn(List.of()); // Current class has no completed sessions
        when(sessionRepository.findByClassIdAndDateBefore(targetClass.getId(), LocalDate.now()))
                .thenReturn(List.of(targetPastSession1, targetPastSession2)); // Target has 2 past sessions

        // Act
        List<TransferOptionDTO> result = studentRequestService.getTransferOptions(testUser.getId(), currentClass.getId());

        // Assert
        assertThat(result).isNotEmpty();
        TransferOptionDTO.ContentGapAnalysis gap = result.get(0).getContentGapAnalysis();
        assertThat(gap.getGapLevel()).isEqualTo("MINOR");
        assertThat(gap.getMissedSessions()).isEqualTo(2);
        assertThat(gap.getGapSessions()).hasSize(2);
        assertThat(gap.getRecommendedActions()).contains("Review materials for missed topics");
    }

    // ============== SUBMIT TRANSFER REQUEST TESTS ==============

    @Test
    @DisplayName("Should submit transfer request successfully with all validations")
    void testSubmitTransferRequest_Success() {
        // Arrange
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().plusDays(7))
                .requestReason("Need to change schedule due to work commitments. New job starts next week.")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(0L);
        when(sessionRepository.findByClassEntityIdAndDate(targetClass.getId(), dto.getEffectiveDate()))
                .thenReturn(List.of(effectiveSession));
        when(studentRequestRepository.existsByStudentIdAndCurrentClassIdAndTargetClassIdAndRequestTypeAndStatusIn(
                testStudent.getId(), currentClass.getId(), targetClass.getId(), StudentRequestType.TRANSFER,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(studentRepository.findById(testStudent.getId())).thenReturn(Optional.of(testStudent));
        when(studentRequestRepository.save(any(StudentRequest.class))).thenAnswer(invocation -> {
            StudentRequest request = invocation.getArgument(0);
            request.setId(44L);
            return request;
        });

        // Act & Assert
        assertThatCode(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .doesNotThrowAnyException();

        verify(studentRequestRepository, times(1)).save(any(StudentRequest.class));
    }

    @Test
    @DisplayName("Should reject transfer when effective date has no session")
    void testSubmitTransferRequest_NoSessionOnDate() {
        // Arrange
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().plusDays(7))
                .requestReason("Valid reason with more than twenty characters")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(0L);
        when(sessionRepository.findByClassEntityIdAndDate(targetClass.getId(), dto.getEffectiveDate()))
                .thenReturn(List.of()); // No session on date

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("NO_SESSION_ON_DATE");
    }

    @Test
    @DisplayName("Should reject transfer when quota exceeded")
    void testSubmitTransferRequest_QuotaExceeded() {
        // Arrange
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().plusDays(7))
                .requestReason("Valid reason with more than twenty characters")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(1L); // Already has 1 approved transfer

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("TRANSFER_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Should reject transfer when target class is full")
    void testSubmitTransferRequest_ClassFull() {
        // Arrange
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().plusDays(7))
                .requestReason("Valid reason with more than twenty characters")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(20); // Full capacity

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("INVALID_TRANSFER");
    }

    @Test
    @DisplayName("Should reject transfer when effective date is in the past")
    void testSubmitTransferRequest_PastEffectiveDate() {
        // Arrange
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().minusDays(1)) // Past date
                .requestReason("Valid reason with more than twenty characters")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(0L);

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PAST_EFFECTIVE_DATE");
    }

    // ============== TRANSFER QUOTA TESTS ==============

    @Test
    @DisplayName("Should return false when no transfers used")
    void testHasExceededTransferLimit_NoTransfers() {
        // Arrange
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(0L);

        // Act
        boolean result = studentRequestService.hasExceededTransferLimit(testStudent.getId(), testCourse.getId());

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when 1 transfer already used")
    void testHasExceededTransferLimit_OneTransfer() {
        // Arrange
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(1L);

        // Act
        boolean result = studentRequestService.hasExceededTransferLimit(testStudent.getId(), testCourse.getId());

        // Assert
        assertThat(result).isTrue();
    }

    // ============== TIER VALIDATION TESTS ==============

    @Test
    @DisplayName("Should require AA approval when branch is different")
    void testTierValidation_DifferentBranch() {
        // Arrange
        Branch differentBranch = Branch.builder().id(2L).name("North Branch").build();
        targetClass.setBranch(differentBranch);

        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().plusDays(7))
                .requestReason("Need to relocate to North area for family reasons")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(0L);
        when(sessionRepository.findByClassEntityIdAndDate(targetClass.getId(), dto.getEffectiveDate()))
                .thenReturn(List.of(effectiveSession));

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REQUIRES_AA_APPROVAL");
    }

    @Test
    @DisplayName("Should require AA approval when modality is different")
    void testTierValidation_DifferentModality() {
        // Arrange
        targetClass.setModality(Modality.ONLINE); // Different from OFFLINE

        TransferRequestDTO dto = TransferRequestDTO.builder()
                .currentClassId(currentClass.getId())
                .targetClassId(targetClass.getId())
                .effectiveDate(LocalDate.now().plusDays(7))
                .requestReason("Need to switch to online learning due to health issues")
                .build();

        when(studentRepository.findByUserAccountId(testUser.getId())).thenReturn(Optional.of(testStudent));
        when(userAccountRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(classRepository.findByIdWithCourse(currentClass.getId())).thenReturn(Optional.of(currentClass));
        when(classRepository.findByIdWithCourse(targetClass.getId())).thenReturn(Optional.of(targetClass));
        when(enrollmentRepository.findByStudentIdAndClassId(testStudent.getId(), currentClass.getId()))
                .thenReturn(currentEnrollment);
        when(classRepository.countEnrolledStudents(targetClass.getId())).thenReturn(15);
        when(studentRequestRepository.countByStudentIdAndRequestTypeAndStatusAndTargetClassCourseId(
                testStudent.getId(), StudentRequestType.TRANSFER, RequestStatus.APPROVED, testCourse.getId()))
                .thenReturn(0L);
        when(sessionRepository.findByClassEntityIdAndDate(targetClass.getId(), dto.getEffectiveDate()))
                .thenReturn(List.of(effectiveSession));

        // Act & Assert
        assertThatThrownBy(() -> studentRequestService.submitTransferRequest(testUser.getId(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("REQUIRES_AA_APPROVAL");
    }
}
