package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
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
class CurriculumService_DeactivateCurriculum_Test {

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
                () -> service.deactivateCurriculum(1L));

        verify(curriculumRepository, never()).save(any());
    }

    // =========================
    // TC-DC-02
    // =========================
    @Test
    void TC_DC02_deactivateSuccess() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .status(CurriculumStatus.ACTIVE)
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));

        service.deactivateCurriculum(1L);

        assertEquals(CurriculumStatus.INACTIVE, curriculum.getStatus());
        verify(curriculumRepository).save(curriculum);
    }
}
