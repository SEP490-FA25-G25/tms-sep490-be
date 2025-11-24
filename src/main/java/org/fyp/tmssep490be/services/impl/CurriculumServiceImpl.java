package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.curriculum.*;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.services.CurriculumService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CurriculumServiceImpl implements CurriculumService {

    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;

    @Override
    public List<SubjectWithLevelsDTO> getAllSubjectsWithLevels() {
        log.debug("Fetching all subjects with their levels");

        // Get all subjects with ACTIVE status
        List<Subject> subjects = subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE);

        List<SubjectWithLevelsDTO> result = subjects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.debug("Found {} subjects with levels", result.size());
        return result;
    }

    private SubjectWithLevelsDTO convertToDTO(Subject subject) {
        // Get levels for this subject, sorted by sort_order
        List<Level> levels = levelRepository.findBySubjectIdOrderBySortOrderAsc(subject.getId());

        List<SubjectWithLevelsDTO.LevelDTO> levelDTOs = levels.stream()
                .map(this::convertLevelToDTO)
                .collect(Collectors.toList());

        return SubjectWithLevelsDTO.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .status(subject.getStatus().name())
                .createdAt(subject.getCreatedAt())
                .levels(levelDTOs)
                .build();
    }

    private SubjectWithLevelsDTO.LevelDTO convertLevelToDTO(Level level) {
        return SubjectWithLevelsDTO.LevelDTO.builder()
                .id(level.getId())
                .code(level.getCode())
                .name(level.getName())
                .description(level.getDescription())
                .expectedDurationHours(level.getExpectedDurationHours())
                .sortOrder(level.getSortOrder())
                .build();
    }

    @Override
    @Transactional
    public SubjectResponseDTO createSubject(CreateSubjectDTO request) {
        log.info("Creating new subject: {}", request.getCode());

        if (subjectRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Subject code already exists: " + request.getCode());
        }

        Subject subject = new Subject();
        subject.setCode(request.getCode());
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        subject.setStatus(SubjectStatus.ACTIVE);

        subject = subjectRepository.save(subject);
        log.info("Subject created with ID: {}", subject.getId());

        return SubjectResponseDTO.builder()
                .id(subject.getId().toString())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .status(subject.getStatus().name())
                .createdAt(subject.getCreatedAt() != null ? subject.getCreatedAt().toString() : null)
                .levelCount(0)
                .build();
    }

    @Override
    @Transactional
    public LevelResponseDTO createLevel(CreateLevelDTO request) {
        log.info("Creating new level for subject ID: {}", request.getSubjectId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Subject not found with ID: " + request.getSubjectId()));

        Level level = new Level();
        level.setSubject(subject);
        level.setCode(request.getCode());
        level.setName(request.getName());
        level.setDescription(request.getDescription());
        level.setExpectedDurationHours(request.getDurationHours());

        // Calculate sort order (append to end)
        Integer maxSortOrder = levelRepository.findMaxSortOrderBySubjectId(subject.getId());
        level.setSortOrder(maxSortOrder != null ? maxSortOrder + 1 : 1);

        level = levelRepository.save(level);
        log.info("Level created with ID: {}", level.getId());

        return LevelResponseDTO.builder()
                .id(level.getId().toString())
                .code(level.getCode())
                .name(level.getName())
                .description(level.getDescription())
                .durationHours(level.getExpectedDurationHours())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .build();
    }

    @Override
    public List<LevelResponseDTO> getLevels(Long subjectId) {
        log.debug("Fetching levels with subjectId: {}", subjectId);

        List<Level> levels;
        if (subjectId != null) {
            levels = levelRepository.findBySubjectIdOrderBySortOrderAsc(subjectId);
        } else {
            levels = levelRepository.findAll();
        }

        return levels.stream()
                .map(level -> LevelResponseDTO.builder()
                        .id(level.getId().toString())
                        .code(level.getCode())
                        .name(level.getName())
                        .description(level.getDescription())
                        .durationHours(level.getExpectedDurationHours())
                        .subjectName(level.getSubject().getName())
                        .subjectCode(level.getSubject().getCode())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public java.math.BigDecimal getStandardTimeslotDuration() {
        log.debug("Calculating standard timeslot duration");
        List<TimeSlotTemplate> templates = timeSlotTemplateRepository.findAll();
        if (templates.isEmpty()) {
            return java.math.BigDecimal.valueOf(2.0); // Default 2 hours
        }

        // Use the first template as standard
        TimeSlotTemplate template = templates.get(0);
        long minutes = java.time.Duration.between(template.getStartTime(), template.getEndTime()).toMinutes();
        return java.math.BigDecimal.valueOf(minutes).divide(java.math.BigDecimal.valueOf(60), 2,
                java.math.RoundingMode.HALF_UP);
    }
}