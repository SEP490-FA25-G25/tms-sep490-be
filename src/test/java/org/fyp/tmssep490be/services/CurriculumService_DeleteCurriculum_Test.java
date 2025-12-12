package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.Curriculum;
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
class CurriculumService_DeleteCurriculum_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-DC-01
    // =========================
    @Test
    void TC_DC01_notFound_throwException() {
        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.deleteCurriculum(1L));

        verify(curriculumRepository, never()).delete(any());
    }

    // =========================
    // TC-DC-02
    // =========================
    @Test
    void TC_DC02_hasLevel_throwException() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.countByCurriculumId(1L))
                .thenReturn(1L);

        assertThrows(IllegalStateException.class,
                () -> service.deleteCurriculum(1L));

        verify(curriculumRepository, never()).delete(any());
    }

    // =========================
    // TC-DC-03
    // =========================
    @Test
    void TC_DC03_deleteSuccess() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(levelRepository.countByCurriculumId(1L))
                .thenReturn(0L);

        service.deleteCurriculum(1L);

        verify(curriculumRepository).delete(curriculum);
    }
}
