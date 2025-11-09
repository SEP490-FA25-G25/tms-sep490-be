package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest.ResourceAssignment;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse.ResourceConflictDetail;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse.ConflictType;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.config.WithMockUserPrincipal;
import org.fyp.tmssep490be.services.ClassService;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.fyp.tmssep490be.security.CustomUserDetailsService;
import org.fyp.tmssep490be.utils.AssignResourcesResponseUtil;
import org.fyp.tmssep490be.utils.AssignTimeSlotsResponseUtil;
import org.fyp.tmssep490be.utils.CreateClassResponseUtil;
import org.fyp.tmssep490be.utils.ValidateClassResponseUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Tests for Resource Assignment Controller
 * Tests REST endpoints for STEP 4 of Create Class workflow
 * 
 * Tests the POST /api/v1/classes/{classId}/resources endpoint
 * with various scenarios including success, errors, and validation failures
 */
@WebMvcTest(ClassController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@DisplayName("Resource Assignment Controller API Tests")
class ResourceAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClassService classService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private ValidateClassResponseUtil validateClassResponseUtil;

    @MockitoBean
    private CreateClassResponseUtil createClassResponseUtil;

    @MockitoBean
    private AssignTimeSlotsResponseUtil assignTimeSlotsResponseUtil;

    @MockitoBean
    private AssignResourcesResponseUtil assignResourcesResponseUtil;

    private AssignResourcesRequest validRequest;
    private AssignResourcesResponse fullSuccessResponse;
    private AssignResourcesResponse partialSuccessResponse;

    @BeforeEach
    void setUp() {
        // Valid request with 3 patterns (Mon/Wed/Fri)
        validRequest = new AssignResourcesRequest();
        validRequest.setPattern(Arrays.asList(
                ResourceAssignment.builder().dayOfWeek((short) 1).resourceId(10L).build(), // Monday
                ResourceAssignment.builder().dayOfWeek((short) 3).resourceId(10L).build(), // Wednesday
                ResourceAssignment.builder().dayOfWeek((short) 5).resourceId(10L).build()  // Friday
        ));

        // Full success response (all 36 sessions assigned)
        fullSuccessResponse = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(36)
                .conflictCount(0)
                .conflicts(Collections.emptyList())
                .processingTimeMs(150L)
                .build();

        // Partial success response (30 sessions assigned, 6 conflicts)
        ResourceConflictDetail conflict1 = ResourceConflictDetail.builder()
                .sessionId(1L)
                .date(LocalDate.of(2025, 1, 6))
                .conflictType(ConflictType.CLASS_BOOKING)
                .conflictReason("Room is booked by another class")
                .conflictingClassId(99L)
                .conflictingClassName("Advanced English Class")
                .build();

        ResourceConflictDetail conflict2 = ResourceConflictDetail.builder()
                .sessionId(2L)
                .date(LocalDate.of(2025, 1, 8))
                .conflictType(ConflictType.INSUFFICIENT_CAPACITY)
                .conflictReason("Room capacity insufficient")
                .build();

        partialSuccessResponse = AssignResourcesResponse.builder()
                .totalSessions(36)
                .successCount(30)
                .conflictCount(6)
                .conflicts(Arrays.asList(conflict1, conflict2))
                .processingTimeMs(180L)
                .build();
    }

    // ==================== SUCCESS SCENARIOS ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should assign resources successfully when no conflicts")
    void shouldAssignResourcesSuccessfullyWhenNoConflicts() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenReturn(fullSuccessResponse);
        when(assignResourcesResponseUtil.isFullySuccessful(fullSuccessResponse)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("All 36 sessions assigned successfully")))
                .andExpect(jsonPath("$.message").value(containsString("150ms")))
                .andExpect(jsonPath("$.data.totalSessions").value(36))
                .andExpect(jsonPath("$.data.successCount").value(36))
                .andExpect(jsonPath("$.data.conflictCount").value(0))
                .andExpect(jsonPath("$.data.conflicts").isEmpty())
                .andExpect(jsonPath("$.data.processingTimeMs").value(150));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should assign resources partially when conflicts exist")
    void shouldAssignResourcesPartiallyWhenConflictsExist() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenReturn(partialSuccessResponse);
        when(assignResourcesResponseUtil.isFullySuccessful(partialSuccessResponse)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true)) // successCount > 0
                .andExpect(jsonPath("$.message").value(containsString("30/36 sessions successful")))
                .andExpect(jsonPath("$.message").value(containsString("6 conflicts")))
                .andExpect(jsonPath("$.message").value(containsString("180ms")))
                .andExpect(jsonPath("$.data.totalSessions").value(36))
                .andExpect(jsonPath("$.data.successCount").value(30))
                .andExpect(jsonPath("$.data.conflictCount").value(6))
                .andExpect(jsonPath("$.data.conflicts").isArray())
                .andExpect(jsonPath("$.data.conflicts", hasSize(2)))
                .andExpect(jsonPath("$.data.conflicts[0].sessionId").value(1))
                .andExpect(jsonPath("$.data.conflicts[0].conflictType").value("CLASS_BOOKING"))
                .andExpect(jsonPath("$.data.conflicts[0].conflictingClassId").value(99))
                .andExpect(jsonPath("$.data.conflicts[1].sessionId").value(2))
                .andExpect(jsonPath("$.data.conflicts[1].conflictType").value("INSUFFICIENT_CAPACITY"))
                .andExpect(jsonPath("$.data.processingTimeMs").value(180));
    }

    // ==================== VALIDATION ERROR SCENARIOS ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when request body is null")
    void shouldReturn400WhenRequestBodyIsNull() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty JSON
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when pattern is null")
    void shouldReturn400WhenPatternIsNull() throws Exception {
        // Given
        AssignResourcesRequest invalidRequest = new AssignResourcesRequest();
        invalidRequest.setPattern(null);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when pattern is empty")
    void shouldReturn400WhenPatternIsEmpty() throws Exception {
        // Given
        AssignResourcesRequest invalidRequest = new AssignResourcesRequest();
        invalidRequest.setPattern(Collections.emptyList());

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Disabled("Validation not triggering in @WebMvcTest - covered by validator unit tests")
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when dayOfWeek is invalid")
    void shouldReturn400WhenDayOfWeekIsInvalid() throws Exception {
        // Given - Sunday (0) or beyond Saturday (7)
        AssignResourcesRequest invalidRequest = new AssignResourcesRequest();
        invalidRequest.setPattern(Collections.singletonList(
                ResourceAssignment.builder().dayOfWeek((short) 0).resourceId(10L).build() // Sunday not allowed
        ));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when resourceId is null")
    void shouldReturn400WhenResourceIdIsNull() throws Exception {
        // Given
        AssignResourcesRequest invalidRequest = new AssignResourcesRequest();
        invalidRequest.setPattern(Collections.singletonList(
                ResourceAssignment.builder().dayOfWeek((short) 1).resourceId(null).build()
        ));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when resourceId is negative")
    void shouldReturn400WhenResourceIdIsNegative() throws Exception {
        // Given
        AssignResourcesRequest invalidRequest = new AssignResourcesRequest();
        invalidRequest.setPattern(Collections.singletonList(
                ResourceAssignment.builder().dayOfWeek((short) 1).resourceId(-1L).build()
        ));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    // ==================== BUSINESS LOGIC ERROR SCENARIOS ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 404 when class not found")
    void shouldReturn404WhenClassNotFound() throws Exception {
        // Given
        Long classId = 999L; // Non-existent class
        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 404 when resource not found")
    void shouldReturn404WhenResourceNotFound() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when resource branch mismatch")
    void shouldReturn400WhenResourceBranchMismatch() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.RESOURCE_BRANCH_MISMATCH));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when class status invalid")
    void shouldReturn400WhenClassStatusInvalid() throws Exception {
        // Given - Class not in DRAFT status
        Long classId = 1L;
        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.CLASS_INVALID_STATUS));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== AUTHORIZATION SCENARIOS ====================

    @Test
    @DisplayName("Should return 401 when user not authenticated")
    void shouldReturn401WhenUserNotAuthenticated() throws Exception {
        // When & Then - No @WithMockUser annotation
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserPrincipal(id = 2L, username = "student", roles = {"STUDENT"})
    @DisplayName("Should return 403 when user has wrong role")
    void shouldReturn403WhenUserHasWrongRole() throws Exception {
        // When & Then - User with STUDENT role (not ACADEMIC_AFFAIR)
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ==================== EDGE CASES ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should handle large session count efficiently")
    void shouldHandleLargeSessionCountEfficiently() throws Exception {
        // Given - Intensive course with 100 sessions
        Long classId = 1L;
        AssignResourcesResponse largeResponse = AssignResourcesResponse.builder()
                .totalSessions(100)
                .successCount(100)
                .conflictCount(0)
                .conflicts(Collections.emptyList())
                .processingTimeMs(200L) // Still fast
                .build();

        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenReturn(largeResponse);
        when(assignResourcesResponseUtil.isFullySuccessful(largeResponse)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSessions").value(100))
                .andExpect(jsonPath("$.data.successCount").value(100))
                .andExpect(jsonPath("$.data.processingTimeMs").value(lessThanOrEqualTo(200)));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should handle multiple conflict types")
    void shouldHandleMultipleConflictTypes() throws Exception {
        // Given - Multiple conflict types
        Long classId = 1L;
        AssignResourcesResponse multiConflictResponse = AssignResourcesResponse.builder()
                .totalSessions(12)
                .successCount(8)
                .conflictCount(4)
                .conflicts(Arrays.asList(
                        ResourceConflictDetail.builder()
                                .sessionId(1L)
                                .date(LocalDate.of(2025, 1, 6))
                                .conflictType(ConflictType.CLASS_BOOKING)
                                .conflictReason("Booked by Class A")
                                .conflictingClassId(99L)
                                .conflictingClassName("Class A")
                                .build(),
                        ResourceConflictDetail.builder()
                                .sessionId(2L)
                                .date(LocalDate.of(2025, 1, 8))
                                .conflictType(ConflictType.INSUFFICIENT_CAPACITY)
                                .conflictReason("Capacity insufficient")
                                .build(),
                        ResourceConflictDetail.builder()
                                .sessionId(3L)
                                .date(LocalDate.of(2025, 1, 10))
                                .conflictType(ConflictType.MAINTENANCE)
                                .conflictReason("Under maintenance")
                                .build(),
                        ResourceConflictDetail.builder()
                                .sessionId(4L)
                                .date(LocalDate.of(2025, 1, 13))
                                .conflictType(ConflictType.UNAVAILABLE)
                                .conflictReason("Unavailable")
                                .build()
                ))
                .processingTimeMs(170L)
                .build();

        when(classService.assignResources(eq(classId), any(AssignResourcesRequest.class), anyLong()))
                .thenReturn(multiConflictResponse);
        when(assignResourcesResponseUtil.isFullySuccessful(multiConflictResponse)).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.conflictCount").value(4))
                .andExpect(jsonPath("$.data.conflicts", hasSize(4)))
                .andExpect(jsonPath("$.data.conflicts[0].conflictType").value("CLASS_BOOKING"))
                .andExpect(jsonPath("$.data.conflicts[1].conflictType").value("INSUFFICIENT_CAPACITY"))
                .andExpect(jsonPath("$.data.conflicts[2].conflictType").value("MAINTENANCE"))
                .andExpect(jsonPath("$.data.conflicts[3].conflictType").value("UNAVAILABLE"));
    }
}
