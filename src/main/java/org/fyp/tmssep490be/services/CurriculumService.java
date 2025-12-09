package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.curriculum.*;
import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.PLO;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.fyp.tmssep490be.entities.enums.LevelStatus;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.CurriculumRepository;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.PLORepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CurriculumService {

        private final CurriculumRepository curriculumRepository;
        private final LevelRepository levelRepository;
        private final PLORepository ploRepository;
        private final SubjectRepository subjectRepository;
        private final TimeSlotTemplateRepository timeSlotTemplateRepository;

        // ==================== CURRICULUM METHODS ====================

        public List<CurriculumWithLevelsDTO> getAllCurriculumsWithLevels() {
                log.debug("Fetching all curriculums with their levels");

                // Lấy tất cả curriculums, sắp xếp theo updatedAt DESC (mới nhất trước)
                List<Curriculum> curriculums = curriculumRepository.findAll(
                                Sort.by(Sort.Direction.DESC, "updatedAt"));

                List<CurriculumWithLevelsDTO> result = curriculums.stream()
                                .map(this::convertToCurriculumWithLevelsDTO)
                                .collect(Collectors.toList());

                log.debug("Found {} curriculums with levels", result.size());
                return result;
        }

        // CREATE CURRICULUM
        @Transactional
        public CurriculumResponseDTO createCurriculum(CreateCurriculumDTO request) {
                log.info("Creating new curriculum: {}", request.getCode());

                // 1. Kiểm tra code không trùng
                if (curriculumRepository.existsByCode(request.getCode())) {
                        throw new IllegalArgumentException("Mã chương trình đã tồn tại: " + request.getCode());
                }

                // 2. Tạo entity
                Curriculum curriculum = new Curriculum();
                curriculum.setCode(request.getCode());
                curriculum.setName(request.getName());
                curriculum.setDescription(request.getDescription());
                curriculum.setLanguage(request.getLanguage() != null ? request.getLanguage() : "English");
                curriculum.setStatus(CurriculumStatus.DRAFT);

                curriculum = curriculumRepository.save(curriculum);
                log.info("Curriculum created with ID: {}", curriculum.getId());

                // 3. Lưu PLOs nếu có
                if (request.getPlos() != null && !request.getPlos().isEmpty()) {
                        Curriculum finalCurriculum = curriculum;
                        List<PLO> plos = request.getPlos().stream()
                                        .map(ploDTO -> PLO.builder()
                                                        .curriculum(finalCurriculum)
                                                        .code(ploDTO.getCode())
                                                        .description(ploDTO.getDescription())
                                                        .build())
                                        .collect(Collectors.toList());
                        ploRepository.saveAll(plos);
                }

                // 4. Trả về DTO
                return CurriculumResponseDTO.builder()
                                .id(curriculum.getId().toString())
                                .code(curriculum.getCode())
                                .name(curriculum.getName())
                                .description(curriculum.getDescription())
                                .language(curriculum.getLanguage())
                                .status(curriculum.getStatus().name())
                                .createdAt(curriculum.getCreatedAt() != null ? curriculum.getCreatedAt().toString()
                                                : null)
                                .levelCount(0)
                                .plos(request.getPlos() != null ? request.getPlos() : java.util.Collections.emptyList())
                                .build();
        }

        public CurriculumResponseDTO getCurriculum(Long id) {
                log.debug("Fetching curriculum with ID: {}", id);
                Curriculum curriculum = curriculumRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy chương trình với ID: " + id));

                return CurriculumResponseDTO.builder()
                                .id(curriculum.getId().toString())
                                .code(curriculum.getCode())
                                .name(curriculum.getName())
                                .description(curriculum.getDescription())
                                .language(curriculum.getLanguage())
                                .status(curriculum.getStatus().name())
                                .createdAt(curriculum.getCreatedAt() != null ? curriculum.getCreatedAt().toString()
                                                : null)
                                .levelCount(curriculum.getLevels().size())
                                .plos(curriculum.getPlos().stream()
                                                .map(plo -> CreatePLODTO.builder()
                                                                .code(plo.getCode())
                                                                .description(plo.getDescription())
                                                                .build())
                                                .collect(Collectors.toList()))
                                .levels(levelRepository.findByCurriculumIdOrderBySortOrderAsc(id).stream()
                                                .map(this::toLevelResponseDTO)
                                                .collect(Collectors.toList()))
                                .build();
        }

        @Transactional
        public CurriculumResponseDTO updateCurriculum(Long id, CreateCurriculumDTO request) {
                log.info("Updating curriculum with ID: {}", id);
                Curriculum curriculum = curriculumRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy chương trình với ID: " + id));

                // Update basic info
                curriculum.setName(request.getName());
                curriculum.setDescription(request.getDescription());
                curriculum.setLanguage(
                                request.getLanguage() != null ? request.getLanguage() : curriculum.getLanguage());

                // Check unique code if changed
                if (!curriculum.getCode().equals(request.getCode())) {
                        if (curriculumRepository.existsByCode(request.getCode())) {
                                throw new IllegalArgumentException("Mã chương trình đã tồn tại: " + request.getCode());
                        }
                        curriculum.setCode(request.getCode());
                }

                // Update PLOs (Merge strategy)
                if (request.getPlos() != null) {
                        // Map existing PLOs by code
                        Map<String, PLO> existingPlosMap = curriculum.getPlos().stream()
                                        .collect(Collectors.toMap(PLO::getCode, plo -> plo));

                        // Remove PLOs not in new list
                        Set<String> newPloCodes = request.getPlos().stream()
                                        .map(CreatePLODTO::getCode)
                                        .collect(Collectors.toSet());
                        curriculum.getPlos().removeIf(plo -> !newPloCodes.contains(plo.getCode()));

                        // Add or Update
                        for (CreatePLODTO ploDTO : request.getPlos()) {
                                PLO existingPlo = existingPlosMap.get(ploDTO.getCode());
                                if (existingPlo != null) {
                                        existingPlo.setDescription(ploDTO.getDescription());
                                } else {
                                        curriculum.getPlos().add(PLO.builder()
                                                        .curriculum(curriculum)
                                                        .code(ploDTO.getCode())
                                                        .description(ploDTO.getDescription())
                                                        .build());
                                }
                        }
                } else {
                        curriculum.getPlos().clear();
                }

                curriculum.setUpdatedAt(OffsetDateTime.now());
                curriculum = curriculumRepository.save(curriculum);
                log.info("Curriculum updated with ID: {}", curriculum.getId());

                return getCurriculum(curriculum.getId());
        }

        @Transactional
        public void deactivateCurriculum(Long id) {
                log.info("Deactivating curriculum with ID: {}", id);
                Curriculum curriculum = curriculumRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với ID: " + id));

                curriculum.setStatus(CurriculumStatus.INACTIVE);
                curriculumRepository.save(curriculum);
        }

        @Transactional
        public void reactivateCurriculum(Long id) {
                log.info("Reactivating curriculum with ID: {}", id);
                Curriculum curriculum = curriculumRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với ID: " + id));

                // Kiểm tra có Subject nào đang ACTIVE không (thông qua Levels)
                boolean hasActiveSubject = curriculum.getLevels().stream()
                        .anyMatch(level -> subjectRepository.existsByLevelIdAndStatus(level.getId(), SubjectStatus.ACTIVE));

                if (hasActiveSubject) {
                        curriculum.setStatus(CurriculumStatus.ACTIVE);
                } else {
                        curriculum.setStatus(CurriculumStatus.DRAFT);
                }
                curriculumRepository.save(curriculum);
        }

        @Transactional
        public void deleteCurriculum(Long id) {
                log.info("Deleting curriculum with ID: {}", id);
                Curriculum curriculum = curriculumRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với ID: " + id));

                // Kiểm tra có level nào phụ thuộc không
                long levelCount = levelRepository.countByCurriculumId(id);
                if (levelCount > 0) {
                        throw new IllegalStateException("Không thể xóa chương trình vì đã có cấp độ phụ thuộc.");
                }

                curriculumRepository.delete(curriculum);
                log.info("Curriculum deleted successfully: {}", id);
        }

        // ==================== CURRICULUM METHODS ====================

        @Transactional
        public LevelResponseDTO createLevel(CreateLevelDTO request) {
                log.info("Creating new level for curriculum ID: {}", request.getCurriculumId());

                Curriculum curriculum = curriculumRepository.findById(request.getCurriculumId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Không tìm thấy chương trình với ID: " + request.getCurriculumId()));

                Level level = new Level();
                level.setCurriculum(curriculum);
                level.setCode(request.getCode());
                level.setName(request.getName());
                level.setDescription(request.getDescription());

                // Tính sortOrder (thêm vào cuối)
                Integer maxSortOrder = levelRepository.findMaxSortOrderByCurriculumId(curriculum.getId());
                level.setSortOrder(maxSortOrder != null ? maxSortOrder + 1 : 1);

                level = levelRepository.save(level);
                log.info("Level created with ID: {}", level.getId());

                return LevelResponseDTO.builder()
                        .id(level.getId().toString())
                        .code(level.getCode())
                        .name(level.getName())
                        .description(level.getDescription())
                        .curriculumName(curriculum.getName())
                        .curriculumCode(curriculum.getCode())
                        .build();
        }

        public List<LevelResponseDTO> getLevels(Long curriculumId) {
                log.debug("Fetching levels with curriculumId: {}", curriculumId);

                List<Level> levels;
                if (curriculumId != null) {
                        levels = levelRepository.findByCurriculumIdOrderByUpdatedAtDesc(curriculumId);
                } else {
                        levels = levelRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"));
                }

                return levels.stream()
                        .map(this::toLevelResponseDTO)
                        .collect(Collectors.toList());
        }

        public LevelResponseDTO getLevel(Long id) {
                log.debug("Fetching level with ID: {}", id);
                Level level = levelRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ với ID: " + id));
                return toLevelResponseDTO(level);
        }

        @Transactional
        public LevelResponseDTO updateLevel(Long id, CreateLevelDTO request) {
                log.info("Updating level with ID: {}", id);
                Level level = levelRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ với ID: " + id));

                level.setCode(request.getCode());
                level.setName(request.getName());
                level.setDescription(request.getDescription());
                level.setUpdatedAt(OffsetDateTime.now());

                level = levelRepository.save(level);
                log.info("Level updated with ID: {}", level.getId());

                return toLevelResponseDTO(level);
        }

        @Transactional
        public void deactivateLevel(Long id) {
                log.info("Deactivating level with ID: {}", id);
                Level level = levelRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ với ID: " + id));

                level.setStatus(LevelStatus.INACTIVE);
                levelRepository.save(level);
        }

        @Transactional
        public void reactivateLevel(Long id) {
                log.info("Reactivating level with ID: {}", id);
                Level level = levelRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ với ID: " + id));

                // Kiểm tra có Subject nào đang ACTIVE không
                boolean hasActiveSubject = subjectRepository.existsByLevelIdAndStatus(id, SubjectStatus.ACTIVE);

                if (hasActiveSubject) {
                        level.setStatus(LevelStatus.ACTIVE);
                } else {
                        level.setStatus(LevelStatus.DRAFT);
                }
                levelRepository.save(level);
        }

        @Transactional
        public void updateLevelSortOrder(Long curriculumId, List<Long> levelIds) {
                log.info("Updating level sort order for curriculum ID: {}", curriculumId);

                List<Level> levels = levelRepository.findByCurriculumIdOrderBySortOrderAsc(curriculumId);

                Map<Long, Level> levelMap = levels.stream()
                        .collect(Collectors.toMap(Level::getId, level -> level));

                for (int i = 0; i < levelIds.size(); i++) {
                        Long levelId = levelIds.get(i);
                        Level level = levelMap.get(levelId);

                        if (level != null) {
                                level.setSortOrder(i + 1);
                        } else {
                                log.warn("Level ID {} not found for curriculum ID {}", levelId, curriculumId);
                        }
                }

                levelRepository.saveAll(levels);
        }

        @Transactional
        public void deleteLevel(Long id) {
                log.info("Deleting level with ID: {}", id);
                Level level = levelRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ với ID: " + id));

                long subjectCount = subjectRepository.countByLevelId(id);
                if (subjectCount > 0) {
                        throw new IllegalStateException("Không thể xóa cấp độ vì đã có khóa học phụ thuộc.");
                }

                levelRepository.delete(level);
                log.info("Level deleted successfully: {}", id);
        }

        public BigDecimal getStandardTimeslotDuration() {
                log.debug("Calculating standard timeslot duration");
                var templates = timeSlotTemplateRepository.findAll();
                if (templates.isEmpty()) {
                        return BigDecimal.valueOf(2.0); // Default 2 hours
                }

                var template = templates.get(0);
                long minutes = Duration.between(template.getStartTime(), template.getEndTime()).toMinutes();
                return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        public List<BigDecimal> getAllTimeslotDurations() {
                log.debug("Fetching all unique timeslot durations");
                var templates = timeSlotTemplateRepository.findAll();
                if (templates.isEmpty()) {
                        return List.of(BigDecimal.valueOf(2.0));
                }

                return templates.stream()
                        .map(template -> {
                                long minutes = Duration.between(template.getStartTime(), template.getEndTime()).toMinutes();
                                return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
                        })
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());
        }

        // ==================== HELPER METHODS ====================

        private CurriculumWithLevelsDTO convertToCurriculumWithLevelsDTO(Curriculum curriculum) {
                log.info("Curriculum: {}, CreatedAt: {}, UpdatedAt: {}",
                                curriculum.getName(), curriculum.getCreatedAt(), curriculum.getUpdatedAt());

                // Lấy levels của curriculum, sắp xếp theo sortOrder
                List<Level> levels = levelRepository.findByCurriculumIdOrderBySortOrderAsc(curriculum.getId());

                List<CurriculumWithLevelsDTO.LevelDTO> levelDTOs = levels.stream()
                                .map(this::convertLevelToDTO)
                                .collect(Collectors.toList());

                // Convert PLOs
                List<CurriculumWithLevelsDTO.PLODTO> ploDTOs = curriculum.getPlos().stream()
                                .map(plo -> CurriculumWithLevelsDTO.PLODTO.builder()
                                                .code(plo.getCode())
                                                .description(plo.getDescription())
                                                .build())
                                .collect(Collectors.toList());

                return CurriculumWithLevelsDTO.builder()
                                .id(curriculum.getId())
                                .code(curriculum.getCode())
                                .name(curriculum.getName())
                                .description(curriculum.getDescription())
                                .status(curriculum.getStatus().name())
                                .createdAt(curriculum.getCreatedAt())
                                .updatedAt(curriculum.getUpdatedAt())
                                .levels(levelDTOs)
                                .plos(ploDTOs)
                                .build();
        }

        private CurriculumWithLevelsDTO.LevelDTO convertLevelToDTO(Level level) {
                return CurriculumWithLevelsDTO.LevelDTO.builder()
                                .id(level.getId())
                                .code(level.getCode())
                                .name(level.getName())
                                .description(level.getDescription())
                                .sortOrder(level.getSortOrder())
                                .status(level.getStatus().name())
                                .createdAt(level.getCreatedAt())
                                .updatedAt(level.getUpdatedAt())
                                .build();
        }

        private LevelResponseDTO toLevelResponseDTO(Level level) {
                return LevelResponseDTO.builder()
                                .id(level.getId().toString())
                                .code(level.getCode())
                                .name(level.getName())
                                .description(level.getDescription())
                                .curriculumId(level.getCurriculum().getId())
                                .curriculumName(level.getCurriculum().getName())
                                .curriculumCode(level.getCurriculum().getCode())
                                .status(level.getStatus().name())
                                .sortOrder(level.getSortOrder())
                                .createdAt(level.getCreatedAt())
                                .updatedAt(level.getUpdatedAt())
                                .build();
        }
}