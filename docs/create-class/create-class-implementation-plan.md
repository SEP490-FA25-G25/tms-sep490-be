# CREATE CLASS WORKFLOW - IMPLEMENTATION PLAN

**Status:** 📋 READY TO START (ALL TASKS RESET)
**Created:** 2025-11-05
**Based on:**
- `create-class-workflow-final.md` (7-step workflow specification)
- `openapi.yaml` (API specification v1.1.0)
- `enrollment-implementation-status.md` (reference implementation pattern)

---

## OVERVIEW

This document outlines the complete implementation plan for the **Create Class Workflow** - a 7-step process for Academic Staff to create, configure, and submit classes for Center Head approval.

### Workflow Summary

```
1. CREATE CLASS          → Academic Staff creates basic class info
2. GENERATE SESSIONS     → System auto-generates sessions from course template
3. ASSIGN TIME SLOTS     → Academic Staff assigns time slots per schedule day
4. ASSIGN RESOURCES      → Academic Staff assigns rooms with HYBRID auto-propagation
5. ASSIGN TEACHERS       → Academic Staff assigns teachers with PRE-CHECK availability
6. VALIDATE             → System validates completeness
7. SUBMIT & APPROVE     → Academic Staff submits → Center Head approves/rejects
```

### Key Features

- ⏳ **PRE-CHECK Approach (v1.1)**: Teachers queried WITH availability status upfront
- ⏳ **HYBRID Auto-propagation**: SQL bulk operations + detailed conflict analysis
- ⏳ **'general' Skill**: Universal skill that can teach any session
- ⏳ **Intelligent Matching**: Smart resource/teacher assignment with conflict detection
- ⏳ **Multi-day Support**: Different time slots/resources per day of week

---

## IMPLEMENTATION COMPONENTS

### 1. Controllers (3 files)

#### ClassController (Main Controller)
**Path:** `controllers/ClassController.java`

| Endpoint | Method | Auth | Description | Status |
|----------|--------|------|-------------|--------|
| `/api/v1/classes` | POST | ACADEMIC_AFFAIR | **STEP 1**: Create class + auto-generate sessions | ⏳ TODO |
| `/api/v1/classes` | GET | ACADEMIC_AFFAIR | List classes with filters | ⏳ TODO |
| `/api/v1/classes/{classId}` | GET | ACADEMIC_AFFAIR | Get class detail | ⏳ TODO |
| `/api/v1/classes/{classId}/time-slots` | POST | ACADEMIC_AFFAIR | **STEP 3**: Assign time slots per day | ⏳ TODO |
| `/api/v1/classes/{classId}/resources` | POST | ACADEMIC_AFFAIR | **STEP 4**: Assign resources with auto-propagation | ⏳ TODO |
| `/api/v1/classes/{classId}/teachers` | POST | ACADEMIC_AFFAIR | **STEP 5**: Assign teacher to sessions | ⏳ TODO |
| `/api/v1/classes/{classId}/validate` | POST | ACADEMIC_AFFAIR | **STEP 6**: Validate class completeness | ⏳ TODO |
| `/api/v1/classes/{classId}/submit` | POST | ACADEMIC_AFFAIR | **STEP 7**: Submit for approval | ⏳ TODO |
| `/api/v1/classes/{classId}/approve` | POST | CENTER_HEAD | **STEP 7**: Approve class | ⏳ TODO |
| `/api/v1/classes/{classId}/reject` | POST | CENTER_HEAD | **STEP 7**: Reject class with reason | ⏳ TODO |

#### ResourceController (Utility Controller)
**Path:** `controllers/ResourceController.java`

| Endpoint | Method | Auth | Description | Status |
|----------|--------|------|-------------|--------|
| `/api/v1/classes/{classId}/available-resources` | GET | ACADEMIC_AFFAIR | Query available resources for sessions | ⏳ TODO |
| `/api/v1/branches/{branchId}/time-slot-templates` | GET | ACADEMIC_AFFAIR | Get available time slot templates | ⏳ TODO |

#### TeacherController (Utility Controller)
**Path:** `controllers/TeacherController.java`

| Endpoint | Method | Auth | Description | Status |
|----------|--------|------|-------------|--------|
| `/api/v1/classes/{classId}/available-teachers` | GET | ACADEMIC_AFFAIR | **STEP 5 PRE-CHECK**: Query teachers with availability | ⏳ TODO |

---

### 2. Services (6 services = 6 Interfaces + 6 Implementations)

**Pattern:** Each service follows **Interface + Implementation** pattern (as per codebase convention)
- Interface: `services/XxxService.java` (defines method signatures)
- Implementation: `services/impl/XxxServiceImpl.java` (business logic)

**Service Breakdown:**

| Service | Interface | Implementation | Status |
|---------|-----------|----------------|--------|
| ClassService | `services/ClassService.java` | `services/impl/ClassServiceImpl.java` | ⏳ TODO (need to ADD new methods) |
| SessionGenerationService | `services/SessionGenerationService.java` | `services/impl/SessionGenerationServiceImpl.java` | ⏳ TODO (new service) |
| ResourceAssignmentService | `services/ResourceAssignmentService.java` | `services/impl/ResourceAssignmentServiceImpl.java` | ⏳ TODO (new service) |
| TeacherAssignmentService | `services/TeacherAssignmentService.java` | `services/impl/TeacherAssignmentServiceImpl.java` | ⏳ TODO (new service) |
| ValidationService | `services/ValidationService.java` | `services/impl/ValidationServiceImpl.java` | ⏳ TODO (new service) |
| ApprovalService | `services/ApprovalService.java` | `services/impl/ApprovalServiceImpl.java` | ⏳ TODO (new service) |

**Note:** Following existing codebase pattern (e.g., `EnrollmentService` + `EnrollmentServiceImpl`, `StudentService` + `StudentServiceImpl`)

---

#### ClassService (Main Business Logic)
**Interface:** `services/ClassService.java`
**Implementation:** `services/impl/ClassServiceImpl.java`

**Status:**
- ⏳ Interface EXISTS (with getClasses, getClassDetail, etc.)
- ⏳ Implementation EXISTS (ClassServiceImpl.java - 685 lines)
- ⏳ Need to ADD new methods for Create Class Workflow

**Existing Methods (Already Implemented):**
```java
// ⏳ TODO - Already in ClassService interface
Page<ClassListItemDTO> getClasses(...);
ClassDetailDTO getClassDetail(Long classId, Long userId);
Page<ClassStudentDTO> getClassStudents(Long classId, String search, Pageable pageable, Long userId);
ClassEnrollmentSummaryDTO getClassEnrollmentSummary(Long classId, Long userId);
Page<AvailableStudentDTO> getAvailableStudentsForClass(Long classId, String search, Pageable pageable, Long userId);
```

**New Methods to ADD (for Create Class Workflow):**
```java
// ⏳ TODO - Add to ClassService interface
ClassEntity createClass(CreateClassRequest request, Long userId);  // STEP 1
void assignTimeSlots(Long classId, AssignTimeSlotsRequest request, Long userId);  // STEP 3
AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request, Long userId);  // STEP 4
AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest request, Long userId);  // STEP 5
ValidateClassResponse validateClass(Long classId, Long userId);  // STEP 6
void submitClass(Long classId, Long userId);  // STEP 7
void approveClass(Long classId, Long approverUserId);  // STEP 7
void rejectClass(Long classId, String reason, Long rejecterUserId);  // STEP 7
```

---

#### SessionGenerationService (Session Auto-generation)
**Interface:** `services/SessionGenerationService.java` ⏳ TODO (create new)
**Implementation:** `services/impl/SessionGenerationServiceImpl.java` ⏳ TODO (create new)

**Key Methods:**
```java
// STEP 2: Auto-generate sessions based on course template
List<Session> generateSessionsForClass(ClassEntity classEntity, Course course);
```

**Algorithm:**
```java
// Pseudocode from workflow doc (lines 42-92)
public List<Session> generateSessionsForClass(ClassEntity classEntity, Course course) {
    LocalDate currentDate = classEntity.getStartDate();
    Short[] scheduleDays = classEntity.getScheduleDays();  // [1, 3, 5] = Mon, Wed, Fri
    List<Session> sessions = new ArrayList<>();
    int sessionIndex = 0;

    List<CourseSession> courseSessions = course.getCourseSessions(); // Ordered by phase, sequence

    for (CourseSession courseSession : courseSessions) {
        int targetDayOfWeek = scheduleDays[sessionIndex % scheduleDays.length];

        // Find next occurrence of target day
        while (currentDate.getDayOfWeek().getValue() != targetDayOfWeek) {
            currentDate = currentDate.plusDays(1);
        }

        Session session = Session.builder()
            .classEntity(classEntity)
            .courseSession(courseSession)
            .date(currentDate)
            .type(SessionType.CLASS)
            .status(SessionStatus.PLANNED)
            .build();

        sessions.add(session);
        sessionIndex++;
        currentDate = currentDate.plusDays(1);
    }

    return sessions;
}
```

**Status:** ⏳ TODO (new service)

---

#### ResourceAssignmentService (Resource Auto-propagation)
**Interface:** `services/ResourceAssignmentService.java` ⏳ TODO (create new)
**Implementation:** `services/impl/ResourceAssignmentServiceImpl.java` ⏳ TODO (create new)

**Key Methods:**
- `assignResourcesWithPropagation(Long classId, Map<Integer, Long> dayToResourceMap)`
  - **Phase 1 (SQL Bulk)**: Bulk INSERT for non-conflict sessions (~90%)
  - **Phase 2 (Java Analysis)**: Detailed conflict detection for remaining sessions (~10%)
  - Returns: `AutoPropagateResult` with success count + conflict list

- `queryAvailableResources(Long classId, Long sessionId, LocalDate date, Long timeSlotId)`
  - Filters: Branch match, Resource type match, Capacity >= class max_capacity
  - Conflict check: No booking on same date + time slot

**HYBRID Approach** (from workflow lines 303-408):
```sql
-- Phase 1: SQL Bulk Insert (Fast Path)
INSERT INTO session_resource (session_id, resource_type, resource_id)
SELECT s.id, 'room', :resourceId
FROM session s
WHERE s.class_id = :classId
  AND EXTRACT(ISODOW FROM s.date) = :dayOfWeek
  AND NOT EXISTS (
    -- Skip sessions where resource has conflict
    SELECT 1 FROM session_resource sr2
    JOIN session s2 ON sr2.session_id = s2.id
    WHERE sr2.resource_id = :resourceId
      AND s2.date = s.date
      AND s2.time_slot_template_id = s.time_slot_template_id
  )
RETURNING session_id;
```

```java
// Phase 2: Conflict Detection (Java)
FUNCTION analyzeResourceConflict(session, resourceId):
    conflictingSession ← findConflictingSession(resourceId, session.date, session.timeSlotTemplateId)

    IF conflictingSession EXISTS:
        RETURN ConflictDetail(
            reason: "Room booked by Class " + conflictingSession.class.code,
            conflictingClass: conflictingSession.class.code
        )
    END IF

    RETURN ConflictDetail(reason: "Resource unavailable", conflictingClass: null)
```

**Performance:** ~100-200ms for 36 sessions

**Status:** ⏳ TODO

---

#### TeacherAssignmentService (Teacher PRE-CHECK + Assignment)
**Interface:** `services/TeacherAssignmentService.java` ⏳ TODO (create new)
**Implementation:** `services/impl/TeacherAssignmentServiceImpl.java` ⏳ TODO (create new)

**Key Methods:**

**PRE-CHECK (v1.1 Enhancement):**
- `queryAvailableTeachersWithPrecheck(Long classId, List<String> skillSet)`
  - Checks 3 conditions for ALL sessions BEFORE user selection:
    1. **Availability**: Teacher has registered availability for day_of_week + time_slot
    2. **Teaching Conflict**: Not teaching another class at same time
    3. **Leave Conflict**: Not on approved leave
  - Returns: `List<TeacherAvailability>` with availability breakdown
    - `availabilityStatus`: "fully_available", "partially_available", "unavailable"
    - `totalSessions`: Sessions teacher CAN teach (skill-matched)
    - `availableSessions`: Sessions without conflicts
    - `conflicts`: Breakdown (noAvailability, teachingConflict, leaveConflict)

**Skill Matching Logic** (FIXED in v1.1):
```sql
-- ⚡ FIXED: 'general' skill = UNIVERSAL (can teach ANY session)
WHERE (
  EXISTS (
    SELECT 1 FROM teacher_skill ts
    WHERE ts.teacher_id = :teacherId
      AND ts.skill = 'general'  -- Teacher with 'general' can teach ANY session
  )
  OR EXISTS (
    SELECT 1 FROM teacher_skill ts
    WHERE ts.teacher_id = :teacherId
      AND ts.skill = ANY(cs.skill_set)  -- Teacher skill matches session skill
  )
)
```

**Direct Assignment (v1.1 Simplified):**
- `assignTeacher(Long classId, AssignTeacherRequest request)`
  - No need to re-check conflicts (already done in PRE-CHECK)
  - Direct INSERT to available sessions
  - If partially available: Assign to available sessions, return conflict list for substitute

**SQL Query** (from workflow lines 500-653):
```sql
-- Full PRE-CHECK query with 3 conditions
-- See openapi.yaml lines 500-653 for complete SQL
WITH skill_matched_teachers AS (...),
     session_conflicts AS (
       -- Check 3 conditions for ALL sessions
       SELECT teacher_id,
              COUNT(*) FILTER (WHERE NOT EXISTS availability) as no_availability_count,
              COUNT(*) FILTER (WHERE EXISTS teaching_conflict) as teaching_conflict_count,
              COUNT(*) FILTER (WHERE EXISTS leave_conflict) as leave_conflict_count,
              COUNT(*) as total_sessions
       FROM skill_matched_teachers
       CROSS JOIN session
       WHERE skill_match_condition AND availability_checks
       GROUP BY teacher_id
     )
SELECT teacher.*,
       availability_status,
       available_sessions,
       availability_percentage,
       conflict_breakdown
FROM skill_matched_teachers
LEFT JOIN session_conflicts
ORDER BY contract_type, available_sessions DESC, has_general_skill DESC
```

**Performance:** ~100-120ms (20% faster than old approach)

**Status:** ⏳ TODO

---

#### ValidationService (Class Completeness Validation)
**Interface:** `services/ValidationService.java` ⏳ TODO (create new)
**Implementation:** `services/impl/ValidationServiceImpl.java` ⏳ TODO (create new)

**Key Methods:**
- `validateClassComplete(Long classId)` - **STEP 6**: Validate all requirements met

**Validation Checks:**
```java
FUNCTION validateClassComplete(classId):
    errors ← empty list
    warnings ← empty list

    // Check 1: All sessions have timeslot
    IF COUNT sessions WHERE time_slot_template_id IS NULL > 0:
        ADD error: "{count} sessions missing timeslot"

    // Check 2: All sessions have resource
    IF COUNT sessions WHERE NOT EXISTS session_resource > 0:
        ADD error: "{count} sessions missing resource"

    // Check 3: All sessions have primary teacher
    IF COUNT sessions WHERE NOT EXISTS teaching_slot > 0:
        ADD error: "{count} sessions missing teacher"

    // Warning: Multiple teachers per skill group
    IF COUNT DISTINCT skill groups with >1 teacher > 0:
        ADD warning: "Using multiple teachers for {count} skill groups"

    // Warning: Start date in past
    IF class.start_date < TODAY:
        ADD warning: "Start date is in the past"

    RETURN {
        isValid: errors.isEmpty(),
        canSubmit: errors.isEmpty(),
        errors: errors,
        warnings: warnings
    }
```

**Status:** ⏳ TODO

---

#### ApprovalService (Approval Workflow)
**Interface:** `services/ApprovalService.java` ⏳ TODO (create new)
**Implementation:** `services/impl/ApprovalServiceImpl.java` ⏳ TODO (create new)

**Key Methods:**
- `submitForApproval(Long classId, Long submitterUserId)` - Set `submitted_at`, notify Center Head
- `approveClass(Long classId, Long approverUserId)` - Update status to SCHEDULED, set `approved_by`/`approved_at`
- `rejectClass(Long classId, String reason, Long rejecterUserId)` - Reset to DRAFT, store `rejection_reason`

**Approval Workflow:**
```sql
-- Submit for approval
UPDATE class
SET submitted_at = NOW(), updated_at = NOW()
WHERE id = :classId;

-- Approve
UPDATE class
SET status = 'scheduled',
    approval_status = 'approved',
    approved_by = :approverUserId,
    approved_at = NOW(),
    updated_at = NOW()
WHERE id = :classId;

-- Reject
UPDATE class
SET status = 'draft',
    approval_status = 'rejected',
    rejection_reason = :reason,
    submitted_at = NULL,
    updated_at = NOW()
WHERE id = :classId;
```

**Status:** ⏳ TODO

---

### 3. DTOs (15+ files)

**Path:** `dtos/class/`

#### Request DTOs

| DTO | Purpose | Fields | Status |
|-----|---------|--------|--------|
| `CreateClassRequest` | **STEP 1**: Create class | branchId, courseId, code, name, modality, startDate, scheduleDays, maxCapacity | ⏳ TODO |
| `AssignTimeSlotsRequest` | **STEP 3**: Assign time slots | assignments: [{dayOfWeek, timeSlotTemplateId}] | ⏳ TODO |
| `AssignResourcesRequest` | **STEP 4**: Assign resources | pattern: [{dayOfWeek, resourceId}] | ⏳ TODO |
| `AssignTeacherRequest` | **STEP 5**: Assign teacher | teacherId, role, sessionIds?, skillSet? | ⏳ TODO |
| `RejectClassRequest` | **STEP 7**: Reject class | reason | ⏳ TODO |

#### Response DTOs

| DTO | Purpose | Fields | Status |
|-----|---------|--------|--------|
| `CreateClassResponse` | Class created + sessions generated | classId, code, sessionsGenerated, status | ⏳ TODO |
| `AssignTimeSlotsResponse` | Time slots assigned | classId, assignedSessions, assignments | ⏳ TODO |
| `AssignResourcesResponse` | Resources assigned with conflicts | classId, totalSessions, successCount, conflictCount, conflicts | ⏳ TODO |
| `AssignTeacherResponse` | Teacher assigned | classId, teacherId, assignedCount, needsSubstitute, remainingSessions | ⏳ TODO |
| `ValidateClassResponse` | Validation result | isValid, canSubmit, checks, errors, warnings | ⏳ TODO |
| `SubmitClassResponse` | Class submitted | classId, status, submittedAt, submittedBy | ⏳ TODO |
| `ApproveClassResponse` | Class approved | classId, status, approvedBy, approvedAt | ⏳ TODO |
| `RejectClassResponse` | Class rejected | classId, status, rejectionReason, rejectedBy | ⏳ TODO |

#### Utility DTOs

| DTO | Purpose | Fields | Status |
|-----|---------|--------|--------|
| `TeacherAvailability` | PRE-CHECK teacher info | teacherId, fullName, skills, hasGeneralSkill, availabilityStatus, totalSessions, availableSessions, availabilityPercentage, conflicts | ⏳ TODO |
| `ResourceConflict` | Resource conflict detail | sessionId, date, conflictType, reason, conflictingClassCode | ⏳ TODO |
| `AutoPropagateResult` | Resource auto-propagation result | successCount, conflictCount, conflicts | ⏳ TODO |
| `TimeSlotTemplate` | Time slot info | id, name, startTime, endTime, durationMin | ⏳ TODO |
| `AvailableResource` | Available resource info | id, code, name, resourceType, capacity, isAvailable | ⏳ TODO |

#### Enum DTOs

| Enum | Values | Status |
|------|--------|--------|
| `ClassStatus` | DRAFT, SCHEDULED, ONGOING, COMPLETED, CANCELLED | ⏳ TODO |
| `ApprovalStatus` | PENDING, APPROVED, REJECTED | ⏳ TODO |
| `Modality` | ONLINE, OFFLINE, HYBRID | ⏳ TODO |
| `ResourceType` | ROOM, ONLINE_ACCOUNT, EQUIPMENT | ⏳ TODO |

---

### 4. Entity Verification

#### ClassEntity
**Path:** `entities/ClassEntity.java`

**Fields to Verify (should exist):**
```java
@Column(name = "submitted_at")
private LocalDateTime submittedAt;

@Column(name = "decided_by")  // Maps to approved_by in database
private Long decidedBy;

@Column(name = "decided_at")  // Maps to approved_at in database
private LocalDateTime decidedAt;

@Column(name = "rejection_reason", columnDefinition = "TEXT")
private String rejectionReason;

@Enumerated(EnumType.STRING)
@Column(name = "approval_status", nullable = false)
private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
```

**Purpose:** Verify approval workflow fields exist and map correctly to database schema

**Status:** ⏳ TODO (verify existing entity)

---

#### Session Entity
**Path:** `entities/Session.java`

**Existing Fields (Verify):**
- `time_slot_template_id` (FK to TimeSlotTemplate)
- `date` (calculated date)
- `course_session_id` (FK to CourseSession template)
- `type` (enum: 'class', 'makeup', etc.)
- `status` (enum: 'planned', 'completed', etc.)

**Status:** ⏳ TODO (verify fields)

---

#### SessionResource Entity (Junction Table)
**Path:** `entities/SessionResource.java`

**Fields to Verify (should exist):**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@ManyToOne
@JoinColumn(name = "session_id", nullable = false)
private Session session;

@Enumerated(EnumType.STRING)
@Column(name = "resource_type", nullable = false)
private ResourceType resourceType;

@Column(name = "resource_id", nullable = false)
private Long resourceId;
```

**Purpose:** Verify junction table exists for resource assignments (supports HYBRID auto-propagation)

**Status:** ⏳ TODO (verify existing entity)

---

#### TeachingSlot Entity
**Path:** `entities/TeachingSlot.java`

**Existing Fields (Verify):**
- `session_id` (FK to Session)
- `teacher_id` (FK to Teacher)
- `status` (enum: 'scheduled', 'on_leave', etc.)
- `teaching_role` (enum: 'primary', 'substitute', etc.)

**Status:** ⏳ TODO (verify fields)

---

#### TeacherAvailability Entity
**Path:** `entities/TeacherAvailability.java`

**Fields to Verify (should exist):**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

@ManyToOne
@JoinColumn(name = "teacher_id", nullable = false)
private Teacher teacher;

@Column(name = "day_of_week", nullable = false)
private Integer dayOfWeek; // 1=Mon, 7=Sun (ISODOW)

@ManyToOne
@JoinColumn(name = "time_slot_template_id", nullable = false)
private TimeSlotTemplate timeSlotTemplate;
```

**Purpose:** Verify entity exists for teacher availability tracking (used in PRE-CHECK)

**Status:** ⏳ TODO (verify existing entity)

---

### 5. Repository Methods

#### ClassRepository
**Path:** `repositories/ClassRepository.java`

**New Methods:**
```java
// Step 1: Find by branch + code for uniqueness check
Optional<ClassEntity> findByBranchIdAndCode(Long branchId, String code);

// Step 6: Count sessions missing timeslot/resource/teacher
@Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND s.timeSlotTemplate IS NULL")
long countSessionsWithoutTimeslot(@Param("classId") Long classId);

@Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM SessionResource sr WHERE sr.session.id = s.id)")
long countSessionsWithoutResource(@Param("classId") Long classId);

@Query("SELECT COUNT(s) FROM Session s WHERE s.classEntity.id = :classId AND NOT EXISTS (SELECT 1 FROM TeachingSlot ts WHERE ts.session.id = s.id)")
long countSessionsWithoutTeacher(@Param("classId") Long classId);
```

**Status:** ⏳ TODO

---

#### SessionRepository
**Path:** `repositories/SessionRepository.java`

**New Methods:**
```java
// Step 2: Delete sessions (if regenerating)
void deleteByClassEntityId(Long classId);

// Step 3: Bulk update time slots by day of week
@Modifying
@Query("UPDATE Session s SET s.timeSlotTemplate.id = :timeSlotId, s.updatedAt = CURRENT_TIMESTAMP WHERE s.classEntity.id = :classId AND EXTRACT(ISODOW FROM s.date) = :dayOfWeek")
int updateTimeSlotByDayOfWeek(@Param("classId") Long classId, @Param("dayOfWeek") Integer dayOfWeek, @Param("timeSlotId") Long timeSlotId);

// Step 4: Find unassigned sessions by day of week (for conflict analysis)
@Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId AND EXTRACT(ISODOW FROM s.date) = :dayOfWeek AND NOT EXISTS (SELECT 1 FROM SessionResource sr WHERE sr.session.id = s.id)")
List<Session> findUnassignedSessionsByDayOfWeek(@Param("classId") Long classId, @Param("dayOfWeek") Integer dayOfWeek);

// Step 4: Find conflicting session (for conflict detail)
@Query("SELECT s FROM Session s JOIN SessionResource sr ON sr.session.id = s.id WHERE sr.resourceId = :resourceId AND s.date = :date AND s.timeSlotTemplate.id = :timeSlotId")
Optional<Session> findConflictingSession(@Param("resourceId") Long resourceId, @Param("date") LocalDate date, @Param("timeSlotId") Long timeSlotId);
```

**Status:** ⏳ TODO

---

#### SessionResourceRepository
**Path:** `repositories/SessionResourceRepository.java`

**New Methods:**
```java
// Step 4: Bulk insert available sessions (HYBRID Phase 1)
@Modifying
@Query(value = """
    INSERT INTO session_resource (session_id, resource_type, resource_id)
    SELECT s.id, CAST(:resourceType AS resource_type_enum), :resourceId
    FROM session s
    WHERE s.class_id = :classId
      AND EXTRACT(ISODOW FROM s.date) = :dayOfWeek
      AND s.id NOT IN (SELECT session_id FROM session_resource WHERE resource_id = :resourceId)
      AND NOT EXISTS (
        SELECT 1 FROM session_resource sr2
        JOIN session s2 ON sr2.session_id = s2.id
        WHERE sr2.resource_id = :resourceId
          AND s2.date = s.date
          AND s2.time_slot_template_id = s.time_slot_template_id
      )
    RETURNING session_id
    """, nativeQuery = true)
List<Long> bulkAssignResource(
    @Param("classId") Long classId,
    @Param("dayOfWeek") Integer dayOfWeek,
    @Param("resourceId") Long resourceId,
    @Param("resourceType") String resourceType
);
```

**Status:** ⏳ TODO

---

#### ResourceRepository
**Path:** `repositories/ResourceRepository.java`

**New Methods:**
```java
// Step 4: Query available resources for a session
@Query("""
    SELECT r FROM Resource r
    WHERE r.branch.id = :branchId
      AND r.resourceType = :resourceType
      AND r.capacity >= :minCapacity
      AND NOT EXISTS (
        SELECT 1 FROM SessionResource sr
        JOIN Session s ON sr.session.id = s.id
        WHERE sr.resourceId = r.id
          AND s.date = :date
          AND s.timeSlotTemplate.id = :timeSlotId
      )
    ORDER BY r.capacity ASC
    """)
List<Resource> findAvailableResources(
    @Param("branchId") Long branchId,
    @Param("resourceType") ResourceType resourceType,
    @Param("minCapacity") Integer minCapacity,
    @Param("date") LocalDate date,
    @Param("timeSlotId") Long timeSlotId
);
```

**Status:** ⏳ TODO

---

#### TeachingSlotRepository
**Path:** `repositories/TeachingSlotRepository.java`

**New Methods:**
```java
// Step 5: Direct bulk insert (SIMPLIFIED in v1.1)
@Modifying
@Query(value = """
    INSERT INTO teaching_slot (session_id, teacher_id, status)
    SELECT s.id, :teacherId, CAST('scheduled' AS teaching_slot_status_enum)
    FROM session s
    JOIN course_session cs ON s.course_session_id = cs.id
    WHERE s.class_id = :classId
      AND (
        EXISTS (
          SELECT 1 FROM teacher_skill ts
          WHERE ts.teacher_id = :teacherId
            AND ts.skill = 'general'
        )
        OR EXISTS (
          SELECT 1 FROM teacher_skill ts
          WHERE ts.teacher_id = :teacherId
            AND ts.skill = ANY(cs.skill_set)
        )
      )
      AND (:sessionIds IS NULL OR s.id = ANY(:sessionIds))
    RETURNING session_id
    """, nativeQuery = true)
List<Long> bulkAssignTeacher(
    @Param("classId") Long classId,
    @Param("teacherId") Long teacherId,
    @Param("sessionIds") Long[] sessionIds
);
```

**Status:** ⏳ TODO

---

#### TeacherRepository
**Path:** `repositories/TeacherRepository.java`

**New Methods:**
```java
// Step 5: PRE-CHECK query (complex CTE query)
// See openapi.yaml lines 500-653 for full SQL
@Query(value = """
    WITH skill_matched_teachers AS (
      SELECT t.id, ua.full_name, ua.email, t.employee_code, t.contract_type,
             array_agg(DISTINCT ts.skill ORDER BY ts.skill) as skills,
             array_agg(DISTINCT ts.level ORDER BY ts.level DESC) as skill_levels,
             MAX(ts.level) as max_level,
             COUNT(DISTINCT ts.skill) FILTER (WHERE ts.skill != 'general') as matched_specific_skills,
             bool_or(ts.skill = 'general') as has_general_skill
      FROM teacher t
      JOIN user_account ua ON t.user_account_id = ua.id
      JOIN teacher_skill ts ON t.id = ts.teacher_id
      GROUP BY t.id, ua.full_name, ua.email, t.employee_code, t.contract_type
      HAVING COUNT(ts.skill) > 0
    ),
    session_conflicts AS (
      SELECT smt.id as teacher_id,
             COUNT(*) FILTER (WHERE NOT EXISTS (...)) as no_availability_count,
             COUNT(*) FILTER (WHERE EXISTS (...)) as teaching_conflict_count,
             COUNT(*) FILTER (WHERE EXISTS (...)) as leave_conflict_count,
             COUNT(*) as total_sessions
      FROM skill_matched_teachers smt
      CROSS JOIN session s
      JOIN course_session cs ON s.course_session_id = cs.id
      WHERE s.class_id = :classId
        AND (smt.has_general_skill = true OR EXISTS (...))
      GROUP BY smt.id
    )
    SELECT smt.*, sc.*,
           CASE WHEN ... THEN 'fully_available'
                WHEN ... THEN 'partially_available'
                ELSE 'unavailable' END as availability_status
    FROM skill_matched_teachers smt
    LEFT JOIN session_conflicts sc ON sc.teacher_id = smt.id
    ORDER BY ...
    """, nativeQuery = true)
List<Object[]> findAvailableTeachersWithPrecheck(@Param("classId") Long classId);
```

**Status:** ⏳ TODO

---

#### TimeSlotTemplateRepository
**Path:** `repositories/TimeSlotTemplateRepository.java`

**New Methods:**
```java
// Step 3: Find available time slots for a branch
List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(Long branchId);
```

**Status:** ⏳ TODO

---

### 6. Error Codes (15+ new codes)

**Path:** `exceptions/ErrorCode.java`

**Proposed Error Codes (4000-4099 range for Class):**

| Code | Error | Description |
|------|-------|-------------|
| 4000 | `CLASS_CODE_DUPLICATE` | Class code already exists for this branch |
| 4001 | `CLASS_NOT_FOUND` | Class not found |
| 4002 | `COURSE_NOT_APPROVED` | Course must be approved before creating class |
| 4003 | `START_DATE_NOT_IN_SCHEDULE_DAYS` | Start date must be in schedule_days |
| 4004 | `INVALID_SCHEDULE_DAYS` | Invalid schedule_days (must be 1-7) |
| 4005 | `TIME_SLOT_ASSIGNMENT_FAILED` | Failed to assign time slots |
| 4006 | `RESOURCE_ASSIGNMENT_FAILED` | Failed to assign resources |
| 4007 | `TEACHER_ASSIGNMENT_FAILED` | Failed to assign teacher |
| 4008 | `CLASS_VALIDATION_FAILED` | Class validation failed |
| 4009 | `CLASS_INCOMPLETE_CANNOT_SUBMIT` | Class has incomplete assignments |
| 4010 | `CLASS_NOT_SUBMITTED` | Class not submitted for approval |
| 4011 | `CLASS_ALREADY_APPROVED` | Class already approved |
| 4012 | `REJECTION_REASON_REQUIRED` | Rejection reason is required |
| 4013 | `INVALID_APPROVAL_STATUS` | Invalid approval status transition |
| 4014 | `UNAUTHORIZED_APPROVER` | Only CENTER_HEAD can approve classes |
| 4015 | `SESSIONS_MISSING_TIMESLOT` | {count} sessions missing timeslot |
| 4016 | `SESSIONS_MISSING_RESOURCE` | {count} sessions missing resource |
| 4017 | `SESSIONS_MISSING_TEACHER` | {count} sessions missing teacher |

**Pattern:** All use `throw new CustomException(ErrorCode.XXX)`

**Status:** ⏳ TODO

---

## IMPLEMENTATION NOTES

### 1. Session Date Calculation Algorithm

**Key Points:**
- Use Java `LocalDate` API with `DayOfWeek` enum
- PostgreSQL ISODOW standard: 1=Monday, 7=Sunday
- Cycle through `scheduleDays` array using modulo operator
- Handle week rollover when all days in week are consumed

**Java Implementation Sketch:**
```java
public List<Session> generateSessions(ClassEntity classEntity, List<CourseSession> courseSessions) {
    LocalDate currentDate = classEntity.getStartDate();
    List<Integer> scheduleDays = classEntity.getScheduleDays();
    List<Session> sessions = new ArrayList<>();
    int sessionIndex = 0;

    for (CourseSession courseSession : courseSessions) {
        int targetDayOfWeek = scheduleDays.get(sessionIndex % scheduleDays.size());

        // Find next occurrence of target day
        while (currentDate.getDayOfWeek().getValue() != targetDayOfWeek) {
            currentDate = currentDate.plusDays(1);
        }

        Session session = Session.builder()
            .classEntity(classEntity)
            .courseSession(courseSession)
            .date(currentDate)
            .type(SessionType.CLASS)
            .status(SessionStatus.PLANNED)
            .build();

        sessions.add(session);
        sessionIndex++;
        currentDate = currentDate.plusDays(1);
    }

    return sessions;
}
```

---

### 2. HYBRID Auto-propagation Strategy

**Why HYBRID?**
- **Pure SQL**: Fast (~50ms) but no detailed conflict info
- **Pure Java**: Detailed (~2-3s) but slow
- **HYBRID**: Fast (~100-200ms) + Detailed conflict reporting

**Implementation:**
1. **Phase 1 (SQL)**: Bulk INSERT 90% of sessions using `NOT EXISTS` subquery
2. **Phase 2 (Java)**: Find remaining unassigned sessions, analyze conflicts

**Benefits:**
- Fast execution for majority of sessions
- Actionable error messages for conflicts
- Academic Staff can resolve conflicts manually

---

### 3. PRE-CHECK vs Old Approach

**Old Approach (trial-and-error):**
```
1. Show all teachers (no availability info)
2. User selects teacher
3. Try to assign
4. Failed → Show conflicts
5. User tries another teacher
6. Repeat until success
```

**New PRE-CHECK Approach (v1.1):**
```
1. Query teachers WITH availability status
   - Show: "Jane Doe: 10/10 ✅" or "John Smith: 7/10 ⚠️, 3 conflicts"
2. User selects teacher with full visibility
3. Direct assignment SUCCESS (no re-checking)
```

**Benefits:**
- No trial-and-error
- Better UX (informed decision)
- 20% faster (120ms vs 200ms)
- Simpler code (no Phase 2/3 conflict analysis)

---

### 4. 'general' Skill = UNIVERSAL Skill

**Fixed in v1.1:**
- Old: 'general' skill counted as 1 skill, limited matching
- New: 'general' skill can teach ANY session (universal)

**Logic:**
```sql
WHERE (
  EXISTS (SELECT 1 FROM teacher_skill WHERE skill = 'general')  -- Can teach anything
  OR
  EXISTS (SELECT 1 FROM teacher_skill WHERE skill = ANY(session.skill_set))  -- Specific match
)
```

**Example:**
- Teacher Bob has 'general' skill (level 5)
- Can teach: Listening sessions, Speaking sessions, Writing sessions, ANY session!
- No more skill mismatch issues

---

### 5. Transaction Management

**All write operations in single transaction:**
- **@Transactional(readOnly = true)** at service class level (default)
- **@Transactional** on write operations (creates, updates, deletes)
- Rollback on any error

**Example:**
```java
@Transactional
public ClassEntity createClass(CreateClassRequest request) {
    // 1. Create class
    ClassEntity classEntity = classRepository.save(...);

    // 2. Generate sessions (auto-triggered)
    List<Session> sessions = sessionGenerationService.generateSessionsForClass(classEntity, course);
    sessionRepository.saveAll(sessions);

    // If any error occurs, entire transaction rolls back
    return classEntity;
}
```

---

### 6. Performance Benchmarks

| Operation | Pure Java | Pure SQL | HYBRID (Recommended) |
|-----------|-----------|----------|---------------------|
| **Generate 36 sessions** | 50ms | N/A | 50ms |
| **Assign timeslots (3 days)** | 500ms (144 queries) | 20ms (3 queries) | **20ms** ⏳ |
| **Assign resources (3 days)** | 2-3s (144 queries) | 50ms (no conflicts) | **150ms + conflicts** ⏳ |
| **Assign teachers (old)** | 3-5s (216 queries) | 80ms (no conflicts) | 200ms + conflicts |
| **Assign teachers (v1.1 PRE-CHECK)** | N/A | N/A | **120ms** ⏳ |
| **Total workflow (v1.1)** | ~8-10s | ~200ms ❌ | **~400ms** ⏳ |

**Winner: HYBRID with PRE-CHECK** - Fast + Detailed + Great UX

---

## TESTING COMMANDS

```bash
# Run all class-related tests
mvn test -Dtest=*Class*

# Run specific test class
mvn test -Dtest=SessionGenerationServiceImplTest

# Run with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html  # macOS
start target/site/jacoco/index.html  # Windows
```

---

## WHAT'S NOT IMPLEMENTED (Future Enhancements)

### Future Features
- Email notifications (submit/approve/reject events)
- Bulk class operations (clone class, bulk approve)
- Class schedule visualization (calendar view)
- Conflict resolution wizard (auto-suggest alternative resources/teachers)
- Approval delegation (CENTER_HEAD delegates to MANAGER)
- Class history/audit trail (track all changes)
- Draft auto-save (prevent data loss)

### Known Limitations
- No cross-branch class validation (student enrolled in overlapping classes at different branches)
- No teacher workload limit (teacher can be assigned unlimited classes)
- No resource maintenance schedule (block resources for maintenance)
- No auto-scheduling (suggest optimal time slots based on availability)
- Override threshold (20%) hardcoded (not configurable)

---

## API DOCUMENTATION

**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

**Test Credentials:**
- Academic Affairs: ACADEMIC_AFFAIR role with branch assignments
- Center Head: CENTER_HEAD role
- JWT token required in `Authorization: Bearer {token}` header

---

## CONFIGURATION

### application.yml
```yaml
# No additional config needed
# Uses existing Spring Data JPA + PostgreSQL setup
```

### Dependencies (Already in pom.xml)
- Spring Data JPA (with pessimistic locking)
- PostgreSQL (enum types via enum-init.sql)
- Lombok (boilerplate reduction)
- Spring Security (JWT authentication)

---

## IMPLEMENTATION PRIORITY

### Phase 1: Core Foundation (High Priority)
1. ⏳ **STEP 1**: Create Class (Entry point of workflow)
2. ⏳ **STEP 2**: Session auto-generation (Auto-triggered by STEP 1)
3. ⏳ **STEP 3**: Assign Time Slots (Required before assignments)

**Reason:** Build foundation first - need class and sessions before any assignments

### Phase 2: Assignment Features (High Priority - Complex)
4. ⏳ **STEP 4**: Assign Resources (HYBRID auto-propagation) - Creates actual session pool
5. ⏳ **STEP 5 Foundation**: Teacher Availability entity/repository
6. ⏳ **STEP 5 PRE-CHECK**: Query available teachers with availability
7. ⏳ **STEP 5 ASSIGNMENT**: Direct teacher assignment

**Reason:** Resource assignment first creates the actual session pool that teacher assignment works with; Cannot assign teachers to sessions without resources

### Phase 3: Workflow Completion (Medium Priority)
8. ⏳ **STEP 6**: Validate Class (Before submission)
9. ⏳ **STEP 7**: Submit & Approve (Final workflow step)

**Reason:** Complete workflow after all assignment features working

### Phase 4: Testing & Polish (Ongoing)
11. ⏳ Unit tests for all services
12. ⏳ Integration tests for controllers
13. ⏳ Error handling and validation
14. ⏳ OpenAPI documentation
15. ⏳ Performance optimization

---

## SUCCESS CRITERIA

**Definition of Done:**
- ⏳ All 7 steps implemented and tested
- ⏳ Academic Staff can create class from start to approval
- ⏳ Center Head can approve/reject classes
- ⏳ PRE-CHECK shows teacher availability before assignment
- ⏳ HYBRID auto-propagation works for resources
- ⏳ Validation prevents incomplete submissions
- ⏳ All tests pass with 80%+ coverage
- ⏳ OpenAPI documentation complete
- ⏳ Performance: Total workflow < 500ms

**Acceptance Test Scenario:**
```
1. Academic Staff creates class "ENG-A1-2024-01" (Mon/Wed/Fri, 20 students)
2. System generates 36 sessions automatically
3. Academic Staff assigns time slots (Mon/Wed: Morning, Fri: Afternoon)
4. Academic Staff assigns Room 101 (auto-propagates to 33 sessions, 3 conflicts)
5. Academic Staff resolves 3 conflicts manually
6. Academic Staff queries available teachers → sees "Jane: 10/10 ✅, John: 7/10 ⚠️"
7. Academic Staff assigns Jane to all 10 Listening/Reading sessions
8. Validation passes: All 36 sessions have timeslot + resource + teacher
9. Academic Staff submits class
10. Center Head approves class → Status: SCHEDULED
11. Class ready for student enrollment ⏳
```

---

## HANDOFF TO FRONTEND

**When backend is ready, provide:**
1. ⏳ OpenAPI spec (`openapi.yaml`)
2. ⏳ Postman collection (export from Swagger)
3. ⏳ Sample JWT tokens for testing
4. ⏳ Seed data script (demo classes in various states)
5. ⏳ Error code reference (for error handling)
6. ⏳ WebSocket events (for real-time progress, if implemented)

**Frontend responsibilities:**
- 7-step wizard UI (stepper component)
- Conflict resolution UI (show ResourceConflict/TeacherConflict details)
- Teacher availability visualization (10/10 ✅ vs 7/10 ⚠️)
- Validation error display (errors/warnings from validation endpoint)
- Approval workflow UI (Center Head dashboard)

---

## NEXT STEPS

1. ⏳ **Review this plan** with Product Owner and Tech Lead
2. ⏳ **Create Jira tickets** for each implementation component
3. ⏳ **Verify existing entities** match database schema (no migrations needed)
4. ⏳ **Start with Phase 1** (Core Workflow - STEP 1,2,3,6,7)
5. ⏳ **Write tests first** (TDD approach, like enrollment implementation)
6. ⏳ **Implement services** following enrollment pattern
7. ⏳ **Integrate with controllers** and test via Postman
8. ⏳ **Document as you go** (update this file with Status: ⏳ TODO)

---

## CHANGELOG

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-11-05 | 1.0.0 | Initial implementation plan created based on workflow v1.1 and OpenAPI spec | Claude |

---

**Ready to implement? Let's start with Phase 1! 🚀**
