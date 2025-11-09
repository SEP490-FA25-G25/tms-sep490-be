package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.ResourceRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.SessionResourceRepository;
import org.fyp.tmssep490be.services.ResourceAssignmentService;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResourceAssignmentServiceImpl
 * <p>
 * Tests HYBRID approach:
 * - Phase 1: SQL bulk insert (fast path)
 * - Phase 2: Java conflict analysis (detailed path)
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ResourceAssignmentService Unit Tests")
class ResourceAssignmentServiceImplTest {

    @Autowired
    private ResourceAssignmentService resourceAssignmentService;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private SessionResourceRepository sessionResourceRepository;

    @MockitoBean
    private ResourceRepository resourceRepository;

    // ==================== PHASE 1: BULK INSERT TESTS ====================

    @Test
    @DisplayName("Should assign resources successfully with no conflicts (90% case)")
    void shouldAssignResourcesWithNoConflicts() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .maxCapacity(30)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .capacity(30)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build(),
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 3)
                        .resourceId(resourceId)
                        .build(),
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 5)
                        .resourceId(resourceId)
                        .build()
        ));

        // Mock repository responses
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(36L);
        
        // Mock Phase 1: Bulk insert successful (12 sessions per day)
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(eq(classId), eq(1), eq(resourceId)))
                .thenReturn(12);
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(eq(classId), eq(3), eq(resourceId)))
                .thenReturn(12);
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(eq(classId), eq(5), eq(resourceId)))
                .thenReturn(12);
        
        // Mock Phase 2: No conflicts found
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(eq(classId), anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getClassId()).isEqualTo(classId);
        assertThat(response.getTotalSessions()).isEqualTo(36);
        assertThat(response.getSuccessCount()).isEqualTo(36); // All 36 sessions assigned
        assertThat(response.getConflictCount()).isEqualTo(0);
        assertThat(response.getConflicts()).isEmpty();
        assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(0L); // Can be 0 for fast operations

        // Verify Phase 1 was executed
        verify(sessionResourceRepository).bulkInsertResourcesForDayOfWeek(classId, 1, resourceId);
        verify(sessionResourceRepository).bulkInsertResourcesForDayOfWeek(classId, 3, resourceId);
        verify(sessionResourceRepository).bulkInsertResourcesForDayOfWeek(classId, 5, resourceId);
        
        // Verify Phase 2 was executed
        verify(sessionRepository, times(3)).findUnassignedSessionsByDayOfWeek(eq(classId), anyInt());
    }

    @Test
    @DisplayName("Should handle partial success with some conflicts")
    void shouldHandlePartialSuccessWithConflicts() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .maxCapacity(30)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .capacity(30)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        // Mock repository responses
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(12L);
        
        // Mock Phase 1: Only 10 out of 12 sessions assigned
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(classId, 1, resourceId))
                .thenReturn(10);
        
        // Mock Phase 2: 2 unassigned sessions with conflicts
        Object[] session1 = {1L, LocalDate.now(), 1L};
        Object[] session2 = {2L, LocalDate.now().plusDays(7), 1L};
        List<Object[]> unassignedSessions = new ArrayList<>();
        unassignedSessions.add(session1);
        unassignedSessions.add(session2);
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(classId, 1))
                .thenReturn(unassignedSessions);
        
        // Mock conflict details (capacity issue)
        when(sessionResourceRepository.findConflictingSessionDetails(anyLong(), eq(resourceId)))
                .thenReturn(null); // No booking conflict

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSuccessCount()).isEqualTo(10);
        assertThat(response.getConflictCount()).isEqualTo(2);
        assertThat(response.getConflicts()).hasSize(2);
        
        verify(sessionResourceRepository).bulkInsertResourcesForDayOfWeek(classId, 1, resourceId);
        verify(sessionRepository).findUnassignedSessionsByDayOfWeek(classId, 1);
    }

    // ==================== PHASE 2: CONFLICT ANALYSIS TESTS ====================

    @Test
    @DisplayName("Should detect CLASS_BOOKING conflict")
    void shouldDetectClassBookingConflict() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .maxCapacity(30)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .capacity(30)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        // Mock repository responses
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(1L);
        
        // Mock Phase 1: No assignments (conflict)
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(classId, 1, resourceId))
                .thenReturn(0);
        
        // Mock Phase 2: 1 unassigned session
        Object[] session1 = {1L, LocalDate.now(), 1L};
        List<Object[]> unassignedSessions = new ArrayList<>();
        unassignedSessions.add(session1);
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(classId, 1))
                .thenReturn(unassignedSessions);
        
        // Mock conflict details (booking conflict)
        Object[] conflictDetails = {99L, 2L, "Conflicting Class", LocalDate.now(), null, null};
        when(sessionResourceRepository.findConflictingSessionDetails(1L, resourceId))
                .thenReturn(conflictDetails);

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getConflictCount()).isEqualTo(1);
        assertThat(response.getConflicts()).hasSize(1);
        
        AssignResourcesResponse.ResourceConflictDetail conflict = response.getConflicts().get(0);
        assertThat(conflict.getConflictType()).isEqualTo(AssignResourcesResponse.ConflictType.CLASS_BOOKING);
        assertThat(conflict.getConflictingClassId()).isEqualTo(2L);
        assertThat(conflict.getConflictingClassName()).isEqualTo("Conflicting Class");
    }

    @Test
    @DisplayName("Should detect INSUFFICIENT_CAPACITY conflict")
    void shouldDetectInsufficientCapacityConflict() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .maxCapacity(50) // Class needs 50 capacity
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .capacity(30) // Resource only has 30 capacity
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        // Mock repository responses
        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(1L);
        
        // Mock Phase 1: No assignments (capacity issue)
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(classId, 1, resourceId))
                .thenReturn(0);
        
        // Mock Phase 2: 1 unassigned session
        Object[] session1 = {1L, LocalDate.now(), 1L};
        List<Object[]> unassignedSessions = new ArrayList<>();
        unassignedSessions.add(session1);
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(classId, 1))
                .thenReturn(unassignedSessions);
        
        // Mock no booking conflict
        when(sessionResourceRepository.findConflictingSessionDetails(1L, resourceId))
                .thenReturn(null);

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getConflictCount()).isEqualTo(1);
        
        AssignResourcesResponse.ResourceConflictDetail conflict = response.getConflicts().get(0);
        assertThat(conflict.getConflictType()).isEqualTo(AssignResourcesResponse.ConflictType.INSUFFICIENT_CAPACITY);
        assertThat(conflict.getConflictReason()).contains("capacity (30) is less than class max capacity (50)");
    }

    // ==================== PERFORMANCE TRACKING TESTS ====================

    @Test
    @DisplayName("Should track processing time correctly")
    void shouldTrackProcessingTime() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(10L);
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(anyLong(), anyInt(), anyLong()))
                .thenReturn(10);
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response.getProcessingTimeMs()).isGreaterThanOrEqualTo(0L); // Can be 0 for fast operations
        assertThat(response.getProcessingTimeMs()).isLessThan(5000L); // Should be fast
    }

    @Test
    @DisplayName("Should handle large session counts efficiently")
    void shouldHandleLargeSessionCounts() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(100L); // Large count
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(anyLong(), anyInt(), anyLong()))
                .thenReturn(100);
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response.getTotalSessions()).isEqualTo(100);
        assertThat(response.getSuccessCount()).isEqualTo(100);
    }

    // ==================== ERROR SCENARIO TESTS ====================

    @Test
    @DisplayName("Should throw exception when class not found")
    void shouldThrowExceptionWhenClassNotFound() {
        // Given
        Long classId = 999L;
        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(10L)
                        .build()
        ));

        when(classRepository.findById(classId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> resourceAssignmentService.assignResources(classId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLASS_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw exception when resource not found")
    void shouldThrowExceptionWhenResourceNotFound() {
        // Given
        Long classId = 1L;
        Long resourceId = 999L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(Collections.emptyList()); // Resource not found

        // When / Then
        assertThatThrownBy(() -> resourceAssignmentService.assignResources(classId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw exception when resource belongs to different branch")
    void shouldThrowExceptionWhenResourceBelongsToDifferentBranch() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch classBranch = TestDataBuilder.buildBranch().id(1L).build();
        Branch resourceBranch = TestDataBuilder.buildBranch().id(2L).build(); // Different branch!
        
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(classBranch)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(resourceBranch) // Different branch
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));

        // When / Then
        assertThatThrownBy(() -> resourceAssignmentService.assignResources(classId, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_BRANCH_MISMATCH);
    }

    @Test
    @DisplayName("Should handle empty request pattern gracefully")
    void shouldHandleEmptyRequestPattern() {
        // Given
        Long classId = 1L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(Collections.emptyList()); // Empty pattern

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(36L);

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSuccessCount()).isEqualTo(0);
        assertThat(response.getConflictCount()).isEqualTo(0);
        
        verify(sessionResourceRepository, never()).bulkInsertResourcesForDayOfWeek(anyLong(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("Should handle sessions without time slots (not ready for assignment)")
    void shouldHandleSessionsWithoutTimeSlots() {
        // Given
        Long classId = 1L;
        Long resourceId = 10L;
        
        Branch branch = TestDataBuilder.buildBranch().id(1L).build();
        ClassEntity classEntity = TestDataBuilder.buildClassEntity()
                .id(classId)
                .branch(branch)
                .build();
        
        Resource resource = TestDataBuilder.buildResource()
                .id(resourceId)
                .branch(branch)
                .build();

        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setPattern(List.of(
                AssignResourcesRequest.ResourceAssignment.builder()
                        .dayOfWeek((short) 1)
                        .resourceId(resourceId)
                        .build()
        ));

        when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
        when(resourceRepository.findAllById(anyList())).thenReturn(List.of(resource));
        when(sessionRepository.countByClassEntityId(classId)).thenReturn(1L);
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(anyLong(), anyInt(), anyLong()))
                .thenReturn(0);
        
        // Session without time slot (null)
        Object[] session1 = {1L, LocalDate.now(), null}; // timeSlotId is null
        List<Object[]> unassignedSessions = new ArrayList<>();
        unassignedSessions.add(session1);
        when(sessionRepository.findUnassignedSessionsByDayOfWeek(classId, 1))
                .thenReturn(unassignedSessions);

        // When
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getConflictCount()).isEqualTo(0); // Not counted as conflict
        assertThat(response.getConflicts()).isEmpty();
    }
}
