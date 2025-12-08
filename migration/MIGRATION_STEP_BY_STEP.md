# 📋 HƯỚNG DẪN MIGRATION STEP-BY-STEP

> **LƯU Ý**: Với mỗi endpoint hoàn thành → commit 1 lần.
> 
> ✅ = Hoàn thành | ⏳ = Đang làm | ⬜ = Chưa làm

---

## 🎯 NGUYÊN TẮC LÀM VIỆC

### Trước khi copy bất kỳ code nào, bạn PHẢI:
1. **Đọc hiểu** - Hiểu code làm gì
2. **Giải thích được** - Nói lại bằng lời của mình
3. **Tự gõ lại** - Không copy-paste mù quáng

### Commit Message Format:
```
feat(module): [endpoint] - mô tả ngắn

Ví dụ:
feat(timeslot): GET /time-slots - list all time slots with filters
feat(timeslot): POST /time-slots - create new time slot
```

---

# 📦 MODULE 0: BASE SETUP (BẮT BUỘC TRƯỚC)

## 0.1 Common DTO: ResponseObject.java

> **Mục đích**: DTO chung để wrap tất cả response trả về client

### Files cần tạo:
```
src/main/java/org/fyp/tmssep490be/dtos/common/ResponseObject.java
```

### Code:
```java
package org.fyp.tmssep490be.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper chung cho tất cả API.
 * Generic type T là kiểu dữ liệu của data trả về.
 */
@Data                   // Lombok: tự tạo getter, setter, toString, equals, hashCode
@Builder                // Lombok: cho phép dùng Builder pattern để tạo object
@NoArgsConstructor      // Lombok: tạo constructor không tham số (bắt buộc cho Jackson)
@AllArgsConstructor     // Lombok: tạo constructor đầy đủ tham số
public class ResponseObject<T> {
    
    private boolean success;    // true = thành công, false = lỗi
    private String message;     // Message mô tả kết quả
    private T data;             // Dữ liệu trả về (có thể null nếu lỗi)

    /**
     * Factory method tạo response thành công với message custom
     */
    public static <T> ResponseObject<T> success(String message, T data) {
        return ResponseObject.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Factory method tạo response thành công với message mặc định
     */
    public static <T> ResponseObject<T> success(T data) {
        return success("Operation successful", data);
    }

    /**
     * Factory method tạo response lỗi
     */
    public static <T> ResponseObject<T> error(String message) {
        return ResponseObject.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
```

### 📚 Giải thích code:

| Annotation/Keyword | Giải thích |
|-------------------|------------|
| `@Data` | Lombok annotation, tự động tạo: getter, setter, `toString()`, `equals()`, `hashCode()` |
| `@Builder` | Cho phép tạo object bằng Builder pattern: `ResponseObject.builder().success(true).build()` |
| `@NoArgsConstructor` | Tạo constructor không tham số, **bắt buộc** để Jackson deserialize JSON |
| `@AllArgsConstructor` | Tạo constructor với tất cả fields làm tham số |
| `<T>` | Generic type - cho phép ResponseObject chứa bất kỳ kiểu data nào |
| `ResponseObject.<T>builder()` | Gọi builder với generic type, tránh warning "unchecked" |

### ✅ Checklist hiểu:
- [ ] Hiểu tại sao dùng Generic `<T>`?
- [ ] Hiểu tại sao cần `@NoArgsConstructor`?
- [ ] Hiểu Builder pattern hoạt động thế nào?

### 🔧 Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/dtos/common/ResponseObject.java
git commit -m "feat(base): add ResponseObject common DTO"
```

---

# 📦 MODULE 1: TIMESLOT MANAGEMENT (8 Endpoints)

> **Tại sao bắt đầu từ TimeSlot?** 
> - CRUD đơn giản nhất, không phụ thuộc nhiều entity khác
> - Học xong module này → nắm được pattern chuẩn

---

## 1.1 Entity: TimeSlotTemplate.java ✅ (ĐÃ CÓ SẴN)

> **Vị trí**: `src/main/java/org/fyp/tmssep490be/entities/TimeSlotTemplate.java`

### 📚 Giải thích từng dòng:

```java
@Entity                                    // [1] Đánh dấu class này map với table trong DB
@Table(name = "time_slot_template")        // [2] Tên table trong PostgreSQL
@Getter @Setter                            // [3] Lombok: tự tạo getter/setter
@NoArgsConstructor @AllArgsConstructor     // [4] Lombok: tạo constructors
@Builder                                   // [5] Lombok: Builder pattern
public class TimeSlotTemplate {

    @Id                                    // [6] Đánh dấu primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // [7] Auto-increment
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)     // [8] Quan hệ N-1: Nhiều TimeSlot thuộc 1 Branch
    @JoinColumn(name = "branch_id", nullable = false)  // [9] FK column trong DB
    private Branch branch;

    @Column(nullable = false)              // [10] Column không được NULL
    private String name;

    @Column(name = "start_time", nullable = false)  // [11] Column name khác field name
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)           // [12] Lưu enum dạng String ("ACTIVE")
    @Builder.Default                       // [13] Giá trị mặc định khi dùng Builder
    private ResourceStatus status = ResourceStatus.ACTIVE;

    @OneToMany(mappedBy = "timeSlotTemplate", ...)  // [14] Quan hệ 1-N
    private Set<Session> sessions = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

### 🔍 Chi tiết annotations:

| # | Annotation | Giải thích chi tiết |
|---|------------|---------------------|
| 1 | `@Entity` | Đánh dấu class là JPA Entity, sẽ được map với 1 table trong DB |
| 2 | `@Table(name="...")` | Chỉ định tên table. Nếu không có, mặc định = tên class |
| 3 | `@Getter @Setter` | Lombok tự tạo getter/setter cho TẤT CẢ fields |
| 6 | `@Id` | Đánh dấu field này là Primary Key |
| 7 | `GenerationType.IDENTITY` | Auto-increment do DATABASE quản lý (PostgreSQL SERIAL) |
| 8 | `FetchType.LAZY` | Chỉ load Branch khi thực sự cần (truy cập `timeSlot.getBranch()`) |
| 9 | `@JoinColumn` | Chỉ định tên cột FK trong bảng `time_slot_template` |
| 12 | `EnumType.STRING` | Lưu enum dưới dạng text ("ACTIVE"), không phải số (0, 1) |
| 13 | `@Builder.Default` | Khi dùng `TimeSlotTemplate.builder().build()`, status = ACTIVE mặc định |
| 14 | `mappedBy` | Chỉ định field bên entity Session tham chiếu ngược lại |

### ✅ Checklist hiểu:
- [ ] `@ManyToOne` vs `@OneToMany` khác nhau thế nào?
- [ ] `FetchType.LAZY` vs `FetchType.EAGER` khác nhau thế nào?
- [ ] Tại sao dùng `EnumType.STRING` thay vì `EnumType.ORDINAL`?
- [ ] `cascade = CascadeType.ALL` nghĩa là gì?

---

## 1.2 Repository: TimeSlotTemplateRepository.java

> **Mục đích**: Query database cho TimeSlotTemplate

### Files cần tạo:
```
src/main/java/org/fyp/tmssep490be/repositories/TimeSlotTemplateRepository.java
```

### Code:
```java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

/**
 * Repository cho TimeSlotTemplate entity.
 * Kế thừa JpaRepository để có sẵn các method CRUD cơ bản.
 */
@Repository  // Đánh dấu class là Repository (tầng truy cập DB)
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {
    // JpaRepository<Entity, PrimaryKeyType> cung cấp sẵn:
    // - findById(id): Optional<TimeSlotTemplate>
    // - findAll(): List<TimeSlotTemplate>
    // - save(entity): TimeSlotTemplate
    // - deleteById(id): void
    // - existsById(id): boolean
    // - count(): long

    // ==================== CUSTOM QUERY METHODS ====================
    
    /**
     * Tìm TimeSlot theo branchId, sắp xếp theo giờ bắt đầu.
     * 
     * JPA tự động tạo query từ tên method:
     * findBy + BranchId + OrderBy + StartTime + Asc
     * → SELECT * FROM time_slot_template WHERE branch_id = ? ORDER BY start_time ASC
     */
    List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(Long branchId);

    /**
     * Tìm TimeSlot ACTIVE của branch.
     * findBy + BranchId + And + Status + OrderBy...
     */
    List<TimeSlotTemplate> findByBranchIdAndStatusOrderByStartTimeAsc(Long branchId, ResourceStatus status);

    /**
     * Kiểm tra trùng tên trong cùng branch (case-insensitive).
     * Dùng khi CREATE để validate.
     * 
     * existsBy = trả về boolean
     * IgnoreCase = không phân biệt hoa/thường
     */
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    /**
     * Kiểm tra trùng khung giờ trong cùng branch.
     * Dùng để validate: không cho tạo slot 08:00-10:00 nếu đã có.
     */
    boolean existsByBranchIdAndStartTimeAndEndTime(Long branchId, LocalTime startTime, LocalTime endTime);

    /**
     * Kiểm tra trùng tên nhưng LOẠI TRỪ chính nó.
     * Dùng khi UPDATE - cho phép giữ nguyên tên cũ.
     * 
     * IdNot = id != excludeId
     */
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long excludeId);

    /**
     * Kiểm tra trùng khung giờ nhưng LOẠI TRỪ chính nó.
     * Dùng khi UPDATE.
     */
    boolean existsByBranchIdAndStartTimeAndEndTimeAndIdNot(
        Long branchId, LocalTime startTime, LocalTime endTime, Long excludeId);
}
```

### 📚 JPA Query Method Naming Convention:

```
findBy + Field + Condition + OrderBy + Field + Direction

Ví dụ: findByBranchIdAndStatusOrderByStartTimeAsc
├── findBy          → SELECT ... WHERE
├── BranchId        → branch_id = ?
├── And             → AND
├── Status          → status = ?
├── OrderBy         → ORDER BY
├── StartTime       → start_time
└── Asc             → ASC
```

| Keyword | SQL tương ứng | Ví dụ |
|---------|---------------|-------|
| `findBy` | `SELECT * FROM ... WHERE` | `findById(1L)` |
| `existsBy` | `SELECT EXISTS(...)` trả về boolean | `existsByName("x")` |
| `countBy` | `SELECT COUNT(*)` trả về Long | `countByStatus(ACTIVE)` |
| `deleteBy` | `DELETE FROM ... WHERE` | `deleteByBranchId(1L)` |
| `And` | `AND` | `findByBranchIdAndStatus(...)` |
| `Or` | `OR` | `findByStatusOrBranchId(...)` |
| `IgnoreCase` | `LOWER(field) = LOWER(?)` | `findByNameIgnoreCase("X")` |
| `Not` | `!= ?` | `findByIdNot(5L)` |
| `OrderBy...Asc` | `ORDER BY ... ASC` | `findAllOrderByNameAsc()` |
| `OrderBy...Desc` | `ORDER BY ... DESC` | `findAllOrderByCreatedAtDesc()` |
| `Between` | `BETWEEN ? AND ?` | `findByDateBetween(d1, d2)` |
| `LessThan` | `< ?` | `findByAgeLessThan(18)` |
| `GreaterThan` | `> ?` | `findByPriceGreaterThan(100)` |
| `IsNull` | `IS NULL` | `findByDeletedAtIsNull()` |
| `IsNotNull` | `IS NOT NULL` | `findByEmailIsNotNull()` |
| `In` | `IN (?, ?, ?)` | `findByStatusIn(list)` |
| `Like` | `LIKE ?` | `findByNameLike("%abc%")` |

### ✅ Checklist hiểu:
- [ ] JpaRepository cung cấp sẵn những method nào?
- [ ] Cách đặt tên method để JPA tự tạo query?
- [ ] Tại sao cần `IdNot` khi UPDATE?

### 🔧 Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/repositories/TimeSlotTemplateRepository.java
git commit -m "feat(timeslot): add TimeSlotTemplateRepository with query methods"
```

---

## 1.3 DTOs: Request và Response

### Files cần tạo:
```
src/main/java/org/fyp/tmssep490be/dtos/timeslot/
├── TimeSlotRequestDTO.java      # Request khi tạo/sửa
├── TimeSlotResponseDTO.java     # Response đầy đủ
├── TimeSlotTemplateDTO.java     # Response rút gọn (dropdown)
└── SessionInfoDTO.java          # Session info (dùng chung)
```

### 1.3.1 TimeSlotRequestDTO.java

```java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho request tạo/sửa TimeSlot.
 * Client gửi JSON → Spring tự động parse thành object này.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotRequestDTO {
    
    private Long branchId;      // Branch ID (optional nếu CENTER_HEAD tự động lấy)
    private String name;        // Tên slot: "Morning 1", "Afternoon"...
    private String startTime;   // Format: "08:00" hoặc "08:00:00"
    private String endTime;     // Format: "10:00" hoặc "10:00:00"
    
    // Ví dụ JSON từ client:
    // {
    //   "branchId": 1,
    //   "name": "Morning 1",
    //   "startTime": "08:00",
    //   "endTime": "10:00"
    // }
}
```

### 1.3.2 TimeSlotResponseDTO.java

```java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO response đầy đủ cho TimeSlot.
 * Trả về client sau khi query/create/update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponseDTO {
    
    private Long id;
    private Long branchId;
    private String branchName;       // Tên branch (join từ Branch entity)
    private String name;
    private String startTime;        // Format: "08:00:00"
    private String endTime;          // Format: "10:00:00"
    private String createdAt;        // Format: ISO 8601
    private String updatedAt;
    private String status;           // "ACTIVE" hoặc "INACTIVE"
    
    // Thống kê (hữu ích cho UI)
    private Long activeClassesCount;      // Số lớp đang dùng slot này
    private Long totalSessionsCount;      // Tổng số buổi học dùng slot này
    private Boolean hasAnySessions;       // Có session nào không?
    private Boolean hasFutureSessions;    // Có session tương lai không?
    private Boolean hasTeacherAvailability;  // Có GV đăng ký rảnh slot này không?
}
```

### 1.3.3 TimeSlotTemplateDTO.java

```java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO rút gọn cho dropdown/select component.
 * Chỉ chứa info cần thiết để hiển thị.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotTemplateDTO {
    
    private Long id;
    private String name;
    private String startTime;    // "08:00:00"
    private String endTime;      // "10:00:00"
    private String displayName;  // "08:00 - 10:00" (cho UI hiển thị)
}
```

### 1.3.4 SessionInfoDTO.java

```java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO thông tin session - dùng để show cảnh báo.
 * Khi xóa TimeSlot, show danh sách sessions đang dùng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoDTO {
    
    private Long id;
    private Long classId;
    private String classCode;    // "IELTS-B1-HN-25-001"
    private String className;
    private String date;         // "2025-01-15"
    private String startTime;
    private String endTime;
    private String status;       // "PLANNED", "COMPLETED", "CANCELLED"
    private String type;         // "REGULAR", "MAKEUP"
}
```

### 📚 Tại sao dùng DTO thay vì trả Entity trực tiếp?

| Lý do | Giải thích |
|-------|------------|
| **Bảo mật** | Không lộ cấu trúc database, không lộ fields nhạy cảm |
| **Kiểm soát data** | Chỉ trả về fields cần thiết, tránh over-fetching |
| **Tránh circular reference** | Entity có quan hệ 2 chiều (TimeSlot ↔ Session) gây vòng lặp vô hạn khi serialize JSON |
| **Format dữ liệu** | `LocalTime` → `String` để JSON serialize dễ dàng |
| **Flexibility** | Có thể thêm computed fields (statistics, displayName) |

### ✅ Checklist hiểu:
- [ ] Sự khác biệt giữa RequestDTO và ResponseDTO?
- [ ] Tại sao cần TimeSlotTemplateDTO riêng cho dropdown?
- [ ] Tại sao dùng String cho time/date thay vì LocalTime/LocalDate?

### 🔧 Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/dtos/timeslot/
git commit -m "feat(timeslot): add TimeSlot DTOs (request, response, template, session)"
```

---

## 1.4 Service Interface: TimeSlotTemplateService.java

### Files cần tạo:
```
src/main/java/org/fyp/tmssep490be/services/TimeSlotTemplateService.java
```

### Code:

```java
package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.resource.TimeSlotRequestDTO;
import org.fyp.tmssep490be.dtos.timeslot.*;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;

import java.util.List;

/**
 * Interface định nghĩa business logic cho TimeSlot.
 *
 * TẠI SAO DÙNG INTERFACE?
 * 1. Loose coupling: Controller không phụ thuộc trực tiếp vào Implementation
 * 2. Dễ test: Mock interface khi viết unit test
 * 3. Dễ thay đổi: Có thể swap implementation mà không sửa controller
 */
public interface TimeSlotTemplateService {

    /**
     * Lấy tất cả time slots với filter.
     *
     * @param branchId - filter theo branch (null = tất cả)
     * @param search - tìm theo tên (null = không filter)
     * @param currentUserId - ID user đang request
     * @param isCenterHead - true nếu user là CENTER_HEAD
     * @param isTeacher - true nếu user là TEACHER
     * @return danh sách TimeSlotResponseDTO
     */
    List<TimeSlotResponseDTO> getAllTimeSlots(
            Long branchId,
            String search,
            Long currentUserId,
            boolean isCenterHead,
            boolean isTeacher
    );

    /**
     * Lấy time slot theo ID.
     */
    TimeSlotResponseDTO getTimeSlotById(Long id);

    /**
     * Tạo time slot mới.
     *
     * @param request - dữ liệu từ client
     * @param currentUserId - user tạo
     * @param forcedBranchId - branch bắt buộc (CENTER_HEAD chỉ tạo được cho branch của mình)
     */
    TimeSlotResponseDTO createTimeSlot(TimeSlotRequestDTO request, Long currentUserId, Long forcedBranchId);

    /**
     * Cập nhật time slot.
     */
    TimeSlotResponseDTO updateTimeSlot(Long id, TimeSlotRequestDTO request, Long currentUserId);

    /**
     * Xóa time slot.
     * Chỉ xóa được nếu chưa có session nào sử dụng.
     */
    void deleteTimeSlot(Long id);

    /**
     * Cập nhật status (ACTIVE/INACTIVE).
     */
    TimeSlotResponseDTO updateTimeSlotStatus(Long id, ResourceStatus status);

    /**
     * Lấy danh sách sessions đang dùng time slot này.
     * Dùng để show cảnh báo trước khi xóa/deactivate.
     */
    List<SessionInfoDTO> getSessionsByTimeSlotId(Long id);

    /**
     * Lấy time slots của branch cho dropdown.
     * Chỉ lấy ACTIVE time slots.
     */
    List<TimeSlotTemplateDTO> getBranchTimeSlotTemplates(Long branchId);
}
```

### 📚 Tại sao dùng Interface?

```
                    ┌─────────────────────┐
                    │     Controller      │
                    │  (gọi interface)    │
                    └──────────┬──────────┘
                               │
                               ▼
              ┌────────────────────────────────┐
              │    TimeSlotTemplateService     │  ← INTERFACE
              │          (Contract)            │
              └────────────────────────────────┘
                               ▲
           ┌───────────────────┼───────────────────┐
           │                   │                   │
┌──────────┴─────────┐ ┌───────┴────────┐ ┌────────┴─────────┐
│ TimeSlotServiceImpl│ │ MockTimeSlot   │ │  NewTimeSlot     │
│ (Production)       │ │ Service        │ │  ServiceImpl     │
│                    │ │ (Unit Test)    │ │  (New logic)     │
└────────────────────┘ └────────────────┘ └──────────────────┘
```

| Benefit | Giải thích |
|---------|------------|
| **Loose coupling** | Controller chỉ biết interface, không biết implementation |
| **Testability** | Có thể mock interface khi test controller |
| **Flexibility** | Swap implementation mà không cần sửa controller |
| **Contract** | Interface là "hợp đồng" - định nghĩa rõ ràng input/output |

### 🔧 Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/services/TimeSlotTemplateService.java
git commit -m "feat(timeslot): add TimeSlotTemplateService interface"
```

---

## 1.5 Service Implementation: TimeSlotTemplateServiceImpl.java

> ⚠️ **ĐÂY LÀ FILE QUAN TRỌNG NHẤT** - Chứa toàn bộ business logic

### Files cần tạo:
```
src/main/java/org/fyp/tmssep490be/services/impl/TimeSlotTemplateServiceImpl.java
```

### Để tiếp tục, hãy xem file `MIGRATION_GUIDE.md` phần **TimeSlotTemplateServiceImpl.java** (dòng 329-750) để copy code.

### 📚 Các điểm quan trọng cần hiểu:

#### 1. Dependency Injection qua Constructor:

```java
@Service                        // [1] Đánh dấu class là Spring Bean
@RequiredArgsConstructor        // [2] Lombok tạo constructor với final fields
@Slf4j                          // [3] Lombok tạo logger: log.info(), log.error()
@Transactional(readOnly = true) // [4] Mặc định là read-only transaction
public class TimeSlotTemplateServiceImpl implements TimeSlotTemplateService {

    // [5] Dependency Injection thông qua constructor (best practice)
    private final TimeSlotTemplateRepository timeSlotRepository;
    private final BranchRepository branchRepository;
    // ... other repositories
```

| Annotation | Giải thích |
|------------|------------|
| `@Service` | Đánh dấu class là Service bean, Spring quản lý lifecycle |
| `@RequiredArgsConstructor` | Lombok tự tạo constructor với các field `final` |
| `@Slf4j` | Lombok tự tạo `private static final Logger log = ...` |
| `@Transactional(readOnly = true)` | Default transaction cho cả class là read-only |

#### 2. Override Transactional cho Write operations:

```java
@Override
@Transactional  // [6] Ghi đè readOnly = true, cho phép write
public TimeSlotResponseDTO createTimeSlot(...) {
    // INSERT vào database
}
```

#### 3. Validation pattern:

```java
// VALIDATION 1: Entity exists
Branch branch = branchRepository.findById(branchId)
        .orElseThrow(() -> new RuntimeException("Branch not found with id: " + branchId));

// VALIDATION 2: Unique constraint
if (timeSlotRepository.existsByBranchIdAndNameIgnoreCase(branchId, request.getName())) {
    throw new RuntimeException("Time slot name already exists");
}

// VALIDATION 3: Business rule
if (startTime.isAfter(endTime)) {
    throw new RuntimeException("Start time must be before end time");
}
```

#### 4. Entity → DTO conversion:

```java
private TimeSlotResponseDTO convertToDTO(TimeSlotTemplate timeSlot) {
    return TimeSlotResponseDTO.builder()
            .id(timeSlot.getId())
            .branchId(timeSlot.getBranch().getId())
            .branchName(timeSlot.getBranch().getName())
            .name(timeSlot.getName())
            .startTime(timeSlot.getStartTime().toString())  // LocalTime → String
            .endTime(timeSlot.getEndTime().toString())
            .status(timeSlot.getStatus().name())            // Enum → String
            // ... statistics fields
            .build();
}
```

### ✅ Checklist hiểu:
- [ ] `@Transactional` hoạt động thế nào?
- [ ] Tại sao default là `readOnly = true`?
- [ ] Pattern validation trong service?
- [ ] Cách convert Entity → DTO?

### 🔧 Commit:
```bash
git add src/main/java/org/fyp/tmssep490be/services/impl/TimeSlotTemplateServiceImpl.java
git commit -m "feat(timeslot): add TimeSlotTemplateServiceImpl with business logic"
```

---

## 1.6 Controller: ResourceController.java (TimeSlot endpoints)

> **Mục đích**: Nhận HTTP request, gọi service, trả response

### Files cần tạo:
```
src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java
```

### Cấu trúc Controller:

```java
@RestController                          // [1] REST API controller
@RequestMapping("/api/v1")               // [2] Base path
@RequiredArgsConstructor                 // [3] Constructor injection
@Slf4j                                   // [4] Logger
@Tag(name = "Resource Management")       // [5] Swagger grouping
@SecurityRequirement(name = "bearerAuth") // [6] Swagger: yêu cầu JWT
public class ResourceController {

    private final TimeSlotTemplateService timeSlotTemplateService;
    
    // ... endpoints
}
```

### 📚 HTTP Methods tương ứng:

| Annotation | HTTP Method | Mục đích | Ví dụ |
|------------|-------------|----------|-------|
| `@GetMapping` | GET | Lấy dữ liệu | Lấy danh sách, lấy chi tiết |
| `@PostMapping` | POST | Tạo mới | Tạo time slot mới |
| `@PutMapping` | PUT | Cập nhật toàn bộ | Sửa time slot |
| `@PatchMapping` | PATCH | Cập nhật 1 phần | Chỉ sửa status |
| `@DeleteMapping` | DELETE | Xóa | Xóa time slot |

### 📚 Các annotation trong Controller:

| Annotation | Vị trí | Giải thích |
|------------|--------|------------|
| `@PreAuthorize("hasAnyRole(...)")` | Method | Kiểm tra role trước khi vào method |
| `@RequestParam(required = false)` | Parameter | Query param: `?branchId=1` |
| `@PathVariable` | Parameter | Path param: `/time-slots/{id}` |
| `@RequestBody` | Parameter | Parse JSON body thành object |
| `@AuthenticationPrincipal` | Parameter | Inject user từ JWT token |
| `@Operation(summary = "...")` | Method | Mô tả cho Swagger |

### Endpoints cần implement:

| # | Method | Endpoint | Mô tả |
|---|--------|----------|-------|
| 1 | GET | `/time-slots` | Lấy danh sách |
| 2 | GET | `/time-slots/{id}` | Lấy chi tiết |
| 3 | POST | `/time-slots` | Tạo mới |
| 4 | PUT | `/time-slots/{id}` | Cập nhật |
| 5 | DELETE | `/time-slots/{id}` | Xóa |
| 6 | PATCH | `/time-slots/{id}/status` | Đổi status |
| 7 | GET | `/time-slots/{id}/sessions` | Lấy sessions đang dùng |
| 8 | GET | `/branches/{branchId}/time-slot-templates` | Templates cho dropdown |

### 🔧 Commit mỗi endpoint:
```bash
# Sau khi hoàn thành endpoint GET /time-slots
git add src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java
git commit -m "feat(timeslot): GET /time-slots - list all time slots with filters"

# Sau khi thêm endpoint GET /time-slots/{id}
git add src/main/java/org/fyp/tmssep490be/controllers/ResourceController.java
git commit -m "feat(timeslot): GET /time-slots/{id} - get time slot by ID"

# Tiếp tục tương tự cho các endpoints khác...
```

---

## 📋 CHECKLIST MODULE 1: TIMESLOT MANAGEMENT

### Files cần tạo/sửa:

| # | File | Status |
|---|------|--------|
| 0.1 | `dtos/common/ResponseObject.java` | ⬜ |
| 1.1 | `entities/TimeSlotTemplate.java` | ✅ ĐÃ CÓ |
| 1.2 | `repositories/TimeSlotTemplateRepository.java` | ⬜ |
| 1.3.1 | `dtos/timeslot/TimeSlotRequestDTO.java` | ⬜ |
| 1.3.2 | `dtos/timeslot/TimeSlotResponseDTO.java` | ⬜ |
| 1.3.3 | `dtos/timeslot/TimeSlotTemplateDTO.java` | ⬜ |
| 1.3.4 | `dtos/timeslot/SessionInfoDTO.java` | ⬜ |
| 1.4 | `services/TimeSlotTemplateService.java` | ⬜ |
| 1.5 | `services/impl/TimeSlotTemplateServiceImpl.java` | ⬜ |
| 1.6 | `controllers/ResourceController.java` | ⬜ |

### Endpoints checklist:

| # | Endpoint | Commit Message | Status |
|---|----------|----------------|--------|
| 1 | GET `/time-slots` | `feat(timeslot): GET /time-slots - list all` | ⬜ |
| 2 | GET `/time-slots/{id}` | `feat(timeslot): GET /time-slots/{id} - get by id` | ⬜ |
| 3 | POST `/time-slots` | `feat(timeslot): POST /time-slots - create new` | ⬜ |
| 4 | PUT `/time-slots/{id}` | `feat(timeslot): PUT /time-slots/{id} - update` | ⬜ |
| 5 | DELETE `/time-slots/{id}` | `feat(timeslot): DELETE /time-slots/{id} - delete` | ⬜ |
| 6 | PATCH `/time-slots/{id}/status` | `feat(timeslot): PATCH status - toggle active` | ⬜ |
| 7 | GET `/time-slots/{id}/sessions` | `feat(timeslot): GET sessions using slot` | ⬜ |
| 8 | GET `/branches/{id}/time-slot-templates` | `feat(timeslot): GET templates for dropdown` | ⬜ |

---

# 📦 MODULE 2: RESOURCE MANAGEMENT (7 Endpoints)

> Tương tự TimeSlot, Resource là CRUD cho phòng học/Zoom account.
> Xem file `MIGRATION_GUIDE.md` để copy pattern tương tự.

*Sẽ tiếp tục sau khi hoàn thành Module 1*

---

# 📦 MODULE 3: CURRICULUM MANAGEMENT (15 Endpoints)

> Subject → Level → Course hierarchy
> Xem file `MIGRATION_GUIDE.md` để copy pattern.

*Sẽ tiếp tục sau khi hoàn thành Module 2*

---

# 📦 MODULE 4: CLASS CREATION WORKFLOW (22 Endpoints)

> 7-step wizard tạo lớp học - phức tạp nhất
> Xem file `MIGRATION_GUIDE.md` để copy pattern.

*Sẽ tiếp tục sau khi hoàn thành Module 3*

---

## 🔧 HƯỚNG DẪN TEST VỚI SWAGGER/POSTMAN

### 1. Chạy application:
```bash
cd tms-sep490-be
./mvnw spring-boot:run
```

### 2. Mở Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

### 3. Authenticate:
1. Gọi API login để lấy JWT token
2. Click "Authorize" button
3. Nhập: `Bearer <token>`

### 4. Test endpoint:
1. Expand endpoint cần test
2. Click "Try it out"
3. Nhập parameters
4. Click "Execute"
5. Kiểm tra response

---

## 📝 GHI CHÚ QUAN TRỌNG

### Khi gặp khó khăn:
1. Đọc lại phần giải thích trong file này
2. Xem code mẫu trong `MIGRATION_GUIDE.md`
3. Google: `Spring Boot <keyword> example`
4. Hỏi giáo viên hoặc bạn học

### Các lỗi thường gặp:
| Lỗi | Nguyên nhân | Cách sửa |
|-----|-------------|----------|
| `NullPointerException` | Field chưa được inject | Kiểm tra `@RequiredArgsConstructor` và `final` |
| `LazyInitializationException` | Truy cập lazy field ngoài transaction | Dùng `@Transactional` trong service |
| `Circular dependency` | 2 bean phụ thuộc lẫn nhau | Dùng `@Lazy` hoặc refactor design |
| `No qualifying bean` | Spring không tìm thấy bean | Kiểm tra `@Repository`, `@Service`, `@Component` |

