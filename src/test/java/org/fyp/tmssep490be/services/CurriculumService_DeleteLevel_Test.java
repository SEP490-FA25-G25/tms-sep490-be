package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_DeleteLevel_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-DL-01
    // =========================
    @Test
    void TC_DL01_notFound() {
        when(levelRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteLevel(1L));

        verify(levelRepository, never()).delete(any());
    }

    // =========================
    // TC-DL-02
    // =========================
    @Test
    void TC_DL02_hasSubject_throwException() {
        Level level = Level.builder()
                .id(1L)
                .build();

        when(levelRepository.findById(1L))
                .thenReturn(Optional.of(level));
        when(subjectRepository.countByLevelId(1L))
                .thenReturn(2L);

        assertThrows(IllegalStateException.class,
                () -> service.deleteLevel(1L));

        verify(levelRepository, never()).delete(any());
    }

    // =========================
    // TC-DL-03
    // =========================
    @Test
    void TC_DL03_deleteSuccess() {
        Level level = Level.builder()
                .id(1L)
                .build();

        when(levelRepository.findById(1L))
                .thenReturn(Optional.of(level));
        when(subjectRepository.countByLevelId(1L))
                .thenReturn(0L);

        service.deleteLevel(1L);

        verify(levelRepository).delete(level);
    }
}
