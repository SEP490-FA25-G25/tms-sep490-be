# 🛠️ RESOURCE MANAGEMENT - HƯỚNG DẪN COPY PASTE

> **Module**: Quản lý Tài nguyên (Phòng học, Zoom Account)
> **Tổng cộng**: 7 endpoints
> **Thời gian ước tính**: 1-2 giờ

---

## 📋 DANH SÁCH ENDPOINTS

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 1 | GET | `/api/v1/resources` | Lấy danh sách resources |
| 2 | GET | `/api/v1/resources/{id}` | Lấy chi tiết resource |
| 3 | POST | `/api/v1/resources` | Tạo resource mới |
| 4 | PUT | `/api/v1/resources/{id}` | Cập nhật resource |
| 5 | DELETE | `/api/v1/resources/{id}` | Xóa resource |
| 6 | PATCH | `/api/v1/resources/{id}/status` | Đổi trạng thái |
| 7 | GET | `/api/v1/resources/{id}/sessions` | Lấy sessions đang dùng |

---

## 📁 ENDPOINT 0: BASE SETUP (Files cơ bản)

### 0.1. ResourceDTO.java (Response DTO)

📍 **File**: `src/main/java/org/fyp/tmssep490be/dtos/resource/ResourceDTO.java`

```java
package org.fyp.tmssep490be.dtos.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDTO {
    private Long id;
    private Long branchId;
    private String branchName;
    private String resourceType;          // "ROOM" hoặc "VIRTUAL"
    private String code;                   // Mã: "HN-ROOM-101"
    private String name;                   // Tên: "Phòng 101"
    private String description;
    private Integer capacity;              // Sức chứa
    private Integer capacityOverride;      // Ghi đè sức chứa (nếu cần)
    private String equipment;              // Thiết bị (cho ROOM)
    
    // Các trường cho VIRTUAL (Zoom)
    private String meetingUrl;
    private String meetingId;
    private String meetingPasscode;
    private String accountEmail;
    private String licenseType;
    private String startDate;              // Ngày bắt đầu license
    private String expiryDate;             // Ngày hết hạn
    private String renewalDate;            // Ngày gia hạn
    
    private String createdAt;
    private String updatedAt;
    private String status;                 // "ACTIVE" hoặc "INACTIVE"
    
    // Thống kê
    private Long activeClassesCount;
    private Long totalSessionsCount;
    private String nextSessionInfo;
    private Boolean hasAnySessions;
    private Boolean hasFutureSessions;
}
```

📖 **Giải thích**:
- `resourceType`: 2 loại - `ROOM` (phòng vật lý) và `VIRTUAL` (Zoom account)
- Các trường `meeting*`, `account*`, `license*` chỉ dùng cho VIRTUAL
- Các trường `equipment` chỉ dùng cho ROOM
- Statistics để hiển thị liên kết với sessions

---

### 0.2. ResourceRequestDTO.java (Request DTO)

📍 **File**: `src/main/java/org/fyp/tmssep490be/dtos/resource/ResourceRequestDTO.java`

```java
package org.fyp.tmssep490be.dtos.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequestDTO {
    private Long branchId;
    private String resourceType;           // "ROOM" hoặc "VIRTUAL"
    private String code;                   // Mã tài nguyên (VD: "ROOM-101")
    private String name;                   // Tên tài nguyên
    private String description;            // Mô tả (tối thiểu 10 ký tự nếu có)
    private Integer capacity;              // Sức chứa (ROOM: max 40, VIRTUAL: max 100)
    private Integer capacityOverride;      // Ghi đè sức chứa
    private String equipment;              // Thiết bị (chỉ cho ROOM)
    
    // Các trường cho VIRTUAL (Zoom)
    private String meetingUrl;             // URL phòng Zoom
    private String meetingId;              // Meeting ID
    private String meetingPasscode;        // Passcode
    private String accountEmail;           // Email tài khoản Zoom
    private String accountPassword;        // Password (không trả về trong response)
    private String licenseType;            // Loại license: "Basic", "Pro", etc.
    private String startDate;              // Ngày bắt đầu (YYYY-MM-DD)
    private String expiryDate;             // Ngày hết hạn (YYYY-MM-DD)
    private String renewalDate;            // Ngày gia hạn (YYYY-MM-DD)
}
```

---

### 0.3. ResourceRepository.java

📍 **File**: `src/main/java/org/fyp/tmssep490be/repositories/ResourceRepository.java`

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Resource;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    // Lấy resources theo branch
    List<Resource> findByBranchIdOrderByNameAsc(Long branchId);
    
    // Kiểm tra trùng code trong cùng branch
    boolean existsByBranchIdAndCodeIgnoreCase(Long branchId, String code);
    
    // Kiểm tra trùng code (loại trừ chính nó - dùng khi UPDATE)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM Resource r " +
           "WHERE r.branch.id = :branchId " +
           "AND LOWER(r.code) = LOWER(:code) " +
           "AND r.id <> :excludeId")
    boolean existsByBranchIdAndCodeIgnoreCaseAndIdNot(
            @Param("branchId") Long branchId,
            @Param("code") String code,
            @Param("excludeId") Long excludeId);
    
    // Kiểm tra trùng tên trong cùng branch
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);
    
    // Kiểm tra trùng tên (loại trừ chính nó)
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM Resource r " +
           "WHERE r.branch.id = :branchId " +
           "AND LOWER(r.name) = LOWER(:name) " +
           "AND r.id <> :excludeId")
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(
            @Param("branchId") Long branchId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);
    
    // Đếm resources theo branch
    long countByBranchId(Long branchId);
    
    // Đếm resources theo branch và status
    long countByBranchIdAndStatus(Long branchId, ResourceStatus status);
    
    // Lấy resources VIRTUAL có ngày hết hạn (cho scheduler job)
    @Query("SELECT r FROM Resource r " +
           "WHERE r.resourceType = :resourceType " +
           "AND r.expiryDate IS NOT NULL " +
           "ORDER BY r.expiryDate ASC")
    List<Resource> findByResourceTypeAndExpiryDateIsNotNull(
            @Param("resourceType") ResourceType resourceType);
}
```

📖 **Giải thích**:
- `existsBy...IgnoreCase`: Check trùng không phân biệt hoa thường
- `...AndIdNot`: Loại trừ record hiện tại khi update

---

### 0.4. SessionResourceRepository.java

📍 **File**: `src/main/java/org/fyp/tmssep490be/repositories/SessionResourceRepository.java`

⚠️ **LƯU Ý**: File này có thể đã tồn tại. Chỉ thêm các methods còn thiếu.

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.SessionResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessionResourceRepository extends JpaRepository<SessionResource, SessionResource.SessionResourceId> {

    // Kiểm tra resource có được dùng trong session nào không
    boolean existsByResourceId(Long resourceId);
    
    // Đếm số lớp đang dùng resource
    @Query("SELECT COUNT(DISTINCT s.classEntity.id) FROM SessionResource sr " +
           "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED'")
    Long countDistinctClassesByResourceId(@Param("resourceId") Long resourceId);
    
    // Đếm tổng số sessions dùng resource
    @Query("SELECT COUNT(sr) FROM SessionResource sr " +
           "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED'")
    Long countSessionsByResourceId(@Param("resourceId") Long resourceId);
    
    // Tìm session tiếp theo của resource
    @Query(value = """
        SELECT s.* FROM session s
        JOIN session_resource sr ON s.id = sr.session_id
        JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
        WHERE sr.resource_id = :resourceId
        AND s.status != 'CANCELLED'
        AND (s.date > :currentDate OR (s.date = :currentDate AND tst.start_time > :currentTime))
        ORDER BY s.date ASC, tst.start_time ASC
        LIMIT 1
        """, nativeQuery = true)
    Session findNextSessionByResourceId(
            @Param("resourceId") Long resourceId,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime);
    
    // Lấy tất cả sessions của resource
    @Query("SELECT s FROM SessionResource sr " +
           "JOIN sr.session s WHERE sr.resource.id = :resourceId AND s.status != 'CANCELLED' " +
           "ORDER BY s.date DESC")
    List<Session> findSessionsByResourceId(@Param("resourceId") Long resourceId);
    
    // Tìm sức chứa lớn nhất của các lớp đang dùng resource (để validate khi giảm capacity)
    @Query("SELECT MAX(s.classEntity.maxCapacity) FROM SessionResource sr " +
           "JOIN sr.session s WHERE sr.resource.id = :resourceId")
    Integer findMaxClassCapacityByResourceId(@Param("resourceId") Long resourceId);
}
```

---

### 0.5. Commit BASE SETUP

```bash
git add src/main/java/org/fyp/tmssep490be/dtos/resource/ResourceDTO.java \
        src/main/java/org/fyp/tmssep490be/dtos/resource/ResourceRequestDTO.java \
        src/main/java/org/fyp/tmssep490be/repositories/ResourceRepository.java \
        src/main/java/org/fyp/tmssep490be/repositories/SessionResourceRepository.java

git commit -m "feat(resource): add base DTOs and repositories for Resource module"
```

---

## 📁 ENDPOINT 1: GET /resources (Lấy danh sách)

### 1.1. ResourceService.java

📍 **File**: `src/main/java/org/fyp/tmssep490be/services/ResourceService.java`

```java
package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ResourceType;
import org.fyp.tmssep490be.exceptions.*;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionResourceRepository sessionResourceRepository;

    // Lấy danh sách resources với filter
    @Transactional(readOnly = true)
    public List<ResourceDTO> getAllResources(Long branchId, String resourceType, String search, Long userId) {
        log.info("Getting resources - branchId: {}, type: {}, search: {}, userId: {}",
                branchId, resourceType, search, userId);

        // 1. Lấy branches user có quyền
        List<Long> userBranches = getBranchIdsForUser(userId);
        if (userBranches.isEmpty()) {
            log.warn("User {} has no branch access", userId);
            return List.of();
        }

        // 2. Validate branchId
        if (branchId != null && !userBranches.contains(branchId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Không có quyền truy cập chi nhánh này");
        }

        // 3. Query
        List<Resource> resources;
        if (branchId != null) {
            resources = resourceRepository.findByBranchIdOrderByNameAsc(branchId);
        } else {
            resources = resourceRepository.findAll().stream()
                    .filter(r -> userBranches.contains(r.getBranch().getId()))
                    .collect(Collectors.toList());
        }

        // 4. Filter theo type
        if (resourceType != null && !resourceType.isEmpty()) {
            ResourceType type = ResourceType.valueOf(resourceType);
            resources = resources.stream()
                    .filter(r -> r.getResourceType() == type)
                    .collect(Collectors.toList());
        }

        // 5. Filter theo search (tên hoặc mã)
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            resources = resources.stream()
                    .filter(r -> r.getName().toLowerCase().contains(searchLower) ||
                            r.getCode().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        return resources.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Lấy resource theo ID
    @Transactional(readOnly = true)
    public ResourceDTO getResourceById(Long id) {
        log.info("Getting resource by id: {}", id);
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
        return convertToDTO(resource);
    }

    // ==================== HELPER METHODS ====================

    private List<Long> getBranchIdsForUser(Long userId) {
        if (userId == null) return List.of();
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null && !user.getUserBranches().isEmpty()) {
            return user.getUserBranches().stream()
                    .map(ub -> ub.getBranch().getId())
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private ResourceDTO convertToDTO(Resource resource) {
        ResourceDTO.ResourceDTOBuilder builder = ResourceDTO.builder()
                .id(resource.getId())
                .branchId(resource.getBranch().getId())
                .branchName(resource.getBranch().getName())
                .resourceType(resource.getResourceType().toString())
                .code(resource.getCode())
                .name(resource.getName())
                .description(resource.getDescription())
                .capacity(resource.getCapacity())
                .capacityOverride(resource.getCapacityOverride())
                .equipment(resource.getEquipment())
                .meetingUrl(resource.getMeetingUrl())
                .meetingId(resource.getMeetingId())
                .meetingPasscode(resource.getMeetingPasscode())
                .accountEmail(resource.getAccountEmail())
                .licenseType(resource.getLicenseType())
                .startDate(resource.getStartDate() != null ? resource.getStartDate().toString() : null)
                .expiryDate(resource.getExpiryDate() != null ? resource.getExpiryDate().toString() : null)
                .renewalDate(resource.getRenewalDate() != null ? resource.getRenewalDate().toString() : null)
                .createdAt(resource.getCreatedAt() != null ? resource.getCreatedAt().toString() : null)
                .updatedAt(resource.getUpdatedAt() != null ? resource.getUpdatedAt().toString() : null)
                .status(resource.getStatus().name());

        // Thêm statistics
        try {
            Long activeClasses = sessionResourceRepository.countDistinctClassesByResourceId(resource.getId());
            Long totalSessions = sessionResourceRepository.countSessionsByResourceId(resource.getId());
            Session nextSession = sessionResourceRepository.findNextSessionByResourceId(
                    resource.getId(), LocalDate.now(), LocalTime.now());

            builder.activeClassesCount(activeClasses)
                    .totalSessionsCount(totalSessions)
                    .hasAnySessions(totalSessions > 0)
                    .hasFutureSessions(nextSession != null);

            if (nextSession != null) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String nextSessionInfo = String.format("%s lúc %s | %s - %s",
                        nextSession.getDate().format(dateFormatter),
                        nextSession.getTimeSlotTemplate().getStartTime().format(timeFormatter),
                        nextSession.getClassEntity().getCode(),
                        nextSession.getClassEntity().getName());
                builder.nextSessionInfo(nextSessionInfo);
            }
        } catch (Exception e) {
            log.error("Error calculating statistics: {}", e.getMessage());
            builder.activeClassesCount(0L).totalSessionsCount(0L)
                    .hasAnySessions(false).hasFutureSessions(false);
        }

        return builder.build();
    }

    private SessionInfoDTO convertSessionToDTO(Session session) {
        return SessionInfoDTO.builder()
                .id(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .className(session.getClassEntity().getName())
                .date(session.getDate().toString())
                .startTime(session.getTimeSlotTemplate().getStartTime().toString())
                .endTime(session.getTimeSlotTemplate().getEndTime().toString())
                .status(session.getStatus().toString())
                .type(session.getType().toString())
                .build();
    }
}
```

---

### 1.2. ResourceController.java

📍 **File**: `src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java`

```java
package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quản lý Tài nguyên")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

    private final ResourceService resourceService;

    // GET /resources - Lấy danh sách
    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get all resources")
    public ResponseEntity<List<ResourceDTO>> getAllResources(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        List<ResourceDTO> resources = resourceService.getAllResources(
                branchId, resourceType, search, currentUser.getId());
        return ResponseEntity.ok(resources);
    }

    // GET /resources/{id} - Lấy chi tiết
    @GetMapping("/resources/{id}")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<ResourceDTO> getResourceById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ResourceDTO resource = resourceService.getResourceById(id);
        return ResponseEntity.ok(resource);
    }
}
```

---

### 1.3. Commit ENDPOINT 1

```bash
git add src/main/java/org/fyp/tmssep490be/services/ResourceService.java \
        src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java

git commit -m "feat(resource): add GET /resources and GET /resources/{id} endpoints"
```

---

## 📁 ENDPOINT 2: POST /resources (Tạo mới)

### 2.1. Thêm vào ResourceService.java

```java
    // Tạo resource mới
    @Transactional
    public ResourceDTO createResource(ResourceRequestDTO request, Long userId) {
        log.info("Creating resource: {}", request);

        // 1. Validate request cơ bản
        validateCreateRequest(request);

        // 2. Lấy branchId và validate quyền
        Long branchId = request.getBranchId();
        List<Long> userBranches = getBranchIdsForUser(userId);
        if (!userBranches.contains(branchId)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Không có quyền truy cập chi nhánh này");
        }

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // 3. Tạo full code với prefix branch
        String code = request.getCode().trim();
        String branchCode = branch.getCode();
        String fullCode = code.startsWith(branchCode + "-") ? code : branchCode + "-" + code;

        // 4. Kiểm tra trùng code
        if (resourceRepository.existsByBranchIdAndCodeIgnoreCase(branchId, fullCode)) {
            throw new BusinessRuleException("Mã tài nguyên '" + fullCode + "' đã tồn tại trong chi nhánh này");
        }

        // 5. Kiểm tra trùng tên
        if (resourceRepository.existsByBranchIdAndNameIgnoreCase(branchId, request.getName().trim())) {
            throw new BusinessRuleException("Tên tài nguyên '" + request.getName() + "' đã tồn tại trong chi nhánh này");
        }

        // 6. Validate type-specific fields
        validateResourceTypeFields(request);

        // 7. Tạo entity
        Resource resource = new Resource();
        resource.setBranch(branch);
        resource.setCode(fullCode);
        resource.setStatus(ResourceStatus.ACTIVE);
        resource.setCreatedAt(OffsetDateTime.now());
        resource.setUpdatedAt(OffsetDateTime.now());

        updateResourceFromRequest(resource, request, userId);

        Resource saved = resourceRepository.save(resource);
        log.info("Created resource with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    // ==================== VALIDATION METHODS ====================

    private void validateCreateRequest(ResourceRequestDTO request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new BusinessRuleException("Mã tài nguyên là bắt buộc");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessRuleException("Tên tài nguyên là bắt buộc");
        }
        if (request.getResourceType() == null || request.getResourceType().trim().isEmpty()) {
            throw new BusinessRuleException("Loại tài nguyên là bắt buộc");
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()
                && request.getDescription().trim().length() < 10) {
            throw new BusinessRuleException("Mô tả phải có ít nhất 10 ký tự hoặc để trống");
        }
        validateCapacity(request, "VIRTUAL".equals(request.getResourceType()));
    }

    private void validateCapacity(ResourceRequestDTO request, boolean isVirtual) {
        if (request.getCapacity() != null) {
            int maxCapacity = isVirtual ? 100 : 40;
            if (request.getCapacity() <= 0) {
                throw new BusinessRuleException("Sức chứa phải là số dương lớn hơn 0");
            }
            if (request.getCapacity() > maxCapacity) {
                if (isVirtual) {
                    throw new BusinessRuleException("Sức chứa của phòng ảo (Zoom) tối đa là 100 người");
                } else {
                    throw new BusinessRuleException("Sức chứa của phòng học tối đa là 40 người");
                }
            }
        }
    }

    private void validateResourceTypeFields(ResourceRequestDTO request) {
        if ("VIRTUAL".equals(request.getResourceType())) {
            boolean hasMeetingUrl = request.getMeetingUrl() != null && !request.getMeetingUrl().trim().isEmpty();
            boolean hasAccountEmail = request.getAccountEmail() != null && !request.getAccountEmail().trim().isEmpty();

            if (!hasMeetingUrl && !hasAccountEmail) {
                throw new BusinessRuleException("Tài nguyên ảo cần có Meeting URL hoặc Account Email");
            }

            if (hasMeetingUrl && !request.getMeetingUrl().matches("^https?://.*")) {
                throw new BusinessRuleException("Meeting URL phải bắt đầu bằng http:// hoặc https://");
            }

            if (hasAccountEmail && !request.getAccountEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new BusinessRuleException("Account Email không đúng định dạng email");
            }

            validateExpiryDate(request.getExpiryDate());
        }
    }

    private void validateExpiryDate(String expiryDateStr) {
        if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
            try {
                LocalDate expiryDate = LocalDate.parse(expiryDateStr);
                if (expiryDate.isBefore(LocalDate.now())) {
                    throw new BusinessRuleException("Ngày hết hạn phải là ngày trong tương lai");
                }
            } catch (DateTimeParseException e) {
                throw new BusinessRuleException("Ngày hết hạn không đúng định dạng (YYYY-MM-DD)");
            }
        }
    }

    private void updateResourceFromRequest(Resource resource, ResourceRequestDTO request, Long userId) {
        if (request.getResourceType() != null) {
            resource.setResourceType(ResourceType.valueOf(request.getResourceType()));
        }
        if (request.getName() != null) {
            resource.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription().trim());
        }
        if (request.getCapacity() != null) {
            resource.setCapacity(request.getCapacity());
        }
        if (request.getCapacityOverride() != null) {
            resource.setCapacityOverride(request.getCapacityOverride());
        }
        if (request.getEquipment() != null) {
            resource.setEquipment(request.getEquipment());
        }
        if (request.getMeetingUrl() != null) {
            resource.setMeetingUrl(request.getMeetingUrl());
        }
        if (request.getMeetingId() != null) {
            resource.setMeetingId(request.getMeetingId());
        }
        if (request.getMeetingPasscode() != null) {
            resource.setMeetingPasscode(request.getMeetingPasscode());
        }
        if (request.getAccountEmail() != null) {
            resource.setAccountEmail(request.getAccountEmail());
        }
        if (request.getAccountPassword() != null) {
            resource.setAccountPassword(request.getAccountPassword());
        }
        if (request.getLicenseType() != null) {
            resource.setLicenseType(request.getLicenseType());
        }
        if (request.getStartDate() != null) {
            resource.setStartDate(request.getStartDate().isEmpty() ? null : LocalDate.parse(request.getStartDate()));
        }
        if (request.getExpiryDate() != null) {
            resource.setExpiryDate(request.getExpiryDate().isEmpty() ? null : LocalDate.parse(request.getExpiryDate()));
        }
        if (request.getRenewalDate() != null) {
            resource.setRenewalDate(request.getRenewalDate().isEmpty() ? null : LocalDate.parse(request.getRenewalDate()));
        }

        if (resource.getCreatedBy() == null && userId != null) {
            UserAccount user = userAccountRepository.findById(userId).orElse(null);
            resource.setCreatedBy(user);
        }
    }
```

---

### 2.2. Thêm vào ResourceController.java

```java
    // POST /resources - Tạo mới
    @PostMapping("/resources")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Create new resource")
    public ResponseEntity<ResourceDTO> createResource(
            @RequestBody ResourceRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        ResourceDTO saved = resourceService.createResource(request, currentUser.getId());
        return ResponseEntity.ok(saved);
    }
```

---

### 2.3. Commit ENDPOINT 2

```bash
git add src/main/java/org/fyp/tmssep490be/services/ResourceService.java \
        src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java

git commit -m "feat(resource): add POST /resources endpoint for creating resources"
```

---

## 📁 ENDPOINT 3: PUT /resources/{id} (Cập nhật)

### 3.1. Thêm vào ResourceService.java

```java
    // Cập nhật resource
    @Transactional
    public ResourceDTO updateResource(Long id, ResourceRequestDTO request, Long userId) {
        log.info("Updating resource {}: {}", id, request);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        Long branchId = resource.getBranch().getId();

        // Validate code nếu thay đổi
        if (request.getCode() != null && !request.getCode().trim().isEmpty()) {
            String code = request.getCode().trim();
            String branchCode = resource.getBranch().getCode();
            String fullCode = code.startsWith(branchCode + "-") ? code : branchCode + "-" + code;

            if (resourceRepository.existsByBranchIdAndCodeIgnoreCaseAndIdNot(branchId, fullCode, id)) {
                throw new BusinessRuleException("Mã tài nguyên '" + fullCode + "' đã tồn tại trong chi nhánh này");
            }
            resource.setCode(fullCode);
        }

        // Validate tên nếu thay đổi
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            if (resourceRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(branchId, request.getName().trim(), id)) {
                throw new BusinessRuleException("Tên tài nguyên '" + request.getName() + "' đã tồn tại trong chi nhánh này");
            }
        }

        // Validate mô tả
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()
                && request.getDescription().trim().length() < 10) {
            throw new BusinessRuleException("Mô tả phải có ít nhất 10 ký tự hoặc để trống");
        }

        // Validate capacity
        validateCapacity(request, resource.getResourceType() == ResourceType.VIRTUAL);

        // Validate VIRTUAL resource fields
        if (resource.getResourceType() == ResourceType.VIRTUAL) {
            validateVirtualResourceFieldsForUpdate(request);
        }

        // Kiểm tra giảm capacity
        if (request.getCapacity() != null) {
            Integer maxRequired = sessionResourceRepository.findMaxClassCapacityByResourceId(id);
            if (maxRequired != null && request.getCapacity() < maxRequired) {
                throw new BusinessRuleException("Không thể giảm sức chứa xuống " + request.getCapacity() +
                        " vì tài nguyên này đang được sử dụng cho lớp học có sĩ số tối đa là " + maxRequired);
            }
        }

        updateResourceFromRequest(resource, request, userId);
        resource.setUpdatedAt(OffsetDateTime.now());

        Resource saved = resourceRepository.save(resource);
        return convertToDTO(saved);
    }

    private void validateVirtualResourceFieldsForUpdate(ResourceRequestDTO request) {
        if (request.getMeetingUrl() != null && !request.getMeetingUrl().trim().isEmpty()
                && !request.getMeetingUrl().matches("^https?://.*")) {
            throw new BusinessRuleException("Meeting URL phải bắt đầu bằng http:// hoặc https://");
        }

        if (request.getAccountEmail() != null && !request.getAccountEmail().trim().isEmpty()
                && !request.getAccountEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new BusinessRuleException("Account Email không đúng định dạng email");
        }

        validateExpiryDate(request.getExpiryDate());
    }
```

---

### 3.2. Thêm vào ResourceController.java

```java
    // PUT /resources/{id} - Cập nhật
    @PutMapping("/resources/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update resource")
    public ResponseEntity<ResourceDTO> updateResource(
            @PathVariable Long id,
            @RequestBody ResourceRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ResourceDTO saved = resourceService.updateResource(id, request, currentUser.getId());
        return ResponseEntity.ok(saved);
    }
```

---

### 3.3. Commit ENDPOINT 3

```bash
git add src/main/java/org/fyp/tmssep490be/services/ResourceService.java \
        src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java

git commit -m "feat(resource): add PUT /resources/{id} endpoint for updating resources"
```

---

## 📁 ENDPOINT 4: DELETE /resources/{id} (Xóa)

### 4.1. Thêm vào ResourceService.java

```java
    // Xóa resource
    @Transactional
    public void deleteResource(Long id) {
        log.info("Deleting resource {}", id);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        // Phải ngưng hoạt động trước khi xóa
        if (resource.getStatus() != ResourceStatus.INACTIVE) {
            throw new BusinessRuleException("Vui lòng ngưng hoạt động tài nguyên trước khi xóa");
        }

        // Không thể xóa nếu có session đang dùng
        if (sessionResourceRepository.existsByResourceId(id)) {
            throw new BusinessRuleException("Không thể xóa vì tài nguyên này đang được sử dụng trong buổi học");
        }

        resourceRepository.deleteById(id);
        log.info("Deleted resource with ID: {}", id);
    }
```

---

### 4.2. Thêm vào ResourceController.java

```java
    // DELETE /resources/{id} - Xóa
    @DeleteMapping("/resources/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Delete resource")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
```

---

### 4.3. Commit ENDPOINT 4

```bash
git add src/main/java/org/fyp/tmssep490be/services/ResourceService.java \
        src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java

git commit -m "feat(resource): add DELETE /resources/{id} endpoint"
```

---

## 📁 ENDPOINT 5: PATCH /resources/{id}/status (Đổi trạng thái)

### 5.1. Thêm vào ResourceService.java

```java
    // Đổi trạng thái hoạt động/ngưng hoạt động
    @Transactional
    public ResourceDTO updateResourceStatus(Long id, ResourceStatus status) {
        log.info("Updating status for resource {}: {}", id, status);

        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));

        // Nếu ngưng hoạt động → kiểm tra không có session tương lai
        if (status == ResourceStatus.INACTIVE) {
            Session nextSession = sessionResourceRepository.findNextSessionByResourceId(
                    id, LocalDate.now(), LocalTime.now());
            if (nextSession != null) {
                throw new BusinessRuleException(
                        "Không thể ngưng hoạt động vì tài nguyên này đang được sử dụng cho các buổi học trong tương lai");
            }
        }

        resource.setStatus(status);
        resource.setUpdatedAt(OffsetDateTime.now());
        Resource saved = resourceRepository.save(resource);
        return convertToDTO(saved);
    }
```

---

### 5.2. Thêm vào ResourceController.java

```java
    // PATCH /resources/{id}/status - Đổi trạng thái
    @PatchMapping("/resources/{id}/status")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update resource status")
    public ResponseEntity<ResourceDTO> updateResourceStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (!request.containsKey("status")) {
            throw new RuntimeException("Field 'status' is required");
        }
        ResourceStatus status = ResourceStatus.valueOf(request.get("status"));
        ResourceDTO saved = resourceService.updateResourceStatus(id, status);
        return ResponseEntity.ok(saved);
    }
```

---

### 5.3. Commit ENDPOINT 5

```bash
git add src/main/java/org/fyp/tmssep490be/services/ResourceService.java \
        src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java

git commit -m "feat(resource): add PATCH /resources/{id}/status endpoint"
```

---

## 📁 ENDPOINT 6: GET /resources/{id}/sessions (Lấy sessions)

### 6.1. Thêm vào ResourceService.java

```java
    // Lấy danh sách sessions đang dùng resource
    @Transactional(readOnly = true)
    public List<SessionInfoDTO> getSessionsByResourceId(Long id) {
        log.info("Getting sessions for resource {}", id);

        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found with id: " + id);
        }

        List<Session> sessions = sessionResourceRepository.findSessionsByResourceId(id);
        return sessions.stream().map(this::convertSessionToDTO).collect(Collectors.toList());
    }
```

---

### 6.2. Thêm vào ResourceController.java

```java
    // GET /resources/{id}/sessions - Lấy sessions đang dùng
    @GetMapping("/resources/{id}/sessions")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get sessions using a resource")
    public ResponseEntity<List<SessionInfoDTO>> getSessionsByResourceId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SessionInfoDTO> sessions = resourceService.getSessionsByResourceId(id);
        return ResponseEntity.ok(sessions);
    }
```

---

### 6.3. Commit ENDPOINT 6

```bash
git add src/main/java/org/fyp/tmssep490be/services/ResourceService.java \
        src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java

git commit -m "feat(resource): add GET /resources/{id}/sessions endpoint"
```

---

## ✅ CHECKLIST HOÀN THÀNH

- [ ] **ENDPOINT 0**: Base Setup (DTOs, Repositories)
- [ ] **ENDPOINT 1**: GET /resources, GET /resources/{id}
- [ ] **ENDPOINT 2**: POST /resources
- [ ] **ENDPOINT 3**: PUT /resources/{id}
- [ ] **ENDPOINT 4**: DELETE /resources/{id}
- [ ] **ENDPOINT 5**: PATCH /resources/{id}/status
- [ ] **ENDPOINT 6**: GET /resources/{id}/sessions

---

## 📊 SO SÁNH VỚI TIMESLOT

| Mục | TimeSlot | Resource |
|-----|----------|----------|
| Entity phức tạp | Đơn giản (5 fields chính) | Phức tạp (20+ fields) |
| Có 2 loại | Không | ROOM vs VIRTUAL |
| Validation đặc biệt | Thời gian, tên | Capacity, URL, Email, Date |
| Code prefix | Không | Branch code prefix |
| Statistics | Sessions count | Sessions + nextSessionInfo |

---

## 🔧 LƯU Ý QUAN TRỌNG

1. **Resource có 2 loại**:
   - `ROOM` (Phòng vật lý): capacity max 40, có equipment
   - `VIRTUAL` (Zoom): capacity max 100, có meeting URL, account info

2. **Code tự động thêm prefix**: 
   - Input: "ROOM-101"
   - Output: "HN-ROOM-101" (với HN là branch code)

3. **Giảm capacity bị block**:
   - Nếu có lớp đang dùng với sĩ số 30
   - Không thể giảm capacity xuống < 30
