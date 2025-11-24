package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.entities.Course;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.CourseRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "test@example.com", roles = { "ACADEMIC_AFFAIR", "ADMIN", "CENTER_HEAD", "MANAGER" })
class CourseControllerIT {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private SubjectRepository subjectRepository;

        @Autowired
        private LevelRepository levelRepository;

        @Autowired
        private CourseRepository courseRepository;

        private Subject englishSubject;
        private Subject japaneseSubject;
        private Level levelA1;
        private Level levelA2;
        private Level levelN5;

        @BeforeEach
        void setUp() {
                courseRepository.deleteAll();
                levelRepository.deleteAll();
                subjectRepository.deleteAll();

                // Create Subjects
                englishSubject = TestDataBuilder.buildSubject()
                                .code("ENG")
                                .name("English")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                englishSubject = subjectRepository.save(englishSubject);

                japaneseSubject = TestDataBuilder.buildSubject()
                                .code("JPN")
                                .name("Japanese")
                                .status(SubjectStatus.ACTIVE)
                                .build();
                japaneseSubject = subjectRepository.save(japaneseSubject);

                // Create Levels
                levelA1 = TestDataBuilder.buildLevel()
                                .subject(englishSubject)
                                .code("A1")
                                .name("Beginner A1")
                                .sortOrder(1)
                                .build();

                levelA2 = TestDataBuilder.buildLevel()
                                .subject(englishSubject)
                                .code("A2")
                                .name("Beginner A2")
                                .sortOrder(2)
                                .build();

                levelN5 = TestDataBuilder.buildLevel()
                                .subject(japaneseSubject)
                                .code("N5")
                                .name("Beginner N5")
                                .sortOrder(1)
                                .build();

                List<Level> levels = levelRepository.saveAll(List.of(levelA1, levelA2, levelN5));
                levelA1 = levels.get(0);
                levelA2 = levels.get(1);
                levelN5 = levels.get(2);

                // Create Courses
                Course courseEngA1 = new Course();
                courseEngA1.setCode("ENG-A1");
                courseEngA1.setName("English A1 Course");
                courseEngA1.setSubject(englishSubject);
                courseEngA1.setLevel(levelA1);
                courseEngA1.setTotalHours(48);
                courseEngA1.setStatus(org.fyp.tmssep490be.entities.enums.CourseStatus.ACTIVE);

                Course courseEngA2 = new Course();
                courseEngA2.setCode("ENG-A2");
                courseEngA2.setName("English A2 Course");
                courseEngA2.setSubject(englishSubject);
                courseEngA2.setLevel(levelA2);
                courseEngA2.setTotalHours(48);
                courseEngA2.setStatus(org.fyp.tmssep490be.entities.enums.CourseStatus.ACTIVE);

                Course courseJpnN5 = new Course();
                courseJpnN5.setCode("JPN-N5");
                courseJpnN5.setName("Japanese N5 Course");
                courseJpnN5.setSubject(japaneseSubject);
                courseJpnN5.setLevel(levelN5);
                courseJpnN5.setTotalHours(60);
                courseJpnN5.setStatus(org.fyp.tmssep490be.entities.enums.CourseStatus.ACTIVE);

                courseRepository.saveAll(List.of(courseEngA1, courseEngA2, courseJpnN5));
        }

        @Test
        @DisplayName("Should return all courses when no filters provided")
        void shouldReturnAllCourses() throws Exception {
                mockMvc.perform(get("/api/v1/courses")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data", hasSize(3)));
        }

        @Test
        @DisplayName("Should filter courses by subjectId")
        void shouldFilterCoursesBySubjectId() throws Exception {
                mockMvc.perform(get("/api/v1/courses")
                                .param("subjectId", englishSubject.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data", hasSize(2)))
                                .andExpect(jsonPath("$.data[0].code", is("ENG-A1")))
                                .andExpect(jsonPath("$.data[1].code", is("ENG-A2")));
        }

        @Test
        @DisplayName("Should filter courses by levelId")
        void shouldFilterCoursesByLevelId() throws Exception {
                mockMvc.perform(get("/api/v1/courses")
                                .param("levelId", levelA1.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data", hasSize(1)))
                                .andExpect(jsonPath("$.data[0].code", is("ENG-A1")));
        }

        @Test
        @DisplayName("Should filter courses by subjectId and levelId")
        void shouldFilterCoursesBySubjectIdAndLevelId() throws Exception {
                mockMvc.perform(get("/api/v1/courses")
                                .param("subjectId", englishSubject.getId().toString())
                                .param("levelId", levelA1.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data", hasSize(1)))
                                .andExpect(jsonPath("$.data[0].code", is("ENG-A1")));
        }

        @Test
        @DisplayName("Should return empty list when no courses match filters")
        void shouldReturnEmptyListWhenNoMatch() throws Exception {
                mockMvc.perform(get("/api/v1/courses")
                                .param("subjectId", japaneseSubject.getId().toString())
                                .param("levelId", levelA1.getId().toString()) // Mismatch
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)))
                                .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("Should allow SUBJECT_LEADER to create course")
        @WithMockUser(roles = { "SUBJECT_LEADER" })
        void shouldAllowSubjectLeaderToCreateCourse() throws Exception {
                org.fyp.tmssep490be.dtos.course.CreateCourseRequestDTO request = new org.fyp.tmssep490be.dtos.course.CreateCourseRequestDTO();
                org.fyp.tmssep490be.dtos.course.CourseBasicInfoDTO basicInfo = new org.fyp.tmssep490be.dtos.course.CourseBasicInfoDTO();
                basicInfo.setName("New Course");
                basicInfo.setCode("NEW-COURSE");
                basicInfo.setSubjectId(englishSubject.getId());
                basicInfo.setLevelId(levelA1.getId());
                basicInfo.setDurationHours(48);
                request.setBasicInfo(basicInfo);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/courses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success", is(true)));
        }

        @Test
        @DisplayName("Should forbid ADMIN from creating course")
        @WithMockUser(roles = { "ADMIN" })
        void shouldForbidAdminFromCreatingCourse() throws Exception {
                org.fyp.tmssep490be.dtos.course.CreateCourseRequestDTO request = new org.fyp.tmssep490be.dtos.course.CreateCourseRequestDTO();
                org.fyp.tmssep490be.dtos.course.CourseBasicInfoDTO basicInfo = new org.fyp.tmssep490be.dtos.course.CourseBasicInfoDTO();
                basicInfo.setName("New Course");
                basicInfo.setCode("NEW-COURSE");
                basicInfo.setSubjectId(englishSubject.getId());
                basicInfo.setLevelId(levelA1.getId());
                basicInfo.setDurationHours(48);
                request.setBasicInfo(basicInfo);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/v1/courses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }
}
