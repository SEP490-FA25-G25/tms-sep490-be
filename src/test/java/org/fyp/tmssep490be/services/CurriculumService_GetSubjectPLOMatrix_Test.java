package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.curriculum.SubjectPLOMatrixDTO;
import org.fyp.tmssep490be.entities.*;
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
class CurriculumService_GetSubjectPLOMatrix_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-SPM-01
    // =========================
    @Test
    void TC_SPM01_notFound() {
        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getSubjectPLOMatrix(1L));
    }

    // =========================
    // TC-SPM-02
    // =========================
    @Test
    void TC_SPM02_noPLO_noSubject() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum")
                .build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(ploRepository.findByCurriculumIdOrderByCodeAsc(1L))
                .thenReturn(List.of());
        when(subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of());

        SubjectPLOMatrixDTO result =
                service.getSubjectPLOMatrix(1L);

        assertEquals(0, result.getPlos().size());
        assertEquals(0, result.getSubjects().size());
    }

    // =========================
    // TC-SPM-03
    // =========================
    @Test
    void TC_SPM03_subjectWithMappings() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum")
                .build();

        PLO plo1 = PLO.builder().id(100L).code("PLO1").build();
        PLO plo2 = PLO.builder().id(101L).code("PLO2").build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(ploRepository.findByCurriculumIdOrderByCodeAsc(1L))
                .thenReturn(List.of(plo1, plo2));

        Level level = Level.builder()
                .id(10L)
                .name("Level 1")
                .build();

        CLO clo = CLO.builder()
                .id(20L)
                .build();

        PLOCLOMapping mapping = PLOCLOMapping.builder()
                .plo(plo1) // map PLO1
                .clo(clo)
                .build();

        clo.setPloCloMappings(Set.of(mapping));

        Subject subject = Subject.builder()
                .id(30L)
                .code("SUB-01")
                .name("Subject 1")
                .level(level)
                .updatedAt(OffsetDateTime.now())
                .build();

        subject.setClos(Set.of(clo));

        when(subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(subject));

        SubjectPLOMatrixDTO result =
                service.getSubjectPLOMatrix(1L);

        assertEquals(1, result.getSubjects().size());

        SubjectPLOMatrixDTO.SubjectPLORow row =
                result.getSubjects().get(0);

        assertEquals("SUB-01", row.getSubjectCode());
        assertEquals(List.of(true, false), row.getPloMappings());
    }

    // =========================
    // TC-SPM-04
    // =========================
    @Test
    void TC_SPM04_subjectWithoutAnyMapping() {
        Curriculum curriculum = Curriculum.builder()
                .id(1L)
                .code("CURR-01")
                .name("Curriculum")
                .build();

        PLO plo1 = PLO.builder().id(100L).code("PLO1").build();
        PLO plo2 = PLO.builder().id(101L).code("PLO2").build();

        when(curriculumRepository.findById(1L))
                .thenReturn(Optional.of(curriculum));
        when(ploRepository.findByCurriculumIdOrderByCodeAsc(1L))
                .thenReturn(List.of(plo1, plo2));

        Subject subject = Subject.builder()
                .id(30L)
                .code("SUB-01")
                .name("Subject 1")
                .updatedAt(OffsetDateTime.now())
                .build();

        subject.setClos(Set.of());

        when(subjectRepository.findByCurriculumIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(subject));

        SubjectPLOMatrixDTO result =
                service.getSubjectPLOMatrix(1L);

        assertEquals(List.of(false, false),
                result.getSubjects().get(0).getPloMappings());
    }
}
