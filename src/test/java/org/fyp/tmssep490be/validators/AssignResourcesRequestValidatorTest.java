package org.fyp.tmssep490be.validators;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AssignResourcesRequestValidator
 * <p>
 * Tests validation logic for resource assignment requests including:
 * - Request structure validation
 * - Day of week format (PostgreSQL DOW: 0-6)
 * - Resource ID validity
 * - Duplicate day detection
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AssignResourcesRequestValidator Tests")
class AssignResourcesRequestValidatorTest {

    @Autowired
    private AssignResourcesRequestValidator validator;

    private AssignResourcesRequest validRequest;

    @BeforeEach
    void setUp() {
        // Create a valid request with Monday, Wednesday, Friday pattern
        validRequest = new AssignResourcesRequest();
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L), // Monday
                createAssignment((short) 3, 10L), // Wednesday
                createAssignment((short) 5, 10L)  // Friday
        ));
    }

    // ==================== isValid() Tests ====================

    @Test
    @DisplayName("Should validate request with valid pattern")
    void shouldValidateRequestWithValidPattern() {
        // When
        boolean result = validator.isValid(validRequest);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject null request")
    void shouldRejectNullRequest() {
        // When
        boolean result = validator.isValid(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject request with null pattern")
    void shouldRejectRequestWithNullPattern() {
        // Given
        validRequest.setPattern(null);

        // When
        boolean result = validator.isValid(validRequest);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject request with empty pattern")
    void shouldRejectRequestWithEmptyPattern() {
        // Given
        validRequest.setPattern(List.of());

        // When
        boolean result = validator.isValid(validRequest);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== isValidAssignment() Tests ====================

    @Test
    @DisplayName("Should validate assignment with valid day and resource")
    void shouldValidateAssignmentWithValidDayAndResource() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) 1, 10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject assignment with null day of week")
    void shouldRejectAssignmentWithNullDayOfWeek() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = new AssignResourcesRequest.ResourceAssignment();
        assignment.setDayOfWeek(null);
        assignment.setResourceId(10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject assignment with null resource ID")
    void shouldRejectAssignmentWithNullResourceId() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = new AssignResourcesRequest.ResourceAssignment();
        assignment.setDayOfWeek((short) 1);
        assignment.setResourceId(null);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject assignment with invalid day of week (negative)")
    void shouldRejectAssignmentWithNegativeDayOfWeek() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) -1, 10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject assignment with invalid day of week (greater than 6)")
    void shouldRejectAssignmentWithDayOfWeekGreaterThan6() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) 7, 10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should accept assignment with day 0 (Sunday)")
    void shouldAcceptAssignmentWithDay0() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) 0, 10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should accept assignment with day 6 (Saturday)")
    void shouldAcceptAssignmentWithDay6() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) 6, 10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject assignment with zero resource ID")
    void shouldRejectAssignmentWithZeroResourceId() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) 1, 0L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should reject assignment with negative resource ID")
    void shouldRejectAssignmentWithNegativeResourceId() {
        // Given
        AssignResourcesRequest.ResourceAssignment assignment = createAssignment((short) 1, -10L);

        // When
        boolean result = validator.isValidAssignment(assignment);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== hasDuplicateDays() Tests ====================

    @Test
    @DisplayName("Should detect no duplicates in valid pattern")
    void shouldDetectNoDuplicatesInValidPattern() {
        // When
        boolean result = validator.hasDuplicateDays(validRequest);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should detect duplicate days")
    void shouldDetectDuplicateDays() {
        // Given
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L), // Monday - Resource 10
                createAssignment((short) 1, 11L), // Monday - Resource 11 (DUPLICATE DAY)
                createAssignment((short) 3, 10L)  // Wednesday
        ));

        // When
        boolean result = validator.hasDuplicateDays(validRequest);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should handle null pattern in duplicate check")
    void shouldHandleNullPatternInDuplicateCheck() {
        // Given
        validRequest.setPattern(null);

        // When
        boolean result = validator.hasDuplicateDays(validRequest);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should handle empty pattern in duplicate check")
    void shouldHandleEmptyPatternInDuplicateCheck() {
        // Given
        validRequest.setPattern(List.of());

        // When
        boolean result = validator.hasDuplicateDays(validRequest);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== getDuplicateDay() Tests ====================

    @Test
    @DisplayName("Should return null for pattern with no duplicates")
    void shouldReturnNullForPatternWithNoDuplicates() {
        // When
        Short duplicateDay = validator.getDuplicateDay(validRequest);

        // Then
        assertThat(duplicateDay).isNull();
    }

    @Test
    @DisplayName("Should return duplicate day when found")
    void shouldReturnDuplicateDayWhenFound() {
        // Given
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L), // Monday
                createAssignment((short) 3, 10L), // Wednesday
                createAssignment((short) 1, 11L)  // Monday - DUPLICATE
        ));

        // When
        Short duplicateDay = validator.getDuplicateDay(validRequest);

        // Then
        assertThat(duplicateDay).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("Should return null for null pattern in getDuplicateDay")
    void shouldReturnNullForNullPatternInGetDuplicateDay() {
        // Given
        validRequest.setPattern(null);

        // When
        Short duplicateDay = validator.getDuplicateDay(validRequest);

        // Then
        assertThat(duplicateDay).isNull();
    }

    @Test
    @DisplayName("Should return null for single assignment in getDuplicateDay")
    void shouldReturnNullForSingleAssignmentInGetDuplicateDay() {
        // Given
        validRequest.setPattern(List.of(createAssignment((short) 1, 10L)));

        // When
        Short duplicateDay = validator.getDuplicateDay(validRequest);

        // Then
        assertThat(duplicateDay).isNull();
    }

    // ==================== getValidationErrors() Tests ====================

    @Test
    @DisplayName("Should return empty list for valid request")
    void shouldReturnEmptyListForValidRequest() {
        // When
        List<String> errors = validator.getValidationErrors(validRequest);

        // Then
        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("Should return error for invalid request")
    void shouldReturnErrorForInvalidRequest() {
        // Given
        validRequest.setPattern(null);

        // When
        List<String> errors = validator.getValidationErrors(validRequest);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Invalid resource assignments");
    }

    @Test
    @DisplayName("Should return error for duplicate days")
    void shouldReturnErrorForDuplicateDays() {
        // Given
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L),
                createAssignment((short) 1, 11L)
        ));

        // When
        List<String> errors = validator.getValidationErrors(validRequest);

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).contains("Duplicate day assignments found");
        assertThat(errors.get(0)).contains("Day 1");
    }

    @Test
    @DisplayName("Should return multiple errors when both validation and duplicate errors exist")
    void shouldReturnMultipleErrorsWhenBothValidationAndDuplicateErrorsExist() {
        // Given - Create invalid request with duplicates
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L),
                createAssignment((short) 7, 10L),  // Invalid day
                createAssignment((short) 1, 11L)   // Duplicate day
        ));

        // When
        List<String> errors = validator.getValidationErrors(validRequest);

        // Then
        assertThat(errors).hasSize(2);
        assertThat(errors).anyMatch(e -> e.contains("Invalid resource assignments"));
        assertThat(errors).anyMatch(e -> e.contains("Duplicate day assignments"));
    }

    // ==================== getValidAssignmentCount() Tests ====================

    @Test
    @DisplayName("Should count valid assignments correctly")
    void shouldCountValidAssignmentsCorrectly() {
        // When
        int count = validator.getValidAssignmentCount(validRequest);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should return zero for null pattern in getValidAssignmentCount")
    void shouldReturnZeroForNullPatternInGetValidAssignmentCount() {
        // Given
        validRequest.setPattern(null);

        // When
        int count = validator.getValidAssignmentCount(validRequest);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should count only valid assignments when mixed with invalid")
    void shouldCountOnlyValidAssignmentsWhenMixedWithInvalid() {
        // Given
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L),  // Valid
                createAssignment((short) 7, 10L),  // Invalid day
                createAssignment((short) 3, -1L),  // Invalid resource ID
                createAssignment((short) 5, 10L)   // Valid
        ));

        // When
        int count = validator.getValidAssignmentCount(validRequest);

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== getInvalidAssignmentCount() Tests ====================

    @Test
    @DisplayName("Should return zero invalid assignments for valid pattern")
    void shouldReturnZeroInvalidAssignmentsForValidPattern() {
        // When
        int count = validator.getInvalidAssignmentCount(validRequest);

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should count invalid assignments correctly")
    void shouldCountInvalidAssignmentsCorrectly() {
        // Given
        validRequest.setPattern(List.of(
                createAssignment((short) 1, 10L),  // Valid
                createAssignment((short) 7, 10L),  // Invalid day
                createAssignment((short) 3, -1L),  // Invalid resource ID
                createAssignment((short) 5, 10L)   // Valid
        ));

        // When
        int count = validator.getInvalidAssignmentCount(validRequest);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return zero for null pattern in getInvalidAssignmentCount")
    void shouldReturnZeroForNullPatternInGetInvalidAssignmentCount() {
        // Given
        validRequest.setPattern(null);

        // When
        int count = validator.getInvalidAssignmentCount(validRequest);

        // Then
        assertThat(count).isEqualTo(0);
    }

    // ==================== Helper Methods ====================

    /**
     * Create a ResourceAssignment with specified day and resource ID
     */
    private AssignResourcesRequest.ResourceAssignment createAssignment(Short dayOfWeek, Long resourceId) {
        AssignResourcesRequest.ResourceAssignment assignment = new AssignResourcesRequest.ResourceAssignment();
        assignment.setDayOfWeek(dayOfWeek);
        assignment.setResourceId(resourceId);
        return assignment;
    }
}
