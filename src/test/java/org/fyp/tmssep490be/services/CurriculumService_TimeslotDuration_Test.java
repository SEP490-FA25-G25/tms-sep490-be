package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.repositories.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurriculumService_TimeslotDuration_Test {

    @InjectMocks
    private CurriculumService service;

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private LevelRepository levelRepository;
    @Mock private PLORepository ploRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private TimeSlotTemplateRepository timeSlotTemplateRepository;

    // =========================
    // TC-TD-01
    // =========================
    @Test
    void TC_TD01_standard_emptyTemplate() {
        when(timeSlotTemplateRepository.findAll())
                .thenReturn(List.of());

        BigDecimal result = service.getStandardTimeslotDuration();

        assertEquals(BigDecimal.valueOf(2.0), result);
    }

    // =========================
    // TC-TD-02
    // =========================
    @Test
    void TC_TD02_standard_withTemplate() {
        TimeSlotTemplate template = TimeSlotTemplate.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 30)) // 150 phút = 2.50h
                .build();

        when(timeSlotTemplateRepository.findAll())
                .thenReturn(List.of(template));

        BigDecimal result = service.getStandardTimeslotDuration();

        assertEquals(new BigDecimal("2.50"), result);
    }

    // =========================
    // TC-TD-03
    // =========================
    @Test
    void TC_TD03_all_emptyTemplate() {
        when(timeSlotTemplateRepository.findAll())
                .thenReturn(List.of());

        List<BigDecimal> result = service.getAllTimeslotDurations();

        assertEquals(List.of(BigDecimal.valueOf(2.0)), result);
    }

    // =========================
    // TC-TD-04
    // =========================
    @Test
    void TC_TD04_all_multipleTemplates_distinct_sorted() {
        TimeSlotTemplate t1 = TimeSlotTemplate.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0)) // 2.0
                .build();

        TimeSlotTemplate t2 = TimeSlotTemplate.builder()
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(15, 0)) // 2.0
                .build();

        TimeSlotTemplate t3 = TimeSlotTemplate.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 30)) // 3.5
                .build();

        when(timeSlotTemplateRepository.findAll())
                .thenReturn(List.of(t1, t2, t3));

        List<BigDecimal> result = service.getAllTimeslotDurations();

        assertEquals(
                List.of(
                        new BigDecimal("2.0"),
                        new BigDecimal("3.5")
                ),
                result
        );
    }
}
