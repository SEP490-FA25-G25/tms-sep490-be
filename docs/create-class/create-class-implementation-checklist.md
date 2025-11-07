# CREATE CLASS WORKFLOW - IMPLEMENTATION CHECKLIST

**Status:** 📋 IN PROGRESS - PHASE 1.1, 1.2, 1.3, 1.4 & 1.5 COMPLETED ✅
**Last Updated:** 2025-11-07
**Reference:** `create-class-implementation-plan.md`

---

## 🚨 CRITICAL FIXES APPLIED (Phase 1.1 & 1.2)

### Issues Resolved:
1. **HQL Syntax Errors**: `EXTRACT(ISODOW FROM s.date)` không được HQL hỗ trợ → **FIXED** bằng native PostgreSQL query
2. **JPA Entity Navigation**: `cs.course.id` không tồn tại → **FIXED** thành `cs.phase.course.id`
3. **Method Naming**: `sequence` field không tồn tại → **FIXED** thành `sequenceNo`
4. **Schedule Days Format**: ISODOW vs PostgreSQL DOW mismatch → **FIXED** với PostgreSQL DOW (0=Sunday, ..., 6=Saturday)
5. **JaCoCo Version**: Java 21 compatibility → **FIXED** downgrade từ 0.8.12 → 0.8.11

### ✅ Application Status:
- **Build**: ✅ Spring Boot builds successfully (0 compilation errors)
- **Database**: ✅ All queries working with PostgreSQL
- **API**: ✅ Create Class, Time Slot Assignment, Validation & Approval endpoints functional
- **Testing**: ✅ Phase 1.5 COMPLETED - Ready for Phase 2 testing

---

## QUICK PROGRESS OVERVIEW

```
Phase 1: Core Foundation         [████████████████] 5/5 (100%) ✅ 1.1, 1.2, 1.3, 1.4 & 1.5 COMPLETED
Phase 2: Assignment Features     [░░░░░░░░░░] 0/4 (0%)
Phase 3: Teacher Availability    [░░░░░░░░░░] 0/1 (0%)
Phase 4: Polish & Finalization   [░░░░░░░░░░] 0/4 (0%)

Overall Progress:                [████████░░░░] 5/14 (36%) - Phase 2 Ready
```

---

## PHASE 1: CORE FOUNDATION (STEP 1, 2, 3, 6, 7)

### 1.1 Create Class (STEP 1) ⏳ TODO

**Priority:** 🔴 HIGH (Entry point of workflow)

- [x] **DTOs:**

  - [x] Create `dtos/class/CreateClassRequest.java`
    - [x] Add validation annotations (@NotNull, @NotBlank, @Valid)
    - [x] Fields: branchId, courseId, code, name, modality, startDate, scheduleDays, maxCapacity
    - [x] Additional validation methods: isValid(), getPrimaryScheduleDay(), includesWeekends()
  - [x] Create `dtos/class/CreateClassResponse.java`
    - [x] Fields: classId, code, sessionsGenerated, status, approvalStatus, createdAt
    - [x] SessionGenerationSummary nested class with detailed session info
    - [x] Helper methods: isSuccess(), hasSessions(), isReadyForNextStep(), getWorkflowProgress()

- [x] **Error Codes:** Add to `exceptions/ErrorCode.java`

  - [x] 4010: `CLASS_CODE_DUPLICATE`
  - [x] 4011: `COURSE_NOT_APPROVED`
  - [x] 4012: `START_DATE_NOT_IN_SCHEDULE_DAYS`
  - [x] 4013: `INVALID_SCHEDULE_DAYS`
  - [x] Additional workflow errors for future steps (4014-4026)

- [x] **Repository:** Update `ClassRepository.java`

  - [x] Add method: `Optional<ClassEntity> findByBranchIdAndCode(Long, String)`
  - [x] Add validation methods: countSessionsWithoutTimeslot(), countSessionsWithoutResource(), countSessionsWithoutTeacher()

- [x] **Service:** Update `ClassService.java` interface

  - [x] Add method: `CreateClassResponse createClass(CreateClassRequest, Long userId)`

- [x] **Service Implementation:** Update `ClassServiceImpl.java`

  - [x] Validate user has access to branch using userBranchesRepository
  - [x] Validate course is approved and active (CourseStatus.APPROVED)
  - [x] Validate start_date is in schedule_days using ISODOW format
  - [x] Check uniqueness: (branch_id, code) using findByBranchIdAndCode
  - [x] Create ClassEntity with status=DRAFT, approvalStatus=PENDING
  - [x] Call SessionGenerationService to generate sessions
  - [x] Create Session entities from generation response linked to CourseSession templates
  - [x] Save class + sessions in @Transactional
  - [x] Return comprehensive CreateClassResponse with session summary

- [x] **Controller:** Update `ClassController.java`
  - [x] Add endpoint: `POST /api/v1/classes`
  - [x] Add @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
  - [x] Add comprehensive OpenAPI annotations with workflow documentation
  - [x] Return ResponseObject<CreateClassResponse>
  - [x] Include detailed workflow steps and example requests in API docs

**Estimated Time:** 6-8 hours

---

### 1.2 Session Generation Service (STEP 2) ✅ COMPLETED

**Priority:** 🔴 HIGH (Auto-triggered by STEP 1)

- [x] **Interface:** Create `services/SessionGenerationService.java`

  - [x] Define method: `List<Session> generateSessionsForClass(ClassEntity classEntity, Course course)`
  - [x] Add proper Javadoc documentation

- [x] **Implementation:** Create `services/impl/SessionGenerationServiceImpl.java`

  - [x] Implement date calculation algorithm (from workflow lines 42-92)
  - [x] Use LocalDate API with DayOfWeek enum
  - [x] Handle ISODOW format (1=Monday, 7=Sunday)
  - [x] Cycle through scheduleDays array using modulo operator
  - [x] Handle week rollover when all days consumed
  - [x] Link each session to CourseSession template (course_session_id)
  - [x] Set default: type=CLASS, status=PLANNED
  - [x] Return list of Session entities (not saved yet)

- [x] **Repository:** Verify `SessionRepository.java` exists

  - [x] Verify standard JpaRepository methods
  - [x] Will add custom methods in later phases

- [x] **Repository:** Update `CourseSessionRepository.java` exists

  - [x] Add method: `findByCourseIdOrderByPhaseAscSequenceAsc`
  - [x] Add method: `countByCourseId`

- [x] **Integration:** Called from ClassServiceImpl.java

  - [x] ClassServiceImpl calls SessionGenerationService
  - [x] Generated sessions saved in same @Transactional block as class creation
  - [x] Return session count in CreateClassResponse

**Estimated Time:** 4-6 hours

---

### 1.3 Assign Time Slots (STEP 3) ✅ COMPLETED

**Priority:** 🔴 HIGH (Required before resource/teacher assignment)

- [x] **DTOs:** ✅ **COMPLETED**

  - [x] Create `dtos/createclass/AssignTimeSlotsRequest.java` ✅ **COMPLETED**
    - [x] Nested class: `TimeSlotAssignment` (dayOfWeek, timeSlotId) ✅ **COMPLETED**
    - [x] Field: `List<TimeSlotAssignment> assignments` ✅ **COMPLETED**
    - [x] Validation annotations (@NotNull, @Valid) ✅ **COMPLETED**
  - [x] Create `dtos/createclass/AssignTimeSlotsResponse.java` ✅ **COMPLETED**
    - [x] Fields: success, message, classId, sessionsUpdated, assignmentDetails ✅ **COMPLETED**
    - [x] Nested AssignmentDetail class with comprehensive information ✅ **COMPLETED**
    - [x] Helper method: isSuccess() ✅ **COMPLETED**

- [x] **Repository:** Update `SessionRepository.java` ✅ **COMPLETED - ISSUES FIXED**

  - [x] Add method: `int updateTimeSlotByDayOfWeek(Long classId, Integer dayOfWeek, Long timeSlotId)`
  - [x] **FIXED:** Use native PostgreSQL query with EXTRACT(DOW FROM date) instead of HQL
  - [x] **FIXED:** PostgreSQL DOW format (0=Sunday, 1=Monday, ..., 6=Saturday)
  - [x] Add @Transactional annotation for proper transaction management
  - [x] Add countByClassEntityId() method for validation

- [x] **Repository:** Update `TimeSlotTemplateRepository.java` ✅ **COMPLETED**

  - [x] Add method: `List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(Long branchId)`
  - [x] Add proper @Query annotation for branch filtering and ordering

- [x] **Service:** Update `ClassService.java` interface ✅ **COMPLETED**

  - [x] Add method: `AssignTimeSlotsResponse assignTimeSlots(Long classId, AssignTimeSlotsRequest request, Long userId)` ✅ **COMPLETED**

- [x] **Service Implementation:** Update `ClassServiceImpl.java` ✅ **COMPLETED**

  - [x] Add TimeSlotTemplateRepository dependency ✅ **COMPLETED**
  - [x] Validate request parameters and user branch access ✅ **COMPLETED**
  - [x] Validate time slot belongs to class's branch ✅ **COMPLETED**
  - [x] Loop through assignments, call updateTimeSlotByDayOfWeek for each ✅ **COMPLETED**
  - [x] Prevent duplicate day assignments ✅ **COMPLETED**
  - [x] **FIXED:** Use PostgreSQL DOW standard (0=Sunday, 1=Monday, ..., 6=Saturday) for day validation ✅ **COMPLETED**
  - [x] Create comprehensive response with assignment details ✅ **COMPLETED**
  - [x] Add proper error handling and logging ✅ **COMPLETED**

- [x] **Controller:** Update `ClassController.java` ✅ **COMPLETED**

  - [x] Add endpoint: `POST /api/v1/classes/{classId}/time-slots` ✅ **COMPLETED**
  - [x] Add @PreAuthorize("hasRole('ACADEMIC_AFFAIR')") ✅ **COMPLETED**
  - [x] Add comprehensive OpenAPI documentation with workflow context ✅ **COMPLETED**
  - [x] Include example requests and response documentation ✅ **COMPLETED**

- [x] **Controller:** Create `ResourceController.java` ✅ **COMPLETED**
  - [x] Add endpoint: `GET /api/v1/branches/{branchId}/time-slot-templates` ✅ **COMPLETED**
  - [x] Return list of available time slots ordered by start time ✅ **COMPLETED**
  - [x] Add proper security annotations and documentation ✅ **COMPLETED**

**Estimated Time:** 3-4 hours

---

### 1.4 Validation Service (STEP 6) ✅ COMPLETED

**Priority:** 🟡 MEDIUM (Blocks submission)

- [x] **DTOs:** ✅ **COMPLETED**

  - [x] Create `dtos/classmanagement/ValidateClassResponse.java` ✅ **COMPLETED**
    - [x] Fields: valid, canSubmit, classId, message, checks, errors, warnings ✅ **COMPLETED**
    - [x] Nested class: `ValidationChecks` with comprehensive details ✅ **COMPLETED**
    - [x] Helper methods: isValid(), canSubmit() ✅ **COMPLETED**

- [x] **Interface:** Create `services/ValidationService.java` ✅ **COMPLETED**

  - [x] Define method: `ValidateClassResponse validateClassComplete(Long classId)` ✅ **COMPLETED**

- [x] **Implementation:** Create `services/impl/ValidationServiceImpl.java` ✅ **COMPLETED**

  - [x] Check 1: All sessions have timeslot assignment ✅ **COMPLETED**
  - [x] Check 2: All sessions have resource assignment ✅ **COMPLETED**
  - [x] Check 3: All sessions have teacher assignment ✅ **COMPLETED**
  - [x] Warning: Multiple teachers per skill group ✅ **COMPLETED**
  - [x] Warning: Start date in past ✅ **COMPLETED**
  - [x] Completion percentage calculation (0-100%) ✅ **COMPLETED**
  - [x] Return comprehensive ValidateClassResponse ✅ **COMPLETED**

- [x] **Repository:** Update `SessionRepository.java` ✅ **COMPLETED**

  - [x] Add method: `long countByClassEntityId(Long classId)` ✅ **ALREADY EXISTED**
  - [x] Add validation count methods for sessions with/without assignments ✅ **COMPLETED**

- [x] **Service:** Update `ClassService.java` interface ✅ **COMPLETED**

  - [x] Add method: `ValidateClassResponse validateClass(Long classId, Long userId)` ✅ **COMPLETED**

- [x] **Service Implementation:** Update `ClassServiceImpl.java` ✅ **COMPLETED**

  - [x] Add ValidationService dependency ✅ **COMPLETED**
  - [x] Validate class exists and user has access ✅ **COMPLETED**
  - [x] Delegate to ValidationService ✅ **COMPLETED**
  - [x] Return response with proper error handling ✅ **COMPLETED**

- [x] **Controller:** Update `ClassController.java` ✅ **COMPLETED**
  - [x] Add endpoint: `POST /api/v1/classes/{classId}/validate` ✅ **COMPLETED**
  - [x] Add @PreAuthorize("hasRole('ACADEMIC_AFFAIR')") ✅ **COMPLETED**
  - [x] Add comprehensive OpenAPI documentation with workflow context ✅ **COMPLETED**
  - [x] Include detailed validation checks and use cases ✅ **COMPLETED**

**Estimated Time:** 2-3 hours

---

### 1.5 Approval Service (STEP 7) ✅ COMPLETED

**Priority:** 🟡 MEDIUM (Final step)

- [x] **DTOs:** ✅ **COMPLETED**

  - [x] Create `dtos/classmanagement/SubmitClassResponse.java` ✅ **COMPLETED**
  - [x] Create `dtos/classmanagement/RejectClassRequest.java` ✅ **COMPLETED**
  - [x] Create `dtos/classmanagement/RejectClassResponse.java` ✅ **COMPLETED**

- [x] **Enum:** ✅ **ALREADY EXISTED**

  - [x] Values: PENDING, APPROVED, REJECTED ✅ **ALREADY EXISTED**

- [x] **Entity:** ✅ **VERIFIED EXISTING**

  - [x] Verify field: `OffsetDateTime submittedAt` exists ✅ **VERIFIED**
  - [x] Verify field: `UserAccount decidedBy` exists ✅ **VERIFIED**
  - [x] Verify field: `OffsetDateTime decidedAt` exists ✅ **VERIFIED**
  - [x] Verify field: `String rejectionReason` exists ✅ **VERIFIED**
  - [x] Verify field: `ApprovalStatus approvalStatus` exists with default PENDING ✅ **VERIFIED**
  - [x] Verify @Enumerated(EnumType.STRING) annotations match VARCHAR columns ✅ **VERIFIED**

- [x] **Error Codes:** ✅ **ALREADY EXISTED**

  - [x] 4018: `CLASS_INCOMPLETE_CANNOT_SUBMIT` ✅ **ALREADY EXISTED**
  - [x] 4019: `CLASS_NOT_SUBMITTED` ✅ **ALREADY EXISTED**
  - [x] 4020: `CLASS_ALREADY_APPROVED` ✅ **ALREADY EXISTED**
  - [x] 4021: `CLASS_REJECTION_REASON_REQUIRED` ✅ **ALREADY EXISTED**
  - [x] 4022: `INVALID_APPROVAL_STATUS` ✅ **ALREADY EXISTED**
  - [x] 4023: `UNAUTHORIZED_APPROVER` ✅ **ALREADY EXISTED**

- [x] **Interface:** ✅ **COMPLETED**

  - [x] Define method: `SubmitClassResponse submitForApproval(Long classId, Long submitterUserId)` ✅ **COMPLETED**
  - [x] Define method: `void approveClass(Long classId, Long approverUserId)` ✅ **COMPLETED**
  - [x] Define method: `RejectClassResponse rejectClass(Long classId, String reason, Long rejecterUserId)` ✅ **COMPLETED**

- [x] **Implementation:** ✅ **COMPLETED**

  - [x] submitForApproval: Validate complete → Set submitted_at ✅ **COMPLETED**
  - [x] approveClass: Validate submitted → Set status=SCHEDULED, approved_by, approved_at ✅ **COMPLETED**
  - [x] rejectClass: Validate submitted → Set status=DRAFT, rejection_reason, reset submitted_at ✅ **COMPLETED**

- [x] **Service:** ✅ **COMPLETED**

  - [x] Add method: `SubmitClassResponse submitClass(Long classId, Long userId)` ✅ **COMPLETED**
  - [x] Add method: `void approveClass(Long classId, Long approverUserId)` ✅ **COMPLETED**
  - [x] Add method: `RejectClassResponse rejectClass(Long classId, String reason, Long rejecterUserId)` ✅ **COMPLETED**

- [x] **Service Implementation:** ✅ **COMPLETED**

  - [x] Delegate to ApprovalService ✅ **COMPLETED**
  - [x] Add authorization checks ✅ **COMPLETED**

- [x] **Controller:** ✅ **COMPLETED**
  - [x] Add endpoint: `POST /api/v1/classes/{classId}/submit` (ACADEMIC_AFFAIR) ✅ **COMPLETED**
  - [x] Add endpoint: `POST /api/v1/classes/{classId}/approve` (CENTER_HEAD) ✅ **COMPLETED**
  - [x] Add endpoint: `POST /api/v1/classes/{classId}/reject` (CENTER_HEAD) ✅ **COMPLETED**

**Estimated Time:** 4-5 hours

---

## PHASE 2: ASSIGNMENT FEATURES (STEP 4, 5)

### 2.1 Resource Assignment Service (HYBRID) ⏳ TODO

**Priority:** 🔴 HIGH (Creates actual session pool for teacher assignment)

- [ ] **DTOs:**

  - [ ] Create `dtos/class/AssignResourcesRequest.java`
  - [ ] Create `dtos/class/AssignResourcesResponse.java`
  - [ ] Create `dtos/class/ResourceConflict.java`
  - [ ] Create `dtos/class/AutoPropagateResult.java`
  - [ ] Create `dtos/class/AvailableResource.java`

- [ ] **Entity:** Verify `SessionResource.java` exists

  - [ ] Verify fields: session, resource exist
  - [ ] Verify UNIQUE constraint (session_id, resource_id)
  - [ ] Check if additional fields needed: assignedBy, assignmentMethod, notes, audit timestamps
  - [ ] Verify helper methods exist or add if needed

- [ ] **Error Codes:**

  - [ ] 4006: `RESOURCE_ASSIGNMENT_FAILED`
  - [ ] Additional resource assignment error codes (4015-4026)

- [ ] **Repository:** Create `SessionResourceRepository.java`

  - [ ] Add method: `List<Long> bulkAssignResource()` with HYBRID Phase 1 SQL bulk operations
  - [ ] Add conflict detection methods: `findConflictingSession()`, `isResourceAvailable()`
  - [ ] Add assignment statistics and management methods

- [ ] **Repository:** Update `SessionRepository.java`

  - [ ] Add method: `findUnassignedSessionsByDayOfWeek()` for HYBRID Phase 2 analysis
  - [ ] Add method: `countSessionsWithoutResource()` for validation
  - [ ] Add session summary and conflict checking methods

- [ ] **Repository:** Update `ResourceRepository.java`

  - [ ] Add method: `findAvailableResources()` with real-time conflict detection
  - [ ] Add branch-based filtering and capacity validation methods
  - [ ] Add resource availability and statistics methods

- [ ] **Interface:** Create `services/ResourceAssignmentService.java`

  - [ ] Define method: `assignResourcesWithPropagation()` with HYBRID approach
  - [ ] Define method: `queryAvailableResources()` for manual conflict resolution
  - [ ] Define validation, statistics, and management methods

- [ ] **Implementation:** Create `services/impl/ResourceAssignmentServiceImpl.java`

  - [ ] Phase 1 (SQL Bulk): Bulk INSERT non-conflict sessions using native SQL
  - [ ] Phase 2 (Java Analysis): Find unassigned sessions, analyze conflicts with detailed reporting
  - [ ] Return AutoPropagateResult with success/conflict counts and processing statistics
  - [ ] Comprehensive validation and error handling

- [ ] **Service:** Update `ClassService.java` interface

  - [ ] Add method: `assignResources()` with HYBRID auto-propagation
  - [ ] Add method: `getAvailableResourcesForSession()` for manual conflict resolution

- [ ] **Service Implementation:** Update `ClassServiceImpl.java`

  - [ ] Validate class exists and user has access
  - [ ] Delegate to ResourceAssignmentService with proper error handling
  - [ ] Comprehensive logging and exception handling

- [ ] **Controller:** Update `ClassController.java`

  - [ ] Add endpoint: `POST /api/v1/classes/{classId}/resources/assign`
  - [ ] Add endpoint: `GET /api/v1/classes/{classId}/sessions/{sessionId}/resources/available`
  - [ ] Comprehensive OpenAPI documentation with HYBRID workflow details

- [ ] **Controller:** ResourceController.java
  - [ ] Available resources functionality exposed through ClassController (better architectural design)
  - [ ] ResourceController remains focused on TimeSlotTemplate operations

**Estimated Time:** 4-6 hours

---

### 2.2 Teacher Availability Entity ⏳ TODO

**Priority:** 🔴 HIGH (Foundation for PRE-CHECK)

- [ ] **Entity:** Verify `TeacherAvailability.java`

  - [ ] Verify fields: teacher, dayOfWeek, timeSlotTemplate, effectiveDate, note
  - [ ] Verify UNIQUE constraint (teacher_id, day_of_week, time_slot_template_id)
  - [ ] Verify extends BaseEntity for automatic timestamps
  - [ ] Verify helper methods: isCurrentlyValid(), isForDay(), isForTimeSlot()
  - [ ] Check if additional fields needed: isActive, priorityLevel, maxConcurrentClasses

- [ ] **Repository:** Update `TeacherAvailabilityRepository.java`
  - [ ] Standard JpaRepository methods
  - [ ] findActiveAvailabilitiesForTeacher() - Get all active availabilities for a teacher
  - [ ] findActiveAvailabilityForTeacherDayTimeSlot() - PRE-CHECK validation
  - [ ] hasTeacherAvailabilityForDayTimeSlot() - Quick boolean check
  - [ ] findAvailableTeachersForDayTimeSlot() - Find available teachers for specific time slot
  - [ ] findAvailabilitiesByDayOfWeek() - Get all availabilities for a day
  - [ ] countActiveAvailabilitiesForTeacher() - Statistics
  - [ ] findAvailabilitiesByBranch() - Branch-level availability management
  - [ ] findAvailabilitiesExpiringSoon() - Maintenance notifications
  - [ ] deleteByTeacherId() and deleteByIdAndTeacherId() - Cleanup methods

**Estimated Time:** 2-3 hours

---

### 2.3 Teacher Assignment Service (PRE-CHECK) ⏳ TODO

**Priority:** 🔴 HIGH (Most complex feature)

- [ ] **DTOs:**

  - [ ] Create `dtos/classmanagement/TeacherAvailabilityDTO.java`
    - [ ] Fields: teacherId, fullName, skills, hasGeneralSkill, availabilityStatus, totalSessions, availableSessions, availabilityPercentage, conflictDetails
  - [ ] Create `dtos/classmanagement/AssignTeacherRequest.java`
  - [ ] Create `dtos/classmanagement/AssignTeacherResponse.java`

- [ ] **Error Codes:**

  - [ ] 4007: `TEACHER_ASSIGNMENT_FAILED`
  - [ ] Additional 7 error codes (4027-4033) for PRE-CHECK validation

- [ ] **Repository:** Update `TeacherRepository.java`

  - [ ] Add method: `List<Object[]> findAvailableTeachersWithPrecheck(Long classId)`
  - [ ] Implement complex CTE query (skill_matched_teachers + session_conflicts)
  - [ ] Check 3 conditions: Availability, Teaching Conflict, Leave Conflict
  - [ ] Order by: contract_type, available_sessions DESC, has_general_skill DESC

- [ ] **Repository:** Update `TeachingSlotRepository.java`

  - [ ] Add method: `List<Long> bulkAssignTeacher(Long classId, Long teacherId, Long[] sessionIds)`
  - [ ] Use native query with skill validation ('general' OR specific match)

- [ ] **Interface:** Create `services/TeacherAssignmentService.java`

  - [ ] Define method: `List<TeacherAvailabilityDTO> queryAvailableTeachersWithPrecheck(Long classId)`
  - [ ] Define method: `AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest)`
  - [ ] Define method: `List<TeacherAvailabilityDTO> findSubstituteTeachersForSessions(Long classId, List<Long> sessionIds)`

- [ ] **Implementation:** Create `services/impl/TeacherAssignmentServiceImpl.java`

  - [ ] queryAvailableTeachersWithPrecheck: Execute CTE query, map Object[] to DTO
  - [ ] assignTeacher: Call bulkAssignTeacher, return response with counts
  - [ ] findSubstituteTeachersForSessions: Handle substitute teacher finding
  - [ ] Comprehensive validation and error handling

- [ ] **Service:** Update `ClassService.java` interface

  - [ ] Add method: `List<TeacherAvailabilityDTO> getAvailableTeachersWithPrecheck(Long classId, Long userId)`
  - [ ] Add method: `AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest, Long userId)`
  - [ ] Add method: `List<TeacherAvailabilityDTO> findSubstituteTeachersForSessions(Long classId, List<Long> sessionIds, Long userId)`

- [ ] **Service Implementation:** Update `ClassServiceImpl.java`

  - [ ] Validate class exists and user has access
  - [ ] Delegate to TeacherAssignmentService
  - [ ] Return response with proper error handling

- [ ] **Controller:** Create `TeacherController.java`

  - [ ] Add endpoint: `GET /api/v1/teachers/available?classId=123`
  - [ ] Add endpoint: `POST /api/v1/teachers/substitutes/find`

- [ ] **Controller:** Update `ClassController.java`
  - [ ] Add endpoint: `GET /api/v1/classes/{classId}/teachers/available`
  - [ ] Add endpoint: `POST /api/v1/classes/{classId}/teachers/assign`
  - [ ] Add endpoint: `POST /api/v1/classes/{classId}/teachers/substitutes/find`

**Estimated Time:** 6-8 hours

---

### 2.4 Available Resources Query ⏳ TODO

**Priority:** 🟢 LOW (Utility endpoint)

- [ ] **Implementation:** Already covered in 2.2
  - [ ] Available resources endpoint: `GET /api/v1/classes/{classId}/sessions/{sessionId}/resources/available`
  - [ ] Real-time availability checking with conflict detection
  - [ ] Comprehensive OpenAPI documentation

**Estimated Time:** Included in 2.2

---

## PHASE 3: TEACHER AVAILABILITY MANAGEMENT (FUTURE)

### 3.1 Teacher Availability Management (Future) ⏳ TODO

**Priority:** 🟢 LOW (Not required for MVP)

- [ ] **DTOs:** Create comprehensive Teacher Availability DTOs

  - [ ] CreateTeacherAvailabilityRequest.java with validation
  - [ ] UpdateTeacherAvailabilityRequest.java for partial updates
  - [ ] TeacherAvailabilityResponse.java with comprehensive data
  - [ ] CreatedTeacherAvailabilityResponse.java and UpdatedTeacherAvailabilityResponse.java
  - [ ] TeacherAvailabilityStatisticsResponse.java for analytics
  - [ ] WeeklyAvailabilityResponse.java for schedule visualization
  - [ ] BulkUpdateTeacherAvailabilityRequest.java and Response.java for batch operations
  - [ ] TeacherAvailabilityValidationResponse.java for detailed validation
  - [ ] BranchTeacherAvailabilityResponse.java and AvailableTeacherResponse.java

- [ ] **Service Interface:** Update TeacherAvailabilityService.java

  - [ ] Add 15+ comprehensive methods for CRUD operations
  - [ ] Include statistics, validation, bulk operations, and weekly scheduling methods
  - [ ] Add proper method documentation and parameter validation

- [ ] **Service Implementation:** Create TeacherAvailabilityServiceImpl.java

  - [ ] Full CRUD operations with comprehensive business logic
  - [ ] Statistics calculation and analytics (availability coverage, patterns, insights)
  - [ ] Weekly schedule generation with detailed day-wise breakdown
  - [ ] Bulk operations with detailed results and error handling
  - [ ] Comprehensive validation with detailed error/warning reporting
  - [ ] Availability conflict detection and duplicate prevention
  - [ ] Branch-based filtering and authorization controls
  - [ ] Pagination support and efficient data retrieval
  - [ ] Proper transaction management and logging

- [ ] **Repository:** Update TeacherAvailabilityRepository.java

  - [ ] Add missing methods: findAllByTeacherId(Long, Pageable)
  - [ ] Add countByTeacherId(Long) for statistics
  - [ ] Add findAvailableTeachersForDayTimeSlotAndBranch() for branch filtering
  - [ ] Add findAllByTeacherId(Long) for non-paginated retrieval
  - [ ] All methods include proper @Query annotations and parameter binding

- [ ] **Controller:** Create TeacherAvailabilityController.java
  - [ ] POST /api/v1/teacher-availability - Create availability
  - [ ] PUT /api/v1/teacher-availability/{id} - Update availability
  - [ ] DELETE /api/v1/teacher-availability/{id} - Delete availability
  - [ ] GET /api/v1/teacher-availability - Get paginated availabilities
  - [ ] GET /api/v1/teacher-availability/statistics - Get availability statistics
  - [ ] GET /api/v1/teacher-availability/weekly-schedule - Get weekly schedule
  - [ ] POST /api/v1/teacher-availability/bulk-update - Bulk operations
  - [ ] POST /api/v1/teacher-availability/validate - Validate availability
  - [ ] GET /api/v1/teacher-availability/available-teachers - Get available teachers
  - [ ] Role-based access control (Teachers, Academic Affairs, Managers)
  - [ ] Comprehensive OpenAPI documentation with workflow examples

**Estimated Time:** 5-6 hours

---

## PHASE 4: POLISH & FINALIZATION

### 4.1 Error Handling & Validation ⏳ TODO

**Priority:** 🟡 MEDIUM (Robustness)

- [ ] **Global Exception Handling:**

  - [ ] Verify all CustomException with ErrorCode
  - [ ] Verify all validation annotations work

- [ ] **Edge Cases:**
  - [ ] Invalid schedule_days (0, 8, null)
  - [ ] Class not found
  - [ ] Unauthorized access (wrong branch)
  - [ ] Duplicate class code
  - [ ] Course not approved
  - [ ] Invalid approval status transitions

**Estimated Time:** 3-4 hours

---

### 4.2 OpenAPI Documentation ⏳ TODO

**Priority:** 🟡 MEDIUM (Developer experience)

- [ ] **Update Swagger Annotations:**

  - [ ] All endpoints have @Operation (already existed)
  - [ ] All parameters have @Parameter (already existed)
  - [ ] **NEW:** All responses have @ApiResponse with comprehensive examples
  - [ ] **NEW:** Complete request/response examples for all workflow steps

- [ ] **Test Swagger UI:**
  - [ ] All endpoints visible (annotations verified)
  - [ ] Try-it-out ready (syntax verified)
  - [ ] Examples are correct (real workflow data provided)

**Estimated Time:** 2-3 hours

---

### 4.3 Performance Optimization ⏳ TODO

**Priority:** 🟢 LOW (Nice to have)

- [ ] **Benchmarking:**

  - [ ] Measure total workflow time (target: <500ms)
  - [ ] Measure individual operations
  - [ ] Identify bottlenecks

- [ ] **Optimizations:**
  - [ ] Add database indexes
  - [ ] Batch operations where possible
  - [ ] Cache lookup data (subjects, levels, time slots)

**Estimated Time:** 4-5 hours

---

### 4.4 Code Review & Refactoring ⏳ TODO

**Priority:** 🟡 MEDIUM (Code quality)

- [ ] **Code Review Checklist:**

  - [ ] Follow SOLID principles
  - [ ] No code duplication (DRY)
  - [ ] Proper exception handling
  - [ ] Comprehensive logging
  - [ ] Javadoc comments
  - [ ] Consistent naming conventions

- [ ] **Refactoring:**
  - [ ] Extract long methods
  - [ ] Remove magic numbers
  - [ ] Simplify complex logic

**Estimated Time:** 3-4 hours

---

## DEFERRED TO FUTURE (Not in MVP)

### Email Notifications

- [ ] Submit notification to CENTER_HEAD
- [ ] Approval/rejection notification to ACADEMIC_AFFAIR
- [ ] Async processing with RabbitMQ/Kafka

### Bulk Operations

- [ ] Clone class
- [ ] Bulk approve classes
- [ ] Bulk delete drafts

### Advanced Features

- [ ] Class schedule visualization (calendar view)
- [ ] Conflict resolution wizard
- [ ] Approval delegation
- [ ] Class history/audit trail
- [ ] Draft auto-save
- [ ] Teacher workload dashboard

---

## TIME ESTIMATES

| Phase     | Tasks              | Estimated Time  | Priority        |
| --------- | ------------------ | --------------- | --------------- |
| Phase 1   | 5 major tasks      | 21-30 hours     | 🔴 HIGH         |
| Phase 2   | 4 major tasks      | 12-17 hours     | 🔴 HIGH         |
| Phase 3   | 1 major task       | 5-6 hours       | 🟢 LOW          |
| Phase 4   | 4 major tasks      | 12-16 hours     | 🟡 MEDIUM       |
| **TOTAL** | **14 major tasks** | **50-69 hours** | **(7-9 days)**  |

**Note:** Time estimates are for implementation only, not including debugging or additional refinements.

---

## DAILY PROGRESS TRACKING

### Week 1

**Monday:**

- [ ] Phase 1.1: Create Class (6-8 hours)

**Tuesday:**

- [ ] Phase 1.2: Session Generation Service (4-6 hours)
- [ ] Phase 1.3: Assign Time Slots (start) (2-3 hours)

**Wednesday:**

- [ ] Phase 1.3: Assign Time Slots (finish) (1-2 hours)
- [ ] Phase 1.4: Validation Service (2-3 hours)

**Thursday:**

- [ ] Phase 1.4: Validation Service (finish) (1 hour)
- [ ] Phase 1.5: Approval Service (4-5 hours)

**Friday:**

- [ ] Phase 1.5: Approval Service (finish) (1-2 hours)
- [ ] Phase 2.1: Resource Assignment Service (start) (3-4 hours)

---

### Week 2

**Monday:**

- [ ] Phase 2.1: Resource Assignment Service (finish) (2-3 hours)
- [ ] Phase 2.2: Teacher Availability Entity (2-3 hours)
- [ ] Phase 2.3: Teacher Assignment Service (start) (2-3 hours)

**Tuesday:**

- [ ] Phase 2.3: Teacher Assignment Service (continue) (6-8 hours)

**Wednesday:**

- [ ] Phase 2.3: Teacher Assignment Service (finish) (2-3 hours)
- [ ] Phase 2.4: Available Resources Query (included in 2.1)

**Thursday:**

- [ ] Phase 4.1: Error Handling & Validation (3-4 hours)
- [ ] Phase 4.2: OpenAPI Documentation (2-3 hours)

**Friday:**

- [ ] Phase 4.3: Performance Optimization (4-6 hours)

---

### Week 3 (Buffer)

**Monday-Friday:**

- [ ] Phase 4.4: Code Review & Refactoring (4-5 hours)
- [ ] Buffer time for debugging and fixes (30+ hours)

---

## SUCCESS METRICS

**Performance:**

- [ ] Total workflow: <500ms
- [ ] Generate 36 sessions: <50ms
- [ ] Assign time slots: <20ms
- [ ] Assign resources (HYBRID): <200ms
- [ ] Assign teachers (PRE-CHECK): <120ms
- [ ] Validation: <50ms

**Quality:**

- [ ] Zero SonarQube critical issues
- [ ] All error codes documented
- [ ] All endpoints have OpenAPI docs

---

## HANDOFF CHECKLIST

**Before Frontend Integration:**

- [ ] All Phase 1-3 complete and tested
- [ ] OpenAPI spec exported (openapi.yaml)
- [ ] Postman collection exported
- [ ] Sample JWT tokens generated
- [ ] Seed data script created (demo classes in various states)
- [ ] Error code reference document
- [ ] API documentation complete
- [ ] Demo video recorded (optional)

**Frontend Team Needs:**

- [ ] 7-step wizard UI mockups
- [ ] Conflict resolution UI design
- [ ] Teacher availability visualization design
- [ ] Validation error display patterns
- [ ] Approval workflow UI design

---

## BLOCKER TRACKING

| Date | Blocker | Impact | Resolution | Status |
| ---- | ------- | ------ | ---------- | ------ |
| -    | -       | -      | -          | -      |

---

## NOTES & DECISIONS

**2025-11-06:**

- All tasks reset to pending status for fresh implementation
- Ready to begin Phase 1 (Core Workflow) and Phase 3 (Teacher Assignment)
- Estimated 8-11 days for full implementation
- Priority: Phase 1 (Core Workflow) and Phase 3 (Teacher Assignment)

---

**Last Updated:** 2025-11-06
**Current Status:** 📋 READY TO START
**Overall Progress:** 0/14 phases completed (0%)
