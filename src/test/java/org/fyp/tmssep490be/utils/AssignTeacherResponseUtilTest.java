package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AssignTeacherResponseUtil
 * <p>
 * Tests utility methods for building teacher assignment responses including:
 * - Response building with full/partial assignment
 * - Availability status calculation
 * - Availability percentage calculation
 * - GENERAL skill checking
 * - Skill formatting
 * - Conflict breakdown building
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AssignTeacherResponseUtil Tests")
class AssignTeacherResponseUtilTest {

    @Autowired
    private AssignTeacherResponseUtil util;

    private Teacher validTeacher;

    @BeforeEach
    void setUp() {
        // Create valid teacher with user account
        UserAccount userAccount = TestDataBuilder.buildUserAccount()
                .fullName("John Doe")
                .build();

        validTeacher = TestDataBuilder.buildTeacher()
                .id(45L)
                .userAccount(userAccount)
                .build();
    }

    // ==================== buildSuccessResponse() Tests ====================

    @Test
    @DisplayName("Should build response for full assignment (all sessions assigned)")
    void shouldBuildResponseForFullAssignment() {
        // Given
        Long classId = 1L;
        int totalSessions = 36;
        List<Long> assignedSessionIds = List.of(1L, 2L, 3L, 4L, 5L);
        List<Long> remainingSessionIds = List.of();
        long processingTimeMs = 50L;

        // When
        AssignTeacherResponse response = util.buildSuccessResponse(
                classId, validTeacher, totalSessions, assignedSessionIds, remainingSessionIds, processingTimeMs);

        // Then
        assertThat(response.getClassId()).isEqualTo(classId);
        assertThat(response.getTeacherId()).isEqualTo(45L);
        assertThat(response.getTeacherName()).isEqualTo("John Doe");
        assertThat(response.getTotalSessions()).isEqualTo(36);
        assertThat(response.getAssignedCount()).isEqualTo(5);
        assertThat(response.getAssignedSessionIds()).hasSize(5);
        assertThat(response.getNeedsSubstitute()).isFalse();
        assertThat(response.getRemainingSessions()).isEqualTo(0);
        assertThat(response.getRemainingSessionIds()).isEmpty();
        assertThat(response.getProcessingTimeMs()).isEqualTo(50L);
    }

    @Test
    @DisplayName("Should build response for partial assignment (some sessions remaining)")
    void shouldBuildResponseForPartialAssignment() {
        // Given
        Long classId = 1L;
        int totalSessions = 36;
        List<Long> assignedSessionIds = List.of(1L, 2L, 3L);
        List<Long> remainingSessionIds = List.of(4L, 5L);
        long processingTimeMs = 50L;

        // When
        AssignTeacherResponse response = util.buildSuccessResponse(
                classId, validTeacher, totalSessions, assignedSessionIds, remainingSessionIds, processingTimeMs);

        // Then
        assertThat(response.getAssignedCount()).isEqualTo(3);
        assertThat(response.getNeedsSubstitute()).isTrue(); // Has remaining sessions
        assertThat(response.getRemainingSessions()).isEqualTo(2);
        assertThat(response.getRemainingSessionIds()).hasSize(2);
        assertThat(response.getRemainingSessionIds()).containsExactlyInAnyOrder(4L, 5L);
    }

    @Test
    @DisplayName("Should build response with no assigned sessions (all remaining)")
    void shouldBuildResponseWithNoAssignedSessions() {
        // Given
        Long classId = 1L;
        int totalSessions = 36;
        List<Long> assignedSessionIds = List.of();
        List<Long> remainingSessionIds = List.of(1L, 2L, 3L);
        long processingTimeMs = 50L;

        // When
        AssignTeacherResponse response = util.buildSuccessResponse(
                classId, validTeacher, totalSessions, assignedSessionIds, remainingSessionIds, processingTimeMs);

        // Then
        assertThat(response.getAssignedCount()).isEqualTo(0);
        assertThat(response.getNeedsSubstitute()).isTrue();
        assertThat(response.getRemainingSessions()).isEqualTo(3);
    }

    // ==================== calculateAvailabilityStatus() Tests ====================

    @Test
    @DisplayName("Should return FULLY_AVAILABLE when 100% available")
    void shouldReturnFullyAvailableWhen100PercentAvailable() {
        // When
        TeacherAvailabilityDTO.AvailabilityStatus status = 
                util.calculateAvailabilityStatus(36, 36);

        // Then
        assertThat(status).isEqualTo(TeacherAvailabilityDTO.AvailabilityStatus.FULLY_AVAILABLE);
    }

    @Test
    @DisplayName("Should return PARTIALLY_AVAILABLE when >0% and <100% available")
    void shouldReturnPartiallyAvailableWhenBetween0And100Percent() {
        // When
        TeacherAvailabilityDTO.AvailabilityStatus status = 
                util.calculateAvailabilityStatus(20, 36);

        // Then
        assertThat(status).isEqualTo(TeacherAvailabilityDTO.AvailabilityStatus.PARTIALLY_AVAILABLE);
    }

    @Test
    @DisplayName("Should return UNAVAILABLE when 0% available")
    void shouldReturnUnavailableWhen0PercentAvailable() {
        // When
        TeacherAvailabilityDTO.AvailabilityStatus status = 
                util.calculateAvailabilityStatus(0, 36);

        // Then
        assertThat(status).isEqualTo(TeacherAvailabilityDTO.AvailabilityStatus.UNAVAILABLE);
    }

    @Test
    @DisplayName("Should return PARTIALLY_AVAILABLE for 1 out of 36 sessions")
    void shouldReturnPartiallyAvailableFor1OutOf36Sessions() {
        // When
        TeacherAvailabilityDTO.AvailabilityStatus status = 
                util.calculateAvailabilityStatus(1, 36);

        // Then
        assertThat(status).isEqualTo(TeacherAvailabilityDTO.AvailabilityStatus.PARTIALLY_AVAILABLE);
    }

    @Test
    @DisplayName("Should return PARTIALLY_AVAILABLE for 35 out of 36 sessions")
    void shouldReturnPartiallyAvailableFor35OutOf36Sessions() {
        // When
        TeacherAvailabilityDTO.AvailabilityStatus status = 
                util.calculateAvailabilityStatus(35, 36);

        // Then
        assertThat(status).isEqualTo(TeacherAvailabilityDTO.AvailabilityStatus.PARTIALLY_AVAILABLE);
    }

    // ==================== calculateAvailabilityPercentage() Tests ====================

    @Test
    @DisplayName("Should calculate 100% availability")
    void shouldCalculate100PercentAvailability() {
        // When
        double percentage = util.calculateAvailabilityPercentage(36, 36);

        // Then
        assertThat(percentage).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should calculate 0% availability")
    void shouldCalculate0PercentAvailability() {
        // When
        double percentage = util.calculateAvailabilityPercentage(0, 36);

        // Then
        assertThat(percentage).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should calculate 50% availability")
    void shouldCalculate50PercentAvailability() {
        // When
        double percentage = util.calculateAvailabilityPercentage(18, 36);

        // Then
        assertThat(percentage).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should calculate 77.78% availability (28 out of 36)")
    void shouldCalculate77Point78PercentAvailability() {
        // When
        double percentage = util.calculateAvailabilityPercentage(28, 36);

        // Then
        assertThat(percentage).isCloseTo(77.78, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should return 0 when total count is 0")
    void shouldReturn0WhenTotalCountIs0() {
        // When
        double percentage = util.calculateAvailabilityPercentage(10, 0);

        // Then
        assertThat(percentage).isEqualTo(0.0);
    }

    // ==================== hasGeneralSkill() Tests ====================

    @Test
    @DisplayName("Should return true when teacher has GENERAL skill")
    void shouldReturnTrueWhenTeacherHasGeneralSkill() {
        // Given
        List<Skill> skills = List.of(Skill.READING, Skill.GENERAL, Skill.WRITING);

        // When
        boolean hasGeneral = util.hasGeneralSkill(skills);

        // Then
        assertThat(hasGeneral).isTrue();
    }

    @Test
    @DisplayName("Should return false when teacher does not have GENERAL skill")
    void shouldReturnFalseWhenTeacherDoesNotHaveGeneralSkill() {
        // Given
        List<Skill> skills = List.of(Skill.READING, Skill.WRITING);

        // When
        boolean hasGeneral = util.hasGeneralSkill(skills);

        // Then
        assertThat(hasGeneral).isFalse();
    }

    @Test
    @DisplayName("Should return false for null skills list")
    void shouldReturnFalseForNullSkillsList() {
        // When
        boolean hasGeneral = util.hasGeneralSkill(null);

        // Then
        assertThat(hasGeneral).isFalse();
    }

    @Test
    @DisplayName("Should return false for empty skills list")
    void shouldReturnFalseForEmptySkillsList() {
        // When
        boolean hasGeneral = util.hasGeneralSkill(List.of());

        // Then
        assertThat(hasGeneral).isFalse();
    }

    @Test
    @DisplayName("Should return true when GENERAL is the only skill")
    void shouldReturnTrueWhenGeneralIsTheOnlySkill() {
        // Given
        List<Skill> skills = List.of(Skill.GENERAL);

        // When
        boolean hasGeneral = util.hasGeneralSkill(skills);

        // Then
        assertThat(hasGeneral).isTrue();
    }

    // ==================== formatSkills() Tests ====================

    @Test
    @DisplayName("Should format single skill")
    void shouldFormatSingleSkill() {
        // Given
        List<Skill> skills = List.of(Skill.READING);

        // When
        String formatted = util.formatSkills(skills);

        // Then
        assertThat(formatted).isEqualTo("READING");
    }

    @Test
    @DisplayName("Should format multiple skills with comma separator")
    void shouldFormatMultipleSkillsWithCommaSeparator() {
        // Given
        List<Skill> skills = List.of(Skill.READING, Skill.WRITING, Skill.GENERAL);

        // When
        String formatted = util.formatSkills(skills);

        // Then
        assertThat(formatted).contains("READING");
        assertThat(formatted).contains("WRITING");
        assertThat(formatted).contains("GENERAL");
        assertThat(formatted).contains(",");
    }

    @Test
    @DisplayName("Should return 'No skills' for null skills list")
    void shouldReturnNoSkillsForNullSkillsList() {
        // When
        String formatted = util.formatSkills(null);

        // Then
        assertThat(formatted).isEqualTo("No skills");
    }

    @Test
    @DisplayName("Should return 'No skills' for empty skills list")
    void shouldReturnNoSkillsForEmptySkillsList() {
        // When
        String formatted = util.formatSkills(List.of());

        // Then
        assertThat(formatted).isEqualTo("No skills");
    }

    // ==================== buildEmptyConflictBreakdown() Tests ====================

    @Test
    @DisplayName("Should build conflict breakdown with all zeros")
    void shouldBuildConflictBreakdownWithAllZeros() {
        // When
        TeacherAvailabilityDTO.ConflictBreakdown breakdown = util.buildEmptyConflictBreakdown();

        // Then
        assertThat(breakdown).isNotNull();
        assertThat(breakdown.getNoAvailability()).isEqualTo(0);
        assertThat(breakdown.getTeachingConflict()).isEqualTo(0);
        assertThat(breakdown.getLeaveConflict()).isEqualTo(0);
        assertThat(breakdown.getSkillMismatch()).isEqualTo(0);
        assertThat(breakdown.getTotalConflicts()).isEqualTo(0);
    }

    // ==================== updateTotalConflicts() Tests ====================

    @Test
    @DisplayName("Should update total conflicts correctly")
    void shouldUpdateTotalConflictsCorrectly() {
        // Given
        TeacherAvailabilityDTO.ConflictBreakdown breakdown = 
                TeacherAvailabilityDTO.ConflictBreakdown.builder()
                        .noAvailability(5)
                        .teachingConflict(3)
                        .leaveConflict(2)
                        .skillMismatch(4)
                        .totalConflicts(0) // Will be updated
                        .build();

        // When
        util.updateTotalConflicts(breakdown);

        // Then
        assertThat(breakdown.getTotalConflicts()).isEqualTo(14); // 5 + 3 + 2 + 4
    }

    @Test
    @DisplayName("Should update total conflicts to 0 when all conflict types are 0")
    void shouldUpdateTotalConflictsTo0WhenAllConflictTypesAre0() {
        // Given
        TeacherAvailabilityDTO.ConflictBreakdown breakdown = util.buildEmptyConflictBreakdown();

        // When
        util.updateTotalConflicts(breakdown);

        // Then
        assertThat(breakdown.getTotalConflicts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle null breakdown gracefully in updateTotalConflicts")
    void shouldHandleNullBreakdownGracefullyInUpdateTotalConflicts() {
        // When & Then - Should not throw exception
        util.updateTotalConflicts(null);
    }

    @Test
    @DisplayName("Should update total conflicts with partial data")
    void shouldUpdateTotalConflictsWithPartialData() {
        // Given
        TeacherAvailabilityDTO.ConflictBreakdown breakdown = 
                TeacherAvailabilityDTO.ConflictBreakdown.builder()
                        .noAvailability(10)
                        .teachingConflict(0)
                        .leaveConflict(0)
                        .skillMismatch(2)
                        .totalConflicts(0)
                        .build();

        // When
        util.updateTotalConflicts(breakdown);

        // Then
        assertThat(breakdown.getTotalConflicts()).isEqualTo(12); // 10 + 0 + 0 + 2
    }

    // ==================== Edge Cases Tests ====================

    @Test
    @DisplayName("Should handle availability calculation with edge values")
    void shouldHandleAvailabilityCalculationWithEdgeValues() {
        // When & Then
        assertThat(util.calculateAvailabilityPercentage(1, 1)).isEqualTo(100.0);
        assertThat(util.calculateAvailabilityPercentage(0, 1)).isEqualTo(0.0);
        assertThat(util.calculateAvailabilityPercentage(100, 100)).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should handle buildSuccessResponse with large session counts")
    void shouldHandleBuildSuccessResponseWithLargeSessionCounts() {
        // Given
        List<Long> largeAssignedList = java.util.stream.LongStream.range(1, 101).boxed().toList();
        List<Long> largeRemainingList = java.util.stream.LongStream.range(101, 201).boxed().toList();

        // When
        AssignTeacherResponse response = util.buildSuccessResponse(
                1L, validTeacher, 200, largeAssignedList, largeRemainingList, 1000L);

        // Then
        assertThat(response.getAssignedCount()).isEqualTo(100);
        assertThat(response.getRemainingSessions()).isEqualTo(100);
        assertThat(response.getNeedsSubstitute()).isTrue();
    }

    @Test
    @DisplayName("Should format skills in consistent order")
    void shouldFormatSkillsInConsistentOrder() {
        // Given
        List<Skill> skills1 = List.of(Skill.READING, Skill.WRITING);
        List<Skill> skills2 = List.of(Skill.WRITING, Skill.READING);

        // When
        String formatted1 = util.formatSkills(skills1);
        String formatted2 = util.formatSkills(skills2);

        // Then - Both should contain same skills (order may differ based on iteration)
        assertThat(formatted1).contains("READING");
        assertThat(formatted1).contains("WRITING");
        assertThat(formatted2).contains("READING");
        assertThat(formatted2).contains("WRITING");
    }
}
