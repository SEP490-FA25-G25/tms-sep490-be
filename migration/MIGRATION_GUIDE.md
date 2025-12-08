# TMS Backend Migration Guide

> **📌 CÁCH DÙNG FILE NÀY**: Copy nội dung file này và gửi cho AI assistant khi bắt đầu conversation mới. AI sẽ hiểu context và tiếp tục công việc từ chỗ dừng.

---

## 🎯 MỤC TIÊU DỰ ÁN

### Bối cảnh
- **Lý do**: Project review FAIL - giáo viên yêu cầu viết lại code để hiểu và giải thích được
- **Yêu cầu**: Không copy-paste mù quáng, phải hiểu từng dòng code

### Scope cần migrate
1. **Quản lý TimeSlot** - CRUD khung giờ học
2. **Quản lý Resource** - CRUD phòng học/Zoom account  
3. **Quản lý Curriculum** - Curriculum → Subject → Level → Course
4. **Workflow Tạo lớp học** - Create Class 7-step wizard

### Nguyên tắc làm việc
1. **Hiểu từng dòng code** - Giải thích được annotations, logic
2. **Giải thích được** - Trả lời câu hỏi giáo viên về bất kỳ đoạn code nào
3. **Sửa được** - Debug và modify code độc lập
4. **One commit per endpoint** - Mỗi endpoint hoàn thành → commit 1 lần

---

# 📖 PHẦN 1: HƯỚNG DẪN CHI TIẾT TỪNG ENDPOINT

## Module 2: TimeSlot Management (8 endpoints)

> **Tại sao bắt đầu từ Module 2?** TimeSlot là CRUD đơn giản nhất, không phụ thuộc vào nhiều entity khác. Học xong module này bạn sẽ nắm được pattern chuẩn: Entity → Repository → DTO → Service → Controller.

---

### 📁 DANH SÁCH FILES CẦN TẠO CHO MODULE TIMESLOT

```
src/main/java/org/fyp/tmssep490be/
├── dtos/
│   ├── common/
│   │   └── ResponseObject.java              # DTO chung cho response
│   └── timeslot/
│       ├── TimeSlotRequestDTO.java          # Request khi tạo/sửa
│       ├── TimeSlotResponseDTO.java         # Response trả về client
│       ├── TimeSlotTemplateDTO.java         # Response rút gọn (cho dropdown)
│       └── SessionInfoDTO.java              # Session info (dùng chung)
├── repositories/
│   └── TimeSlotTemplateRepository.java      # Query database
├── services/
│   ├── TimeSlotTemplateService.java         # Interface
│   └── impl/
│       └── TimeSlotTemplateServiceImpl.java # Business logic
└── controllers/
    └── ResourceController.java              # API endpoints
```

**Files ĐÃ CÓ SẴN** (không cần tạo):
- `entities/TimeSlotTemplate.java` - Entity đã có
- `entities/enums/ResourceStatus.java` - Enum đã có

---

### ENDPOINT 1: GET /api/v1/time-slots

**Mục đích**: Lấy danh sách tất cả TimeSlot templates, có filter theo branch và search.

#### FILES LIÊN QUAN:

**1. Entity: TimeSlotTemplate.java** (ĐÃ CÓ)
```java
// File: entities/TimeSlotTemplate.java
@Entity                                    // Đánh dấu class này map với table trong DB
@Table(name = "time_slot_template")        // Tên table trong PostgreSQL
@Getter @Setter                            // Lombok: tự tạo getter/setter cho tất cả fields
@NoArgsConstructor @AllArgsConstructor     // Lombok: tự tạo constructor không/có tham số
@Builder                                   // Lombok: cho phép dùng Builder pattern
public class TimeSlotTemplate {

    @Id                                    // Đánh dấu primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)     // Quan hệ N-1: Nhiều TimeSlot thuộc 1 Branch
    @JoinColumn(name = "branch_id", nullable = false)  // FK column trong DB
    private Branch branch;                 // FetchType.LAZY = chỉ load khi cần (performance)

    @Column(nullable = false)              // Column không được NULL
    private String name;                   // Tên slot: "Morning 1", "Afternoon"...

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;           // Giờ bắt đầu: 08:00, 10:00...

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;             // Giờ kết thúc: 10:00, 12:00...

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)           // Lưu enum dạng String (không phải số)
    @Builder.Default                       // Giá trị mặc định khi dùng Builder
    private ResourceStatus status = ResourceStatus.ACTIVE;

    // Quan hệ 1-N: 1 TimeSlot có nhiều Sessions (buổi học)
    @OneToMany(mappedBy = "timeSlotTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();
    // mappedBy = tên field bên entity Session
    // cascade = ALL: mọi thao tác (save, delete) lan truyền sang sessions
    // orphanRemoval = true: xóa session không còn thuộc timeslot nào

    // Quan hệ 1-N: 1 TimeSlot có nhiều TeacherAvailability (lịch rảnh GV)
    @OneToMany(mappedBy = "timeSlotTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeacherAvailability> teacherAvailabilities = new HashSet<>();

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
```

**2. Repository: TimeSlotTemplateRepository.java** (CẦN TẠO)
```java
// File: repositories/TimeSlotTemplateRepository.java
package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository  // Đánh dấu class này là Repository (tầng truy cập DB)
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {
    // JpaRepository<Entity, PrimaryKeyType> cung cấp sẵn:
    // - findById(id), findAll()
    // - save(entity), delete(entity)
    // - count(), existsById(id)

    // ==================== CUSTOM QUERIES ====================
    
    /**
     * Tìm TimeSlot theo branchId, sắp xếp theo giờ bắt đầu
     * JPA tự động tạo query từ tên method:
     * findBy + Branch_Id + OrderBy + StartTime + Asc
     * → SELECT * FROM time_slot_template WHERE branch_id = ? ORDER BY start_time ASC
     */
    List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(Long branchId);

    /**
     * Tìm TimeSlot active của branch
     * findBy + Branch_Id + And + Status + OrderBy...
     */
    List<TimeSlotTemplate> findByBranchIdAndStatusOrderByStartTimeAsc(Long branchId, ResourceStatus status);

    /**
     * Kiểm tra trùng tên trong cùng branch (case-insensitive)
     * existsBy = trả về true/false
     * IgnoreCase = không phân biệt hoa thường
     */
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    /**
     * Kiểm tra trùng khung giờ trong cùng branch
     * Dùng để validate: không cho tạo slot 08:00-10:00 nếu đã có
     */
    boolean existsByBranchIdAndStartTimeAndEndTime(Long branchId, LocalTime startTime, LocalTime endTime);

    /**
     * Kiểm tra trùng tên nhưng loại trừ chính nó (dùng khi UPDATE)
     */
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long excludeId);

    /**
     * Kiểm tra trùng khung giờ nhưng loại trừ chính nó (dùng khi UPDATE)
     */
    boolean existsByBranchIdAndStartTimeAndEndTimeAndIdNot(
        Long branchId, LocalTime startTime, LocalTime endTime, Long excludeId);
}
```

**GIẢI THÍCH JPA QUERY METHOD NAMING:**
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

Các keyword khác:
- IgnoreCase        → LOWER(name) = LOWER(?)
- existsBy...       → trả về boolean
- countBy...        → trả về Long
- deleteBy...       → DELETE WHERE
- Between           → BETWEEN ? AND ?
- LessThan          → < ?
- GreaterThan       → > ?
- Like              → LIKE ?
- In                → IN (?, ?, ?)
- IsNull            → IS NULL
- Not               → NOT
```

**3. DTO: TimeSlotResponseDTO.java** (CẦN TẠO)
```java
// File: dtos/timeslot/TimeSlotResponseDTO.java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                   // Lombok: tạo getter, setter, toString, equals, hashCode
@Builder                // Lombok: Builder pattern
@NoArgsConstructor      // Constructor không tham số
@AllArgsConstructor     // Constructor đầy đủ tham số
public class TimeSlotResponseDTO {
    private Long id;
    private Long branchId;
    private String branchName;
    private String name;
    private String startTime;    // Format: "08:00:00"
    private String endTime;      // Format: "10:00:00"
    private String createdAt;
    private String updatedAt;
    private String status;       // "ACTIVE" hoặc "INACTIVE"
    
    // Thống kê (không bắt buộc, nhưng hữu ích cho UI)
    private Long activeClassesCount;      // Số lớp đang dùng slot này
    private Long totalSessionsCount;      // Tổng số buổi học dùng slot này
    private Boolean hasAnySessions;       // Có session nào không?
    private Boolean hasFutureSessions;    // Có session tương lai không?
    private Boolean hasTeacherAvailability;  // Có GV đăng ký rảnh slot này không?
}
```

**TẠI SAO DÙNG DTO THAY VÌ TRẢ ENTITY TRỰC TIẾP?**
1. **Bảo mật**: Không lộ cấu trúc database
2. **Kiểm soát dữ liệu**: Chỉ trả về fields cần thiết
3. **Tránh vòng lặp vô hạn**: Entity có quan hệ 2 chiều (TimeSlot → Session → TimeSlot...)
4. **Format dữ liệu**: LocalTime → String để JSON serialize dễ dàng

**4. Service Interface: TimeSlotTemplateService.java** (CẦN TẠO)

```java
// File: services/TimeSlotTemplateService.java
package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.resource.TimeSlotRequestDTO;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;

import java.util.List;

/**
 * Interface định nghĩa các method cho TimeSlot management.
 * Tại sao dùng Interface?
 * 1. Loose coupling: Controller không phụ thuộc trực tiếp vào Implementation
 * 2. Dễ test: Mock interface khi viết unit test
 * 3. Dễ thay đổi: Có thể swap implementation mà không sửa controller
 */
public interface TimeSlotTemplateService {

    /**
     * Lấy tất cả time slots với filter
     * @param branchId - filter theo branch (optional)
     * @param search - tìm theo tên (optional)
     * @param currentUserId - user đang request
     * @param isCenterHead - có phải CENTER_HEAD không
     * @param isTeacher - có phải TEACHER không
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
     * Lấy time slot theo ID
     */
    TimeSlotResponseDTO getTimeSlotById(Long id);

    /**
     * Tạo time slot mới
     * @param request - dữ liệu từ client
     * @param currentUserId - user tạo
     * @param forcedBranchId - branch bắt buộc (CENTER_HEAD chỉ tạo được cho branch của mình)
     */
    TimeSlotResponseDTO createTimeSlot(TimeSlotRequestDTO request, Long currentUserId, Long forcedBranchId);

    /**
     * Cập nhật time slot
     */
    TimeSlotResponseDTO updateTimeSlot(Long id, TimeSlotRequestDTO request, Long currentUserId);

    /**
     * Xóa time slot (chỉ xóa được nếu chưa có session nào dùng)
     */
    void deleteTimeSlot(Long id);

    /**
     * Cập nhật status (ACTIVE/INACTIVE)
     */
    TimeSlotResponseDTO updateTimeSlotStatus(Long id, ResourceStatus status);

    /**
     * Lấy danh sách sessions đang dùng time slot này
     */
    List<SessionInfoDTO> getSessionsByTimeSlotId(Long id);

    /**
     * Lấy time slots của branch (dùng cho dropdown khi tạo class)
     */
    List<TimeSlotTemplateDTO> getBranchTimeSlotTemplates(Long branchId);
}
```

**5. Service Implementation: TimeSlotTemplateServiceImpl.java** (CẦN TẠO)

```java
// File: services/impl/TimeSlotTemplateServiceImpl.java
package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.resource.TimeSlotRequestDTO;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TimeSlotTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service                        // Đánh dấu class này là Service (Spring bean)
@RequiredArgsConstructor        // Lombok: tạo constructor với final fields → Dependency Injection
@Slf4j                          // Lombok: tạo logger field: log.info(), log.error()
@Transactional(readOnly = true) // Mặc định các method là read-only transaction
public class TimeSlotTemplateServiceImpl implements TimeSlotTemplateService {

    // Dependency Injection thông qua constructor (best practice)
    private final TimeSlotTemplateRepository timeSlotRepository;
    private final BranchRepository branchRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    /**
     * GET ALL TIME SLOTS
     * Logic:
     * 1. Nếu là CENTER_HEAD → chỉ xem được branch của mình
     * 2. Nếu là TEACHER → xem branches được assign
     * 3. Nếu có branchId filter → filter theo đó
     * 4. Nếu có search → filter theo name
     * 5. Convert Entity → DTO
     */
    @Override
    public List<TimeSlotResponseDTO> getAllTimeSlots(
            Long branchId,
            String search,
            Long currentUserId,
            boolean isCenterHead,
            boolean isTeacher) {

        log.info("Getting time slots - branchId: {}, search: {}, userId: {}",
                branchId, search, currentUserId);

        List<TimeSlotTemplate> timeSlots;

        // STEP 1: Xác định branchId cần query
        Long effectiveBranchId = branchId;

        if (isCenterHead && branchId == null) {
            // CENTER_HEAD không truyền branchId → lấy branch của user
            effectiveBranchId = getBranchIdForUser(currentUserId);
        } else if (isTeacher && branchId == null) {
            // TEACHER → lấy tất cả timeslots của branches được assign
            List<Long> branchIds = getBranchIdsForUser(currentUserId);
            // Query tất cả timeslots của các branches này
            // (Cần thêm method vào Repository nếu chưa có)
            timeSlots = branchIds.stream()
                    .flatMap(bid -> timeSlotRepository.findByBranchIdOrderByStartTimeAsc(bid).stream())
                    .collect(Collectors.toList());

            // Filter by search nếu có
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                timeSlots = timeSlots.stream()
                        .filter(ts -> ts.getName().toLowerCase().contains(searchLower))
                        .collect(Collectors.toList());
            }

            return timeSlots.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

        // STEP 2: Query theo branchId
        if (effectiveBranchId != null) {
            timeSlots = timeSlotRepository.findByBranchIdOrderByStartTimeAsc(effectiveBranchId);
        } else {
            // Không có filter → lấy tất cả (chỉ ADMIN mới được)
            timeSlots = timeSlotRepository.findAll();
        }

        // STEP 3: Filter by search
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            timeSlots = timeSlots.stream()
                    .filter(ts -> ts.getName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        // STEP 4: Convert to DTO
        return timeSlots.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET BY ID
     * Logic đơn giản:
     * 1. Tìm theo ID
     * 2. Không tìm thấy → throw exception
     * 3. Convert → DTO
     */
    @Override
    public TimeSlotResponseDTO getTimeSlotById(Long id) {
        log.info("Getting time slot by id: {}", id);

        TimeSlotTemplate timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time slot not found with id: " + id));

        return convertToDTO(timeSlot);
    }

    /**
     * CREATE TIME SLOT
     * Logic:
     * 1. Validate branch exists
     * 2. Validate không trùng tên trong cùng branch
     * 3. Validate không trùng khung giờ trong cùng branch
     * 4. Tạo entity mới
     * 5. Save và return DTO
     */
    @Override
    @Transactional  // Ghi đè readOnly = true, cho phép write
    public TimeSlotResponseDTO createTimeSlot(
            TimeSlotRequestDTO request,
            Long currentUserId,
            Long forcedBranchId) {

        log.info("Creating time slot: {} by user: {}", request, currentUserId);

        // VALIDATION 1: Branch tồn tại
        Long branchId = forcedBranchId != null ? forcedBranchId : request.getBranchId();
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + branchId));

        // VALIDATION 2: Không trùng tên trong cùng branch
        if (timeSlotRepository.existsByBranchIdAndNameIgnoreCase(branchId, request.getName())) {
            throw new RuntimeException(
                    "Time slot with name '" + request.getName() + "' already exists in this branch");
        }

        // VALIDATION 3: Parse time và validate không trùng khung giờ
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new RuntimeException("Start time must be before end time");
        }

        if (timeSlotRepository.existsByBranchIdAndStartTimeAndEndTime(branchId, startTime, endTime)) {
            throw new RuntimeException(
                    "Time slot with this time range already exists in this branch");
        }

        // CREATE ENTITY
        TimeSlotTemplate timeSlot = TimeSlotTemplate.builder()
                .branch(branch)
                .name(request.getName())
                .startTime(startTime)
                .endTime(endTime)
                .status(ResourceStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        // SAVE
        TimeSlotTemplate saved = timeSlotRepository.save(timeSlot);
        log.info("Created time slot with id: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * UPDATE TIME SLOT
     * Logic tương tự CREATE, nhưng:
     * 1. Tìm entity existing
     * 2. Validate trùng tên/giờ LOẠI TRỪ chính nó (IdNot)
     * 3. Update fields
     * 4. Save
     */
    @Override
    @Transactional
    public TimeSlotResponseDTO updateTimeSlot(Long id, TimeSlotRequestDTO request, Long currentUserId) {
        log.info("Updating time slot {}: {} by user: {}", id, request, currentUserId);

        // FIND EXISTING
        TimeSlotTemplate timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time slot not found with id: " + id));

        Long branchId = timeSlot.getBranch().getId();

        // VALIDATION: Trùng tên (loại trừ chính nó)
        if (timeSlotRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                branchId, request.getName(), id)) {
            throw new RuntimeException(
                    "Time slot with name '" + request.getName() + "' already exists in this branch");
        }

        // Parse time
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());

        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new RuntimeException("Start time must be before end time");
        }

        // VALIDATION: Trùng khung giờ (loại trừ chính nó)
        if (timeSlotRepository.existsByBranchIdAndStartTimeAndEndTimeAndIdNot(
                branchId, startTime, endTime, id)) {
            throw new RuntimeException(
                    "Time slot with this time range already exists in this branch");
        }

        // UPDATE FIELDS
        timeSlot.setName(request.getName());
        timeSlot.setStartTime(startTime);
        timeSlot.setEndTime(endTime);
        timeSlot.setUpdatedAt(OffsetDateTime.now());

        // SAVE
        TimeSlotTemplate saved = timeSlotRepository.save(timeSlot);
        log.info("Updated time slot: {}", id);

        return convertToDTO(saved);
    }

    /**
     * DELETE TIME SLOT
     * Logic:
     * 1. Kiểm tra tồn tại
     * 2. Kiểm tra có session nào đang dùng không → không cho xóa
     * 3. Xóa
     */
    @Override
    @Transactional
    public void deleteTimeSlot(Long id) {
        log.info("Deleting time slot: {}", id);

        TimeSlotTemplate timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time slot not found with id: " + id));

        // SAFE DELETE: Check có session nào đang dùng không
        if (sessionRepository.existsByTimeSlotTemplateId(id)) {
            throw new RuntimeException(
                    "Cannot delete time slot: it is being used by sessions. " +
                            "Deactivate it instead.");
        }

        timeSlotRepository.delete(timeSlot);
        log.info("Deleted time slot: {}", id);
    }

    /**
     * UPDATE STATUS
     * Dùng để ACTIVE/INACTIVE thay vì xóa
     * INACTIVE time slot sẽ không hiện trong dropdown khi tạo class
     */
    @Override
    @Transactional
    public TimeSlotResponseDTO updateTimeSlotStatus(Long id, ResourceStatus status) {
        log.info("Updating time slot {} status to: {}", id, status);

        TimeSlotTemplate timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Time slot not found with id: " + id));

        timeSlot.setStatus(status);
        timeSlot.setUpdatedAt(OffsetDateTime.now());

        TimeSlotTemplate saved = timeSlotRepository.save(timeSlot);
        log.info("Updated time slot status: {} → {}", id, status);

        return convertToDTO(saved);
    }

    /**
     * GET SESSIONS BY TIME SLOT
     * Trả về danh sách sessions đang dùng time slot này
     * Dùng để hiện cảnh báo trước khi xóa/deactivate
     */
    @Override
    public List<SessionInfoDTO> getSessionsByTimeSlotId(Long id) {
        log.info("Getting sessions for time slot: {}", id);

        // Validate time slot exists
        if (!timeSlotRepository.existsById(id)) {
            throw new RuntimeException("Time slot not found with id: " + id);
        }

        List<Session> sessions = sessionRepository.findByTimeSlotTemplateId(id);

        return sessions.stream()
                .map(this::convertSessionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * GET BRANCH TIME SLOT TEMPLATES
     * Trả về list đơn giản cho dropdown (không cần statistics)
     * Chỉ lấy ACTIVE time slots
     */
    @Override
    public List<TimeSlotTemplateDTO> getBranchTimeSlotTemplates(Long branchId) {
        log.info("Getting time slot templates for branch: {}", branchId);

        List<TimeSlotTemplate> timeSlots = timeSlotRepository
                .findByBranchIdAndStatusOrderByStartTimeAsc(branchId, ResourceStatus.ACTIVE);

        return timeSlots.stream()
                .map(ts -> TimeSlotTemplateDTO.builder()
                        .id(ts.getId())
                        .name(ts.getName())
                        .startTime(ts.getStartTime().toString())
                        .endTime(ts.getEndTime().toString())
                        .displayName(ts.getStartTime() + " - " + ts.getEndTime())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== HELPER METHODS ====================

    /**
     * Lấy branchId của user (CENTER_HEAD chỉ có 1 branch)
     */
    private Long getBranchIdForUser(Long userId) {
        if (userId == null) return null;

        UserAccount user = userAccountRepository.findById(userId).orElse(null);
        if (user != null && !user.getUserBranches().isEmpty()) {
            return user.getUserBranches().iterator().next().getBranch().getId();
        }
        return null;
    }

    /**
     * Lấy danh sách branchIds của user (TEACHER có thể có nhiều branches)
     */
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

    /**
     * Convert Entity → Response DTO
     * Thêm statistics về số sessions đang dùng
     */
    private TimeSlotResponseDTO convertToDTO(TimeSlotTemplate timeSlot) {
        TimeSlotResponseDTO.TimeSlotResponseDTOBuilder builder = TimeSlotResponseDTO.builder()
                .id(timeSlot.getId())
                .branchId(timeSlot.getBranch().getId())
                .branchName(timeSlot.getBranch().getName())
                .name(timeSlot.getName())
                .startTime(timeSlot.getStartTime().toString())
                .endTime(timeSlot.getEndTime().toString())
                .createdAt(timeSlot.getCreatedAt().toString())
                .updatedAt(timeSlot.getUpdatedAt().toString())
                .status(timeSlot.getStatus().name());

        // Thêm statistics
        try {
            Long activeClasses = sessionRepository.countDistinctClassesByTimeSlotId(timeSlot.getId());
            Long totalSessions = sessionRepository.countSessionsByTimeSlotId(timeSlot.getId());
            Long futureSessions = sessionRepository.countFutureSessionsByTimeSlotId(
                    timeSlot.getId(), LocalDate.now(), LocalTime.now());
            boolean hasTeacherAvailability = teacherAvailabilityRepository
                    .existsById_TimeSlotTemplateId(timeSlot.getId());

            builder.activeClassesCount(activeClasses)
                    .totalSessionsCount(totalSessions)
                    .hasAnySessions(totalSessions > 0)
                    .hasFutureSessions(futureSessions > 0)
                    .hasTeacherAvailability(hasTeacherAvailability);
        } catch (Exception e) {
            log.error("Error calculating time slot statistics for id {}: {}",
                    timeSlot.getId(), e.getMessage());
            builder.activeClassesCount(0L)
                    .totalSessionsCount(0L)
                    .hasAnySessions(false)
                    .hasFutureSessions(false)
                    .hasTeacherAvailability(false);
        }

        return builder.build();
    }

    /**
     * Convert Session Entity → SessionInfoDTO
     */
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

**6. Controller: ResourceController.java** (CẦN TẠO)

```java
// File: controllers/ResourceController.java
package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.resource.TimeSlotRequestDTO;
import org.fyp.tmssep490be.entities.enums.ResourceStatus;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TimeSlotTemplateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController                          // REST API controller
@RequestMapping("/api/v1")               // Base path cho tất cả endpoints
@RequiredArgsConstructor                 // Constructor injection
@Slf4j                                   // Logger
@Tag(name = "Resource Management", description = "APIs for managing time slots and resources")
@SecurityRequirement(name = "bearerAuth") // Swagger: yêu cầu JWT
public class ResourceController {

    private final TimeSlotTemplateService timeSlotTemplateService;

    // ==================== TIME SLOT ENDPOINTS ====================

    /**
     * GET /api/v1/time-slots
     * Lấy danh sách time slots với filter
     */
    @GetMapping("/time-slots")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'TEACHER', 'MANAGER')")
    // PreAuthorize: Check role trước khi vào method
    // hasAnyRole: Cho phép các role trong danh sách
    @Operation(summary = "Get all time slots",
            description = "Get all time slot templates with optional filters")
    public ResponseEntity<List<TimeSlotResponseDTO>> getAllTimeSlots(
            @RequestParam(required = false) Long branchId,    // Query param: ?branchId=1
            @RequestParam(required = false) String search,     // Query param: ?search=morning
            @AuthenticationPrincipal UserPrincipal currentUser) {
        // @AuthenticationPrincipal: Inject user từ JWT token

        log.info("User {} requesting time slots - branchId: {}, search: {}",
                currentUser.getId(), branchId, search);

        // Check roles
        boolean isCenterHead = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CENTER_HEAD"));
        boolean isTeacher = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));

        List<TimeSlotResponseDTO> timeSlots = timeSlotTemplateService.getAllTimeSlots(
                branchId, search, currentUser.getId(), isCenterHead, isTeacher);

        log.info("Found {} time slots", timeSlots.size());
        return ResponseEntity.ok(timeSlots);
    }

    /**
     * GET /api/v1/time-slots/{id}
     * Lấy chi tiết 1 time slot
     */
    @GetMapping("/time-slots/{id}")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get time slot by ID")
    public ResponseEntity<TimeSlotResponseDTO> getTimeSlotById(
            @PathVariable Long id,  // Path variable: /time-slots/5
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting time slot {}", currentUser.getId(), id);
        TimeSlotResponseDTO timeSlot = timeSlotTemplateService.getTimeSlotById(id);
        return ResponseEntity.ok(timeSlot);
    }

    /**
     * POST /api/v1/time-slots
     * Tạo time slot mới
     */
    @PostMapping("/time-slots")
    @PreAuthorize("hasRole('CENTER_HEAD')")  // Chỉ CENTER_HEAD mới được tạo
    @Operation(summary = "Create new time slot")
    public ResponseEntity<TimeSlotResponseDTO> createTimeSlot(
            @RequestBody TimeSlotRequestDTO request,  // JSON body
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} creating time slot: {}", currentUser.getId(), request);

        // CENTER_HEAD chỉ tạo được cho branch của mình
        // → forcedBranchId sẽ override request.branchId
        Long forcedBranchId = getBranchIdForCenterHead(currentUser.getId());

        TimeSlotResponseDTO saved = timeSlotTemplateService
                .createTimeSlot(request, currentUser.getId(), forcedBranchId);

        log.info("Created time slot with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * PUT /api/v1/time-slots/{id}
     * Cập nhật time slot
     */
    @PutMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update time slot")
    public ResponseEntity<TimeSlotResponseDTO> updateTimeSlot(
            @PathVariable Long id,
            @RequestBody TimeSlotRequestDTO request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} updating time slot {}: {}", currentUser.getId(), id, request);

        TimeSlotResponseDTO saved = timeSlotTemplateService
                .updateTimeSlot(id, request, currentUser.getId());

        log.info("Updated time slot with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * DELETE /api/v1/time-slots/{id}
     * Xóa time slot (chỉ khi chưa có session nào dùng)
     */
    @DeleteMapping("/time-slots/{id}")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Delete time slot")
    public ResponseEntity<Void> deleteTimeSlot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} deleting time slot {}", currentUser.getId(), id);
        timeSlotTemplateService.deleteTimeSlot(id);
        log.info("Deleted time slot with ID: {}", id);

        return ResponseEntity.noContent().build();  // HTTP 204 No Content
    }

    /**
     * PATCH /api/v1/time-slots/{id}/status
     * Cập nhật status (ACTIVE/INACTIVE)
     * PATCH vs PUT: PATCH chỉ update 1 phần, PUT update toàn bộ
     */
    @PatchMapping("/time-slots/{id}/status")
    @PreAuthorize("hasRole('CENTER_HEAD')")
    @Operation(summary = "Update time slot status (ACTIVE/INACTIVE)")
    public ResponseEntity<TimeSlotResponseDTO> updateTimeSlotStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,  // { "status": "INACTIVE" }
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} updating time slot status for id {}: {}",
                currentUser.getId(), id, request);

        if (!request.containsKey("status")) {
            throw new RuntimeException("Field 'status' is required");
        }
        ResourceStatus status = ResourceStatus.valueOf(request.get("status"));

        TimeSlotResponseDTO saved = timeSlotTemplateService.updateTimeSlotStatus(id, status);

        log.info("Updated time slot status for ID: {} to {}", saved.getId(), status);
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/v1/time-slots/{id}/sessions
     * Lấy danh sách sessions đang dùng time slot này
     */
    @GetMapping("/time-slots/{id}/sessions")
    @PreAuthorize("hasAnyRole('CENTER_HEAD', 'ACADEMIC_AFFAIR', 'MANAGER')")
    @Operation(summary = "Get sessions using a time slot")
    public ResponseEntity<List<SessionInfoDTO>> getSessionsByTimeSlotId(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        log.info("User {} requesting sessions for time slot {}", currentUser.getId(), id);
        List<SessionInfoDTO> sessions = timeSlotTemplateService.getSessionsByTimeSlotId(id);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/v1/branches/{branchId}/time-slot-templates
     * Lấy time slots của branch (cho dropdown khi tạo class)
     */
    @GetMapping("/branches/{branchId}/time-slot-templates")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(summary = "Get branch time slot templates",
            description = "Get available time slot templates for a branch (for class creation)")
    public ResponseEntity<ResponseObject<List<TimeSlotTemplateDTO>>> getBranchTimeSlotTemplates(
            @PathVariable Long branchId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long userId = currentUser != null ? currentUser.getId() : null;
        log.info("User {} requesting time slot templates for branch {}", userId, branchId);

        List<TimeSlotTemplateDTO> timeSlotDTOs = timeSlotTemplateService
                .getBranchTimeSlotTemplates(branchId);

        log.info("Found {} time slot templates for branch {}", timeSlotDTOs.size(), branchId);

        return ResponseEntity.ok(ResponseObject.<List<TimeSlotTemplateDTO>>builder()
                .success(true)
                .message("Time slot templates retrieved successfully")
                .data(timeSlotDTOs)
                .build());
    }

    // ==================== HELPER METHOD ====================

    /**
     * Lấy branchId của CENTER_HEAD
     * (Tạm thời đặt ở đây, sau này nên move sang Service)
     */
    private Long getBranchIdForCenterHead(Long userId) {
        // TODO: Implement properly - get from user's branch assignment
        return null;  // Tạm return null, service sẽ dùng request.branchId
    }
}
```

---

### CÁC DTOs BỔ SUNG

**TimeSlotRequestDTO.java**
```java
// File: dtos/timeslot/TimeSlotRequestDTO.java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotRequestDTO {
    private Long branchId;      // Branch ID (optional nếu CENTER_HEAD tự động lấy)
    private String name;        // Tên slot: "Morning 1", "Afternoon"...
    private String startTime;   // Format: "08:00" hoặc "08:00:00"
    private String endTime;     // Format: "10:00" hoặc "10:00:00"
}
```

**TimeSlotTemplateDTO.java** (cho dropdown)
```java
// File: dtos/timeslot/TimeSlotTemplateDTO.java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

**SessionInfoDTO.java**
```java
// File: dtos/timeslot/SessionInfoDTO.java
package org.fyp.tmssep490be.dtos.timeslot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

**ResponseObject.java** (Common DTO)
```java
// File: dtos/common/ResponseObject.java
package org.fyp.tmssep490be.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseObject<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ResponseObject<T> success(String message, T data) {
        return ResponseObject.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ResponseObject<T> success(T data) {
        return success("Operation successful", data);
    }

    public static <T> ResponseObject<T> error(String message) {
        return ResponseObject.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
```

---

## 📁 CẤU TRÚC PROJECT

### Workspace Structure
```
SEP490-Capstone/
├── tms-sep490-be/           # Backend MỚI (viết code ở đây)
├── tms-sep490-deprecated/   # Backend CŨ (tham khảo)
└── tms-sep490-fe/           # Frontend (giữ nguyên)
```

### Backend Package Structure
```
tms-sep490-be/src/main/java/org/fyp/tmssep490be/
├── config/           # Cấu hình (CORS, OpenAPI, Security)
├── controllers/      # REST API endpoints (@RestController)
├── dtos/             # Data Transfer Objects
│   ├── request/      # Request DTOs (client → server)
│   └── response/     # Response DTOs (server → client)
├── entities/         # JPA Entities (ánh xạ database)
│   └── enums/        # Enum types (status, modality...)
├── exceptions/       # Xử lý lỗi tập trung
├── repositories/     # JPA Repositories (query database)
├── security/         # JWT, UserPrincipal, Authentication
├── services/         # Business logic interfaces
│   └── impl/         # Service implementations
└── utils/            # Helper/Utility classes
```

---

## � DATABASE SCHEMA

### Entity Dependency Tiers
```
TIER 1 - Độc lập (không FK):
├── branch              # Chi nhánh
├── curriculum          # Chương trình đào tạo
└── user_account        # Tài khoản người dùng

TIER 2 - Phụ thuộc Tier 1:
├── subject             # Môn học → curriculum_id
├── time_slot_template  # Khung giờ → branch_id
├── resource            # Phòng/Zoom → branch_id
└── teacher             # Giáo viên → user_account_id

TIER 3 - Phụ thuộc Tier 2:
├── level               # Cấp độ → subject_id
└── teacher_skill       # Kỹ năng GV → teacher_id, subject_id

TIER 4 - Phụ thuộc Tier 3:
└── course              # Khóa học → level_id

TIER 5 - Class Management:
├── class               # Lớp học → course_id, branch_id
├── session             # Buổi học → class_id, time_slot_id
├── session_resource    # session_id, resource_id
└── teaching_slot       # session_id, teacher_id
```

---

## 📋 DANH SÁCH ENDPOINT (69 endpoints)

### Module 1: Resource Management (15 endpoints)

#### 1.1 Resource CRUD (`/api/v1/resources`)
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 1 | `GET` | `/resources` | Lấy danh sách resources | CENTER_HEAD, ACADEMIC_AFFAIR, MANAGER |
| 2 | `GET` | `/resources/{id}` | Lấy resource theo ID | CENTER_HEAD, ACADEMIC_AFFAIR, MANAGER |
| 3 | `POST` | `/resources` | Tạo resource mới | CENTER_HEAD |
| 4 | `PUT` | `/resources/{id}` | Cập nhật resource | CENTER_HEAD |
| 5 | `DELETE` | `/resources/{id}` | Xóa resource | CENTER_HEAD |
| 6 | `PATCH` | `/resources/{id}/status` | Cập nhật status (ACTIVE/INACTIVE) | CENTER_HEAD |
| 7 | `GET` | `/resources/{id}/sessions` | Lấy sessions đang dùng resource | CENTER_HEAD, ACADEMIC_AFFAIR, MANAGER |

#### 1.2 TimeSlot CRUD (`/api/v1/time-slots`)
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 8 | `GET` | `/time-slots` | Lấy danh sách time slots | CENTER_HEAD, ACADEMIC_AFFAIR, MANAGER |
| 9 | `GET` | `/time-slots/{id}` | Lấy time slot theo ID | CENTER_HEAD, ACADEMIC_AFFAIR, MANAGER |
| 10 | `POST` | `/time-slots` | Tạo time slot mới | CENTER_HEAD |
| 11 | `PUT` | `/time-slots/{id}` | Cập nhật time slot | CENTER_HEAD |
| 12 | `DELETE` | `/time-slots/{id}` | Xóa time slot | CENTER_HEAD |
| 13 | `PATCH` | `/time-slots/{id}/status` | Cập nhật status | CENTER_HEAD |
| 14 | `GET` | `/time-slots/{id}/sessions` | Lấy sessions đang dùng slot | CENTER_HEAD, ACADEMIC_AFFAIR, MANAGER |
| 15 | `GET` | `/branches/{branchId}/time-slot-templates` | Templates theo branch | ACADEMIC_AFFAIR |

---

### Module 2: Curriculum Management (15 endpoints)

#### 2.1 Subject CRUD (`/api/v1/curriculum`)
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 16 | `GET` | `/subjects-with-levels` | Lấy tất cả subjects + levels | ACADEMIC_AFFAIR, CENTER_HEAD, MANAGER, ADMIN, SUBJECT_LEADER |
| 17 | `GET` | `/subjects/{id}` | Chi tiết subject | " |
| 18 | `POST` | `/subjects` | Tạo subject | " |
| 19 | `PUT` | `/subjects/{id}` | Cập nhật subject | " |
| 20 | `PATCH` | `/subjects/{id}/deactivate` | Deactivate | " |
| 21 | `PATCH` | `/subjects/{id}/reactivate` | Reactivate | " |
| 22 | `PUT` | `/subjects/{id}/levels/sort-order` | Sắp xếp levels | " |
| 23 | `DELETE` | `/subjects/{id}` | Xóa subject | " |

#### 2.2 Level CRUD
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 24 | `GET` | `/levels` | Lấy levels (filter by subjectId) | ACADEMIC_AFFAIR, CENTER_HEAD, MANAGER, ADMIN, SUBJECT_LEADER |
| 25 | `GET` | `/levels/{id}` | Chi tiết level | " |
| 26 | `POST` | `/levels` | Tạo level | " |
| 27 | `PUT` | `/levels/{id}` | Cập nhật level | " |
| 28 | `PATCH` | `/levels/{id}/deactivate` | Deactivate | " |
| 29 | `PATCH` | `/levels/{id}/reactivate` | Reactivate | " |
| 30 | `DELETE` | `/levels/{id}` | Xóa level | " |

---

### Module 3: Course Management (17 endpoints)

#### 3.1 Course CRUD (`/api/v1/courses`)
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 31 | `GET` | `/courses` | Danh sách courses | Public |
| 32 | `GET` | `/courses/{id}` | Chi tiết (Admin view) | SUBJECT_LEADER, MANAGER, ADMIN |
| 33 | `GET` | `/courses/{courseId}/detail` | Chi tiết (Student view) | STUDENT, ACADEMIC_AFFAIR, TEACHER |
| 34 | `GET` | `/courses/{courseId}/syllabus` | Syllabus | STUDENT, ACADEMIC_AFFAIR, TEACHER |
| 35 | `POST` | `/courses` | Tạo course | SUBJECT_LEADER |
| 36 | `PUT` | `/courses/{id}` | Cập nhật course | SUBJECT_LEADER |
| 37 | `DELETE` | `/courses/{id}` | Xóa course | SUBJECT_LEADER |
| 38 | `POST` | `/courses/{id}/clone` | Clone thành version mới | SUBJECT_LEADER |
| 39 | `GET` | `/courses/next-version` | Lấy version tiếp theo | SUBJECT_LEADER |

#### 3.2 Course Approval Workflow
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 40 | `POST` | `/courses/{id}/submit` | Submit để duyệt | SUBJECT_LEADER |
| 41 | `POST` | `/courses/{id}/approve` | Duyệt course | MANAGER, ADMIN |
| 42 | `POST` | `/courses/{id}/reject` | Từ chối course | MANAGER, ADMIN |
| 43 | `PATCH` | `/courses/{id}/deactivate` | Deactivate | SUBJECT_LEADER |
| 44 | `PATCH` | `/courses/{id}/reactivate` | Reactivate | SUBJECT_LEADER |

#### 3.3 Course Content & Progress
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 45 | `GET` | `/courses/{courseId}/materials` | Tài liệu học tập | STUDENT, ACADEMIC_AFFAIR, TEACHER |
| 46 | `GET` | `/courses/{courseId}/plos` | Program Learning Outcomes | Multiple |
| 47 | `GET` | `/courses/{courseId}/clos` | Course Learning Outcomes | STUDENT, ACADEMIC_AFFAIR, TEACHER |

---

### Module 4: Class Management (22 endpoints) - CREATE CLASS WORKFLOW

#### 4.1 Class List & Detail (`/api/v1/classes`)
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 48 | `GET` | `/classes` | Danh sách (pagination, filter) | ACADEMIC_AFFAIR, CENTER_HEAD, TEACHER, MANAGER |
| 49 | `GET` | `/classes/{classId}` | Chi tiết class | ACADEMIC_AFFAIR, CENTER_HEAD, STUDENT, TEACHER, MANAGER |
| 50 | `GET` | `/classes/{classId}/students` | Danh sách học sinh | ACADEMIC_AFFAIR, CENTER_HEAD, MANAGER |
| 51 | `GET` | `/classes/{classId}/summary` | Enrollment summary | ACADEMIC_AFFAIR, CENTER_HEAD, MANAGER |
| 52 | `GET` | `/classes/{classId}/available-students` | Students có thể enroll | ACADEMIC_AFFAIR |

#### 4.2 Step 0: Preview & Check
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 53 | `GET` | `/classes/preview-code` | Preview mã lớp | ACADEMIC_AFFAIR |
| 54 | `GET` | `/classes/check-name` | Check trùng tên | ACADEMIC_AFFAIR |

#### 4.3 Step 1: Create/Update Class
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 55 | `POST` | `/classes` | Tạo class (auto-gen sessions) | ACADEMIC_AFFAIR |
| 56 | `PUT` | `/classes/{classId}` | Update class (DRAFT/REJECTED) | ACADEMIC_AFFAIR |
| 57 | `DELETE` | `/classes/{classId}` | Xóa class DRAFT | ACADEMIC_AFFAIR |

#### 4.4 Step 2: Sessions
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 58 | `GET` | `/classes/{classId}/sessions` | Danh sách sessions | ACADEMIC_AFFAIR, CENTER_HEAD, TEACHER, MANAGER |
| 59 | `GET` | `/classes/{classId}/sessions/metrics` | Sessions + attendance metrics | ACADEMIC_AFFAIR, CENTER_HEAD, MANAGER |

#### 4.5 Step 3: Assign TimeSlots
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 60 | `POST` | `/classes/{classId}/time-slots` | Gán time slots theo day | ACADEMIC_AFFAIR |

#### 4.6 Step 4: Assign Resources
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 61 | `GET` | `/classes/{classId}/resources` | Resources khả dụng | ACADEMIC_AFFAIR |
| 62 | `POST` | `/classes/{classId}/resources` | Gán resources (bulk) | ACADEMIC_AFFAIR |
| 63 | `GET` | `/classes/{classId}/sessions/{sessionId}/resources` | Resources cho 1 session | ACADEMIC_AFFAIR |
| 64 | `POST` | `/classes/{classId}/sessions/{sessionId}/resource` | Gán resource (manual fix) | ACADEMIC_AFFAIR |

#### 4.7 Step 5: Assign Teachers
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 65 | `GET` | `/classes/{classId}/available-teachers` | Teachers khả dụng (PRE-CHECK) | ACADEMIC_AFFAIR |
| 66 | `GET` | `/classes/{classId}/teachers/available-by-day` | Teachers theo ngày (multi-teacher) | ACADEMIC_AFFAIR |
| 67 | `POST` | `/classes/{classId}/teachers` | Gán teacher | ACADEMIC_AFFAIR |

#### 4.8 Step 6-7: Validate & Submit
| # | Method | Endpoint | Mô tả | Role |
|---|--------|----------|-------|------|
| 68 | `POST` | `/classes/{classId}/validate` | Validate completeness | ACADEMIC_AFFAIR |
| 69 | `POST` | `/classes/{classId}/submit` | Submit cho duyệt | ACADEMIC_AFFAIR |
| 70 | `POST` | `/classes/{classId}/approve` | Duyệt class | CENTER_HEAD |
| 71 | `POST` | `/classes/{classId}/reject` | Từ chối class | CENTER_HEAD |

---

## 🔄 MIGRATION PROGRESS

### Module 1: Base Setup ⬜
- [ ] 1.1 pom.xml dependencies
- [ ] 1.2 application.yml configuration  
- [ ] 1.3 Enum types (ResourceStatus, ClassStatus, ApprovalStatus, Modality)
- [ ] 1.4 Exception handling (CustomException, GlobalExceptionHandler)
- [ ] 1.5 Common DTOs (ResponseObject, PageResponse)
- [ ] 1.6 Security config (JWT, UserPrincipal)

### Module 2: TimeSlot Management ⬜
- [ ] 2.1 Entity: TimeSlotTemplate
- [ ] 2.2 Repository: TimeSlotTemplateRepository
- [ ] 2.3 DTO: TimeSlotRequestDTO, TimeSlotResponseDTO
- [ ] 2.4 Service: TimeSlotTemplateService
- [ ] 2.5 Controller endpoints (8 endpoints)
- [ ] 2.6 Test với Postman

### Module 3: Resource Management ⬜
- [ ] 3.1 Entity: Resource
- [ ] 3.2 Enum: ResourceType (ROOM, ONLINE_ACCOUNT)
- [ ] 3.3 Repository: ResourceRepository
- [ ] 3.4 DTO: ResourceRequestDTO, ResourceResponseDTO
- [ ] 3.5 Service: ResourceService
- [ ] 3.6 Controller endpoints (7 endpoints)
- [ ] 3.7 Test với Postman

### Module 4: Curriculum Management ⬜
- [ ] 4.1 Entity: Subject
- [ ] 4.2 Entity: Level (thuộc Subject)
- [ ] 4.3 Repositories
- [ ] 4.4 DTOs
- [ ] 4.5 Service: CurriculumService
- [ ] 4.6 Controller endpoints (15 endpoints)
- [ ] 4.7 Test với Postman

### Module 5: Course Management ⬜
- [ ] 5.1 Entity: Course (thuộc Level)
- [ ] 5.2 Entity: CoursePhase, CourseSession
- [ ] 5.3 Repository & DTOs
- [ ] 5.4 Service: CourseService
- [ ] 5.5 Controller endpoints (17 endpoints)
- [ ] 5.6 Course approval workflow

### Module 6: Teacher Management ⬜
- [ ] 6.1 Entity: Teacher, TeacherSkill, TeacherAvailability
- [ ] 6.2 Repository & DTOs
- [ ] 6.3 Service: TeacherService
- [ ] 6.4 Teacher availability logic

### Module 7: Class Creation Workflow ⬜
**Step 1: Create Class**
- [ ] 7.1 Entity: ClassEntity
- [ ] 7.2 DTO: CreateClassRequest, CreateClassResponse
- [ ] 7.3 Service: createClass() - tạo class DRAFT + auto-generate sessions

**Step 2: Sessions Review**
- [ ] 7.4 Entity: Session
- [ ] 7.5 DTO: SessionListResponse
- [ ] 7.6 Service: listSessions()

**Step 3: Assign TimeSlot**
- [ ] 7.7 DTO: AssignTimeSlotsRequest/Response
- [ ] 7.8 Service: assignTimeSlots() - bulk assignment by day of week

**Step 4: Assign Resource**
- [ ] 7.9 Entity: SessionResource
- [ ] 7.10 Service: getAvailableResources() - với conflict detection
- [ ] 7.11 Service: assignResources() - HYBRID approach (bulk + conflict handling)

**Step 5: Assign Teacher**
- [ ] 7.12 Entity: TeachingSlot
- [ ] 7.13 Service: getAvailableTeachers() - PRE-CHECK approach
- [ ] 7.14 Service: getTeachersAvailableByDay() - multi-teacher mode
- [ ] 7.15 Service: assignTeacher()

**Step 6-7: Validate & Submit**
- [ ] 7.16 Service: validateClass()
- [ ] 7.17 Service: submitClass()
- [ ] 7.18 Service: approveClass(), rejectClass()

---

## 📚 KIẾN THỨC CẦN NẮM

### JPA Annotations cơ bản

| Annotation | Ý nghĩa | Ví dụ |
|------------|---------|-------|
| `@Entity` | Class map với table | `@Entity class Branch` |
| `@Table(name="x")` | Tên table trong DB | `@Table(name="branch")` |
| `@Id` | Primary key | Trên field `id` |
| `@GeneratedValue` | Auto-increment | `strategy = IDENTITY` |
| `@Column` | Config column | `nullable, unique, length` |
| `@ManyToOne` | Quan hệ N-1 | Session → Class |
| `@OneToMany` | Quan hệ 1-N | Class → Sessions |
| `@JoinColumn` | FK column name | `@JoinColumn(name="class_id")` |
| `@Enumerated` | Lưu enum | `EnumType.STRING` |
| `@CreationTimestamp` | Auto set khi tạo | `createdAt` |
| `@UpdateTimestamp` | Auto set khi update | `updatedAt` |

### Spring Annotations

| Annotation | Layer | Ý nghĩa |
|------------|-------|---------|
| `@Repository` | Repository | Đánh dấu DAO class |
| `@Service` | Service | Đánh dấu business logic |
| `@RestController` | Controller | REST API endpoint |
| `@RequiredArgsConstructor` | Tất cả | Constructor injection (Lombok) |
| `@Transactional` | Service | Quản lý transaction |
| `@PreAuthorize` | Controller | Kiểm tra quyền |
| `@GetMapping` | Controller | HTTP GET |
| `@PostMapping` | Controller | HTTP POST |
| `@PutMapping` | Controller | HTTP PUT |
| `@DeleteMapping` | Controller | HTTP DELETE |
| `@PatchMapping` | Controller | HTTP PATCH |
| `@RequestBody` | Controller | Parse JSON body |
| `@PathVariable` | Controller | Path param `/users/{id}` |
| `@RequestParam` | Controller | Query param `?name=x` |

### Luồng xử lý Request

```
HTTP Request
    ↓
Controller (@RestController)
├── Validate @RequestBody
├── Parse @PathVariable, @RequestParam
├── Check @PreAuthorize
    ↓
Service (@Service, @Transactional)
├── Business logic
├── Call multiple repositories
├── Entity → DTO conversion
    ↓
Repository (@Repository)
├── JPA methods: findById, save, delete
├── Custom queries: @Query
    ↓
Database (PostgreSQL)
    ↓
Response (ResponseObject<T>)
```

---

## �️ FILE REFERENCE

### Code CŨ (tham khảo - tms-sep490-deprecated):
```
src/main/java/org/fyp/tmssep490be/
├── controllers/
│   ├── ResourceController.java     # TimeSlot + Resource endpoints
│   ├── CurriculumController.java   # Subject + Level endpoints
│   ├── CourseController.java       # Course CRUD + approval
│   └── ClassController.java        # 7-step Create Class workflow
├── entities/
│   ├── TimeSlotTemplate.java
│   ├── Resource.java
│   ├── Subject.java
│   ├── Level.java
│   ├── Course.java
│   ├── ClassEntity.java
│   ├── Session.java
│   ├── SessionResource.java
│   └── TeachingSlot.java
├── services/impl/
│   ├── TimeSlotTemplateServiceImpl.java
│   ├── ResourceServiceImpl.java
│   ├── CurriculumServiceImpl.java
│   ├── CourseServiceImpl.java
│   └── ClassServiceImpl.java       # Logic phức tạp nhất
└── dtos/
    ├── createclass/                # Create Class workflow DTOs
    └── curriculum/                 # Curriculum DTOs
```

### Code MỚI (viết ở đây - tms-sep490-be):
```
src/main/java/org/fyp/tmssep490be/
├── entities/                       # ✅ Đã có sẵn
└── ... (tất cả code mới)
```

### Frontend (giữ nguyên - tms-sep490-fe):
```
src/store/services/
├── classCreationApi.ts             # API tạo lớp (7 steps)
├── resourceApi.ts                  # Resource + TimeSlot CRUD
├── curriculumApi.ts                # Subject + Level
└── courseApi.ts                    # Course CRUD
```

---

## 🎯 CREATE CLASS WORKFLOW CHI TIẾT

### Step 1: Create Class (POST /classes)
**Input**: branchId, courseId, name, startDate, scheduleDays, hoursPerSession, modality
**Output**: classId, code (auto-generated), sessionSummary (auto-generated sessions)

**Logic**:
1. Validate course exists + approved
2. Generate class code: `{SUBJECT}{LEVEL}-{BRANCH}-{YY}-{SEQ}`
3. Create ClassEntity with status=DRAFT
4. Auto-generate sessions based on course.totalSessions + scheduleDays

### Step 2: Review Sessions (GET /classes/{id}/sessions)
**Output**: List sessions với date, dayOfWeek, status

### Step 3: Assign TimeSlots (POST /classes/{id}/time-slots)
**Input**: `{ assignments: [{ dayOfWeek: 1, timeSlotId: 5 }, ...] }`
**Logic**: Bulk update all sessions matching dayOfWeek

### Step 4: Assign Resources (POST /classes/{id}/resources)
**Input**: `{ assignments: [{ dayOfWeek: 1, resourceId: 10 }, ...] }`
**Logic**: 
1. Phase 1 (SQL Bulk): Insert for sessions without conflicts
2. Phase 2 (Java): Detect conflicts, return conflict list for manual fix

### Step 5: Assign Teachers (POST /classes/{id}/teachers)
**Input**: `{ teacherId: 5, sessionIds: null }` (null = all sessions)
**Logic**:
1. PRE-CHECK: Show all teachers với availability breakdown
2. Bulk assign teacher to sessions
3. Support multi-teacher mode (different teachers per day)

### Step 6: Validate (POST /classes/{id}/validate)
**Logic**: Check all sessions have timeSlot + resource + teacher

### Step 7: Submit (POST /classes/{id}/submit)
**Logic**: Change status DRAFT → PENDING (chờ CENTER_HEAD duyệt)

---

## 💬 PROMPT MẪU CHO CONVERSATION MỚI

```
Đọc file tms-sep490-be/MIGRATION_GUIDE.md để hiểu context.

Tôi đang viết lại backend TMS. Yêu cầu:
1. Giải thích từng dòng code trước khi viết
2. Hướng dẫn step-by-step, không generate tự động
3. Tôi phải hiểu và giải thích được cho giáo viên

Hiện tại đang làm: [Module X - Task Y]
Câu hỏi cụ thể: [...]
```

---

## ✅ CHECKLIST TRƯỚC KHI HOÀN THÀNH MODULE

### Mỗi Entity:
- [ ] Hiểu tất cả JPA annotations
- [ ] Hiểu quan hệ với các entity khác
- [ ] Giải thích được schema trong database

### Mỗi Repository:
- [ ] Hiểu JPA method naming convention
- [ ] Hiểu @Query custom queries (nếu có)

### Mỗi Service:
- [ ] Hiểu luồng xử lý từ đầu đến cuối
- [ ] Hiểu @Transactional behavior
- [ ] Giải thích được business logic

### Mỗi Controller:
- [ ] Hiểu HTTP method tương ứng
- [ ] Hiểu @PreAuthorize roles
- [ ] Test thành công với Postman
- [ ] Hiểu frontend gọi API như thế nào

---

## 📝 NOTES VÀ DECISIONS

### Quyết định thiết kế:
1. **TimeSlot + Resource trong cùng controller** - Cả 2 đều là resources của branch
2. **PRE-CHECK cho teacher assignment** - Show all teachers với availability breakdown trước khi assign
3. **HYBRID approach cho resource assignment** - SQL bulk + Java conflict detection
4. **Multi-teacher mode** - Support gán nhiều GV cho các ngày khác nhau

### Technical notes:
- PostgreSQL database
- JWT authentication
- Role-based access control (RBAC)
- ResponseObject<T> wrapper cho tất cả responses

---

## 🚀 NEXT STEPS

1. **Bắt đầu từ Module 1: Base Setup** - Config, enums, exception handling
2. **Tiếp tục Module 2: TimeSlot** - CRUD đơn giản nhất
3. **Module 3: Resource** - Tương tự TimeSlot
4. **Module 4-5: Curriculum + Course** - Phức tạp hơn, có approval workflow
5. **Module 6: Teacher** - Chuẩn bị cho Class workflow
6. **Module 7: Class Creation** - Workflow phức tạp nhất (7 steps)

---

*Last updated: 2025-12-07*
*Tổng số endpoints cần migrate: 69*
*Estimated time: Depends on understanding pace*
