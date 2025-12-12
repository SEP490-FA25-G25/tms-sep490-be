package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.curriculum.CreateLevelDTO;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_UpdateLevel_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-UL-01
    // =========================
    @Test
    void TC_UL01_notFound() {
        CreateLevelDTO request = CreateLevelDTO.builder()
                .code("L1")
                .name("Level 1")
                .description("Desc")
                .build();

        when(levelRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateLevel(1L, request));

        verify(levelRepository, never()).save(any());
    }

    // =========================
    // TC-UL-02
    // =========================
    @Test
    void TC_UL02_updateSuccess() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum")
                .build();

        Level level = Level.builder()
                .id(10L)
                .code("OLD")
                .name("Old Name")
                .description("Old Desc")
                .curriculum(curriculum)
                .build();

        CreateLevelDTO request = CreateLevelDTO.builder()
                .code("NEW")
                .name("New Name")
                .description("New Desc")
                .build();

        when(levelRepository.findById(10L))
                .thenReturn(Optional.of(level));
        when(levelRepository.save(any(Level.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.updateLevel(10L, request);

        assertEquals("NEW", level.getCode());
        assertEquals("New Name", level.getName());
        assertEquals("New Desc", level.getDescription());
    }

    // =========================
    // TC-UL-03
    // =========================
    @Test
    void TC_UL03_updatedAtSet() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .build();

        Level level = Level.builder()
                .id(10L)
                .curriculum(curriculum)
                .updatedAt(null)
                .build();

        CreateLevelDTO request = CreateLevelDTO.builder()
                .code("L1")
                .name("Level 1")
                .build();

        when(levelRepository.findById(10L))
                .thenReturn(Optional.of(level));
        when(levelRepository.save(any(Level.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.updateLevel(10L, request);

        assertNotNull(level.getUpdatedAt());
        assertTrue(level.getUpdatedAt().isAfter(
                OffsetDateTime.now().minusSeconds(5)
        ));
    }
}
