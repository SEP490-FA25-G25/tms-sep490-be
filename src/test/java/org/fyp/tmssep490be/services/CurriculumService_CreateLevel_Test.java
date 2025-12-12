package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.curriculum.CreateLevelDTO;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Level;   // ⭐ BẮT BUỘC
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CurriculumService_CreateLevel_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-CL-01
    // =========================
    @Test
    void TC_CL01_curriculumNotFound() {
        CreateLevelDTO request = CreateLevelDTO.builder()
                .curriculumId(1L)
                .code("L1")
                .name("Level 1")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createLevel(request));

        verify(levelRepository, never()).save(any());
    }

    // =========================
    // TC-CL-02
    // =========================
    @Test
    void TC_CL02_noExistingLevel_sortOrder1() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum")
                .build();

        CreateLevelDTO request = CreateLevelDTO.builder()
                .curriculumId(1L)
                .code("L1")
                .name("Level 1")
                .description("Desc")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.findMaxSortOrderByCurriculumId(1L))
                .thenReturn(null);

        // 🔑 FIX NPE
        when(levelRepository.save(any(Level.class)))
                .thenAnswer(invocation -> {
                    Level l = invocation.getArgument(0);
                    l.setId(100L);
                    return l;
                });

        ArgumentCaptor<Level> captor = ArgumentCaptor.forClass(Level.class);

        service.createLevel(request);

        verify(levelRepository).save(captor.capture());

        assertEquals(1, captor.getValue().getSortOrder());
        assertEquals("L1", captor.getValue().getCode());
    }

    // =========================
    // TC-CL-03
    // =========================
    @Test
    void TC_CL03_existingLevel_sortOrderIncrement() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .build();

        CreateLevelDTO request = CreateLevelDTO.builder()
                .curriculumId(1L)
                .code("L2")
                .name("Level 2")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.findMaxSortOrderByCurriculumId(1L))
                .thenReturn(3);

        // 🔑 FIX NPE
        when(levelRepository.save(any(Level.class)))
                .thenAnswer(invocation -> {
                    Level l = invocation.getArgument(0);
                    l.setId(101L);
                    return l;
                });

        ArgumentCaptor<Level> captor = ArgumentCaptor.forClass(Level.class);

        service.createLevel(request);

        verify(levelRepository).save(captor.capture());

        assertEquals(4, captor.getValue().getSortOrder());
    }
}
