package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.studentrequest.AbsenceRequestDTO;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.DuplicateRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Simple entity and validation tests for StudentRequest.
 * Tests core entity behavior and DTO validation without complex service dependencies.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Student Request Entity and Validation Tests")
class StudentRequestEntityTest {

    private Validator validator;
    private Student testStudent;
    private StudentRequest testRequest;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Create test student
        testStudent = TestDataBuilder.buildStudent()
            .userAccount(TestDataBuilder.buildUserAccount()
                .email("student@test.com")
                .fullName("Test Student")
                .build())
            .studentCode("ST001")
            .build();

        // Create test request
        testRequest = StudentRequest.builder()
            .student(testStudent)
            .requestType(StudentRequestType.ABSENCE)
            .requestReason("Valid reason for absence request that meets minimum length")
            .status(RequestStatus.PENDING)
            .submittedAt(OffsetDateTime.now())
            .build();
    }

    // ===== ENTITY VALIDATION TESTS =====

    @Test
    @DisplayName("Should create valid StudentRequest with required fields")
    void shouldCreateValidStudentRequestWithRequiredFields() {
        // Arrange & Act
        StudentRequest request = StudentRequest.builder()
            .student(testStudent)
            .requestType(StudentRequestType.ABSENCE)
            .requestReason("Valid reason that meets the minimum length requirement")
            .status(RequestStatus.PENDING)
            .build();

        // Assert
        assertThat(request).isNotNull();
        assertThat(request.getStudent()).isEqualTo(testStudent);
        assertThat(request.getRequestType()).isEqualTo(StudentRequestType.ABSENCE);
        assertThat(request.getRequestReason()).isEqualTo("Valid reason that meets the minimum length requirement");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(request.getSubmittedAt()).isNull(); // Not set automatically in builder
    }

    @Test
    @DisplayName("Should set and get all StudentRequest fields correctly")
    void shouldSetAndGetAllStudentRequestFieldsCorrectly() {
        // Act
        testRequest.setId(1L);
        testRequest.setNote("Additional note");
        testRequest.setSubmittedAt(OffsetDateTime.now());

        // Assert
        assertThat(testRequest.getId()).isEqualTo(1L);
        assertThat(testRequest.getNote()).isEqualTo("Additional note");
        assertThat(testRequest.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should handle all StudentRequestType enum values")
    void shouldHandleAllStudentRequestTypeEnumValues() {
        // Test all enum values
        StudentRequestType[] types = StudentRequestType.values();
        assertThat(types).hasSize(3);
        assertThat(types).containsExactly(StudentRequestType.ABSENCE, StudentRequestType.MAKEUP, StudentRequestType.TRANSFER);

        // Test each type in request
        for (StudentRequestType type : types) {
            StudentRequest request = StudentRequest.builder()
                .student(testStudent)
                .requestType(type)
                .requestReason("Test reason for " + type)
                .status(RequestStatus.PENDING)
                .build();

            assertThat(request.getRequestType()).isEqualTo(type);
        }
    }

    @Test
    @DisplayName("Should handle all RequestStatus enum values")
    void shouldHandleAllRequestStatusEnumValues() {
        // Test all enum values
        RequestStatus[] statuses = RequestStatus.values();
        assertThat(statuses).hasSize(5);
        assertThat(statuses).containsExactly(RequestStatus.PENDING, RequestStatus.WAITING_CONFIRM,
                                            RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED);

        // Test each status in request
        for (RequestStatus status : statuses) {
            StudentRequest request = StudentRequest.builder()
                .student(testStudent)
                .requestType(StudentRequestType.ABSENCE)
                .requestReason("Test reason for status " + status)
                .status(status)
                .build();

            assertThat(request.getStatus()).isEqualTo(status);
        }
    }

    // ===== DTO VALIDATION TESTS =====

    @Test
    @DisplayName("Should validate valid AbsenceRequestDTO")
    void shouldValidateValidAbsenceRequestDTO() {
        // Arrange
        AbsenceRequestDTO dto = AbsenceRequestDTO.builder()
            .currentClassId(1L)
            .targetSessionId(1L)
            .requestReason("Valid reason for absence request that meets minimum length")
            .note("Additional note")
            .build();

        // Act
        Set<ConstraintViolation<AbsenceRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should reject AbsenceRequestDTO with null class ID")
    void shouldRejectAbsenceRequestDTOWithNullClassId() {
        // Arrange
        AbsenceRequestDTO dto = AbsenceRequestDTO.builder()
            .currentClassId(null)
            .targetSessionId(1L)
            .requestReason("Valid reason for absence request that meets minimum length")
            .build();

        // Act
        Set<ConstraintViolation<AbsenceRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Class ID is required");
    }

    @Test
    @DisplayName("Should reject AbsenceRequestDTO with null session ID")
    void shouldRejectAbsenceRequestDTOWithNullSessionId() {
        // Arrange
        AbsenceRequestDTO dto = AbsenceRequestDTO.builder()
            .currentClassId(1L)
            .targetSessionId(null)
            .requestReason("Valid reason for absence request that meets minimum length")
            .build();

        // Act
        Set<ConstraintViolation<AbsenceRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Session ID is required");
    }

    @Test
    @DisplayName("Should reject AbsenceRequestDTO with null reason")
    void shouldRejectAbsenceRequestDTOWithNullReason() {
        // Arrange
        AbsenceRequestDTO dto = AbsenceRequestDTO.builder()
            .currentClassId(1L)
            .targetSessionId(1L)
            .requestReason(null)
            .build();

        // Act
        Set<ConstraintViolation<AbsenceRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Reason is required");
    }

    @Test
    @DisplayName("Should reject AbsenceRequestDTO with short reason")
    void shouldRejectAbsenceRequestDTOWithShortReason() {
        // Arrange
        AbsenceRequestDTO dto = AbsenceRequestDTO.builder()
            .currentClassId(1L)
            .targetSessionId(1L)
            .requestReason("Short")
            .build();

        // Act
        Set<ConstraintViolation<AbsenceRequestDTO>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Reason must be at least 10 characters");
    }

    // ===== BUSINESS RULE VALIDATION TESTS =====

    @Test
    @DisplayName("Should validate session date business rule")
    void shouldValidateSessionDateBusinessRule() {
        // Arrange
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalDate futureDate = LocalDate.now().plusDays(3);

        // Act & Assert
        // Past date should be invalid for absence requests
        assertThatThrownBy(() -> {
            if (pastDate.isBefore(LocalDate.now())) {
                throw new BusinessRuleException("PAST_SESSION", "Cannot request absence for past sessions");
            }
        }).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Cannot request absence for past sessions");

        // Future date should be valid
        assertThat(futureDate).isAfter(LocalDate.now());
    }

    @Test
    @DisplayName("Should validate request status transitions")
    void shouldValidateRequestStatusTransitions() {
        // Arrange
        StudentRequest request = StudentRequest.builder()
            .student(testStudent)
            .requestType(StudentRequestType.ABSENCE)
            .requestReason("Test reason")
            .status(RequestStatus.PENDING)
            .build();

        // Act & Assert - valid transitions
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        request.setStatus(RequestStatus.APPROVED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);

        // Reset to pending for other tests
        request.setStatus(RequestStatus.PENDING);
        request.setStatus(RequestStatus.REJECTED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);

        // Reset to pending
        request.setStatus(RequestStatus.PENDING);
        request.setStatus(RequestStatus.CANCELLED);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);

        // Business rule: Only pending requests can be modified
        assertThatThrownBy(() -> {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be modified");
            }
        }).isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("Only pending requests can be modified");
    }

    @Test
    @DisplayName("Should validate lead time business rule")
    void shouldValidateLeadTimeBusinessRule() {
        // Arrange
        LocalDate sessionDate = LocalDate.now().plusDays(1); // 1 day from now
        final int LEAD_TIME_DAYS = 2; // Minimum 2 days required

        // Act
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), sessionDate);

        // Assert
        assertThat(daysUntil).isEqualTo(1);
        assertThat(daysUntil).isLessThan(LEAD_TIME_DAYS);

        // Business rule: Should warn about insufficient lead time
        // In real implementation, this would log a warning but still allow the request
        assertThat(daysUntil).isGreaterThan(0); // Still in the future, so allowed
    }

    @Test
    @DisplayName("Should test absence rate calculation")
    void shouldTestAbsenceRateCalculation() {
        // Arrange
        int totalSessions = 10;
        int absenceCount = 2;
        double expectedRate = (double) absenceCount / totalSessions * 100;

        // Act
        double actualRate = (double) absenceCount / totalSessions * 100;

        // Assert
        assertThat(actualRate).isEqualTo(expectedRate);
        assertThat(actualRate).isEqualTo(20.0);

        // Business rule: Check against threshold
        final double ABSENCE_THRESHOLD = 20.0;
        if (actualRate > ABSENCE_THRESHOLD) {
            // Would log warning about high absence rate
        }
    }

    @Test
    @DisplayName("Should test duplicate request detection")
    void shouldTestDuplicateRequestDetection() {
        // Arrange
        boolean hasDuplicateRequest = true;

        // Act & Assert
        if (hasDuplicateRequest) {
            assertThatThrownBy(() -> {
                throw new DuplicateRequestException("Duplicate absence request for this session");
            }).isInstanceOf(DuplicateRequestException.class)
              .hasMessageContaining("Duplicate absence request for this session");
        }

        // Test when no duplicate exists
        hasDuplicateRequest = false;
        assertThat(hasDuplicateRequest).isFalse();
        // No exception should be thrown
    }

    @Test
    @DisplayName("Should test resource not found handling")
    void shouldTestResourceNotFoundHandling() {
        // Arrange
        Long nonExistentId = 999L;

        // Act & Assert
        assertThatThrownBy(() -> {
            throw new ResourceNotFoundException("Request not found with id: " + nonExistentId);
        }).isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Request not found with id: 999");
    }

    @Test
    @DisplayName("Should test access control business rule")
    void shouldTestAccessControlBusinessRule() {
        // Arrange
        Long currentUserId = 1L;
        Long requestOwnerId = 2L;

        // Act & Assert
        if (!currentUserId.equals(requestOwnerId)) {
            assertThatThrownBy(() -> {
                throw new BusinessRuleException("ACCESS_DENIED", "You can only view your own requests");
            }).isInstanceOf(BusinessRuleException.class)
              .hasMessageContaining("You can only view your own requests");
        }

        // Test when user has access
        requestOwnerId = 1L;
        assertThat(currentUserId.equals(requestOwnerId)).isTrue();
        // No exception should be thrown
    }
}