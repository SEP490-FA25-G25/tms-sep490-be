package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_UpdateLevelSortOrder_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-ULSO-01
    // =========================
    @Test
    void TC_ULSO01_emptyLevelList() {
        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of());

        service.updateLevelSortOrder(1L, List.of());

        verify(levelRepository).saveAll(List.of());
    }

    // =========================
    // TC-ULSO-02
    // =========================
    @Test
    void TC_ULSO02_reorderSuccess() {
        Level l1 = Level.builder().id(1L).sortOrder(1).build();
        Level l2 = Level.builder().id(2L).sortOrder(2).build();
        Level l3 = Level.builder().id(3L).sortOrder(3).build();

        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(l1, l2, l3));

        // New order: 3 → 1 → 2
        service.updateLevelSortOrder(1L, List.of(3L, 1L, 2L));

        assertEquals(2, l1.getSortOrder()); // second
        assertEquals(3, l2.getSortOrder()); // third
        assertEquals(1, l3.getSortOrder()); // first

        verify(levelRepository).saveAll(List.of(l1, l2, l3));
    }

    // =========================
    // TC-ULSO-03
    // =========================
    @Test
    void TC_ULSO03_ignoreUnknownLevelId() {
        Level l1 = Level.builder().id(1L).sortOrder(1).build();
        Level l2 = Level.builder().id(2L).sortOrder(2).build();

        when(levelRepository.findByCurriculumIdOrderBySortOrderAsc(1L))
                .thenReturn(List.of(l1, l2));

        // ID 99 không tồn tại
        service.updateLevelSortOrder(1L, List.of(2L, 99L, 1L));

        assertEquals(3, l1.getSortOrder());
        assertEquals(1, l2.getSortOrder());

        verify(levelRepository).saveAll(List.of(l1, l2));
    }

}
