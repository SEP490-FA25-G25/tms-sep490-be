package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.entities.enums.LevelStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_ReactivateCurriculum_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-RC-01
    // =========================
    @Test
    void TC_RC01_notFound_throwException() {
        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.reactivateCurriculum(1L));

        verify(curriculumRepository, never()).save(any());
    }

    // =========================
    // TC-RC-02
    // =========================
    @Test
    void TC_RC02_hasActiveSubject_setActive() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .status(CurriculumStatus.INACTIVE)
                .build();

        Level level = Level.builder()
                .id(10L)
                .status(LevelStatus.ACTIVE)
                .curriculum(curriculum)
                .build();

        // 🔑 QUAN TRỌNG: set levels vào curriculum
        curriculum.setLevels(Set.of(level));

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));

        when(subjectRepository.existsByLevelIdAndStatus(
                10L, SubjectStatus.ACTIVE))
                .thenReturn(true);

        service.reactivateCurriculum(1L);

        assertEquals(CurriculumStatus.ACTIVE, curriculum.getStatus());
        verify(curriculumRepository).save(curriculum);
    }

    // =========================
    // TC-RC-03
    // =========================
    @Test
    void TC_RC03_noActiveSubject_setDraft() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .status(CurriculumStatus.INACTIVE)
                .build();

        Level level1 = Level.builder()
                .id(10L)
                .status(LevelStatus.DRAFT)
                .curriculum(curriculum)
                .build();

        Level level2 = Level.builder()
                .id(11L)
                .status(LevelStatus.INACTIVE)
                .curriculum(curriculum)
                .build();

        curriculum.setLevels(Set.of(level1, level2));

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));

        when(subjectRepository.existsByLevelIdAndStatus(
                anyLong(), eq(SubjectStatus.ACTIVE)))
                .thenReturn(false);

        service.reactivateCurriculum(1L);

        assertEquals(CurriculumStatus.DRAFT, curriculum.getStatus());
        verify(curriculumRepository).save(curriculum);
    }
}
