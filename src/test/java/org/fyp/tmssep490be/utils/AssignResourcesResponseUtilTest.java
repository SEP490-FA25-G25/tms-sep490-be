package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse.ConflictType.*;

/**
 * Unit tests for AssignResourcesResponseUtil
 * <p>
 * Tests utility methods for processing resource assignment responses including:
 * - Success/failure checking
 * - Progress and conflict rate calculation
 * - Conflict grouping and analysis
 * - Performance evaluation
 * - Summary generation
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AssignResourcesResponseUtil Tests")
class AssignResourcesResponseUtilTest {

    @Autowired
    private AssignResourcesResponseUtil util;

    private AssignResourcesResponse fullSuccessResponse;
    private AssignResourcesResponse partialSuccessResponse;
    private AssignResourcesResponse noSuccessResponse;

    @BeforeEach
    void setUp() {
        // Full success response (no conflicts)
        fullSuccessResponse = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(36)
                .conflictCount(0)
                .conflicts(List.of())
                .processingTimeMs(150L)
                .build();

        // Partial success response (with conflicts)
        partialSuccessResponse = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(30)
                .conflictCount(6)
                .conflicts(List.of(
                        createConflict(1L, (short) 1, CLASS_BOOKING, "Conflict with Class B"),
                        createConflict(2L, (short) 1, CLASS_BOOKING, "Conflict with Class C"),
                        createConflict(3L, (short) 3, MAINTENANCE, "Resource under maintenance"),
                        createConflict(4L, (short) 3, INSUFFICIENT_CAPACITY, "Capacity 20 < Required 25"),
                        createConflict(5L, (short) 5, UNAVAILABLE, "Resource unavailable"),
                        createConflict(6L, (short) 5, CLASS_BOOKING, "Conflict with Class D")
                ))
                .processingTimeMs(180L)
                .build();

        // No success response (all conflicts)
        noSuccessResponse = AssignResourcesResponse.builder()
                .totalSessions(12)
                .successCount(0)
                .conflictCount(12)
                .conflicts(List.of(
                        createConflict(1L, (short) 1, CLASS_BOOKING, "All sessions conflict")
                ))
                .processingTimeMs(50L)
                .build();
    }

    // ==================== isFullySuccessful() Tests ====================

    @Test
    @DisplayName("Should return true for fully successful assignment")
    void shouldReturnTrueForFullySuccessfulAssignment() {
        // When
        boolean result = util.isFullySuccessful(fullSuccessResponse);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for partial success assignment")
    void shouldReturnFalseForPartialSuccessAssignment() {
        // When
        boolean result = util.isFullySuccessful(partialSuccessResponse);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for no success assignment")
    void shouldReturnFalseForNoSuccessAssignment() {
        // When
        boolean result = util.isFullySuccessful(noSuccessResponse);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return false for null response")
    void shouldReturnFalseForNullResponse() {
        // When
        boolean result = util.isFullySuccessful(null);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== hasConflicts() Tests ====================

    @Test
    @DisplayName("Should return false when no conflicts")
    void shouldReturnFalseWhenNoConflicts() {
        // When
        boolean result = util.hasConflicts(fullSuccessResponse);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should return true when conflicts exist")
    void shouldReturnTrueWhenConflictsExist() {
        // When
        boolean result = util.hasConflicts(partialSuccessResponse);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for null response in hasConflicts")
    void shouldReturnFalseForNullResponseInHasConflicts() {
        // When
        boolean result = util.hasConflicts(null);

        // Then
        assertThat(result).isFalse();
    }

    // ==================== getAssignmentProgress() Tests ====================

    @Test
    @DisplayName("Should calculate 100% progress for full success")
    void shouldCalculate100PercentProgressForFullSuccess() {
        // When
        double progress = util.getAssignmentProgress(fullSuccessResponse);

        // Then
        assertThat(progress).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should calculate correct progress for partial success")
    void shouldCalculateCorrectProgressForPartialSuccess() {
        // When
        double progress = util.getAssignmentProgress(partialSuccessResponse);

        // Then
        assertThat(progress).isCloseTo(83.33, within(0.01)); // 30/36 * 100
    }

    @Test
    @DisplayName("Should return 0 progress for no success")
    void shouldReturn0ProgressForNoSuccess() {
        // When
        double progress = util.getAssignmentProgress(noSuccessResponse);

        // Then
        assertThat(progress).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return 0 progress for null response")
    void shouldReturn0ProgressForNullResponse() {
        // When
        double progress = util.getAssignmentProgress(null);

        // Then
        assertThat(progress).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should return 0 progress when total sessions is 0")
    void shouldReturn0ProgressWhenTotalSessionsIs0() {
        // Given
        AssignResourcesResponse response = AssignResourcesResponse.builder()
                .totalSessions(0)
                .successCount(0)
                .conflictCount(0)
                .build();

        // When
        double progress = util.getAssignmentProgress(response);

        // Then
        assertThat(progress).isEqualTo(0.0);
    }

    // ==================== getConflictRate() Tests ====================

    @Test
    @DisplayName("Should calculate 0% conflict rate for full success")
    void shouldCalculate0PercentConflictRateForFullSuccess() {
        // When
        double conflictRate = util.getConflictRate(fullSuccessResponse);

        // Then
        assertThat(conflictRate).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate correct conflict rate for partial success")
    void shouldCalculateCorrectConflictRateForPartialSuccess() {
        // When
        double conflictRate = util.getConflictRate(partialSuccessResponse);

        // Then
        assertThat(conflictRate).isCloseTo(16.67, within(0.01)); // 6/36 * 100
    }

    @Test
    @DisplayName("Should calculate 100% conflict rate for no success")
    void shouldCalculate100PercentConflictRateForNoSuccess() {
        // When
        double conflictRate = util.getConflictRate(noSuccessResponse);

        // Then
        assertThat(conflictRate).isEqualTo(100.0);
    }

    // ==================== getConflictsByType() Tests ====================

    @Test
    @DisplayName("Should return empty map for no conflicts")
    void shouldReturnEmptyMapForNoConflicts() {
        // When
        Map<AssignResourcesResponse.ConflictType, Long> conflictsByType = 
                util.getConflictsByType(fullSuccessResponse);

        // Then
        assertThat(conflictsByType).isEmpty();
    }

    @Test
    @DisplayName("Should group conflicts by type correctly")
    void shouldGroupConflictsByTypeCorrectly() {
        // When
        Map<AssignResourcesResponse.ConflictType, Long> conflictsByType = 
                util.getConflictsByType(partialSuccessResponse);

        // Then
        assertThat(conflictsByType).hasSize(4);
        assertThat(conflictsByType.get(CLASS_BOOKING)).isEqualTo(3);
        assertThat(conflictsByType.get(MAINTENANCE)).isEqualTo(1);
        assertThat(conflictsByType.get(INSUFFICIENT_CAPACITY)).isEqualTo(1);
        assertThat(conflictsByType.get(UNAVAILABLE)).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return empty map for null response in getConflictsByType")
    void shouldReturnEmptyMapForNullResponseInGetConflictsByType() {
        // When
        Map<AssignResourcesResponse.ConflictType, Long> conflictsByType = 
                util.getConflictsByType(null);

        // Then
        assertThat(conflictsByType).isEmpty();
    }

    // ==================== getConflictsByDay() Tests ====================

    @Test
    @DisplayName("Should return empty map for no conflicts in getConflictsByDay")
    void shouldReturnEmptyMapForNoConflictsInGetConflictsByDay() {
        // When
        Map<Short, Long> conflictsByDay = util.getConflictsByDay(fullSuccessResponse);

        // Then
        assertThat(conflictsByDay).isEmpty();
    }

    @Test
    @DisplayName("Should group conflicts by day correctly")
    void shouldGroupConflictsByDayCorrectly() {
        // When
        Map<Short, Long> conflictsByDay = util.getConflictsByDay(partialSuccessResponse);

        // Then
        assertThat(conflictsByDay).hasSize(3);
        assertThat(conflictsByDay.get((short) 1)).isEqualTo(2); // Monday: 2 conflicts
        assertThat(conflictsByDay.get((short) 3)).isEqualTo(2); // Wednesday: 2 conflicts
        assertThat(conflictsByDay.get((short) 5)).isEqualTo(2); // Friday: 2 conflicts
    }

    // ==================== getClassBookingConflicts() Tests ====================

    @Test
    @DisplayName("Should return empty list for no class booking conflicts")
    void shouldReturnEmptyListForNoClassBookingConflicts() {
        // When
        List<Long> conflicts = util.getClassBookingConflicts(fullSuccessResponse);

        // Then
        assertThat(conflicts).isEmpty();
    }

    @Test
    @DisplayName("Should return session IDs with class booking conflicts")
    void shouldReturnSessionIdsWithClassBookingConflicts() {
        // When
        List<Long> conflicts = util.getClassBookingConflicts(partialSuccessResponse);

        // Then
        assertThat(conflicts).hasSize(3);
        assertThat(conflicts).containsExactlyInAnyOrder(1L, 2L, 6L);
    }

    // ==================== getAssignmentSummary() Tests ====================

    @Test
    @DisplayName("Should generate summary for full success")
    void shouldGenerateSummaryForFullSuccess() {
        // When
        String summary = util.getAssignmentSummary(fullSuccessResponse);

        // Then
        assertThat(summary).contains("All 36 sessions assigned successfully");
        assertThat(summary).contains("100%");
        assertThat(summary).contains("150ms");
    }

    @Test
    @DisplayName("Should generate summary for partial success")
    void shouldGenerateSummaryForPartialSuccess() {
        // When
        String summary = util.getAssignmentSummary(partialSuccessResponse);

        // Then
        assertThat(summary).contains("30/36 sessions assigned");
        assertThat(summary).contains("83.3%");
        assertThat(summary).contains("6 conflicts");
        assertThat(summary).contains("180ms");
    }

    @Test
    @DisplayName("Should return default message for null response in summary")
    void shouldReturnDefaultMessageForNullResponseInSummary() {
        // When
        String summary = util.getAssignmentSummary(null);

        // Then
        assertThat(summary).isEqualTo("No assignment data available");
    }

    // ==================== getConflictSummary() Tests ====================

    @Test
    @DisplayName("Should return no conflicts message for full success")
    void shouldReturnNoConflictsMessageForFullSuccess() {
        // When
        String summary = util.getConflictSummary(fullSuccessResponse);

        // Then
        assertThat(summary).isEqualTo("No conflicts");
    }

    @Test
    @DisplayName("Should generate conflict summary with counts")
    void shouldGenerateConflictSummaryWithCounts() {
        // When
        String summary = util.getConflictSummary(partialSuccessResponse);

        // Then
        assertThat(summary).contains("CLASS_BOOKING: 3");
        assertThat(summary).contains("MAINTENANCE: 1");
        assertThat(summary).contains("INSUFFICIENT_CAPACITY: 1");
        assertThat(summary).contains("UNAVAILABLE: 1");
    }

    // ==================== meetsPerformanceTarget() Tests ====================

    @Test
    @DisplayName("Should return true when performance meets target (<200ms)")
    void shouldReturnTrueWhenPerformanceMeetsTarget() {
        // When
        boolean meetsTarget = util.meetsPerformanceTarget(fullSuccessResponse);

        // Then
        assertThat(meetsTarget).isTrue();
    }

    @Test
    @DisplayName("Should return false when performance exceeds target (>=200ms)")
    void shouldReturnFalseWhenPerformanceExceedsTarget() {
        // Given
        AssignResourcesResponse slowResponse = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(36)
                .conflictCount(0)
                .processingTimeMs(250L) // > 200ms
                .build();

        // When
        boolean meetsTarget = util.meetsPerformanceTarget(slowResponse);

        // Then
        assertThat(meetsTarget).isFalse();
    }

    @Test
    @DisplayName("Should return false for null response in performance check")
    void shouldReturnFalseForNullResponseInPerformanceCheck() {
        // When
        boolean meetsTarget = util.meetsPerformanceTarget(null);

        // Then
        assertThat(meetsTarget).isFalse();
    }

    // ==================== getPerformanceStatus() Tests ====================

    @Test
    @DisplayName("Should return success status when performance meets target")
    void shouldReturnSuccessStatusWhenPerformanceMeetsTarget() {
        // When
        String status = util.getPerformanceStatus(fullSuccessResponse);

        // Then
        assertThat(status).contains("✅ Performance target met");
        assertThat(status).contains("150ms < 200ms");
    }

    @Test
    @DisplayName("Should return warning status when performance exceeds target")
    void shouldReturnWarningStatusWhenPerformanceExceedsTarget() {
        // Given
        AssignResourcesResponse slowResponse = AssignResourcesResponse.builder()
                .processingTimeMs(250L)
                .build();

        // When
        String status = util.getPerformanceStatus(slowResponse);

        // Then
        assertThat(status).contains("⚠️ Performance target exceeded");
        assertThat(status).contains("250ms > 200ms");
    }

    @Test
    @DisplayName("Should return unavailable message for null response in performance status")
    void shouldReturnUnavailableMessageForNullResponseInPerformanceStatus() {
        // When
        String status = util.getPerformanceStatus(null);

        // Then
        assertThat(status).isEqualTo("Performance data unavailable");
    }

    // ==================== isReadyForNextStep() Tests ====================

    @Test
    @DisplayName("Should return true when ready for next step (full success)")
    void shouldReturnTrueWhenReadyForNextStepFullSuccess() {
        // When
        boolean ready = util.isReadyForNextStep(fullSuccessResponse);

        // Then
        assertThat(ready).isTrue();
    }

    @Test
    @DisplayName("Should return true when ready for next step (partial success)")
    void shouldReturnTrueWhenReadyForNextStepPartialSuccess() {
        // When
        boolean ready = util.isReadyForNextStep(partialSuccessResponse);

        // Then
        assertThat(ready).isTrue();
    }

    @Test
    @DisplayName("Should return false when not ready (no success)")
    void shouldReturnFalseWhenNotReady() {
        // When
        boolean ready = util.isReadyForNextStep(noSuccessResponse);

        // Then
        assertThat(ready).isFalse();
    }

    @Test
    @DisplayName("Should return false for null response in isReadyForNextStep")
    void shouldReturnFalseForNullResponseInIsReadyForNextStep() {
        // When
        boolean ready = util.isReadyForNextStep(null);

        // Then
        assertThat(ready).isFalse();
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Should handle response with null conflicts list")
    void shouldHandleResponseWithNullConflictsList() {
        // Given
        AssignResourcesResponse response = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(36)
                .conflictCount(0)
                .conflicts(null) // Null conflicts
                .processingTimeMs(150L)
                .build();

        // When & Then
        assertThat(util.getConflictsByType(response)).isEmpty();
        assertThat(util.getConflictsByDay(response)).isEmpty();
        assertThat(util.getClassBookingConflicts(response)).isEmpty();
        assertThat(util.getConflictSummary(response)).isEqualTo("No conflicts");
    }

    @Test
    @DisplayName("Should handle response with null processing time")
    void shouldHandleResponseWithNullProcessingTime() {
        // Given
        AssignResourcesResponse response = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(36)
                .conflictCount(0)
                .processingTimeMs(null)
                .build();

        // When & Then
        assertThat(util.meetsPerformanceTarget(response)).isFalse();
        assertThat(util.getPerformanceStatus(response)).isEqualTo("Performance data unavailable");
    }

    // ==================== Helper Methods ====================

    /**
     * Create a ResourceConflictDetail for testing
     */
    private AssignResourcesResponse.ResourceConflictDetail createConflict(
            Long sessionId, Short dayOfWeek, AssignResourcesResponse.ConflictType type, String reason) {
        return AssignResourcesResponse.ResourceConflictDetail.builder()
                .sessionId(sessionId)
                .date(LocalDate.now().plusDays(sessionId))
                .dayOfWeek(dayOfWeek)
                .conflictType(type)
                .conflictReason(reason)
                .build();
    }
}
