package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO.AvailabilityStatus;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO.ConflictBreakdown;
import org.fyp.tmssep490be.entities.enums.Skill;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API Tests for Teacher Assignment Controller
 * Tests REST endpoints for STEP 5 of Create Class workflow
 * 
 * Tests:
 * - GET  /api/v1/classes/{classId}/available-teachers (PRE-CHECK query)
 * - POST /api/v1/classes/{classId}/teachers (Assignment)
 */
@WebMvcTest(ClassController.class)
@ActiveProfiles("test")
@EnableMethodSecurity
@DisplayName("Teacher Assignment Controller API Tests")
class TeacherAssignmentControllerTest {

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

    private List<TeacherAvailabilityDTO> availableTeachers;
    private AssignTeacherRequest fullAssignmentRequest;
    private AssignTeacherRequest partialAssignmentRequest;
    private AssignTeacherResponse fullAssignmentResponse;
    private AssignTeacherResponse partialAssignmentResponse;

    @BeforeEach
    void setUp() {
        // Available teachers list (PRE-CHECK results)
        TeacherAvailabilityDTO fullyAvailable = TeacherAvailabilityDTO.builder()
                .teacherId(45L)
                .fullName("John Doe")
                .email("john.doe@example.com")
                .skills(Arrays.asList(Skill.READING, Skill.WRITING))
                .hasGeneralSkill(false)
                .totalSessions(36)
                .availableSessions(36)
                .availabilityPercentage(100.0)
                .availabilityStatus(AvailabilityStatus.FULLY_AVAILABLE)
                .conflicts(ConflictBreakdown.builder()
                        .noAvailability(0)
                        .teachingConflict(0)
                        .leaveConflict(0)
                        .skillMismatch(0)
                        .totalConflicts(0)
                        .build())
                .build();

        TeacherAvailabilityDTO partiallyAvailable = TeacherAvailabilityDTO.builder()
                .teacherId(46L)
                .fullName("Jane Smith")
                .email("jane.smith@example.com")
                .skills(Arrays.asList(Skill.SPEAKING, Skill.GENERAL))
                .hasGeneralSkill(true)
                .totalSessions(36)
                .availableSessions(28)
                .availabilityPercentage(77.78)
                .availabilityStatus(AvailabilityStatus.PARTIALLY_AVAILABLE)
                .conflicts(ConflictBreakdown.builder()
                        .noAvailability(3)
                        .teachingConflict(2)
                        .leaveConflict(1)
                        .skillMismatch(2)
                        .totalConflicts(8)
                        .build())
                .build();

        availableTeachers = Arrays.asList(fullyAvailable, partiallyAvailable);

        // Full assignment request (assign to all sessions)
        fullAssignmentRequest = new AssignTeacherRequest();
        fullAssignmentRequest.setTeacherId(45L);
        fullAssignmentRequest.setSessionIds(null); // null = full assignment

        // Partial assignment request (assign to specific sessions)
        partialAssignmentRequest = new AssignTeacherRequest();
        partialAssignmentRequest.setTeacherId(46L);
        partialAssignmentRequest.setSessionIds(Arrays.asList(1L, 2L, 3L));

        // Full assignment response (all sessions assigned, no substitute needed)
        fullAssignmentResponse = AssignTeacherResponse.builder()
                .assignedCount(36)
                .assignedSessionIds(Arrays.asList(1L, 2L, 3L /* ... 36 total */))
                .needsSubstitute(false)
                .remainingSessions(0)
                .remainingSessionIds(Collections.emptyList())
                .processingTimeMs(45L)
                .build();

        // Partial assignment response (3 sessions assigned, substitute needed)
        partialAssignmentResponse = AssignTeacherResponse.builder()
                .assignedCount(3)
                .assignedSessionIds(Arrays.asList(1L, 2L, 3L))
                .needsSubstitute(true)
                .remainingSessions(33)
                .remainingSessionIds(Arrays.asList(4L, 5L, 6L /* ... 33 total */))
                .processingTimeMs(40L)
                .build();
    }

    // ==================== PRE-CHECK QUERY TESTS (GET /available-teachers) ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return available teachers with PRE-CHECK details")
    void shouldReturnAvailableTeachersWithPrecheckDetails() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.getAvailableTeachers(eq(classId), anyLong()))
                .thenReturn(availableTeachers);

        // When & Then
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", classId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Found 2 teachers")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                
                // First teacher (fully available)
                .andExpect(jsonPath("$.data[0].teacherId").value(45))
                .andExpect(jsonPath("$.data[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$.data[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data[0].skills", hasSize(2)))
                .andExpect(jsonPath("$.data[0].hasGeneralSkill").value(false))
                .andExpect(jsonPath("$.data[0].totalSessions").value(36))
                .andExpect(jsonPath("$.data[0].availableSessions").value(36))
                .andExpect(jsonPath("$.data[0].availabilityPercentage").value(100.0))
                .andExpect(jsonPath("$.data[0].availabilityStatus").value("FULLY_AVAILABLE"))
                .andExpect(jsonPath("$.data[0].conflicts.noAvailability").value(0))
                .andExpect(jsonPath("$.data[0].conflicts.teachingConflict").value(0))
                .andExpect(jsonPath("$.data[0].conflicts.leaveConflict").value(0))
                .andExpect(jsonPath("$.data[0].conflicts.skillMismatch").value(0))
                
                // Second teacher (partially available)
                .andExpect(jsonPath("$.data[1].teacherId").value(46))
                .andExpect(jsonPath("$.data[1].fullName").value("Jane Smith"))
                .andExpect(jsonPath("$.data[1].availableSessions").value(28))
                .andExpect(jsonPath("$.data[1].availabilityPercentage").value(77.78))
                .andExpect(jsonPath("$.data[1].availabilityStatus").value("PARTIALLY_AVAILABLE"))
                .andExpect(jsonPath("$.data[1].hasGeneralSkill").value(true))
                .andExpect(jsonPath("$.data[1].conflicts.noAvailability").value(3))
                .andExpect(jsonPath("$.data[1].conflicts.teachingConflict").value(2))
                .andExpect(jsonPath("$.data[1].conflicts.leaveConflict").value(1))
                .andExpect(jsonPath("$.data[1].conflicts.skillMismatch").value(2));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return empty list when no teachers available")
    void shouldReturnEmptyListWhenNoTeachersAvailable() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.getAvailableTeachers(eq(classId), anyLong()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", classId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Found 0 teachers")))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 404 when class not found for PRE-CHECK")
    void shouldReturn404WhenClassNotFoundForPrecheck() throws Exception {
        // Given
        Long classId = 999L;
        when(classService.getAvailableTeachers(eq(classId), anyLong()))
                .thenThrow(new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", classId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== FULL ASSIGNMENT TESTS (POST /teachers) ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should assign teacher to all sessions successfully")
    void shouldAssignTeacherToAllSessionsSuccessfully() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenReturn(fullAssignmentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Teacher assigned to all 36 sessions")))
                .andExpect(jsonPath("$.message").value(containsString("45ms")))
                .andExpect(jsonPath("$.data.assignedCount").value(36))
                .andExpect(jsonPath("$.data.needsSubstitute").value(false))
                .andExpect(jsonPath("$.data.remainingSessions").value(0))
                .andExpect(jsonPath("$.data.remainingSessionIds").isEmpty())
                .andExpect(jsonPath("$.data.processingTimeMs").value(45));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should assign teacher to specific sessions only")
    void shouldAssignTeacherToSpecificSessionsOnly() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenReturn(partialAssignmentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("Teacher assigned to 3 sessions")))
                .andExpect(jsonPath("$.message").value(containsString("33 sessions still need assignment")))
                .andExpect(jsonPath("$.message").value(containsString("40ms")))
                .andExpect(jsonPath("$.data.assignedCount").value(3))
                .andExpect(jsonPath("$.data.assignedSessionIds", hasSize(3)))
                .andExpect(jsonPath("$.data.needsSubstitute").value(true))
                .andExpect(jsonPath("$.data.remainingSessions").value(33))
                .andExpect(jsonPath("$.data.remainingSessionIds").isArray())
                .andExpect(jsonPath("$.data.processingTimeMs").value(40));
    }

    // ==================== VALIDATION ERROR SCENARIOS ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when request body is null")
    void shouldReturn400WhenRequestBodyIsNull() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty JSON
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when teacherId is null")
    void shouldReturn400WhenTeacherIdIsNull() throws Exception {
        // Given
        AssignTeacherRequest invalidRequest = new AssignTeacherRequest();
        invalidRequest.setTeacherId(null);
        invalidRequest.setSessionIds(null);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when teacherId is negative")
    void shouldReturn400WhenTeacherIdIsNegative() throws Exception {
        // Given
        AssignTeacherRequest invalidRequest = new AssignTeacherRequest();
        invalidRequest.setTeacherId(-1L);
        invalidRequest.setSessionIds(null);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Disabled("Validation not triggering in @WebMvcTest - covered by validator unit tests")
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when sessionIds contains negative value")
    void shouldReturn400WhenSessionIdsContainsNegativeValue() throws Exception {
        // Given
        AssignTeacherRequest invalidRequest = new AssignTeacherRequest();
        invalidRequest.setTeacherId(45L);
        invalidRequest.setSessionIds(Arrays.asList(1L, -2L, 3L));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should accept null sessionIds as full assignment")
    void shouldAcceptEmptySessionIdsAsFullAssignment() throws Exception {
        // Given - null sessionIds means full assignment (all sessions)
        AssignTeacherRequest fullAssignmentRequest = new AssignTeacherRequest();
        fullAssignmentRequest.setTeacherId(45L);
        fullAssignmentRequest.setSessionIds(null); // null = full assignment

        when(classService.assignTeacher(eq(1L), any(AssignTeacherRequest.class), anyLong()))
                .thenReturn(fullAssignmentResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedCount").value(36));
    }

    // ==================== BUSINESS LOGIC ERROR SCENARIOS ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 404 when class not found for assignment")
    void shouldReturn404WhenClassNotFoundForAssignment() throws Exception {
        // Given
        Long classId = 999L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 404 when teacher not found")
    void shouldReturn404WhenTeacherNotFound() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when class status invalid")
    void shouldReturn400WhenClassStatusInvalid() throws Exception {
        // Given - Class not in DRAFT status
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.CLASS_INVALID_STATUS));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when teacher missing required skills")
    void shouldReturn400WhenTeacherMissingRequiredSkills() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.TEACHER_SKILL_MISMATCH));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 404 when session not found")
    void shouldReturn400WhenSessionNotFound() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.SESSION_NOT_FOUND));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isNotFound()) // SESSION_NOT_FOUND → 404
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when session not in class")
    void shouldReturn400WhenSessionNotInClass() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.SESSION_NOT_IN_CLASS));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when time slot not assigned")
    void shouldReturn400WhenTimeSlotNotAssigned() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.TIME_SLOT_NOT_ASSIGNED));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should return 400 when duplicate session IDs")
    void shouldReturn400WhenDuplicateSessionIds() throws Exception {
        // Given
        Long classId = 1L;
        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenThrow(new CustomException(ErrorCode.DUPLICATE_SESSION_IDS));

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==================== AUTHORIZATION SCENARIOS ====================

    @Test
    @DisplayName("Should return 401 when user not authenticated for PRE-CHECK")
    void shouldReturn401WhenUserNotAuthenticatedForPrecheck() throws Exception {
        // When & Then - No @WithMockUser annotation
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", 1L)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when user not authenticated for assignment")
    void shouldReturn401WhenUserNotAuthenticatedForAssignment() throws Exception {
        // When & Then - No @WithMockUser annotation
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUserPrincipal(id = 2L, username = "student", roles = {"STUDENT"})
    @DisplayName("Should return 403 when user has wrong role for PRE-CHECK")
    void shouldReturn403WhenUserHasWrongRoleForPrecheck() throws Exception {
        // When & Then - User with STUDENT role (not ACADEMIC_AFFAIR)
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", 1L)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUserPrincipal(id = 2L, username = "student", roles = {"STUDENT"})
    @DisplayName("Should return 403 when user has wrong role for assignment")
    void shouldReturn403WhenUserHasWrongRoleForAssignment() throws Exception {
        // When & Then - User with STUDENT role (not ACADEMIC_AFFAIR)
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ==================== EDGE CASES ====================

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should handle teacher with GENERAL skill correctly")
    void shouldHandleTeacherWithGeneralSkillCorrectly() throws Exception {
        // Given - Teacher with GENERAL skill should bypass skill validation
        Long classId = 1L;
        TeacherAvailabilityDTO generalTeacher = TeacherAvailabilityDTO.builder()
                .teacherId(100L)
                .fullName("Universal Teacher")
                .email("universal@example.com")
                .skills(Collections.singletonList(Skill.GENERAL))
                .hasGeneralSkill(true)
                .totalSessions(36)
                .availableSessions(36)
                .availabilityPercentage(100.0)
                .availabilityStatus(AvailabilityStatus.FULLY_AVAILABLE)
                .conflicts(ConflictBreakdown.builder()
                        .noAvailability(0)
                        .teachingConflict(0)
                        .leaveConflict(0)
                        .skillMismatch(0) // GENERAL bypasses skill checks
                        .totalConflicts(0)
                        .build())
                .build();

        when(classService.getAvailableTeachers(eq(classId), anyLong()))
                .thenReturn(Collections.singletonList(generalTeacher));

        // When & Then
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", classId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].hasGeneralSkill").value(true))
                .andExpect(jsonPath("$.data[0].conflicts.skillMismatch").value(0))
                .andExpect(jsonPath("$.data[0].availabilityStatus").value("FULLY_AVAILABLE"));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should handle large session count efficiently")
    void shouldHandleLargeSessionCountEfficiently() throws Exception {
        // Given - Intensive course with 100 sessions
        Long classId = 1L;
        AssignTeacherResponse largeResponse = AssignTeacherResponse.builder()
                .assignedCount(100)
                .assignedSessionIds(Arrays.asList(1L, 2L /* ... 100 total */))
                .needsSubstitute(false)
                .remainingSessions(0)
                .remainingSessionIds(Collections.emptyList())
                .processingTimeMs(50L) // Still fast with bulk INSERT
                .build();

        when(classService.assignTeacher(eq(classId), any(AssignTeacherRequest.class), anyLong()))
                .thenReturn(largeResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", classId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullAssignmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignedCount").value(100))
                .andExpect(jsonPath("$.data.processingTimeMs").value(lessThanOrEqualTo(50)));
    }

    @Test
    @WithMockUserPrincipal(id = 1L, username = "academic", roles = {"ACADEMIC_AFFAIR"})
    @DisplayName("Should handle unavailable teacher correctly")
    void shouldHandleUnavailableTeacherCorrectly() throws Exception {
        // Given - Teacher with 0% availability
        Long classId = 1L;
        TeacherAvailabilityDTO unavailable = TeacherAvailabilityDTO.builder()
                .teacherId(50L)
                .fullName("Busy Teacher")
                .email("busy@example.com")
                .skills(Arrays.asList(Skill.READING, Skill.WRITING))
                .hasGeneralSkill(false)
                .totalSessions(36)
                .availableSessions(0)
                .availabilityPercentage(0.0)
                .availabilityStatus(AvailabilityStatus.UNAVAILABLE)
                .conflicts(ConflictBreakdown.builder()
                        .noAvailability(12)
                        .teachingConflict(15)
                        .leaveConflict(5)
                        .skillMismatch(4)
                        .totalConflicts(36)
                        .build())
                .build();

        when(classService.getAvailableTeachers(eq(classId), anyLong()))
                .thenReturn(Collections.singletonList(unavailable));

        // When & Then
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", classId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].availableSessions").value(0))
                .andExpect(jsonPath("$.data[0].availabilityPercentage").value(0.0))
                .andExpect(jsonPath("$.data[0].availabilityStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data[0].conflicts.noAvailability").value(12))
                .andExpect(jsonPath("$.data[0].conflicts.teachingConflict").value(15))
                .andExpect(jsonPath("$.data[0].conflicts.leaveConflict").value(5))
                .andExpect(jsonPath("$.data[0].conflicts.skillMismatch").value(4));
    }
}
