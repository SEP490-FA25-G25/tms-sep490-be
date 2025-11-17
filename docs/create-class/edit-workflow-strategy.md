# EDIT WORKFLOW STRATEGY - Create Class Feature

**Version:** 1.0  
**Date:** 2025-11-13  
**Status:** 📋 DESIGN DOCUMENT

---

## 🎯 OVERVIEW

Strategy for allowing users to edit completed steps in Create Class workflow.

**Core Philosophy:** "Make it easy to do the right thing, hard to do the wrong thing"

```
✅ Easy edits in DRAFT (encourage iteration)
⚠️ Guided edits in PENDING (prevent confusion)
🔒 Controlled changes after APPROVED (maintain integrity)
```

---

## 📊 EDIT PERMISSIONS BY STATUS

```
DRAFT → PENDING → APPROVED → SCHEDULED → ONGOING → COMPLETED
  ✅       ⚠️        🔒          🔒         🔒          🔒

✅ = Full edit allowed
⚠️ = Withdraw required first
🔒 = No direct edits, use Change Request workflows
```

---

## 📋 EDIT RULES BY STEP

### **STEP 1-3: Basic Info, Sessions, Time Slots** ✅ ALLOW (in DRAFT)

**Why:** Low impact, easy to regenerate

**Implementation:**

```java
@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    /**
     * Edit basic class information
     * Only allowed in DRAFT status
     */
    @PutMapping("/{classId}/basic-info")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<ClassDTO>> editBasicInfo(
            @PathVariable Long classId,
            @Valid @RequestBody UpdateBasicInfoRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        // Guard check
        EditPermission permission = editGuardService.checkEditPermission(
            classId, EditType.BASIC_INFO
        );

        if (!permission.isAllowed()) {
            throw new CustomException(ErrorCode.EDIT_NOT_ALLOWED,
                permission.getReason());
        }

        // Show warning if schedule_days changed
        if (request.scheduleDaysChanged()) {
            // This will trigger session regeneration
            log.warn("Schedule days changed for class {}, regenerating sessions", classId);
        }

        ClassDTO result = classService.updateBasicInfo(classId, request, user);

        return ResponseEntity.ok(ResponseObject.success(
            "Cập nhật thông tin thành công", result
        ));
    }
}
```

---

### **STEP 4: Resources** ⚠️ ALLOW WITH WARNING

**Impacts:** Clear assignments, may create conflicts

**Implementation:**

```java
@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    /**
     * Clear and re-assign resources
     * Shows impact before allowing edit
     */
    @DeleteMapping("/{classId}/resources")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<ResourceImpactDTO>> clearResourceAssignments(
            @PathVariable Long classId,
            @AuthenticationPrincipal UserPrincipal user) {

        // Guard check
        EditPermission permission = editGuardService.checkEditPermission(
            classId, EditType.RESOURCES
        );

        if (!permission.isAllowed()) {
            throw new CustomException(ErrorCode.EDIT_NOT_ALLOWED);
        }

        // Get impact analysis
        ResourceImpactDTO impact = resourceAssignmentService
            .analyzeImpact(classId);

        if (impact.hasAssignments()) {
            // Return impact for confirmation dialog
            return ResponseEntity.ok(ResponseObject.success(
                "Xem chi tiết ảnh hưởng trước khi xóa", impact
            ));
        }

        // No assignments, can proceed directly
        resourceAssignmentService.clearAssignments(classId);

        // Log the action
        auditService.logEdit(classId, "RESOURCES", "CLEAR", user.getId(),
            "Cleared for re-assignment");

        return ResponseEntity.ok(ResponseObject.success(
            "Đã xóa assignments", impact
        ));
    }

    /**
     * Confirm clear after user sees impact
     */
    @PostMapping("/{classId}/resources/confirm-clear")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<Void>> confirmClearResources(
            @PathVariable Long classId,
            @RequestBody @Valid ConfirmClearRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        resourceAssignmentService.clearAssignments(classId);

        auditService.logEdit(classId, "RESOURCES", "CLEAR", user.getId(),
            request.getReason());

        return ResponseEntity.ok(ResponseObject.success(
            "Đã xóa assignments. Bạn có thể assign lại."
        ));
    }
}
```

---

### **STEP 5: Teachers** ⚠️ ALLOW WITH WARNING + NOTIFICATIONS

**Impacts:** Clear slots, notify teachers, may have prepared classes

**Implementation:**

```java
@Service
public class TeacherAssignmentService {

    public TeacherImpactDTO analyzeImpact(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Check if class has started
        if (classEntity.getStatus() == ClassStatus.ONGOING) {
            throw new CustomException(ErrorCode.CANNOT_EDIT_ONGOING_CLASS,
                "Không thể thay đổi giáo viên sau khi lớp đã bắt đầu. " +
                "Vui lòng dùng chức năng 'Teacher Request' để đổi lịch.");
        }

        // Get affected teachers
        List<Teacher> affectedTeachers = teachingSlotRepository
            .findDistinctTeachersByClassId(classId);

        // Count sessions by teacher
        Map<Long, Long> sessionCountByTeacher = teachingSlotRepository
            .countSessionsByTeacherForClass(classId);

        // Get upcoming sessions only
        long upcomingSessions = sessionRepository
            .countUpcomingSessions(classId);

        return TeacherImpactDTO.builder()
            .affectedTeachers(affectedTeachers)
            .sessionCountByTeacher(sessionCountByTeacher)
            .upcomingSessions(upcomingSessions)
            .totalSessions(classEntity.getSessions().size())
            .requiresNotification(true)
            .build();
    }

    @Transactional
    public void clearTeacherAssignments(Long classId, Long userId, String reason) {
        // Get impact for notifications
        TeacherImpactDTO impact = analyzeImpact(classId);

        // Clear assignments
        teachingSlotRepository.deleteByClassId(classId);

        // Send notifications to affected teachers
        for (Teacher teacher : impact.getAffectedTeachers()) {
            notificationService.notifyTeacherUnassignment(
                teacher,
                classId,
                impact.getSessionCountByTeacher().get(teacher.getId()),
                reason
            );
        }

        // Audit log
        auditService.logEdit(classId, "TEACHERS", "CLEAR", userId, reason);

        log.info("Cleared {} teacher assignments for class {}. Notified {} teachers.",
            impact.getTotalSessions(), classId, impact.getAffectedTeachers().size());
    }
}
```

---

### **STEP 6: Validation** 🔄 ALWAYS ALLOW

Review-only step, no data persistence.

---

### **STEP 7: Submit & Approve** 🔒 RESTRICTED

#### **7A. DRAFT** ✅ FULL EDIT

Can edit freely.

#### **7B. PENDING** ⚠️ WITHDRAW REQUIRED

Must withdraw to DRAFT first, then edit.

**Implementation:**

```java
@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    /**
     * Withdraw class submission
     * Allows editing by returning to DRAFT status
     */
    @PostMapping("/{classId}/withdraw")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    public ResponseEntity<ResponseObject<ClassDTO>> withdrawSubmission(
            @PathVariable Long classId,
            @RequestBody WithdrawRequest request,
            @AuthenticationPrincipal UserPrincipal user) {

        ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Only allow withdraw if PENDING
        if (classEntity.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new CustomException(ErrorCode.CANNOT_WITHDRAW,
                "Chỉ có thể rút lại class đang ở trạng thái PENDING");
        }

        // Check if user is the one who submitted
        if (!classEntity.getCreatedBy().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED,
                "Chỉ người tạo class mới có thể rút lại");
        }

        // Withdraw
        classEntity.setApprovalStatus(ApprovalStatus.PENDING);
        classEntity.setStatus(ClassStatus.DRAFT);
        classEntity.setSubmittedAt(null);

        classRepository.save(classEntity);

        // Notify Center Head
        notificationService.notifyWithdrawal(
            classEntity.getDecidedBy(),
            classEntity,
            request.getReason()
        );

        // Audit log
        auditService.logEdit(classId, "SUBMISSION", "WITHDRAW",
            user.getId(), request.getReason());

        return ResponseEntity.ok(ResponseObject.success(
            "Đã rút lại yêu cầu duyệt. Class quay về trạng thái DRAFT.",
            classMapper.toDTO(classEntity)
        ));
    }
}
```

#### **7C. APPROVED/SCHEDULED/ONGOING** 🔒 NO DIRECT EDITS

**Why:** Officially approved, teachers confirmed, rooms booked

**If changes needed:** Use Request workflows or Change Request

**Implementation:**

```java
@Service
public class ClassEditGuardService {

    public EditPermission checkEditPermission(Long classId, EditType editType) {
        ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        return checkEditPermission(classEntity, editType);
    }

    public EditPermission checkEditPermission(ClassEntity classEntity, EditType editType) {
        switch (classEntity.getStatus()) {
            case DRAFT:
                return EditPermission.allowed();

            case PENDING:
                return EditPermission.withdrawRequired(
                    "Class đang chờ duyệt. Vui lòng rút lại submission để chỉnh sửa."
                );

            case SCHEDULED:
                return EditPermission.denied(
                    "Class đã được duyệt. Để thay đổi, vui lòng tạo Change Request."
                );

            case ONGOING:
                if (editType == EditType.TEACHER) {
                    return EditPermission.denied(
                        "Class đang diễn ra. Dùng Teacher Request để thay đổi giáo viên."
                    );
                }
                return EditPermission.denied(
                    "Class đang diễn ra. Không thể chỉnh sửa trực tiếp."
                );

            case COMPLETED:
            case CANCELLED:
                return EditPermission.denied(
                    "Class đã kết thúc. Không thể chỉnh sửa."
                );

            default:
                return EditPermission.denied(
                    "Class ở trạng thái không cho phép chỉnh sửa."
                );
        }
    }
}

// Helper classes
@Data
@Builder
public class EditPermission {
    private boolean allowed;
    private boolean withdrawRequired;
    private String reason;

    public static EditPermission allowed() {
        return EditPermission.builder()
            .allowed(true)
            .withdrawRequired(false)
            .build();
    }

    public static EditPermission withdrawRequired(String reason) {
        return EditPermission.builder()
            .allowed(false)
            .withdrawRequired(true)
            .reason(reason)
            .build();
    }

    public static EditPermission denied(String reason) {
        return EditPermission.builder()
            .allowed(false)
            .withdrawRequired(false)
            .reason(reason)
            .build();
    }
}

public enum EditType {
    BASIC_INFO, TIME_SLOTS, RESOURCES, TEACHERS, ALL
}
```

---

## 🔍 AUDIT TRAIL

### Database Schema

```sql
CREATE TABLE class_edit_history (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    step_name VARCHAR(50) NOT NULL, -- 'TIME_SLOTS', 'RESOURCES', 'TEACHERS', etc.
    action VARCHAR(20) NOT NULL,    -- 'EDIT', 'REASSIGN', 'CLEAR', 'WITHDRAW'

    old_value JSONB,                -- JSON of old assignments
    new_value JSONB,                -- JSON of new assignments

    edited_by BIGINT NOT NULL,      -- User who made the change
    edited_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,                    -- Required for changes after PENDING
    ip_address VARCHAR(45),         -- For security audit
    user_agent TEXT,                -- For security audit

    CONSTRAINT fk_edit_history_class FOREIGN KEY(class_id) REFERENCES "class"(id) ON DELETE CASCADE,
    CONSTRAINT fk_edit_history_user FOREIGN KEY(edited_by) REFERENCES user_account(id) ON DELETE SET NULL
);

CREATE INDEX idx_edit_history_class ON class_edit_history(class_id);
CREATE INDEX idx_edit_history_user ON class_edit_history(edited_by);
```

### Implementation (AuditService)

````java
@Entity
@Table(name = "class_edit_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_id", nullable = false)
    private Long classId;

    @Column(name = "step_name", length = 50, nullable = false)
    private String stepName;

    @Column(length = 20, nullable = false)
    private String action;

    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(name = "edited_by", nullable = false)
    private Long editedBy;

    @Column(name = "edited_at", nullable = false)
    private OffsetDateTime editedAt;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}

@Service
public class AuditService {

    private final ClassEditHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public void logEdit(Long classId, String stepName, String action,
                       Long userId, String reason) {
        logEdit(classId, stepName, action, null, null, userId, reason, null, null);
    }

    public void logEdit(Long classId, String stepName, String action,
                       Object oldValue, Object newValue,
                       Long userId, String reason,
                       String ipAddress, String userAgent) {
        try {
            ClassEditHistory history = ClassEditHistory.builder()
                .classId(classId)
                .stepName(stepName)
                .action(action)
                .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                .editedBy(userId)
                .editedAt(OffsetDateTime.now())
                .reason(reason)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

            historyRepository.save(history);

        } catch (Exception e) {
            log.error("Failed to log edit history for class {}", classId, e);
            // Don't fail the main operation if audit logging fails
        }
    }

    public List<ClassEditHistory> getEditHistory(Long classId) {
        return historyRepository.findByClassIdOrderByEditedAtDesc(classId);
    }
}

---

## 📊 ERROR CODES

```java
// Edit-related errors (4500-4599)
EDIT_NOT_ALLOWED(4500, "Edit not allowed in current status"),
CANNOT_EDIT_ONGOING_CLASS(4501, "Cannot edit ongoing class"),
CANNOT_WITHDRAW(4503, "Cannot withdraw in current status"),
EDIT_REQUIRES_REASON(4505, "Edit reason required"),
````

---

## 🎯 IMPLEMENTATION PHASES

**Phase 1: Basic Edit (8-10h)** - Guard service, basic/time slots edit, UI buttons, audit logging

**Phase 2: Advanced Guards (12-15h)** - Withdraw, impact analysis, notifications, complete audit

**Phase 3: Change Requests (20-25h)** - Teacher/resource change requests, re-approval workflow

---

## 📚 RELATED DOCUMENTS

- `create-class-workflow-final.md` - Main workflow specification
- `create-class-implementation-plan-v2.md` - Implementation plan
- `create-class-implementation-checklist-v2.md` - Progress tracking

---

## 🔄 CHANGE HISTORY

| Version | Date       | Author | Changes                 |
| ------- | ---------- | ------ | ----------------------- |
| 1.0     | 2025-11-13 | System | Initial design document |

---

**Status:** 📋 READY FOR REVIEW AND IMPLEMENTATION  
**Next Steps:** Review with team → Prioritize phases → Start Phase 1 implementation
