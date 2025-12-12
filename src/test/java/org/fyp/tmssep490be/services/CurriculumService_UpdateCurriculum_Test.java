package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.curriculum.CreateCurriculumDTO;
import org.fyp.tmssep490be.dtos.curriculum.CreatePLODTO;
import org.fyp.tmssep490be.dtos.curriculum.CurriculumResponseDTO;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.PLO;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_UpdateCurriculum_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-UC-01
    // =========================
    @Test
    void TC_UC01_notFound_throwException() {
        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-01")
                .name("Updated")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateCurriculum(1L, request));
    }

    // =========================
    // TC-UC-02  ✅ FIX
    // =========================
    @Test
    void TC_UC02_updateBasicInfo_success() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Old Name")
                .description("Old Desc")
                .language("English")
                .status(CurriculumStatus.DRAFT)
                .createdAt(OffsetDateTime.now())
                .plos(new HashSet<>())   // 🔥 FIX
                .levels(new HashSet<>()) // 🔥 FIX
                .build();

        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-01")
                .name("New Name")
                .description("New Desc")
                .language("Vietnamese")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(curriculumRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        CurriculumResponseDTO result =
                service.updateCurriculum(1L, request);

        assertEquals("New Name", result.getName());
        assertEquals("New Desc", result.getDescription());
        assertEquals("Vietnamese", result.getLanguage());
    }

    // =========================
    // TC-UC-03  ✅ FIX
    // =========================
    @Test
    void TC_UC03_changeCode_unique_success() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("OLD")
                .name("Curriculum")
                .plos(new HashSet<>())   // 🔥 FIX
                .levels(new HashSet<>()) // 🔥 FIX
                .build();

        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("NEW")
                .name("Curriculum")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(curriculumRepository.existsByCode("NEW"))
                .thenReturn(false);
        when(curriculumRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        CurriculumResponseDTO result =
                service.updateCurriculum(1L, request);

        assertEquals("NEW", result.getCode());
    }

    // =========================
    // TC-UC-04
    // =========================
    @Test
    void TC_UC04_changeCode_duplicate_throwException() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("OLD")
                .name("Curriculum")
                .plos(new HashSet<>())
                .levels(new HashSet<>())
                .build();

        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("DUPLICATE")
                .name("Curriculum")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(curriculumRepository.existsByCode("DUPLICATE"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.updateCurriculum(1L, request));
    }

    // =========================
    // TC-UC-05  ✅ FIX (QUAN TRỌNG NHẤT)
    // =========================
    @Test
    void TC_UC05_updatePLO_mergeStrategy() {
        PLO oldPlo = PLO.builder()
                .code("PLO1")
                .description("Old Desc")
                .build();

        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum")
                .plos(new HashSet<>(Set.of(oldPlo))) // 🔥 FIX
                .levels(new HashSet<>())             // 🔥 FIX
                .build();

        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-01")
                .name("Curriculum")
                .plos(List.of(
                        CreatePLODTO.builder()
                                .code("PLO1")
                                .description("Updated Desc")
                                .build(),
                        CreatePLODTO.builder()
                                .code("PLO2")
                                .description("New PLO")
                                .build()
                ))
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(curriculumRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        CurriculumResponseDTO result =
                service.updateCurriculum(1L, request);

        assertEquals(2, result.getPlos().size());
        assertTrue(result.getPlos().stream()
                .anyMatch(p -> p.getCode().equals("PLO2")));
    }
}
