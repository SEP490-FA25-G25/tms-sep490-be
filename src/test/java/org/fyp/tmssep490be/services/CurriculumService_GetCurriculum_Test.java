package org.fyp.tmssep490be.services;
import java.util.Set;
import org.fyp.tmssep490be.dtos.curriculum.CurriculumResponseDTO;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.PLO;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.entities.enums.LevelStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_GetCurriculum_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-GC-01
    // =========================
    @Test
    void TC_GC01_notFound() {
        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getCurriculum(1L));
    }

    // =========================
    // TC-GC-02
    // =========================
    @Test
    void TC_GC02_success_noLevel_noPLO() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Test Curriculum")
                .status(CurriculumStatus.DRAFT)
                .createdAt(OffsetDateTime.now())
                .plos(Set.of())
                .levels(Set.of())
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        CurriculumResponseDTO result = service.getCurriculum(1L);

        assertEquals(0, result.getLevelCount());
        assertTrue(result.getPlos().isEmpty());
        assertTrue(result.getLevels().isEmpty());
    }

    // =========================
    // TC-GC-03
    // =========================
    @Test
    void TC_GC03_success_withLevels() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum With Level")
                .status(CurriculumStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .plos(Set.of())
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

        curriculum.setLevels(Set.of(level1, level2));

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(level1, level2));

        CurriculumResponseDTO result = service.getCurriculum(1L);

        assertEquals(2, result.getLevelCount());
        assertEquals(2, result.getLevels().size());
    }

    // =========================
    // TC-GC-04
    // =========================
    @Test
    void TC_GC04_success_withPLOs() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum With PLO")
                .status(CurriculumStatus.DRAFT)
                .createdAt(OffsetDateTime.now())
                .plos(Set.of(
                        PLO.builder().code("PLO1").description("Desc1").build(),
                        PLO.builder().code("PLO2").description("Desc2").build()
                ))
                .levels(Set.of())
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        CurriculumResponseDTO result = service.getCurriculum(1L);

        assertEquals(2, result.getPlos().size());
    }
}
