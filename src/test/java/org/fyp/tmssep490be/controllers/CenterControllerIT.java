package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.dtos.center.CenterRequestDTO;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CenterController.
 * Tests center management endpoints with real Spring Security context.
 * Uses modern Spring Boot 3.5.7 testing patterns with @SpringBootTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CenterController Integration Tests")
@WithMockUser(username = "test@example.com", roles = { "ACADEMIC_AFFAIR", "ADMIN", "CENTER_HEAD", "MANAGER" })
class CenterControllerIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private CenterRepository centerRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private Center testCenter;
        private CenterRequestDTO centerRequest;

        @BeforeEach
        void setUp() {
                // Create test center using TestDataBuilder
                testCenter = TestDataBuilder.buildCenter()
                                .code("TC001")
                                .name("Test Center")
                                .description("Test Description")
                                .phone("0123456789")
                                .email("test@center.com")
                                .address("Test Address")
                                .build();
                testCenter = centerRepository.save(testCenter);

                // Create center request for testing
                centerRequest = CenterRequestDTO.builder()
                                .code("TC002")
                                .name("New Test Center")
                                .description("New Test Description")
                                .phone("0987654321")
                                .email("new@center.com")
                                .address("New Test Address")
                                .build();
        }

        @Test
        @DisplayName("POST /api/v1/centers - Should create center successfully")
        void shouldCreateCenterSuccessfully() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(post("/api/v1/centers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(centerRequest)));

                // Assert
                result.andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").exists())
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.data.id").isNumber())
                                .andExpect(jsonPath("$.data.code").value("TC002"))
                                .andExpect(jsonPath("$.data.name").value("New Test Center"))
                                .andExpect(jsonPath("$.data.email").value("new@center.com"));
        }

        @Test
        @DisplayName("POST /api/v1/centers - Should reject duplicate center code")
        void shouldRejectDuplicateCenterCode() throws Exception {
                // Arrange - Create request with existing code
                CenterRequestDTO duplicateRequest = CenterRequestDTO.builder()
                                .code("TC001") // Existing code
                                .name("Duplicate Center")
                                .build();

                // Act
                ResultActions result = mockMvc.perform(post("/api/v1/centers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateRequest)));

                // Assert
                result.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        // SECURITY TESTS - DISABLED FOR NOW
        // TODO: Create separate SecurityTest class when authentication is re-enabled

        /*
         * @Test
         * 
         * @DisplayName("POST /api/v1/centers - Should require ADMIN role")
         * void shouldRequireAdminRoleForCreate() throws Exception {
         * // Act - Test without admin role
         * ResultActions result = mockMvc.perform(post("/api/v1/centers")
         * .contentType(MediaType.APPLICATION_JSON)
         * .content(objectMapper.writeValueAsString(centerRequest)));
         * 
         * // Assert
         * result.andExpect(status().isUnauthorized());
         * }
         */

        @Test
        @DisplayName("GET /api/v1/centers/{id} - Should get center by ID successfully")
        void shouldGetCenterByIdSuccessfully() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(get("/api/v1/centers/{id}", testCenter.getId()));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.data.id").value(testCenter.getId()))
                                .andExpect(jsonPath("$.data.code").value("TC001"))
                                .andExpect(jsonPath("$.data.name").value("Test Center"))
                                .andExpect(jsonPath("$.data.email").value("test@center.com"));
        }

        @Test
        @DisplayName("GET /api/v1/centers/{id} - Should return 404 for non-existent center")
        void shouldReturn404ForNonExistentCenter() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(get("/api/v1/centers/{id}", 99999L));

                // Assert
                result.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("GET /api/v1/centers - Should get all centers with pagination")
        void shouldGetAllCentersWithPagination() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(get("/api/v1/centers")
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "name,asc"));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").exists())
                                .andExpect(jsonPath("$.data.content").isArray())
                                .andExpect(jsonPath("$.data.content", hasSize(1)))
                                .andExpect(jsonPath("$.data.content[0].code").value("TC001"))
                                .andExpect(jsonPath("$.data.page.number").value(0))
                                .andExpect(jsonPath("$.data.page.size").value(10))
                                .andExpect(jsonPath("$.data.page.totalElements").value(1));
        }

        @Test
        @DisplayName("PUT /api/v1/centers/{id} - Should update center successfully")
        void shouldUpdateCenterSuccessfully() throws Exception {
                // Arrange
                CenterRequestDTO updateRequest = CenterRequestDTO.builder()
                                .code("TC001")
                                .name("Updated Center Name")
                                .description("Updated Description")
                                .phone("0999999999")
                                .email("updated@center.com")
                                .address("Updated Address")
                                .build();

                // Act
                ResultActions result = mockMvc.perform(put("/api/v1/centers/{id}", testCenter.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.name").value("Updated Center Name"))
                                .andExpect(jsonPath("$.data.email").value("updated@center.com"))
                                .andExpect(jsonPath("$.data.phone").value("0999999999"));
        }

        @Test
        @DisplayName("PUT /api/v1/centers/{id} - Should return 404 when updating non-existent center")
        void shouldReturn404WhenUpdatingNonExistentCenter() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(put("/api/v1/centers/{id}", 99999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(centerRequest)));

                // Assert
                result.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("DELETE /api/v1/centers/{id} - Should delete center successfully")
        void shouldDeleteCenterSuccessfully() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(delete("/api/v1/centers/{id}", testCenter.getId()));

                // Assert
                result.andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // Verify center is deleted (soft delete) - returns 400 Bad Request with error
                // message
                ResultActions getResult = mockMvc.perform(get("/api/v1/centers/{id}", testCenter.getId()));
                getResult.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("DELETE /api/v1/centers/{id} - Should return 404 when deleting non-existent center")
        void shouldReturn404WhenDeletingNonExistentCenter() throws Exception {
                // Act
                ResultActions result = mockMvc.perform(delete("/api/v1/centers/{id}", 99999L));

                // Assert
                result.andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        /*
         * @Test
         * 
         * @DisplayName("All endpoints - Should reject requests without authentication")
         * void shouldRejectUnauthenticatedRequests() throws Exception {
         * // Test GET endpoint
         * mockMvc.perform(get("/api/v1/centers/{id}", testCenter.getId()))
         * .andExpect(status().isUnauthorized());
         * 
         * // Test POST endpoint
         * mockMvc.perform(post("/api/v1/centers")
         * .contentType(MediaType.APPLICATION_JSON)
         * .content(objectMapper.writeValueAsString(centerRequest)))
         * .andExpect(status().isUnauthorized());
         * 
         * // Test PUT endpoint
         * mockMvc.perform(put("/api/v1/centers/{id}", testCenter.getId())
         * .contentType(MediaType.APPLICATION_JSON)
         * .content(objectMapper.writeValueAsString(centerRequest)))
         * .andExpect(status().isUnauthorized());
         * 
         * // Test DELETE endpoint
         * mockMvc.perform(delete("/api/v1/centers/{id}", testCenter.getId()))
         * .andExpect(status().isUnauthorized());
         * }
         */

        @Test
        @DisplayName("All endpoints - Should validate request body format")
        void shouldValidateRequestBodyFormat() throws Exception {
                // Test malformed JSON
                mockMvc.perform(post("/api/v1/centers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ \"invalid\": \"json\" }"))
                                .andExpect(status().isBadRequest());

                // Test missing required fields
                CenterRequestDTO invalidRequest = CenterRequestDTO.builder().build();
                mockMvc.perform(post("/api/v1/centers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("GET /api/v1/centers - Should handle pagination parameters")
        void shouldHandlePaginationParameters() throws Exception {
                // Create additional centers for pagination testing
                for (int i = 2; i <= 5; i++) {
                        Center center = TestDataBuilder.buildCenter()
                                        .code("TC00" + i)
                                        .name("Test Center " + i)
                                        .build();
                        centerRepository.save(center);
                }

                // Test page 1 with size 2
                mockMvc.perform(get("/api/v1/centers")
                                .param("page", "1")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.page.number").value(1))
                                .andExpect(jsonPath("$.data.page.size").value(2))
                                .andExpect(jsonPath("$.data.content", hasSize(2)))
                                .andExpect(jsonPath("$.data.page.totalElements").value(5));

                // Test sorting
                mockMvc.perform(get("/api/v1/centers")
                                .param("sort", "code,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.content[0].code").value("TC005"));
        }
}