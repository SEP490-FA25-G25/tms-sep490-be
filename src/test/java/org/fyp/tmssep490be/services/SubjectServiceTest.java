package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.subject.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.SubjectRepository;

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private SubjectService subjectService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        // Block ambiguous Example findAll
        when(subjectRepository.findAll(Mockito.<org.springframework.data.domain.Example<Subject>>any()))
                .thenThrow(new UnsupportedOperationException("Example findAll not used"));
    }

    // =============================================================================================
    //  REGION: TEST getAllSubjects()
    // =============================================================================================

    @Nested
    class GetAllSubjectsTests {

        @Test
        void test_getAllSubjects_filterByCurriculumAndLevel() {
            Long curriculumId = 1L;
            Long levelId = 2L;

            Subject s = Subject.builder()
                    .id(10L)
                    .name("Subject A")
                    .code("SUB-A")
                    .status(SubjectStatus.DRAFT)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            when(subjectRepository.findByCurriculumIdAndLevelIdOrderByUpdatedAtDesc(curriculumId, levelId))
                    .thenReturn(List.of(s));

            List<SubjectDTO> result = subjectService.getAllSubjects(curriculumId, levelId);

            assertEquals(1, result.size());
            assertEquals("SUB-A", result.get(0).getCode());
        }

        @Test
        void test_getAllSubjects_filterByCurriculumOnly() {
            Long curriculumId = 1L;

            Subject s = Subject.builder()
                    .id(10L)
                    .name("Subject B")
                    .code("SUB-B")
                    .status(SubjectStatus.ACTIVE)
                    .approvalStatus(ApprovalStatus.PENDING)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            when(subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId))
                    .thenReturn(List.of(s));

            List<SubjectDTO> result = subjectService.getAllSubjects(curriculumId, null);

            assertEquals(1, result.size());
            assertEquals("SUB-B", result.get(0).getCode());
        }

        @Test
        void test_getAllSubjects_filterByLevelOnly() {
            Long levelId = 3L;

            Subject s = Subject.builder()
                    .id(20L)
                    .name("Subject C")
                    .code("SUB-C")
                    .status(SubjectStatus.DRAFT)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            when(subjectRepository.findByLevelIdOrderByUpdatedAtDesc(levelId))
                    .thenReturn(List.of(s));

            List<SubjectDTO> result = subjectService.getAllSubjects(null, levelId);

            assertEquals(1, result.size());
            assertEquals("SUB-C", result.get(0).getCode());
        }

        @Test
        void test_getAllSubjects_noFilters() {
            Subject s = Subject.builder()
                    .id(30L)
                    .name("Subject D")
                    .code("SUB-D")
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            when(subjectRepository.findAll(Mockito.<Sort>argThat(sort -> {
                Sort.Order order = sort.getOrderFor("updatedAt");
                return order != null && order.getDirection() == Sort.Direction.DESC;
            }))).thenReturn(List.of(s));

            List<SubjectDTO> result = subjectService.getAllSubjects(null, null);

            assertEquals(1, result.size());
            assertEquals("SUB-D", result.get(0).getCode());
        }
    }

    // =============================================================================================
    //  REGION: TEST getSubjectDetails()
    // =============================================================================================

    @Nested
    class GetSubjectDetailsTests {

        @Test
        void test_getSubjectDetails_success() {

            OffsetDateTime now = OffsetDateTime.now();

            // SUBJECT ------------------------------------------------------
            Subject subject = Subject.builder()
                    .id(1L)
                    .name("Java Programming")
                    .code("JV101")
                    .description("Intro Java")
                    .thumbnailUrl("img.png")
                    .status(SubjectStatus.ACTIVE)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .effectiveDate(LocalDate.now())
                    .totalHours(60)
                    .hoursPerSession(new BigDecimal("2.0"))
                    .submittedAt(now)
                    .decidedAt(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            // CURRICULUM ---------------------------------------------------
            Curriculum curriculum = new Curriculum();
            curriculum.setId(10L);
            curriculum.setName("Software Engineering");
            subject.setCurriculum(curriculum);

            // LEVEL --------------------------------------------------------
            Level level = new Level();
            level.setId(5L);
            level.setName("Level 5");
            subject.setLevel(level);

            // CLO ----------------------------------------------------------
            CLO clo1 = CLO.builder()
                    .id(100L)
                    .subject(subject)
                    .code("CLO1")
                    .description("Understand OOP")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            // PLO mapping
            PLO plo = new PLO();
            plo.setId(99L);
            plo.setCode("PLO1");

            PLOCLOMapping m1 = PLOCLOMapping.builder()
                    .id(new PLOCLOMapping.PLOCLOMappingId(plo.getId(), clo1.getId()))
                    .plo(plo)
                    .clo(clo1)
                    .status(MappingStatus.ACTIVE)
                    .build();
            clo1.getPloCloMappings().add(m1);
            subject.getClos().add(clo1);

            // SUBJECT PHASE -----------------------------------------------
            SubjectPhase phase1 = SubjectPhase.builder()
                    .id(200L)
                    .phaseNumber(1)
                    .name("Phase 1")
                    .description("Basics")
                    .subject(subject)
                    .build();

            // SESSION -----------------------------------------------------
            SubjectSession session1 = SubjectSession.builder()
                    .id(300L)
                    .phase(phase1)
                    .sequenceNo(1)
                    .topic("Intro")
                    .studentTask("Read")
                    .skills(new ArrayList<>(List.of(Skill.READING)))
                    .build();

            phase1.getSubjectSessions().add(session1);

            // SESSION-CLO mapping
            SubjectSessionCLOMapping sm = SubjectSessionCLOMapping.builder()
                    .id(new SubjectSessionCLOMapping.SubjectSessionCLOMappingId(session1.getId(), clo1.getId()))
                    .subjectSession(session1)
                    .clo(clo1)
                    .status(MappingStatus.ACTIVE)
                    .build();
            session1.getSubjectSessionCLOMappings().add(sm);

            subject.getSubjectPhases().add(phase1);

            // PHASE MATERIAL ----------------------------------------------
            SubjectMaterial material1 = SubjectMaterial.builder()
                    .id(400L)
                    .title("Slide 1")
                    .materialType(MaterialType.DOCUMENT)
                    .url("file.pdf")
                    .phase(phase1)
                    .build();
            subject.getSubjectMaterials().add(material1);

            // ASSESSMENT --------------------------------------------------
            SubjectAssessment assess = SubjectAssessment.builder()
                    .id(500L)
                    .subject(subject)
                    .name("Quiz 1")
                    .kind(AssessmentKind.QUIZ)
                    .maxScore(new BigDecimal("10"))
                    .description("Short quiz")
                    .skills(new ArrayList<>(List.of(Skill.GRAMMAR)))   // FIX: List<Skill>
                    .build();

            // ASSESSMENT-CLO mapping
            SubjectAssessmentCLOMapping acm = SubjectAssessmentCLOMapping.builder()
                    .id(new SubjectAssessmentCLOMapping.SubjectAssessmentCLOMappingId(500L, clo1.getId()))
                    .subjectAssessment(assess)
                    .clo(clo1)
                    .status(MappingStatus.ACTIVE)
                    .build();

            assess.getSubjectAssessmentCLOMappings().add(acm);
            subject.getSubjectAssessments().add(assess);

            when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));

            // EXECUTE -----------------------------------------------------
            SubjectDetailDTO dto = subjectService.getSubjectDetails(1L);

            assertEquals("Java Programming", dto.getName());
            assertEquals("JV101", dto.getCode());
            assertEquals(1, dto.getPhases().size());
            assertEquals(1, dto.getClos().size());
            assertEquals(1, dto.getAssessments().size());
        }

        @Test
        void test_getSubjectDetails_notFound() {
            when(subjectRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> subjectService.getSubjectDetails(99L));
        }
    }
}
