package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.curriculum.CurriculumWithLevelsDTO;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.PLO;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.entities.enums.LevelStatus;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_GetAllCurriculumsWithLevels_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-GACL-01
    // =========================
    @Test
    void TC_GACL01_emptyList() {
        when(curriculumRepository.findAll(any(Sort.class)))
                .thenReturn(List.of());

        List<CurriculumWithLevelsDTO> result =
                service.getAllCurriculumsWithLevels();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // =========================
    // TC-GACL-02
    // =========================
    @Test
    void TC_GACL02_single_noLevel_noPLO() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum 1")
                .status(CurriculumStatus.DRAFT)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .plos(Set.of())
                .build();

        when(curriculumRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(curriculum));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        List<CurriculumWithLevelsDTO> result =
                service.getAllCurriculumsWithLevels();

        assertEquals(1, result.size());
        assertEquals("CURR-01", result.get(0).getCode());
        assertTrue(result.get(0).getLevels().isEmpty());
        assertTrue(result.get(0).getPlos().isEmpty());
    }

    // =========================
    // TC-GACL-03
    // =========================
    @Test
    void TC_GACL03_single_withLevels_andPLOs() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum Full")
                .status(CurriculumStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .plos(Set.of(
                        PLO.builder().code("PLO1").description("Desc1").build(),
                        PLO.builder().code("PLO2").description("Desc2").build()
                ))
                .build();

        Level level1 = Level.builder()
                .id(10L)
                .code("L1")
                .name("Level 1")
                .status(LevelStatus.ACTIVE)
                .sortOrder(1)
                .curriculum(curriculum)
                .build();

        Level level2 = Level.builder()
                .id(11L)
                .code("L2")
                .name("Level 2")
                .status(LevelStatus.DRAFT)
                .sortOrder(2)
                .curriculum(curriculum)
                .build();

        when(curriculumRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(curriculum));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(level1, level2));

        List<CurriculumWithLevelsDTO> result =
                service.getAllCurriculumsWithLevels();

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getLevels().size());
        assertEquals(2, result.get(0).getPlos().size());
        assertEquals("L1", result.get(0).getLevels().get(0).getCode());
    }

    // =========================
    // TC-GACL-04
    // =========================
    @Test
    void TC_GACL04_multipleCurriculums() {
        Curriculum c1 = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .updatedAt(OffsetDateTime.now())
                .plos(Set.of())
                .build();

        Curriculum c2 = Curriculum.builder()
                .id(2L)
                .code("CURR-02")
                .updatedAt(OffsetDateTime.now())
                .plos(Set.of())
                .build();

        when(curriculumRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(c1, c2));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(anyLong()))
                .thenReturn(List.of());

        List<CurriculumWithLevelsDTO> result =
                service.getAllCurriculumsWithLevels();

        assertEquals(2, result.size());
    }
}
