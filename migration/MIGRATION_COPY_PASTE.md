# 📋 MIGRATION BY ENDPOINT - TIMESLOT MODULE

> **NGUYÊN TẮC**: 
> - Mỗi endpoint = 1 commit
> - **KHÔNG dùng interface**, viết thẳng vào Service class (giống AuthService.java)

---

# 🚀 ENDPOINT 0: BASE SETUP (BẮT BUỘC TRƯỚC)

## A. REPOSITORIES CẦN TẠO:

### A1. `src/main/java/org/fyp/tmssep490be/repositories/SessionRepository.java`

**📖 Giải thích:**
| Annotation/Code | Ý nghĩa |
|-----------------|---------|
| `@Repository` | Đánh dấu class là Repository (tầng truy cập dữ liệu) |
| `extends JpaRepository<Session, Long>` | Kế thừa các method CRUD sẵn có (findAll, save, delete...) |
| `existsByTimeSlotTemplateId()` | Spring Data tự tạo query từ tên method |
| `@Query(...)` | Viết JPQL query thủ công khi cần logic phức tạp |
| `nativeQuery = true` | Dùng SQL thuần thay vì JPQL |
| `@Param("...")` | Map tham số vào query |

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    // Kiểm tra khung giờ có được dùng trong session không
    boolean existsByTimeSlotTemplateId(Long timeSlotTemplateId);

    // Đếm số lớp đang dùng khung giờ này
    @Query("SELECT COUNT(DISTINCT s.classEntity.id) FROM Session s WHERE s.timeSlotTemplate.id = :timeSlotId AND s.status != 'CANCELLED'")
    Long countDistinctClassesByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    // Đếm tổng số session dùng khung giờ này
    @Query("SELECT COUNT(s) FROM Session s WHERE s.timeSlotTemplate.id = :timeSlotId AND s.status != 'CANCELLED'")
    Long countSessionsByTimeSlotId(@Param("timeSlotId") Long timeSlotId);

    // Đếm session tương lai (kiểm tra trước khi ngưng hoạt động)
    @Query(value = """
        SELECT COUNT(s.id) FROM session s
        JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
        WHERE s.time_slot_template_id = :timeSlotId
        AND s.status IN ('PLANNED', 'ONGOING')
        AND (s.date > :currentDate OR (s.date = :currentDate AND tst.start_time > :currentTime))
        """, nativeQuery = true)
    Long countFutureSessionsByTimeSlotId(
        @Param("timeSlotId") Long timeSlotId,
        @Param("currentDate") LocalDate currentDate,
        @Param("currentTime") LocalTime currentTime);

    // Tìm sessions theo khung giờ
    @Query(value = """
        SELECT s.* FROM session s
        JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
        WHERE tst.id = :timeSlotId AND s.status != 'CANCELLED'
        ORDER BY s.date DESC
        """, nativeQuery = true)
    List<Session> findByTimeSlotTemplateId(@Param("timeSlotId") Long timeSlotId);
}
```

---

### A2. `src/main/java/org/fyp/tmssep490be/repositories/TeacherAvailabilityRepository.java`

**📖 Giải thích:**
| Code | Ý nghĩa |
|------|---------|
| `TeacherAvailability.TeacherAvailabilityId` | Entity có composite key (khóa chính ghép từ nhiều trường) |
| `ta.id.timeSlotTemplateId` | Truy cập field trong embedded ID |

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherAvailabilityRepository extends JpaRepository<TeacherAvailability, TeacherAvailability.TeacherAvailabilityId> {

    // Kiểm tra giáo viên có đăng ký rảnh khung giờ này không
    @Query("SELECT COUNT(ta) > 0 FROM TeacherAvailability ta WHERE ta.id.timeSlotTemplateId = :timeSlotTemplateId")
    boolean existsById_TimeSlotTemplateId(@Param("timeSlotTemplateId") Long timeSlotTemplateId);
}
```

---

## B. EXCEPTIONS VÀ DTOs CẦN TẠO:

### 1. `src/main/java/org/fyp/tmssep490be/exceptions/ResourceNotFoundException.java`

**📖 Giải thích:**
- Custom exception khi không tìm thấy dữ liệu (ví dụ: TimeSlot với ID không tồn tại)
- `extends RuntimeException` = unchecked exception, không cần try-catch

```java
package org.fyp.tmssep490be.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

### 2. `src/main/java/org/fyp/tmssep490be/exceptions/BusinessRuleException.java`

**📖 Giải thích:**
- Exception cho lỗi nghiệp vụ (ví dụ: thời gian kết thúc < thời gian bắt đầu)
- `errorCode` giúp frontend xác định loại lỗi

```java
package org.fyp.tmssep490be.exceptions;

import lombok.Getter;

@Getter
public class BusinessRuleException extends RuntimeException {
    private final String errorCode;

    public BusinessRuleException(String message) {
        super(message);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }
    public BusinessRuleException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BUSINESS_RULE_VIOLATION";
    }
}
```

---

### 3. `src/main/java/org/fyp/tmssep490be/dtos/common/ResponseObject.java`

**📖 Giải thích:**
| Code | Ý nghĩa |
|------|---------|
| `@Data` | Tự tạo getter/setter/toString/equals/hashCode |
| `@Builder` | Cho phép dùng pattern builder: `ResponseObject.builder().success(true).build()` |
| `<T>` | Generic type - data có thể là bất kỳ kiểu nào |
| `static <T>` | Factory method tĩnh để tạo response |

```java
package org.fyp.tmssep490be.dtos.common;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResponseObject<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ResponseObject<T> success(String message, T data) {
        return ResponseObject.<T>builder().success(true).message(message).data(data).build();
    }
    public static <T> ResponseObject<T> success(T data) {
        return success("Operation successful", data);
    }
    public static <T> ResponseObject<T> error(String message) {
        return ResponseObject.<T>builder().success(false).message(message).build();
    }
}
```

---

### 4. `src/main/java/org/fyp/tmssep490be/dtos/resource/TimeSlotRequestDTO.java`

**📖 Giải thích:**
- DTO nhận dữ liệu từ client khi tạo/sửa TimeSlot
- Dùng `String` cho thời gian để dễ parse từ JSON

```java
package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlotRequestDTO {
    private Long branchId;      // ID chi nhánh
    private String name;        // Tên khung giờ (VD: "Ca sáng 1")
    private String startTime;   // Giờ bắt đầu (VD: "08:00")
    private String endTime;     // Giờ kết thúc (VD: "09:30")
}
```

### 5. `src/main/java/org/fyp/tmssep490be/dtos/resource/TimeSlotResponseDTO.java`

**📖 Giải thích:**
- DTO trả về cho client với đầy đủ thông tin
- Bao gồm thống kê (số lớp, số session...) để hiển thị trên UI

```java
package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlotResponseDTO {
    private Long id;
    private Long branchId;
    private String branchName;
    private String name;
    private String startTime;
    private String endTime;
    private String createdAt;
    private String updatedAt;
    private String status;                    // ACTIVE hoặc INACTIVE
    private Long activeClassesCount;          // Số lớp đang dùng khung giờ
    private Long totalSessionsCount;          // Tổng số buổi học
    private Boolean hasAnySessions;           // Có buổi học nào không
    private Boolean hasFutureSessions;        // Có buổi học tương lai không
    private Boolean hasTeacherAvailability;   // GV có đăng ký rảnh không
}
```

---

### 6. `src/main/java/org/fyp/tmssep490be/dtos/resource/TimeSlotTemplateDTO.java`

**📖 Giải thích:**
- DTO đơn giản cho dropdown/select box
- `displayName` = "08:00 - 09:30" để hiển thị đẹp

```java
package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TimeSlotTemplateDTO {
    private Long id;
    private String name;
    private String startTime;
    private String endTime;
    private String displayName;  // VD: "08:00 - 09:30"
}
```

---

### 7. `src/main/java/org/fyp/tmssep490be/dtos/resource/SessionInfoDTO.java`

**📖 Giải thích:**
- DTO hiển thị thông tin session khi xem chi tiết khung giờ

```java
package org.fyp.tmssep490be.dtos.resource;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SessionInfoDTO {
    private Long id;
    private Long classId;
    private String classCode;   // VD: "ENG101-01"
    private String className;   // VD: "Tiếng Anh cơ bản"
    private String date;        // Ngày học
    private String startTime;
    private String endTime;
    private String status;      // PLANNED, ONGOING, COMPLETED...
    private String type;        // CLASS, MAKEUP, EXAM...
}
```

## ✅ Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/repositories/SessionRepository.java
git add src/main/java/org/fyp/tmssep490be/repositories/TeacherAvailabilityRepository.java
git add src/main/java/org/fyp/tmssep490be/exceptions/
git add src/main/java/org/fyp/tmssep490be/dtos/common/
git add src/main/java/org/fyp/tmssep490be/dtos/resource/
git commit -m "feat(base): add repositories, exceptions and DTOs for TimeSlot module"
```

---

# 🚀 ENDPOINT 1: GET /time-slots (Lấy danh sách)

## 1.1 Repository: `src/main/java/org/fyp/tmssep490be/repositories/TimeSlotTemplateRepository.java`

**📖 Giải thích:**
- Repository cho entity TimeSlotTemplate
- `findByBranchIdOrderByStartTimeAsc` = tìm theo branchId, sắp xếp theo startTime tăng dần

```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {

    @Query("SELECT tst FROM TimeSlotTemplate tst WHERE tst.branch.id = :branchId ORDER BY tst.startTime ASC")
    List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(@Param("branchId") Long branchId);
}
```

---

## 1.2 Service: `src/main/java/org/fyp/tmssep490be/services/TimeSlotTemplateService.java`

> **LƯU Ý**: KHÔNG có interface, viết thẳng class giống AuthService

**📖 Giải thích Annotations:**
| Annotation | Ý nghĩa |
|------------|---------|
| `@Service` | Đánh dấu class là Service (tầng business logic) |
| `@RequiredArgsConstructor` | Lombok tự tạo constructor cho các field `final` |
| `@Slf4j` | Tạo biến `log` để ghi log |
| `@Transactional` | Tự động commit/rollback transaction |
| `@Transactional(readOnly = true)` | Tối ưu cho query chỉ đọc |

```java
package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotTemplateService {

    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Transactional(readOnly = true)
    public List<TimeSlotResponseDTO> getAllTimeSlots(Long branchId, String search, Long userId, boolean isCenterHead, boolean isTeacher) {
        log.info("Getting all time slots - branchId: {}, search: {}, userId: {}", branchId, search, userId);

        List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository.findAll();

        if (isCenterHead) {
            Long userBranchId = getBranchIdForUser(userId);
            if (userBranchId != null) {
                branchId = userBranchId;
            }
        }

        if (isTeacher) {
            List<Long> userBranchIds = getBranchIdsForUser(userId);
            if (!userBranchIds.isEmpty()) {
                timeSlots = timeSlots.stream()
                        .filter(ts -> userBranchIds.contains(ts.getBranch().getId()))
                        .collect(Collectors.toList());
            }
        }

        if (branchId != null) {
            Long finalBranchId = branchId;
            timeSlots = timeSlots.stream()
                    .filter(ts -> ts.getBranch().getId().equals(finalBranchId))
                    .collect(Collectors.toList());
        }
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            timeSlots = timeSlots.stream()
                    .filter(ts -> ts.getName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        return timeSlots.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ==================== PHƯƠNG THỨC HỖ TRỢ ====================

    private Long getBranchIdForUser(Long userId) {
        if (userId == null) return null;
        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null && !user.getUserBranches().isEmpty()) {
            return user.getUserBranches().iterator().next().getBranch().getId();
        }
        return null;
    }

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

    private TimeSlotResponseDTO convertToDTO(TimeSlotTemplate ts) {
        TimeSlotResponseDTO.TimeSlotResponseDTOBuilder builder = TimeSlotResponseDTO.builder()
                .id(ts.getId())
                .branchId(ts.getBranch().getId())
                .branchName(ts.getBranch().getName())
                .name(ts.getName())
                .startTime(ts.getStartTime().toString())
                .endTime(ts.getEndTime().toString())
                .createdAt(ts.getCreatedAt() != null ? ts.getCreatedAt().toString() : null)
                .updatedAt(ts.getUpdatedAt() != null ? ts.getUpdatedAt().toString() : null)
                .status(ts.getStatus().name());

        try {
            Long activeClasses = sessionRepository.countDistinctClassesByTimeSlotId(ts.getId());
            Long totalSessions = sessionRepository.countSessionsByTimeSlotId(ts.getId());
            Long futureSessions = sessionRepository.countFutureSessionsByTimeSlotId(ts.getId(), LocalDate.now(), LocalTime.now());
            boolean hasTeacherAvailability = teacherAvailabilityRepository.existsById_TimeSlotTemplateId(ts.getId());

            builder.activeClassesCount(activeClasses)
                    .totalSessionsCount(totalSessions)
                    .hasAnySessions(totalSessions > 0)
                    .hasFutureSessions(futureSessions > 0)
                    .hasTeacherAvailability(hasTeacherAvailability);
        } catch (Exception e) {
            log.error("Error calculating statistics: {}", e.getMessage());
            builder.activeClassesCount(0L).totalSessionsCount(0L)
                    .hasAnySessions(false).hasFutureSessions(false).hasTeacherAvailability(false);
        }
        return builder.build();
    }
}
```

---

## 1.3 Controller: `src/main/java/org/fyp/tmssep490be/controllers/TimeSlotController.java`

**📖 Giải thích Annotations:**
| Annotation | Ý nghĩa |
|------------|---------|
| `@RestController` | Controller trả về JSON (không render view) |
| `@RequestMapping("/api/v1")` | Base URL cho tất cả endpoints |
| `@Tag(name = "...")` | Nhóm endpoints trong Swagger UI |
| `@SecurityRequirement` | Yêu cầu JWT token |
| `@PreAuthorize` | Kiểm tra role trước khi vào method |
| `@AuthenticationPrincipal` | Inject user đang đăng nhập |

```java
package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.*;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TimeSlotTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quản lý Khung giờ")
@SecurityRequirement(name = "bearerAuth")
public class TimeSlotController {

    private final TimeSlotTemplateService timeSlotTemplateService;

    // Lấy danh sách khung giờ
    @GetMapping("/time-slots")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'TEACHER', 'MANAGER')")
    @Operation(summary = "Get all time slots")
    public ResponseEntity<List<TimeSlotResponseDTO>> getAllTimeSlots(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        boolean isCenterHead = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CENTER_HEAD"));
        boolean isTeacher = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<TimeSlotResponseDTO> timeSlots = timeSlotTemplateService.getAllTimeSlots(
                branchId, search, currentUser.getId(), isCenterHead, isTeacher);
        return ResponseEntity.ok(timeSlots);
    }
}
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): GET /time-slots - list all time slots with filters"
```

---

# 🚀 ENDPOINT 2: GET /time-slots/{id} (Lấy chi tiết)

## 2.1 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích logic:**
- `findById(id)` trả về `Optional<TimeSlotTemplate>`
- `.orElseThrow()` = nếu không tìm thấy → throw exception
- `convertToDTO()` = chuyển Entity → DTO (đã viết ở ENDPOINT 1)

```java
    @Transactional(readOnly = true)
    public TimeSlotResponseDTO getTimeSlotById(Long id) {
        log.info("Getting time slot by id: {}", id);
        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));
        return convertToDTO(timeSlot);
    }
```

---

## 2.2 Thêm vào Controller:

**📖 Giải thích:**
- `@PathVariable Long id` = lấy `{id}` từ URL path

```java
    // Lấy chi tiết 1 khung giờ
    @GetMapping("/time-slots/{id}")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get time slot by ID")
    public ResponseEntity<TimeSlotResponseDTO> getTimeSlotById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TimeSlotResponseDTO timeSlot = timeSlotTemplateService.getTimeSlotById(id);
        return ResponseEntity.ok(timeSlot);
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): GET /time-slots/{id} - get time slot by ID"
```

---

# 🚀 ENDPOINT 3: POST /time-slots (Tạo mới)

## 3.1 Thêm vào Repository `TimeSlotTemplateRepository.java`:

**📖 Giải thích:**
- `existsByBranchIdAndNameIgnoreCase` = kiểm tra tên đã tồn tại (không phân biệt hoa thường)
- `excludeId` = bỏ qua ID này khi check (dùng cho UPDATE)
- `LOWER(tst.name) = LOWER(:name)` = so sánh không phân biệt hoa/thường

```java
    // Kiểm tra tên khung giờ đã tồn tại trong branch chưa
    @Query("SELECT COUNT(tst) > 0 FROM TimeSlotTemplate tst " +
           "WHERE tst.branch.id = :branchId " +
           "AND LOWER(tst.name) = LOWER(:name) " +
           "AND (:excludeId IS NULL OR tst.id != :excludeId)")
    boolean existsByBranchIdAndNameIgnoreCase(
            @Param("branchId") Long branchId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    // Kiểm tra khung giờ (startTime-endTime) đã tồn tại chưa
    @Query("SELECT COUNT(tst) > 0 FROM TimeSlotTemplate tst " +
           "WHERE tst.branch.id = :branchId " +
           "AND tst.startTime = :startTime " +
           "AND tst.endTime = :endTime " +
           "AND (:excludeId IS NULL OR tst.id != :excludeId)")
    boolean existsByBranchIdAndStartTimeAndEndTime(
            @Param("branchId") Long branchId,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
```

---

## 3.2 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích logic tạo mới:**
1. Validate branchId
2. Validate tên không rỗng
3. Validate startTime/endTime
4. Check endTime > startTime
5. Check tên không trùng trong branch
6. Check thời gian không trùng trong branch
7. Tạo entity và lưu

```java
    // Tạo khung giờ mới
    @Transactional
    public TimeSlotResponseDTO createTimeSlot(TimeSlotRequestDTO request, Long userId, Long forcedBranchId) {
        log.info("Creating time slot: {}", request);

        // 1. Xác định branchId
        Long branchId = forcedBranchId != null ? forcedBranchId : request.getBranchId();
        if (branchId == null) {
            throw new BusinessRuleException("Vui lòng chọn chi nhánh");
        }

        // 2. Lấy branch entity
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // 3. Validate tên
        String name = request.getName() != null ? request.getName().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new BusinessRuleException("Vui lòng nhập tên khung giờ");
        }

        // 4. Validate thời gian
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessRuleException("Vui lòng nhập giờ bắt đầu và giờ kết thúc");
        }
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        // 5. Check endTime > startTime
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("Giờ kết thúc phải lớn hơn giờ bắt đầu");
        }

        // 6. Check tên không trùng
        if (timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(branchId, name, null)) {
            throw new BusinessRuleException("Tên khung giờ đã tồn tại trong chi nhánh này");
        }
        
        // 7. Check thời gian không trùng
        if (timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(branchId, startTime, endTime, null)) {
            throw new BusinessRuleException("Khung giờ này đã tồn tại trong chi nhánh");
        }

        // 8. Tạo entity và lưu
        TimeSlotTemplate timeSlot = new TimeSlotTemplate();
        timeSlot.setBranch(branch);
        timeSlot.setName(name);
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        timeSlot.setStatus(ResourceStatus.ACTIVE);
        timeSlot.setCreatedAt(OffsetDateTime.now());
        timeSlot.setUpdatedAt(OffsetDateTime.now());

        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        log.info("Created time slot with ID: {}", saved.getId());
        return convertToDTO(saved);
    }
```

---

## 3.3 Thêm vào Controller:

**📖 Giải thích:**
- `@PostMapping` = HTTP POST method
- `@RequestBody` = parse JSON body thành DTO
- Chỉ CENTER_HEAD mới được tạo

```java
    // Tạo khung giờ mới
    @PostMapping("/time-slots")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Create new time slot")
    public ResponseEntity<TimeSlotResponseDTO> createTimeSlot(
            @RequestBody TimeSlotRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long forcedBranchId = null; // TODO: lấy từ branch của user
        TimeSlotResponseDTO saved = timeSlotTemplateService.createTimeSlot(request, currentUser.getId(), forcedBranchId);
        return ResponseEntity.ok(saved);
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): POST /time-slots - create new time slot"
```

---

# 🚀 ENDPOINT 4: PUT /time-slots/{id} (Cập nhật)

## 4.1 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích logic cập nhật:**
1. Tìm entity theo ID
2. Nếu đổi tên → check tên mới không trùng
3. Nếu đổi thời gian → check không có session đang dùng + thời gian mới không trùng
4. Lưu thay đổi

```java
    // Cập nhật khung giờ
    @Transactional
    public TimeSlotResponseDTO updateTimeSlot(Long id, TimeSlotRequestDTO request, Long userId) {
        log.info("Updating time slot {}: {}", id, request);

        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        Long branchId = timeSlot.getBranch().getId();

        if (request.getName() != null) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(timeSlot.getName())) {
                if (timeSlotTemplateRepository.existsByBranchIdAndNameIgnoreCase(branchId, newName, id)) {
                    throw new BusinessRuleException("Tên khung giờ đã tồn tại");
                }
            }
            timeSlot.setName(newName);
        }

        if (request.getStartTime() != null || request.getEndTime() != null) {
            LocalTime newStartTime = request.getStartTime() != null 
                    ? LocalTime.parse(request.getStartTime()) : timeSlot.getStartTime();
            LocalTime newEndTime = request.getEndTime() != null 
                    ? LocalTime.parse(request.getEndTime()) : timeSlot.getEndTime();

            if (!newEndTime.isAfter(newStartTime)) {
                throw new BusinessRuleException("Giờ kết thúc phải lớn hơn giờ bắt đầu");
            }

            boolean isTimeChanged = !newStartTime.equals(timeSlot.getStartTime()) || !newEndTime.equals(timeSlot.getEndTime());
            if (isTimeChanged) {
                if (sessionRepository.existsByTimeSlotTemplateId(id)) {
                    throw new BusinessRuleException("Không thể thay đổi thời gian vì đang được sử dụng");
                }
                if (timeSlotTemplateRepository.existsByBranchIdAndStartTimeAndEndTime(branchId, newStartTime, newEndTime, id)) {
                    throw new BusinessRuleException("Khung giờ này đã tồn tại");
                }
                timeSlot.setStartTime(newStartTime);
                timeSlot.setEndTime(newEndTime);
            }
        }

        timeSlot.setUpdatedAt(OffsetDateTime.now());
        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        return convertToDTO(saved);
    }
```

---

## 4.2 Thêm vào Controller:

**📖 Giải thích:**
- `@PutMapping` = HTTP PUT method (cập nhật toàn bộ)
- Kết hợp `@PathVariable` (ID từ URL) và `@RequestBody` (data từ body)

```java
    // Cập nhật khung giờ
    @PutMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update time slot")
    public ResponseEntity<TimeSlotResponseDTO> updateTimeSlot(
            @PathVariable Long id,
            @RequestBody TimeSlotRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        TimeSlotResponseDTO saved = timeSlotTemplateService.updateTimeSlot(id, request, currentUser.getId());
        return ResponseEntity.ok(saved);
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): PUT /time-slots/{id} - update time slot"
```

---

# 🚀 ENDPOINT 5: DELETE /time-slots/{id} (Xóa)

## 5.1 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích logic xóa:**
1. Tìm entity theo ID
2. Check status = INACTIVE (phải ngưng hoạt động trước khi xóa)
3. Check không có session nào đang dùng
4. Check không có giáo viên nào đăng ký rảnh khung giờ này
5. Xóa

```java
    // Xóa khung giờ
    @Transactional
    public void deleteTimeSlot(Long id) {
        log.info("Deleting time slot {}", id);

        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        // Phải ngưng hoạt động trước khi xóa
        if (timeSlot.getStatus() != ResourceStatus.INACTIVE) {
            throw new BusinessRuleException("Vui lòng ngưng hoạt động trước khi xóa");
        }

        // Không thể xóa nếu có session đang dùng
        if (sessionRepository.existsByTimeSlotTemplateId(id)) {
            throw new BusinessRuleException("Không thể xóa vì đang được sử dụng");
        }

        // Không thể xóa nếu giáo viên đăng ký rảnh
        if (teacherAvailabilityRepository.existsById_TimeSlotTemplateId(id)) {
            throw new BusinessRuleException("Không thể xóa vì đang trong lịch rảnh giáo viên");
        }

        timeSlotTemplateRepository.deleteById(id);
    }
```

---

## 5.2 Thêm vào Controller:

**📖 Giải thích:**
- `@DeleteMapping` = HTTP DELETE method
- `ResponseEntity.noContent()` = trả về HTTP 204 (thành công, không có body)

```java
    // Xóa khung giờ
    @DeleteMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Delete time slot")
    public ResponseEntity<Void> deleteTimeSlot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        timeSlotTemplateService.deleteTimeSlot(id);
        return ResponseEntity.noContent().build();
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): DELETE /time-slots/{id} - delete time slot"
```

---

# 🚀 ENDPOINT 6: PATCH /time-slots/{id}/status (Đổi trạng thái)

## 6.1 Thêm import vào Controller:
```java
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import java.util.Map;
```

---

## 6.2 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích logic đổi trạng thái:**
- Nếu chuyển sang INACTIVE → check không có session tương lai
- Có thể chuyển ACTIVE ↔ INACTIVE

```java
    // Đổi trạng thái hoạt động/ngưng hoạt động
    @Transactional
    public TimeSlotResponseDTO updateTimeSlotStatus(Long id, ResourceStatus status) {
        log.info("Updating status for time slot {}: {}", id, status);

        TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time slot not found with id: " + id));

        // Nếu ngưng hoạt động → check không có session tương lai
        if (status == ResourceStatus.INACTIVE) {
            Long futureSessions = sessionRepository.countFutureSessionsByTimeSlotId(id, LocalDate.now(), LocalTime.now());
            if (futureSessions > 0) {
                throw new BusinessRuleException("Không thể ngưng hoạt động vì có " + futureSessions + " lớp học sắp diễn ra");
            }
        }

        timeSlot.setStatus(status);
        timeSlot.setUpdatedAt(OffsetDateTime.now());
        TimeSlotTemplate saved = timeSlotTemplateRepository.save(timeSlot);
        return convertToDTO(saved);
    }
```

---

## 6.3 Thêm vào Controller:

**📖 Giải thích:**
- `@PatchMapping` = HTTP PATCH (cập nhật một phần)
- `Map<String, String>` = nhận JSON object đơn giản `{"status": "INACTIVE"}`
- `ResourceStatus.valueOf()` = chuyển String → Enum

```java
    // Đổi trạng thái
    @PatchMapping("/time-slots/{id}/status")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update time slot status")
    public ResponseEntity<TimeSlotResponseDTO> updateTimeSlotStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        if (!request.containsKey("status")) {
            throw new RuntimeException("Field 'status' is required");
        }
        ResourceStatus status = ResourceStatus.valueOf(request.get("status"));
        TimeSlotResponseDTO saved = timeSlotTemplateService.updateTimeSlotStatus(id, status);
        return ResponseEntity.ok(saved);
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): PATCH /time-slots/{id}/status - toggle active/inactive"
```

---

# 🚀 ENDPOINT 7: GET /time-slots/{id}/sessions (Lấy sessions)

## 7.1 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích:**
- Lấy danh sách các buổi học đang dùng khung giờ này
- `convertSessionToDTO` = helper method chuyển Session Entity → DTO

```java
    // Lấy danh sách sessions đang dùng khung giờ
    @Transactional(readOnly = true)
    public List<SessionInfoDTO> getSessionsByTimeSlotId(Long id) {
        if (!timeSlotTemplateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Time slot not found with id: " + id);
        }
        List<Session> sessions = sessionRepository.findByTimeSlotTemplateId(id);
        return sessions.stream().map(this::convertSessionToDTO).collect(Collectors.toList());
    }

    // Helper: chuyển Session → DTO
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
```

---

## 7.2 Thêm vào Controller:

```java
    // Lấy sessions đang dùng khung giờ
    @GetMapping("/time-slots/{id}/sessions")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get sessions using a time slot")
    public ResponseEntity<List<SessionInfoDTO>> getSessionsByTimeSlotId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<SessionInfoDTO> sessions = timeSlotTemplateService.getSessionsByTimeSlotId(id);
        return ResponseEntity.ok(sessions);
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): GET /time-slots/{id}/sessions - get sessions using slot"
```

---

# 🚀 ENDPOINT 8: GET /branches/{branchId}/time-slot-templates (Dropdown)

## 8.1 Thêm import vào Controller:

```java
import org.fyp.tmssep490be.dtos.common.ResponseObject;

```

---

## 8.2 Thêm vào Service `TimeSlotTemplateService.java`:

**📖 Giải thích:**
- Endpoint cho dropdown/select box khi tạo lớp
- `displayName` = "08:00 - 09:30" để hiển thị đẹp trên UI

```java
    // Lấy danh sách khung giờ cho dropdown
    @Transactional(readOnly = true)
    public List<TimeSlotTemplateDTO> getBranchTimeSlotTemplates(Long branchId) {
        List<TimeSlotTemplate> timeSlots = timeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc(branchId);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return timeSlots.stream()
                .map(ts -> TimeSlotTemplateDTO.builder()
                        .id(ts.getId())
                        .name(ts.getName())
                        .startTime(ts.getStartTime().toString())
                        .endTime(ts.getEndTime().toString())
                        .displayName(ts.getStartTime().format(formatter) + " - " + ts.getEndTime().format(formatter))
                        .build())
                .collect(Collectors.toList());
    }
```

---

## 8.3 Thêm vào Controller:

**📖 Giải thích:**
- `@Parameter(description = "...")` = mô tả param trong Swagger UI
- Trả về `ResponseObject<List<...>>` = format response chuẩn với success/message/data

```java
    // Lấy khung giờ cho dropdown
    @GetMapping("/branches/{branchId}/time-slot-templates")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get branch time slot templates for dropdown")
    public ResponseEntity<ResponseObject<List<TimeSlotTemplateDTO>>> getBranchTimeSlotTemplates(
            @Parameter(description = "Branch ID") @PathVariable Long branchId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<TimeSlotTemplateDTO> timeSlotDTOs = timeSlotTemplateService.getBranchTimeSlotTemplates(branchId);
        return ResponseEntity.ok(ResponseObject.<List<TimeSlotTemplateDTO>>builder()
                .success(true)
                .message("Time slot templates retrieved successfully")
                .data(timeSlotDTOs)
                .build());
    }
```

## ✅ Commit:
```bash
git add .
git commit -m "feat(timeslot): GET /branches/{id}/time-slot-templates - dropdown data"
```

---

# ✅ TỔNG KẾT - 9 COMMITS

| # | Commit Message |
|---|----------------|
| 0 | `feat(base): add exceptions and DTOs for TimeSlot module` |
| 1 | `feat(timeslot): GET /time-slots - list all time slots with filters` |
| 2 | `feat(timeslot): GET /time-slots/{id} - get time slot by ID` |
| 3 | `feat(timeslot): POST /time-slots - create new time slot` |
| 4 | `feat(timeslot): PUT /time-slots/{id} - update time slot` |
| 5 | `feat(timeslot): DELETE /time-slots/{id} - delete time slot` |
| 6 | `feat(timeslot): PATCH /time-slots/{id}/status - toggle active/inactive` |
| 7 | `feat(timeslot): GET /time-slots/{id}/sessions - get sessions using slot` |
| 8 | `feat(timeslot): GET /branches/{id}/time-slot-templates - dropdown data` |

---

# 🔧 SAU KHI HOÀN THÀNH

```bash
# Build để check lỗi
./mvnw compile

# Chạy ứng dụng
./mvnw spring-boot:run

# Test với Swagger: http://localhost:8080/swagger-ui.html
```
