package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.TeacherAssignmentService;
import org.fyp.tmssep490be.utils.AssignTeacherResponseUtil;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TeacherAssignmentServiceImpl
 * <p>
 * Tests PRE-CHECK approach:
 * - Complex CTE query execution
 * - Object[] to DTO mapping (11 fields)
 * - Type conversions (BigInteger, BigDecimal)
 * - Full vs Partial assignment modes
 * - GENERAL skill bypass logic
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TeacherAssignmentService Unit Tests")
class TeacherAssignmentServiceImplTest {

    @Autowired
    private TeacherAssignmentService teacherAssignmentService;

    @MockitoBean
    private TeacherRepository teacherRepository;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private TeachingSlotRepository teachingSlotRepository;

    @MockitoBean
    private AssignTeacherResponseUtil responseUtil;

    // ==================== PRE-CHECK QUERY EXECUTION TESTS ====================

    @Test
    @DisplayName("Should execute PRE-CHECK query and map results correctly")
    void shouldExecutePrecheckQueryAndMapResults() {
        // Given
        Long classId = 1L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .build();

        // Simulate SQL result with BigInteger/BigDecimal (as returned by PostgreSQL)
        Object[] row1 = new Object[]{
                BigInteger.valueOf(45L),              // teacher_id
                "John Doe",                           // full_name
                "john@example.com",                   // email
                "READING,WRITING",                    // skills
                false,                                // has_general_skill
                BigDecimal.valueOf(36),              // total_sessions
                BigDecimal.valueOf(36),              // available_sessions
                BigDecimal.ZERO,                     // no_availability_count
                BigDecimal.ZERO,                     // teaching_conflict_count
                BigDecimal.ZERO,                     // leave_conflict_count
                BigDecimal.ZERO                      // skill_mismatch_count
        };

        Object[] row2 = new Object[]{
                BigInteger.valueOf(46L),
                "Jane Smith",
                "jane@example.com",
                "SPEAKING,GENERAL",                   // has GENERAL skill
                true,                                 // has GENERAL skill
                BigDecimal.valueOf(36),
                BigDecimal.valueOf(28),               // 28/36 available
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(2),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(2)
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        
        List<Object[]> teacherRows = new ArrayList<>();
        teacherRows.add(row1);
        teacherRows.add(row2);
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(teacherRows);

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers).hasSize(2);

        // Verify first teacher (fully available)
        TeacherAvailabilityDTO teacher1 = teachers.get(0);
        assertThat(teacher1.getTeacherId()).isEqualTo(45L);
        assertThat(teacher1.getFullName()).isEqualTo("John Doe");
        assertThat(teacher1.getEmail()).isEqualTo("john@example.com");
        assertThat(teacher1.getSkills()).containsExactlyInAnyOrder(Skill.READING, Skill.WRITING);
        assertThat(teacher1.getHasGeneralSkill()).isFalse();
        assertThat(teacher1.getTotalSessions()).isEqualTo(36);
        assertThat(teacher1.getAvailableSessions()).isEqualTo(36);
        assertThat(teacher1.getConflicts().getTotalConflicts()).isEqualTo(0);

        // Verify second teacher (partially available with GENERAL skill)
        TeacherAvailabilityDTO teacher2 = teachers.get(1);
        assertThat(teacher2.getTeacherId()).isEqualTo(46L);
        assertThat(teacher2.getFullName()).isEqualTo("Jane Smith");
        assertThat(teacher2.getSkills()).containsExactlyInAnyOrder(Skill.SPEAKING, Skill.GENERAL);
        assertThat(teacher2.getHasGeneralSkill()).isTrue();
        assertThat(teacher2.getTotalSessions()).isEqualTo(36);
        assertThat(teacher2.getAvailableSessions()).isEqualTo(28);
        assertThat(teacher2.getConflicts().getNoAvailability()).isEqualTo(3);
        assertThat(teacher2.getConflicts().getTeachingConflict()).isEqualTo(2);
        assertThat(teacher2.getConflicts().getLeaveConflict()).isEqualTo(1);
        assertThat(teacher2.getConflicts().getSkillMismatch()).isEqualTo(2);
        assertThat(teacher2.getConflicts().getTotalConflicts()).isEqualTo(8);

        verify(teacherRepository).findAvailableTeachersWithPrecheck(classId);
    }

    @Test
    @DisplayName("Should handle empty PRE-CHECK results")
    void shouldHandleEmptyPrecheckResults() {
        // Given
        Long classId = 1L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .build();

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(Collections.emptyList());

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers).isEmpty();
    }

    @Test
    @DisplayName("Should throw exception when class not found for PRE-CHECK query")
    void shouldThrowExceptionWhenClassNotFoundForPrecheck() {
        // Given
        Long classId = 999L;

        when(classRepository.findById(classId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLASS_NOT_FOUND);
    }

    // ==================== OBJECT[] TO DTO MAPPING TESTS ====================

    @Test
    @DisplayName("Should map all 11 fields from Object[] to DTO correctly")
    void shouldMapAllFieldsCorrectly() {
        // Given
        Long classId = 1L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .build();

        // Full field mapping test
        Object[] row = new Object[]{
                BigInteger.valueOf(100L),             // teacher_id
                "Test Teacher",                       // full_name
                "test@teacher.com",                   // email
                "READING,WRITING,SPEAKING",           // skills
                false,                                // has_general_skill
                BigDecimal.valueOf(72),              // total_sessions
                BigDecimal.valueOf(50),              // available_sessions
                BigDecimal.valueOf(10),              // no_availability_count
                BigDecimal.valueOf(5),               // teaching_conflict_count
                BigDecimal.valueOf(4),               // leave_conflict_count
                BigDecimal.valueOf(3)                // skill_mismatch_count
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(Collections.singletonList(row));

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers).hasSize(1);
        TeacherAvailabilityDTO teacher = teachers.get(0);
        
        assertThat(teacher.getTeacherId()).isEqualTo(100L);
        assertThat(teacher.getFullName()).isEqualTo("Test Teacher");
        assertThat(teacher.getEmail()).isEqualTo("test@teacher.com");
        assertThat(teacher.getSkills()).hasSize(3);
        assertThat(teacher.getSkills()).containsExactlyInAnyOrder(Skill.READING, Skill.WRITING, Skill.SPEAKING);
        assertThat(teacher.getHasGeneralSkill()).isFalse();
        assertThat(teacher.getTotalSessions()).isEqualTo(72);
        assertThat(teacher.getAvailableSessions()).isEqualTo(50);
        
        TeacherAvailabilityDTO.ConflictBreakdown conflicts = teacher.getConflicts();
        assertThat(conflicts.getNoAvailability()).isEqualTo(10);
        assertThat(conflicts.getTeachingConflict()).isEqualTo(5);
        assertThat(conflicts.getLeaveConflict()).isEqualTo(4);
        assertThat(conflicts.getSkillMismatch()).isEqualTo(3);
        assertThat(conflicts.getTotalConflicts()).isEqualTo(22); // 10+5+4+3
    }

    @Test
    @DisplayName("Should handle null/empty skills gracefully")
    void shouldHandleNullEmptySkills() {
        // Given
        Long classId = 1L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .build();

        // Test null skills
        Object[] row1 = new Object[]{
                BigInteger.valueOf(1L), "Teacher 1", "t1@test.com",
                null, false, // null skills
                BigDecimal.valueOf(36), BigDecimal.valueOf(36),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        };

        // Test empty skills
        Object[] row2 = new Object[]{
                BigInteger.valueOf(2L), "Teacher 2", "t2@test.com",
                "", false, // empty skills
                BigDecimal.valueOf(36), BigDecimal.valueOf(36),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        
        List<Object[]> teacherRows = new ArrayList<>();
        teacherRows.add(row1);
        teacherRows.add(row2);
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(teacherRows);

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers).hasSize(2);
        assertThat(teachers.get(0).getSkills()).isEmpty();
        assertThat(teachers.get(1).getSkills()).isEmpty();
    }

    @Test
    @DisplayName("Should parse single skill correctly")
    void shouldParseSingleSkill() {
        // Given
        Long classId = 1L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .build();

        Object[] row = new Object[]{
                BigInteger.valueOf(1L), "Teacher", "teacher@test.com",
                "READING", false, // single skill
                BigDecimal.valueOf(36), BigDecimal.valueOf(36),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(Collections.singletonList(row));

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers).hasSize(1);
        assertThat(teachers.get(0).getSkills()).containsExactly(Skill.READING);
    }

    @Test
    @DisplayName("Should handle GENERAL skill correctly")
    void shouldHandleGeneralSkillCorrectly() {
        // Given
        Long classId = 1L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .build();

        Object[] row = new Object[]{
                BigInteger.valueOf(1L), "Teacher", "teacher@test.com",
                "READING,GENERAL,WRITING", true, // GENERAL skill
                BigDecimal.valueOf(36), BigDecimal.valueOf(36),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(Collections.singletonList(row));

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers).hasSize(1);
        TeacherAvailabilityDTO teacher = teachers.get(0);
        assertThat(teacher.getSkills()).contains(Skill.GENERAL);
        assertThat(teacher.getHasGeneralSkill()).isTrue();
    }

    // ==================== TYPE CONVERSION TESTS ====================

    @Test
    @DisplayName("Should convert BigInteger to Long correctly")
    void shouldConvertBigIntegerToLong() {
        // Tested implicitly through PRE-CHECK query tests
        // BigInteger.valueOf(45L) should be converted to Long 45L
        
        // Given
        Long classId = 1L;
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();

        Object[] row = new Object[]{
                BigInteger.valueOf(1L), "Teacher", "t@test.com", "READING", false,
                BigDecimal.valueOf(36), BigDecimal.valueOf(36),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(Collections.singletonList(row));

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        assertThat(teachers.get(0).getTeacherId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should convert BigDecimal to Integer correctly")
    void shouldConvertBigDecimalToInteger() {
        // Given
        Long classId = 1L;
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();

        Object[] row = new Object[]{
                BigInteger.valueOf(1L), "Teacher", "t@test.com", "READING", false,
                BigDecimal.valueOf(100), BigDecimal.valueOf(75), // BigDecimal values
                BigDecimal.valueOf(10), BigDecimal.valueOf(8),
                BigDecimal.valueOf(5), BigDecimal.valueOf(2)
        };

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
                .thenReturn(Collections.singletonList(row));

        // When
        List<TeacherAvailabilityDTO> teachers = teacherAssignmentService.queryAvailableTeachersWithPrecheck(classId);

        // Then
        TeacherAvailabilityDTO teacher = teachers.get(0);
        assertThat(teacher.getTotalSessions()).isEqualTo(100);
        assertThat(teacher.getAvailableSessions()).isEqualTo(75);
        assertThat(teacher.getConflicts().getNoAvailability()).isEqualTo(10);
        assertThat(teacher.getConflicts().getTeachingConflict()).isEqualTo(8);
        assertThat(teacher.getConflicts().getLeaveConflict()).isEqualTo(5);
        assertThat(teacher.getConflicts().getSkillMismatch()).isEqualTo(2);
    }

    // ==================== FULL ASSIGNMENT MODE TESTS ====================

    @Test
    @DisplayName("Should assign teacher to all sessions in FULL mode")
    void shouldAssignTeacherToAllSessionsInFullMode() {
        // Given
        Long classId = 1L;
        Long teacherId = 45L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();
        Teacher teacher = TestDataBuilder.buildTeacher().id(teacherId).build();

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(teacherId);
        request.setSessionIds(null); // Full assignment mode

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(36L);
        
        // Mock bulk assignment returns all session IDs
        List<Long> assignedSessionIds = Arrays.asList(1L, 2L, 3L, 4L, 5L); // Simplified
        when(teachingSlotRepository.bulkAssignTeacher(classId, teacherId))
                .thenReturn(assignedSessionIds);
        
        // No remaining sessions (all assigned)
        when(sessionRepository.findSessionsWithoutTeacher(classId))
                .thenReturn(Collections.emptyList());

        // Mock response util
        AssignTeacherResponse expectedResponse = AssignTeacherResponse.builder()
                .classId(classId)
                .teacherId(teacherId)
                .teacherName(teacher.getUserAccount().getFullName())
                .assignedCount(5)
                .needsSubstitute(false)
                .remainingSessions(0)
                .build();
        when(responseUtil.buildSuccessResponse(eq(classId), eq(teacher), eq(36), 
                any(), any(), anyLong()))
                .thenReturn(expectedResponse);

        // When
        AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAssignedCount()).isEqualTo(5);
        assertThat(response.getNeedsSubstitute()).isFalse();
        assertThat(response.getRemainingSessions()).isEqualTo(0);

        verify(teachingSlotRepository).bulkAssignTeacher(classId, teacherId);
        verify(teachingSlotRepository, never()).bulkAssignTeacherToSessions(any(), anyLong());
    }

    // ==================== PARTIAL ASSIGNMENT MODE TESTS ====================

    @Test
    @DisplayName("Should assign teacher to specific sessions in PARTIAL mode")
    void shouldAssignTeacherToSpecificSessionsInPartialMode() {
        // Given
        Long classId = 1L;
        Long teacherId = 45L;
        List<Long> specificSessionIds = Arrays.asList(1L, 2L, 3L);
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();
        Teacher teacher = TestDataBuilder.buildTeacher().id(teacherId).build();

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(teacherId);
        request.setSessionIds(specificSessionIds); // Partial assignment mode

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(36L);
        
        // Mock partial assignment
        when(teachingSlotRepository.bulkAssignTeacherToSessions(specificSessionIds, teacherId))
                .thenReturn(specificSessionIds);
        
        // Remaining sessions (36 - 3 = 33)
        List<Long> remainingIds = Arrays.asList(4L, 5L, 6L); // Simplified
        when(sessionRepository.findSessionsWithoutTeacher(classId))
                .thenReturn(remainingIds);

        // Mock response util
        AssignTeacherResponse expectedResponse = AssignTeacherResponse.builder()
                .classId(classId)
                .teacherId(teacherId)
                .assignedCount(3)
                .needsSubstitute(true)
                .remainingSessions(3)
                .remainingSessionIds(remainingIds)
                .build();
        when(responseUtil.buildSuccessResponse(eq(classId), eq(teacher), eq(36), 
                any(), any(), anyLong()))
                .thenReturn(expectedResponse);

        // When
        AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAssignedCount()).isEqualTo(3);
        assertThat(response.getNeedsSubstitute()).isTrue();
        assertThat(response.getRemainingSessions()).isEqualTo(3);
        assertThat(response.getRemainingSessionIds()).isEqualTo(remainingIds);

        verify(teachingSlotRepository).bulkAssignTeacherToSessions(specificSessionIds, teacherId);
        verify(teachingSlotRepository, never()).bulkAssignTeacher(anyLong(), anyLong());
    }

    // ==================== NEEDS SUBSTITUTE CALCULATION TESTS ====================

    @Test
    @DisplayName("Should set needsSubstitute=false when all sessions assigned")
    void shouldSetNeedsSubstituteFalseWhenAllAssigned() {
        // Given
        Long classId = 1L;
        Long teacherId = 45L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();
        Teacher teacher = TestDataBuilder.buildTeacher().id(teacherId).build();

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(teacherId);
        request.setSessionIds(null);

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(10L);
        when(teachingSlotRepository.bulkAssignTeacher(classId, teacherId))
                .thenReturn(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
        when(sessionRepository.findSessionsWithoutTeacher(classId))
                .thenReturn(Collections.emptyList()); // No remaining

        AssignTeacherResponse expectedResponse = AssignTeacherResponse.builder()
                .needsSubstitute(false)
                .remainingSessions(0)
                .build();
        when(responseUtil.buildSuccessResponse(any(), any(), anyInt(), any(), any(), anyLong()))
                .thenReturn(expectedResponse);

        // When
        AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

        // Then
        assertThat(response.getNeedsSubstitute()).isFalse();
        assertThat(response.getRemainingSessions()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should set needsSubstitute=true when some sessions remain")
    void shouldSetNeedsSubstituteTrueWhenSessionsRemain() {
        // Given
        Long classId = 1L;
        Long teacherId = 45L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();
        Teacher teacher = TestDataBuilder.buildTeacher().id(teacherId).build();

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(teacherId);
        request.setSessionIds(Arrays.asList(1L, 2L));

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(10L);
        when(teachingSlotRepository.bulkAssignTeacherToSessions(any(), anyLong()))
                .thenReturn(Arrays.asList(1L, 2L));
        when(sessionRepository.findSessionsWithoutTeacher(classId))
                .thenReturn(Arrays.asList(3L, 4L, 5L)); // 3 remaining

        AssignTeacherResponse expectedResponse = AssignTeacherResponse.builder()
                .needsSubstitute(true)
                .remainingSessions(3)
                .build();
        when(responseUtil.buildSuccessResponse(any(), any(), anyInt(), any(), any(), anyLong()))
                .thenReturn(expectedResponse);

        // When
        AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

        // Then
        assertThat(response.getNeedsSubstitute()).isTrue();
        assertThat(response.getRemainingSessions()).isGreaterThan(0);
    }

    // ==================== ERROR SCENARIO TESTS ====================

    @Test
    @DisplayName("Should throw exception when class not found for assignment")
    void shouldThrowExceptionWhenClassNotFoundForAssignment() {
        // Given
        Long classId = 999L;
        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);

        when(classRepository.findById(classId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> teacherAssignmentService.assignTeacher(classId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLASS_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw exception when teacher not found")
    void shouldThrowExceptionWhenTeacherNotFound() {
        // Given
        Long classId = 1L;
        Long teacherId = 999L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(teacherId);

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> teacherAssignmentService.assignTeacher(classId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEACHER_NOT_FOUND);
    }

    @Test
    @DisplayName("Should handle empty sessionIds as full assignment")
    void shouldHandleEmptySessionIdsAsFullAssignment() {
        // Given
        Long classId = 1L;
        Long teacherId = 45L;
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity().id(classId).build();
        Teacher teacher = TestDataBuilder.buildTeacher().id(teacherId).build();

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(teacherId);
        request.setSessionIds(Collections.emptyList()); // Empty list

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(36L);
        when(teachingSlotRepository.bulkAssignTeacher(classId, teacherId))
                .thenReturn(Arrays.asList(1L, 2L, 3L));
        when(sessionRepository.findSessionsWithoutTeacher(classId))
                .thenReturn(Collections.emptyList());

        AssignTeacherResponse expectedResponse = AssignTeacherResponse.builder().build();
        when(responseUtil.buildSuccessResponse(any(), any(), anyInt(), any(), any(), anyLong()))
                .thenReturn(expectedResponse);

        // When
        teacherAssignmentService.assignTeacher(classId, request);

        // Then - Should use full assignment mode
        verify(teachingSlotRepository).bulkAssignTeacher(classId, teacherId);
    }
}
