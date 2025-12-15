package org.fyp.tmssep490be.services.curriculum;

import org.fyp.tmssep490be.dtos.curriculum.CreateCurriculumDTO;
import org.fyp.tmssep490be.dtos.curriculum.CreatePLODTO;
import org.fyp.tmssep490be.dtos.curriculum.CurriculumResponseDTO;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.repositories.*;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_CreateCurriculum_Test {

    @InjectMocks
    private org.fyp.tmssep490be.services.CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-CC-01
    // =========================
    @Test
    void TC_CC01_createCurriculum_success_withoutPLO() {
        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-01")
                .name("Test Curriculum")
                .language("English")
                .build();

        when(curriculumRepository.existsByCode("CURR-01"))
                .thenReturn(false);

        when(curriculumRepository.save(any(Curriculum.class)))
                .thenAnswer(invocation -> {
                    Curriculum c = invocation.getArgument(0);
                    c.setId(1L);
                    return c;
                });

        CurriculumResponseDTO result = service.createCurriculum(request);

        assertNotNull(result);
        assertEquals("CURR-01", result.getCode());
        assertEquals("Test Curriculum", result.getName());
        assertEquals(CurriculumStatus.DRAFT.name(), result.getStatus());
        assertEquals(0, result.getLevelCount());

        verify(curriculumRepository).save(any(Curriculum.class));
        verify(ploRepository, never()).saveAll(any());
    }

    // =========================
    // TC-CC-02
    // =========================
    @Test
    void TC_CC02_createCurriculum_duplicateCode_throwException() {
        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-01")
                .name("Duplicate Curriculum")
                .build();

        when(curriculumRepository.existsByCode("CURR-01"))
                .thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createCurriculum(request));

        verify(curriculumRepository, never()).save(any());
        verify(ploRepository, never()).saveAll(any());
    }

    @Test
    void TC_CC03_createCurriculum_success_withPLOs() {
        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-02")
                .name("Curriculum With PLO")
                .plos(List.of(
                        CreatePLODTO.builder()
                                .code("PLO1")
                                .description("Description 1")
                                .build(),
                        CreatePLODTO.builder()
                                .code("PLO2")
                                .description("Description 2")
                                .build()
                ))
                .build();

        when(curriculumRepository.existsByCode("CURR-02"))
                .thenReturn(false);

        when(curriculumRepository.save(any(Curriculum.class)))
                .thenAnswer(invocation -> {
                    Curriculum c = invocation.getArgument(0);
                    c.setId(2L);
                    return c;
                });

        CurriculumResponseDTO result = service.createCurriculum(request);

        assertNotNull(result);
        assertEquals("CURR-02", result.getCode());
        assertEquals(2, result.getPlos().size());

        verify(ploRepository).saveAll(argThat(plos ->
                ((Collection<?>) plos).size() == 2
        ));
    }


    // =========================
    // TC-CC-04
    // =========================
    @Test
    void TC_CC04_createCurriculum_languageNull_defaultEnglish() {
        CreateCurriculumDTO request = CreateCurriculumDTO.builder()
                .code("CURR-03")
                .name("No Language Curriculum")
                .language(null)
                .build();

        when(curriculumRepository.existsByCode("CURR-03"))
                .thenReturn(false);

        when(curriculumRepository.save(any(Curriculum.class)))
                .thenAnswer(invocation -> {
                    Curriculum c = invocation.getArgument(0);
                    c.setId(3L);
                    return c;
                });

        CurriculumResponseDTO result = service.createCurriculum(request);

        assertNotNull(result);
        assertEquals("English", result.getLanguage());

        verify(curriculumRepository).save(any(Curriculum.class));
    }
}
