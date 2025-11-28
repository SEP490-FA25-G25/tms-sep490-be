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
import org.fyp.tmssep490be.repositories.PLORepository;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.PLO;
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
    private final PLORepository ploRepository;

    @Override
    public List<SubjectWithLevelsDTO> getAllSubjectsWithLevels() {
        log.debug("Fetching all subjects with their levels");

        // Get all subjects (including INACTIVE and DRAFT)
        List<Subject> subjects = subjectRepository.findAll(org.springframework.data.domain.Sort.by("code"));

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
                .status(level.getStatus().name())
                .build();
    }

    private LevelResponseDTO toLevelResponseDTO(Level level) {
        return LevelResponseDTO.builder()
                .id(level.getId().toString())
                .code(level.getCode())
                .name(level.getName())
                .description(level.getDescription())
                .durationHours(level.getExpectedDurationHours())
                .subjectName(level.getSubject().getName())
                .subjectCode(level.getSubject().getCode())
                .status(level.getStatus().name())
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

        // Set status based on PLOs presence
        if (request.getPlos() != null && !request.getPlos().isEmpty()) {
            subject.setStatus(SubjectStatus.ACTIVE);
        } else {
            subject.setStatus(SubjectStatus.DRAFT);
        }

        subject = subjectRepository.save(subject);
        log.info("Subject created with ID: {}", subject.getId());

        if (request.getPlos() != null && !request.getPlos().isEmpty()) {
            Subject finalSubject = subject;
            List<PLO> plos = request.getPlos().stream()
                    .map(ploDTO -> PLO.builder()
                            .subject(finalSubject)
                            .code(ploDTO.getCode())
                            .description(ploDTO.getDescription())
                            .build())
                    .collect(Collectors.toList());
            ploRepository.saveAll(plos);
        }

        return SubjectResponseDTO.builder()
                .id(subject.getId().toString())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .status(subject.getStatus().name())
                .createdAt(subject.getCreatedAt() != null ? subject.getCreatedAt().toString() : null)
                .levelCount(0)
                .plos(request.getPlos() != null ? request.getPlos() : java.util.Collections.emptyList())
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
                .map(this::toLevelResponseDTO)
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

    @Override
    public SubjectResponseDTO getSubject(Long id) {
        log.debug("Fetching subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        return SubjectResponseDTO.builder()
                .id(subject.getId().toString())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .status(subject.getStatus().name())
                .createdAt(subject.getCreatedAt() != null ? subject.getCreatedAt().toString() : null)
                .levelCount(subject.getLevels().size())
                .plos(subject.getPlos().stream()
                        .map(plo -> CreatePLODTO.builder()
                                .code(plo.getCode())
                                .description(plo.getDescription())
                                .build())
                        .collect(Collectors.toList()))
                .levels(subject.getLevels().stream()
                        .map(this::toLevelResponseDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public SubjectResponseDTO updateSubject(Long id, CreateSubjectDTO request) {
        log.info("Updating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        // Update basic info
        subject.setName(request.getName());
        subject.setDescription(request.getDescription());
        // Code is usually not updatable or requires uniqueness check if changed.
        // Assuming code can be updated but need to check uniqueness if it's different
        if (!subject.getCode().equals(request.getCode())) {
            if (subjectRepository.existsByCode(request.getCode())) {
                throw new IllegalArgumentException("Subject code already exists: " + request.getCode());
            }
            subject.setCode(request.getCode());
        }

        // Update PLOs (Merge strategy to avoid unique constraint violation)
        if (request.getPlos() != null) {
            Subject finalSubject = subject;

            // Map existing PLOs by code
            java.util.Map<String, PLO> existingPlosMap = subject.getPlos().stream()
                    .collect(Collectors.toMap(PLO::getCode, plo -> plo));

            // Clear and addAll would still cause issues if flush doesn't happen.
            // Best way with orphanRemoval is to modify the collection directly.

            // 1. Remove PLOs not in the new list
            java.util.Set<String> newPloCodes = request.getPlos().stream()
                    .map(CreatePLODTO::getCode)
                    .collect(Collectors.toSet());

            subject.getPlos().removeIf(plo -> !newPloCodes.contains(plo.getCode()));

            // 2. Add or Update
            for (CreatePLODTO ploDTO : request.getPlos()) {
                PLO existingPlo = existingPlosMap.get(ploDTO.getCode());
                if (existingPlo != null) {
                    existingPlo.setDescription(ploDTO.getDescription());
                } else {
                    subject.getPlos().add(PLO.builder()
                            .subject(finalSubject)
                            .code(ploDTO.getCode())
                            .description(ploDTO.getDescription())
                            .build());
                }
            }

            if (!subject.getPlos().isEmpty()) {
                subject.setStatus(SubjectStatus.ACTIVE);
            } else {
                subject.setStatus(SubjectStatus.DRAFT);
            }
        } else {
            subject.getPlos().clear();
            subject.setStatus(SubjectStatus.DRAFT);
        }
        subject = subjectRepository.save(subject);
        log.info("Subject updated with ID: {}", subject.getId());

        return getSubject(subject.getId());
    }

    @Override
    @Transactional
    public void deactivateSubject(Long id) {
        log.info("Deactivating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        subject.setStatus(SubjectStatus.INACTIVE);
        subjectRepository.save(subject);
    }

    @Override
    @Transactional
    public void reactivateSubject(Long id) {
        log.info("Reactivating subject with ID: {}", id);
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with ID: " + id));

        if (subject.getPlos() != null && !subject.getPlos().isEmpty()) {
            subject.setStatus(SubjectStatus.ACTIVE);
        } else {
            subject.setStatus(SubjectStatus.DRAFT);
        }
        subjectRepository.save(subject);
    }

    @Override
    public LevelResponseDTO getLevel(Long id) {
        log.debug("Fetching level with ID: {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with ID: " + id));
        return toLevelResponseDTO(level);
    }

    @Override
    @Transactional
    public LevelResponseDTO updateLevel(Long id, CreateLevelDTO request) {
        log.info("Updating level with ID: {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with ID: " + id));

        level.setCode(request.getCode());
        level.setName(request.getName());
        level.setDescription(request.getDescription());
        level.setExpectedDurationHours(request.getDurationHours());

        level = levelRepository.save(level);
        log.info("Level updated with ID: {}", level.getId());

        return toLevelResponseDTO(level);
    }

    @Override
    @Transactional
    public void deactivateLevel(Long id) {
        log.info("Deactivating level with ID: {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with ID: " + id));

        level.setStatus(org.fyp.tmssep490be.entities.enums.LevelStatus.INACTIVE);
        levelRepository.save(level);
    }

    @Override
    @Transactional
    public void reactivateLevel(Long id) {
        log.info("Reactivating level with ID: {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with ID: " + id));

        level.setStatus(org.fyp.tmssep490be.entities.enums.LevelStatus.ACTIVE);
        levelRepository.save(level);
    }

    @Override
    @Transactional
    public void updateLevelSortOrder(Long subjectId, List<Long> levelIds) {
        log.info("Updating level sort order for subject ID: {}", subjectId);

        // Fetch all levels for the subject to verify existence and ownership
        List<Level> levels = levelRepository.findBySubjectIdOrderBySortOrderAsc(subjectId);

        // Map ID to Level for quick access
        java.util.Map<Long, Level> levelMap = levels.stream()
                .collect(Collectors.toMap(Level::getId, level -> level));

        for (int i = 0; i < levelIds.size(); i++) {
            Long levelId = levelIds.get(i);
            Level level = levelMap.get(levelId);

            if (level != null) {
                // Update sort order (1-based index)
                level.setSortOrder(i + 1);
            } else {
                log.warn("Level ID {} not found for subject ID {} or invalid ID in list", levelId, subjectId);
                // Optionally throw exception if strict validation is needed
            }
        }

        levelRepository.saveAll(levels);
    }
}