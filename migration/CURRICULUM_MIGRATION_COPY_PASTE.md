# 📋 MIGRATION BY ENDPOINT - CURRICULUM MODULE

> **NGUYÊN TẮC**: 
> - Mỗi endpoint = 1 commit
> - **KHÔNG dùng interface**, viết thẳng vào Service class (giống AuthService.java)

## 🔄 MAPPING ĐỔI TÊN ENTITY

| Deprecated Backend | New Backend | Ghi chú |
|--------------------|-------------|---------|
| Subject | Curriculum | Entity gốc (IELTS, TOEIC...) |
| Level | Level | Giữ nguyên |
| Course | Subject | Phiên bản cụ thể của Level |
| Course_Phase | Subject_Phase | Giai đoạn của Subject |
| Course_Session | Subject_Session | Buổi học trong Phase |
| Course_Material | Subject_Material | Tài liệu học |

**Workflow mới**: `Curriculum → Level → Subject`

---

# 🚀 ENDPOINT 0: BASE SETUP (BẮT BUỘC TRƯỚC)

## A. REPOSITORIES CẦN TẠO/CẬP NHẬT:

### A1. `src/main/java/org/fyp/tmssep490be/repositories/CurriculumRepository.java`

**📖 Giải thích:**
| Annotation/Code | Ý nghĩa |
|-----------------|---------|
| `@Repository` | Đánh dấu class là Repository (tầng truy cập dữ liệu) |
| `extends JpaRepository<Curriculum, Long>` | Kế thừa các method CRUD sẵn có |
| `existsByCode()` | Kiểm tra code đã tồn tại chưa (unique constraint) |

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Curriculum;
import org.fyp.tmssep490be.entities.enums.CurriculumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

    // Kiểm tra code đã tồn tại chưa
    boolean existsByCode(String code);

    // Tìm theo status, sắp xếp theo code
    List<Curriculum> findByStatusOrderByCode(CurriculumStatus status);
}
```

---

### A2. Cập nhật `src/main/java/org/fyp/tmssep490be/repositories/LevelRepository.java`

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {

    // Tìm level theo code (case-insensitive)
    java.util.Optional<Level> findByCodeIgnoreCase(String code);

    // Tìm levels theo curriculum ID, sắp xếp theo sort order
    List<Level> findByCurriculumIdOrderBySortOrderAsc(Long curriculumId);

    // Tìm levels theo curriculum ID, sắp xếp theo updatedAt DESC (mới nhất trước)
    List<Level> findByCurriculumIdOrderByUpdatedAtDesc(Long curriculumId);

    // Lấy max sort order để tính sortOrder cho level mới
    @Query("SELECT MAX(l.sortOrder) FROM Level l WHERE l.curriculum.id = :curriculumId")
    Integer findMaxSortOrderByCurriculumId(@Param("curriculumId") Long curriculumId);

    // Đếm số level thuộc curriculum
    long countByCurriculumId(Long curriculumId);
}
```

---

### A3. `src/main/java/org/fyp/tmssep490be/repositories/PLORepository.java`

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.PLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PLORepository extends JpaRepository<PLO, Long> {
}
```

---

### A4. `src/main/java/org/fyp/tmssep490be/repositories/SubjectRepository.java`

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    // Kiểm tra có Subject nào đang ACTIVE thuộc Level không
    boolean existsByLevelIdAndStatus(Long levelId, SubjectStatus status);

    // Đếm số Subject thuộc Level
    long countByLevelId(Long levelId);
}
```

---

## B. DTOs CẦN TẠO:

### B1. `src/main/java/org/fyp/tmssep490be/dtos/curriculum/CreateCurriculumDTO.java`

**📖 Giải thích:**
- DTO nhận dữ liệu từ client khi tạo/sửa Curriculum
- `@NotBlank` = validation, không được rỗng hoặc chỉ có whitespace

```java
package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCurriculumDTO {
    @NotBlank(message = "Mã chương trình là bắt buộc")
    private String code;

    @NotBlank(message = "Tên chương trình là bắt buộc")
    private String name;

    private String description;

    @lombok.Builder.Default
    private String language = "English";

    // Danh sách PLO (Program Learning Outcomes)
    private List<CreatePLODTO> plos;
}
```

---

### B2. `src/main/java/org/fyp/tmssep490be/dtos/curriculum/CurriculumResponseDTO.java`

```java
package org.fyp.tmssep490be.dtos.curriculum;

import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CurriculumResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private String language;
    private int levelCount;
    private String status;
    private String createdAt;
    private List<CreatePLODTO> plos;
    private List<LevelResponseDTO> levels;
}
```

---

### B3. `src/main/java/org/fyp/tmssep490be/dtos/curriculum/CurriculumWithLevelsDTO.java`

**📖 Giải thích:**
- DTO phức hợp chứa Curriculum + danh sách Levels
- Dùng cho dropdown khi tạo student skill assessment

```java
package org.fyp.tmssep490be.dtos.curriculum;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CurriculumWithLevelsDTO {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<LevelDTO> levels;
    private List<PLODTO> plos;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LevelDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Integer sortOrder;
        private String status;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PLODTO {
        private String code;
        private String description;
    }
}
```

---

### B4. `src/main/java/org/fyp/tmssep490be/dtos/curriculum/CreateLevelDTO.java`

```java
package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateLevelDTO {
    @NotNull(message = "ID chương trình là bắt buộc")
    private Long curriculumId;

    @NotBlank(message = "Mã cấp độ là bắt buộc")
    private String code;

    @NotBlank(message = "Tên cấp độ là bắt buộc")
    private String name;

    private String description;
}
```

---

### B5. `src/main/java/org/fyp/tmssep490be/dtos/curriculum/LevelResponseDTO.java`

```java
package org.fyp.tmssep490be.dtos.curriculum;

import lombok.*;

import java.time.OffsetDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LevelResponseDTO {
    private String id;
    private String code;
    private String name;
    private String description;
    private Long curriculumId;
    private String curriculumName;
    private String curriculumCode;
    private String status;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
```

---

### B6. `src/main/java/org/fyp/tmssep490be/dtos/curriculum/CreatePLODTO.java`

```java
package org.fyp.tmssep490be.dtos.curriculum;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreatePLODTO {
    @NotBlank(message = "Mã PLO là bắt buộc")
    private String code;

    @NotBlank(message = "Mô tả PLO là bắt buộc")
    private String description;
}
```

---

## ✅ Commit sau khi hoàn thành ENDPOINT 0:
```bash
git add src/main/java/org/fyp/tmssep490be/repositories/CurriculumRepository.java
git add src/main/java/org/fyp/tmssep490be/repositories/LevelRepository.java
git add src/main/java/org/fyp/tmssep490be/repositories/PLORepository.java
git add src/main/java/org/fyp/tmssep490be/repositories/SubjectRepository.java
git add src/main/java/org/fyp/tmssep490be/dtos/curriculum/
git commit -m "feat(base): add repositories and DTOs for Curriculum module"
```

---

# 🚀 ENDPOINT 1: GET /curriculum/curriculums-with-levels (Lấy tất cả curriculum + levels)

## 1.1 Service: `src/main/java/org/fyp/tmssep490be/services/CurriculumService.java`

> **LƯU Ý**: KHÔNG có interface, viết thẳng class giống AuthService

**📖 Giải thích Annotations:**
| Annotation | Ý nghĩa |
|------------|---------|
| `@Service` | Đánh dấu class là Service (tầng business logic) |
| `@RequiredArgsConstructor` | Lombok tự tạo constructor cho các field `final` |
| `@Slf4j` | Tạo biến `log` để ghi log |
| `@Transactional(readOnly = true)` | Tối ưu cho query chỉ đọc |

```java
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
```

---

## 1.2 Controller: `src/main/java/org/fyp/tmssep490be/controllers/CurriculumController.java`

**📖 Giải thích Annotations:**
| Annotation | Ý nghĩa |
|------------|---------|
| `@RestController` | Controller trả về JSON (không render view) |
| `@RequestMapping("/api/v1/curriculum")` | Base URL cho tất cả endpoints |
| `@PreAuthorize` | Kiểm tra role trước khi vào method |

```java
package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.curriculum.*;
import org.fyp.tmssep490be.services.CurriculumService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/curriculum")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quản lý Chương trình đào tạo", description = "APIs cho Curriculum và Level")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

    private final CurriculumService curriculumService;

    // Lấy tất cả curriculum với levels
    @GetMapping("/curriculums-with-levels")
    @Operation(summary = "Get all curriculums with their levels", 
        description = "Retrieve list of curriculums with their levels. Used for dropdowns.")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<List<CurriculumWithLevelsDTO>>> getAllCurriculumsWithLevels() {
        log.info("Fetching all curriculums with their levels");
        
        List<CurriculumWithLevelsDTO> curriculums = curriculumService.getAllCurriculumsWithLevels();
        
        log.info("Successfully retrieved {} curriculums with levels", curriculums.size());
        return ResponseEntity.ok(ResponseObject.<List<CurriculumWithLevelsDTO>>builder()
            .success(true)
            .message("Curriculums with levels retrieved successfully")
            .data(curriculums)
            .build());
    }
}
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(curriculum): GET /curriculums-with-levels - list all curriculums"
```

---

# 🚀 ENDPOINT 2: POST /curriculum/curriculums (Tạo curriculum mới)

## 2.1 Thêm vào Service `CurriculumService.java`:

**📖 Giải thích logic tạo mới:**
1. Kiểm tra code không trùng
2. Tạo entity với status = DRAFT
3. Lưu PLOs nếu có
4. Trả về DTO

```java
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
            .createdAt(curriculum.getCreatedAt() != null ? curriculum.getCreatedAt().toString() : null)
            .levelCount(0)
            .plos(request.getPlos() != null ? request.getPlos() : java.util.Collections.emptyList())
            .build();
    }
```

---

## 2.2 Thêm vào Controller:

```java
    // Tạo curriculum mới
    @PostMapping("/curriculums")
    @Operation(summary = "Create a new curriculum")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<CurriculumResponseDTO>> createCurriculum(
            @RequestBody CreateCurriculumDTO request) {
        log.info("Creating new curriculum: {}", request.getCode());
        CurriculumResponseDTO curriculum = curriculumService.createCurriculum(request);
        return ResponseEntity.ok(ResponseObject.<CurriculumResponseDTO>builder()
            .success(true)
            .message("Curriculum created successfully")
            .data(curriculum)
            .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(curriculum): POST /curriculums - create new curriculum"
```

---

# 🚀 ENDPOINT 3: GET /curriculum/curriculums/{id} (Chi tiết curriculum)

## 3.1 Thêm vào Service:

```java
    public CurriculumResponseDTO getCurriculum(Long id) {
        log.debug("Fetching curriculum with ID: {}", id);
        Curriculum curriculum = curriculumRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với ID: " + id));

        return CurriculumResponseDTO.builder()
            .id(curriculum.getId().toString())
            .code(curriculum.getCode())
            .name(curriculum.getName())
            .description(curriculum.getDescription())
            .language(curriculum.getLanguage())
            .status(curriculum.getStatus().name())
            .createdAt(curriculum.getCreatedAt() != null ? curriculum.getCreatedAt().toString() : null)
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
```

## 3.2 Thêm vào Controller:

```java
    // Lấy chi tiết curriculum
    @GetMapping("/curriculums/{id}")
    @Operation(summary = "Get curriculum details")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<CurriculumResponseDTO>> getCurriculum(@PathVariable Long id) {
        log.info("Fetching curriculum details for ID: {}", id);
        CurriculumResponseDTO curriculum = curriculumService.getCurriculum(id);
        return ResponseEntity.ok(ResponseObject.<CurriculumResponseDTO>builder()
            .success(true)
            .message("Curriculum details retrieved successfully")
            .data(curriculum)
            .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(curriculum): GET /curriculums/{id} - get curriculum details"
```

---

# 🚀 ENDPOINT 4: PUT /curriculum/curriculums/{id} (Cập nhật curriculum)

## 4.1 Thêm vào Service:

```java
    @Transactional
    public CurriculumResponseDTO updateCurriculum(Long id, CreateCurriculumDTO request) {
        log.info("Updating curriculum with ID: {}", id);
        Curriculum curriculum = curriculumRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chương trình với ID: " + id));

        // Update basic info
        curriculum.setName(request.getName());
        curriculum.setDescription(request.getDescription());
        curriculum.setLanguage(request.getLanguage() != null ? request.getLanguage() : curriculum.getLanguage());

        // Check unique code if changed
        if (!curriculum.getCode().equals(request.getCode())) {
            if (curriculumRepository.existsByCode(request.getCode())) {
                throw new IllegalArgumentException("Mã chương trình đã tồn tại: " + request.getCode());
            }
            curriculum.setCode(request.getCode());
        }

        // Update PLOs (Merge strategy)
        if (request.getPlos() != null) {
            Curriculum finalCurriculum = curriculum;

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
                        .curriculum(finalCurriculum)
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
```

## 4.2 Thêm vào Controller:

```java
    // Cập nhật curriculum
    @PutMapping("/curriculums/{id}")
    @Operation(summary = "Update curriculum")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<CurriculumResponseDTO>> updateCurriculum(
            @PathVariable Long id,
            @RequestBody CreateCurriculumDTO request) {
        log.info("Updating curriculum with ID: {}", id);
        CurriculumResponseDTO curriculum = curriculumService.updateCurriculum(id, request);
        return ResponseEntity.ok(ResponseObject.<CurriculumResponseDTO>builder()
            .success(true)
            .message("Curriculum updated successfully")
            .data(curriculum)
            .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(curriculum): PUT /curriculums/{id} - update curriculum"
```

---

# 🚀 ENDPOINT 5-6: PATCH deactivate/reactivate curriculum

## 5.1 Thêm vào Service:

```java
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
```

## 5.2 Thêm vào Controller:

```java
    // Deactivate curriculum
    @PatchMapping("/curriculums/{id}/deactivate")
    @Operation(summary = "Deactivate curriculum")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deactivateCurriculum(@PathVariable Long id) {
        log.info("Deactivating curriculum with ID: {}", id);
        curriculumService.deactivateCurriculum(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Curriculum deactivated successfully")
            .build());
    }

    // Reactivate curriculum
    @PatchMapping("/curriculums/{id}/reactivate")
    @Operation(summary = "Reactivate curriculum")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> reactivateCurriculum(@PathVariable Long id) {
        log.info("Reactivating curriculum with ID: {}", id);
        curriculumService.reactivateCurriculum(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Curriculum reactivated successfully")
            .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(curriculum): PATCH deactivate/reactivate curriculum"
```

---

# 🚀 ENDPOINT 7: DELETE /curriculum/curriculums/{id}

## 7.1 Thêm vào Service:

```java
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
```

## 7.2 Thêm vào Controller:

```java
    // Delete curriculum
    @DeleteMapping("/curriculums/{id}")
    @Operation(summary = "Delete curriculum")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deleteCurriculum(@PathVariable Long id) {
        log.info("Deleting curriculum with ID: {}", id);
        curriculumService.deleteCurriculum(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Curriculum deleted successfully")
            .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(curriculum): DELETE /curriculums/{id} - delete curriculum"
```

---

# 🚀 ENDPOINT 8-15: LEVEL CRUD (tương tự Curriculum)

## 8. POST /curriculum/levels (Tạo level mới)

### Thêm vào Service:

```java
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
```

### Thêm vào Controller:

```java
    // Tạo level mới
    @PostMapping("/levels")
    @Operation(summary = "Create a new level")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<LevelResponseDTO>> createLevel(@RequestBody CreateLevelDTO request) {
        log.info("Creating new level for curriculum ID: {}", request.getCurriculumId());
        LevelResponseDTO level = curriculumService.createLevel(request);
        return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
            .success(true)
            .message("Level created successfully")
            .data(level)
            .build());
    }
```

---

## 9. GET /curriculum/levels (Lấy danh sách levels)

### Thêm vào Service:

```java
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
```

### Thêm vào Controller:

```java
    // Lấy danh sách levels
    @GetMapping("/levels")
    @Operation(summary = "Get all levels", description = "Retrieve all levels, optionally filtered by curriculum.")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<List<LevelResponseDTO>>> getLevels(
            @RequestParam(required = false) Long curriculumId) {
        log.info("Fetching levels with curriculumId: {}", curriculumId);
        List<LevelResponseDTO> levels = curriculumService.getLevels(curriculumId);
        return ResponseEntity.ok(ResponseObject.<List<LevelResponseDTO>>builder()
            .success(true)
            .message("Levels retrieved successfully")
            .data(levels)
            .build());
    }
```

---

## 10. GET /curriculum/levels/{id} (Chi tiết level)

### Thêm vào Service:

```java
    public LevelResponseDTO getLevel(Long id) {
        log.debug("Fetching level with ID: {}", id);
        Level level = levelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấp độ với ID: " + id));
        return toLevelResponseDTO(level);
    }
```

### Thêm vào Controller:

```java
    // Lấy chi tiết level
    @GetMapping("/levels/{id}")
    @Operation(summary = "Get level details")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<LevelResponseDTO>> getLevel(@PathVariable Long id) {
        log.info("Fetching level details for ID: {}", id);
        LevelResponseDTO level = curriculumService.getLevel(id);
        return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
            .success(true)
            .message("Level details retrieved successfully")
            .data(level)
            .build());
    }
```

---

## 11. PUT /curriculum/levels/{id} (Cập nhật level)

### Thêm vào Service:

```java
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
```

### Thêm vào Controller:

```java
    // Cập nhật level
    @PutMapping("/levels/{id}")
    @Operation(summary = "Update level")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<LevelResponseDTO>> updateLevel(
            @PathVariable Long id,
            @RequestBody CreateLevelDTO request) {
        log.info("Updating level with ID: {}", id);
        LevelResponseDTO level = curriculumService.updateLevel(id, request);
        return ResponseEntity.ok(ResponseObject.<LevelResponseDTO>builder()
            .success(true)
            .message("Level updated successfully")
            .data(level)
            .build());
    }
```

---

## 12-13. PATCH deactivate/reactivate level

### Thêm vào Service:

```java
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
```

### Thêm vào Controller:

```java
    // Deactivate level
    @PatchMapping("/levels/{id}/deactivate")
    @Operation(summary = "Deactivate level")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deactivateLevel(@PathVariable Long id) {
        log.info("Deactivating level with ID: {}", id);
        curriculumService.deactivateLevel(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Level deactivated successfully")
            .build());
    }

    // Reactivate level
    @PatchMapping("/levels/{id}/reactivate")
    @Operation(summary = "Reactivate level")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> reactivateLevel(@PathVariable Long id) {
        log.info("Reactivating level with ID: {}", id);
        curriculumService.reactivateLevel(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Level reactivated successfully")
            .build());
    }
```

---

## 14. PUT /curriculum/curriculums/{id}/levels/sort-order (Sắp xếp thứ tự levels)

### Thêm vào Service:

```java
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
```

### Thêm vào Controller:

```java
    // Update level sort order
    @PutMapping("/curriculums/{id}/levels/sort-order")
    @Operation(summary = "Update level sort order for a curriculum")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> updateLevelSortOrder(
            @PathVariable Long id,
            @RequestBody List<Long> levelIds) {
        log.info("Updating level sort order for curriculum ID: {}", id);
        curriculumService.updateLevelSortOrder(id, levelIds);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Level sort order updated successfully")
            .build());
    }
```

---

## 15. DELETE /curriculum/levels/{id}

### Thêm vào Service:

```java
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
```

### Thêm vào Controller:

```java
    // Delete level
    @DeleteMapping("/levels/{id}")
    @Operation(summary = "Delete level")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<Void>> deleteLevel(@PathVariable Long id) {
        log.info("Deleting level with ID: {}", id);
        curriculumService.deleteLevel(id);
        return ResponseEntity.ok(ResponseObject.<Void>builder()
            .success(true)
            .message("Level deleted successfully")
            .build());
    }
```

---

# 🚀 ENDPOINT 16-17: Timeslot Duration

## Thêm vào Service:

```java
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
```

## Thêm vào Controller:

```java
    // Get standard timeslot duration
    @GetMapping("/timeslot-duration")
    @Operation(summary = "Get standard timeslot duration", description = "Retrieve standard timeslot duration in hours.")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<BigDecimal>> getStandardTimeslotDuration() {
        log.info("Fetching standard timeslot duration");
        BigDecimal duration = curriculumService.getStandardTimeslotDuration();
        return ResponseEntity.ok(ResponseObject.<BigDecimal>builder()
            .success(true)
            .message("Standard timeslot duration retrieved successfully")
            .data(duration)
            .build());
    }

    // Get all timeslot durations
    @GetMapping("/timeslot-durations")
    @Operation(summary = "Get all unique timeslot durations")
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER', 'ADMIN', 'SUBJECT_LEADER')")
    public ResponseEntity<ResponseObject<List<BigDecimal>>> getAllTimeslotDurations() {
        log.info("Fetching all unique timeslot durations");
        List<BigDecimal> durations = curriculumService.getAllTimeslotDurations();
        return ResponseEntity.ok(ResponseObject.<List<BigDecimal>>builder()
            .success(true)
            .message("Timeslot durations retrieved successfully")
            .data(durations)
            .build());
    }
```

---

## ✅ Commit cuối:
```bash
git add .
git commit -m "feat(curriculum): complete Curriculum and Level management (17 endpoints)"
```

---

# 📝 TỔNG KẾT

## Danh sách 17 Endpoints đã migrate:

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 1 | GET | `/curriculum/curriculums-with-levels` | Tất cả curriculum + levels |
| 2 | POST | `/curriculum/curriculums` | Tạo curriculum |
| 3 | GET | `/curriculum/curriculums/{id}` | Chi tiết curriculum |
| 4 | PUT | `/curriculum/curriculums/{id}` | Cập nhật curriculum |
| 5 | PATCH | `/curriculum/curriculums/{id}/deactivate` | Ngưng hoạt động |
| 6 | PATCH | `/curriculum/curriculums/{id}/reactivate` | Kích hoạt lại |
| 7 | DELETE | `/curriculum/curriculums/{id}` | Xóa curriculum |
| 8 | POST | `/curriculum/levels` | Tạo level |
| 9 | GET | `/curriculum/levels` | Danh sách levels |
| 10 | GET | `/curriculum/levels/{id}` | Chi tiết level |
| 11 | PUT | `/curriculum/levels/{id}` | Cập nhật level |
| 12 | PATCH | `/curriculum/levels/{id}/deactivate` | Ngưng hoạt động level |
| 13 | PATCH | `/curriculum/levels/{id}/reactivate` | Kích hoạt lại level |
| 14 | PUT | `/curriculum/curriculums/{id}/levels/sort-order` | Sắp xếp levels |
| 15 | DELETE | `/curriculum/levels/{id}` | Xóa level |
| 16 | GET | `/curriculum/timeslot-duration` | Duration chuẩn |
| 17 | GET | `/curriculum/timeslot-durations` | Tất cả durations |

## Files đã tạo:

```
src/main/java/org/fyp/tmssep490be/
├── repositories/
│   ├── CurriculumRepository.java    [NEW]
│   ├── LevelRepository.java         [UPDATED]
│   ├── PLORepository.java           [NEW]
│   └── SubjectRepository.java       [NEW]
├── dtos/curriculum/
│   ├── CreateCurriculumDTO.java     [NEW]
│   ├── CurriculumResponseDTO.java   [NEW]
│   ├── CurriculumWithLevelsDTO.java [NEW]
│   ├── CreateLevelDTO.java          [NEW]
│   ├── LevelResponseDTO.java        [NEW]
│   └── CreatePLODTO.java            [NEW]
├── services/
│   └── CurriculumService.java       [NEW]
└── controllers/
    └── CurriculumController.java    [NEW]
```
