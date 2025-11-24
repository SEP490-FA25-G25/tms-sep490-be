package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "test@example.com", roles = { "ACADEMIC_AFFAIR", "ADMIN", "CENTER_HEAD", "MANAGER" })
class CurriculumControllerIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private SubjectRepository subjectRepository;

        @Autowired
        private LevelRepository levelRepository;

        private Subject englishSubject;
        private Subject chineseSubject;
        private List<Level> englishLevels;
        private List<Level> chineseLevels;

        @BeforeEach
        void setUp() {
                // Clean existing data
                levelRepository.deleteAll();
                subjectRepository.deleteAll();

                // Create test subjects
                englishSubject = TestDataBuilder.buildSubject()
                                .code("ENG")
                                .name("English")
                                .description("English Language Courses")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                englishSubject = subjectRepository.save(englishSubject);

                chineseSubject = TestDataBuilder.buildSubject()
                                .code("CHI")
                                .name("Chinese")
                                .description("Chinese Language Courses")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                chineseSubject = subjectRepository.save(chineseSubject);

                // Create test levels for English
                englishLevels = List.of(
                                TestDataBuilder.buildLevel()
                                                .subject(englishSubject)
                                                .code("A1")
                                                .name("Beginner A1")
                                                .description("Beginner Level A1")
                                                .expectedDurationHours(80)
                                                .sortOrder(1)
                                                .build(),
                                TestDataBuilder.buildLevel()
                                                .subject(englishSubject)
                                                .code("A2")
                                                .name("Beginner A2")
                                                .description("Beginner Level A2")
                                                .expectedDurationHours(100)
                                                .sortOrder(2)
                                                .build(),
                                TestDataBuilder.buildLevel()
                                                .subject(englishSubject)
                                                .code("B1")
                                                .name("Intermediate B1")
                                                .description("Intermediate Level B1")
                                                .expectedDurationHours(120)
                                                .sortOrder(3)
                                                .build());
                englishLevels = levelRepository.saveAll(englishLevels);

                // Create test levels for Chinese
                chineseLevels = List.of(
                                TestDataBuilder.buildLevel()
                                                .subject(chineseSubject)
                                                .code("HSK1")
                                                .name("HSK Level 1")
                                                .description("HSK Level 1")
                                                .expectedDurationHours(60)
                                                .sortOrder(1)
                                                .build(),
                                TestDataBuilder.buildLevel()
                                                .subject(chineseSubject)
                                                .code("HSK2")
                                                .name("HSK Level 2")
                                                .description("HSK Level 2")
                                                .expectedDurationHours(80)
                                                .sortOrder(2)
                                                .build());
                chineseLevels = levelRepository.saveAll(chineseLevels);
        }

        @Test
        @DisplayName("Should return all subjects with their levels")
        void shouldReturnAllSubjectsWithLevels() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.message").value("Subjects with levels retrieved successfully"))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data", hasSize(2)))

                                // Check subjects are sorted by code (ENG, CHI)
                                .andExpect(jsonPath("$.data[0].code").value("CHI"))
                                .andExpect(jsonPath("$.data[0].name").value("Chinese"))
                                .andExpect(jsonPath("$.data[0].description").value("Chinese Language Courses"))
                                .andExpect(jsonPath("$.data[0].status").value(SubjectStatus.ACTIVE.name()))
                                .andExpect(jsonPath("$.data[0].levels").isArray())
                                .andExpect(jsonPath("$.data[0].levels", hasSize(2)))

                                // Check Chinese levels are sorted by sortOrder
                                .andExpect(jsonPath("$.data[0].levels[0].code").value("HSK1"))
                                .andExpect(jsonPath("$.data[0].levels[1].code").value("HSK2"))

                                // Check second subject (English)
                                .andExpect(jsonPath("$.data[1].code").value("ENG"))
                                .andExpect(jsonPath("$.data[1].name").value("English"))
                                .andExpect(jsonPath("$.data[1].description").value("English Language Courses"))
                                .andExpect(jsonPath("$.data[1].status").value(SubjectStatus.ACTIVE.name()))
                                .andExpect(jsonPath("$.data[1].levels").isArray())
                                .andExpect(jsonPath("$.data[1].levels", hasSize(3)))

                                // Check English levels are sorted by sortOrder
                                .andExpect(jsonPath("$.data[1].levels[0].code").value("A1"))
                                .andExpect(jsonPath("$.data[1].levels[1].code").value("A2"))
                                .andExpect(jsonPath("$.data[1].levels[2].code").value("B1"));
        }

        @Test
        @DisplayName("Should return only active subjects")
        void shouldReturnOnlyActiveSubjects() throws Exception {
                // Create an inactive subject
                Subject inactiveSubject = TestDataBuilder.buildSubject()
                                .code("INACTIVE")
                                .name("Inactive Subject")
                                .status(SubjectStatus.INACTIVE)
                                .build();
                subjectRepository.save(inactiveSubject);

                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data", hasSize(2))) // Only active subjects
                                .andExpect(jsonPath("$.data[*].code", not(hasItem("INACTIVE"))));
        }

        @Test
        @DisplayName("Should return empty list when no subjects exist")
        void shouldReturnEmptyListWhenNoSubjectsExist() throws Exception {
                // Delete all subjects
                subjectRepository.deleteAll();

                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return subject with empty levels when no levels exist")
        void shouldReturnSubjectWithEmptyLevelsWhenNoLevelsExist() throws Exception {
                // Create subject without levels
                Subject subjectWithoutLevels = TestDataBuilder.buildSubject()
                                .code("NO_LEVELS")
                                .name("Subject Without Levels")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                subjectRepository.save(subjectWithoutLevels);

                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data", hasSize(3)))
                                // Check that we have a subject with empty levels
                                .andExpect(jsonPath("$.data[*].levels", hasItems(hasSize(0))));
        }

        @Test
        @DisplayName("Should return all levels when no subject filter provided")
        void shouldReturnAllLevelsWhenNoFilter() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data", hasSize(5))); // 3 English + 2 Chinese
        }

        @Test
        @DisplayName("Should return levels filtered by subject")
        void shouldReturnLevelsFilteredBySubject() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/levels")
                                .param("subjectId", englishSubject.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data", hasSize(3)))
                                .andExpect(jsonPath("$.data[0].subjectCode").value("ENG"))
                                .andExpect(jsonPath("$.data[1].subjectCode").value("ENG"))
                                .andExpect(jsonPath("$.data[2].subjectCode").value("ENG"));
        }

        // SECURITY TESTS
        // These tests verify role-based access control

        @Test
        @DisplayName("Should require authentication")
        @WithMockUser(roles = { "INVALID_ROLE" })
        void shouldRequireAuthentication() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should require valid role")
        @WithMockUser(roles = { "STUDENT" })
        void shouldRequireValidRole() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should allow access with CENTER_HEAD role")
        @WithMockUser(roles = { "CENTER_HEAD" })
        void shouldAllowAccessWithCenterHeadRole() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access with MANAGER role")
        @WithMockUser(roles = { "MANAGER" })
        void shouldAllowAccessWithManagerRole() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access with ACADEMIC_AFFAIR role")
        @WithMockUser(roles = { "ACADEMIC_AFFAIR" })
        void shouldAllowAccessWithAcademicAffairRole() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access with ADMIN role")
        @WithMockUser(roles = { "ADMIN" })
        void shouldAllowAccessWithAdminRole() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access with SUBJECT_LEADER role")
        @WithMockUser(roles = { "SUBJECT_LEADER" })
        void shouldAllowAccessWithSubjectLeaderRole() throws Exception {
                mockMvc.perform(get("/api/v1/curriculum/subjects-with-levels")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should create subject and return createdAt")
        @WithMockUser(roles = { "ADMIN" })
        void shouldCreateSubjectAndReturnCreatedAt() throws Exception {
                org.fyp.tmssep490be.dtos.curriculum.CreateSubjectDTO request = new org.fyp.tmssep490be.dtos.curriculum.CreateSubjectDTO();
                request.setCode("NEW_SUB");
                request.setName("New Subject");
                request.setDescription("Description");

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/curriculum/subjects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("Should allow SUBJECT_LEADER to create level")
        @WithMockUser(roles = { "SUBJECT_LEADER" })
        void shouldAllowSubjectLeaderToCreateLevel() throws Exception {
                org.fyp.tmssep490be.dtos.curriculum.CreateLevelDTO request = new org.fyp.tmssep490be.dtos.curriculum.CreateLevelDTO();
                request.setSubjectId(englishSubject.getId());
                request.setCode("A3");
                request.setName("Beginner A3");
                request.setDurationHours(100);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/curriculum/levels")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }
}