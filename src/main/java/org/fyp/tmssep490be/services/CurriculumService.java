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