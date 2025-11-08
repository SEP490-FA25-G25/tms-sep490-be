# CREATE CLASS WORKFLOW - IMPLEMENTATION CHECKLIST V2

**Version:** 2.0  
**Date:** 2025-11-08  
**Status:** 📋 IN PROGRESS

---

## 📊 QUICK PROGRESS

```
Phase 1: Core Foundation         [████████████████████] 100% ✅ COMPLETED
Phase 2: Assignment Features     [░░░░░░░░░░░░░░░░░░░░]   0% ⏳ TODO
Phase 3: Polish & Testing        [░░░░░░░░░░░░░░░░░░░░]   0% ⏳ TODO

Overall Progress:                [███████░░░░░░░░░░░░░]  40%
```

---

## ✅ PHASE 1: CORE FOUNDATION (COMPLETED)

### 1.1 Create Class + Session Generation ✅

- [x] **DTOs:**

  - [x] `CreateClassRequest.java` - Pure data container
  - [x] `CreateClassResponse.java` - Pure data with SessionGenerationSummary nested
  - [x] `CreateClassRequestValidator.java` - Input validation logic
  - [x] `CreateClassResponseUtil.java` - Response processing logic

- [x] **Services:**

  - [x] `SessionGenerationService` interface
  - [x] `SessionGenerationServiceImpl` - Date calculation algorithm
  - [x] `ClassService.createClass()` - Main business logic

- [x] **Repository:**

  - [x] `ClassRepository.findByBranchIdAndCode()` - Uniqueness check
  - [x] `CourseSessionRepository.findByPhase_Course_IdOrderByPhaseAscSequenceNoAsc()` - Load templates

- [x] **Controller:**

  - [x] `POST /api/v1/classes` - Create class endpoint

- [x] **Testing:**
  - [x] Session generation algorithm verified
  - [x] Date calculation works for Mon/Wed/Fri schedule

**Status:** ✅ Application builds, endpoint functional

---

### 1.2 Assign Time Slots ✅

- [x] **DTOs:**

  - [x] `AssignTimeSlotsRequest.java` - Pure data with TimeSlotAssignment nested
  - [x] `AssignTimeSlotsResponse.java` - Pure data with detailed assignment info
  - [x] `AssignTimeSlotsRequestValidator.java` - Validation logic
  - [x] `AssignTimeSlotsResponseUtil.java` - Response processing

- [x] **Repository:**

  - [x] `SessionRepository.updateTimeSlotByDayOfWeek()` - Native SQL query (PostgreSQL DOW)
  - [x] `TimeSlotTemplateRepository.findByBranchIdOrderByStartTimeAsc()` - Available slots

- [x] **Services:**

  - [x] `ClassService.assignTimeSlots()` - Business logic
  - [x] PostgreSQL DOW format support (0=Sun, 1=Mon, ..., 6=Sat)

- [x] **Controller:**

  - [x] `POST /api/v1/classes/{classId}/time-slots`
  - [x] `GET /api/v1/branches/{branchId}/time-slot-templates`

- [x] **Testing:**
  - [x] Bulk update by day of week works
  - [x] Different time slots per day verified

**Status:** ✅ Works with PostgreSQL DOW format

---

### 1.3 Validation Service ✅

- [x] **DTOs:**

  - [x] `ValidateClassResponse.java` - Pure data with ValidationChecks nested
  - [x] `ValidateClassResponseUtil.java` - Validation result processing

- [x] **Services:**

  - [x] `ValidationService` interface
  - [x] `ValidationServiceImpl` - Check completeness logic
  - [x] `ClassService.validateClass()` - Wrapper method

- [x] **Repository:**

  - [x] `SessionRepository.countByClassEntityId()` - Total sessions
  - [x] Count methods for sessions without timeslot/resource/teacher

- [x] **Controller:**

  - [x] `POST /api/v1/classes/{classId}/validate`

- [x] **Testing:**
  - [x] Validation checks work correctly
  - [x] Completion percentage calculated

**Status:** ✅ Validation logic complete

---

### 1.4 Approval Service ✅

- [x] **DTOs:**

  - [x] `SubmitClassResponse.java` - Simple response
  - [x] `RejectClassRequest.java` - Rejection reason
  - [x] `RejectClassResponse.java` - Simple response

- [x] **Services:**

  - [x] `ApprovalService` interface
  - [x] `ApprovalServiceImpl` - Submit/approve/reject logic
  - [x] `ClassService` wrapper methods

- [x] **Repository:**

  - [x] Entity fields verified: `submittedAt`, `decidedBy`, `decidedAt`, `rejectionReason`

- [x] **Controller:**

  - [x] `POST /api/v1/classes/{classId}/submit` (ACADEMIC_AFFAIR)
  - [x] `POST /api/v1/classes/{classId}/approve` (CENTER_HEAD)
  - [x] `POST /api/v1/classes/{classId}/reject` (CENTER_HEAD)

- [x] **Testing:**
  - [x] State transitions work (DRAFT → SCHEDULED)
  - [x] Approval status tracking

**Status:** ✅ Approval workflow functional

---

### 1.5 DTO Architecture Refactoring ✅

- [x] **Lessons Learned:**

  - [x] DTOs = Pure data only (NO business logic)
  - [x] Request validation → Validator classes
  - [x] Response processing → Util classes
  - [x] No wrapper methods → Inline simple checks
  - [x] Use wrapper types (Boolean/Integer/Long)

- [x] **Refactored Files:**
  - [x] All Phase 1 DTOs follow pattern
  - [x] All Phase 1 Validators created
  - [x] All Phase 1 Utils created
  - [x] No business logic in DTOs

**Status:** ✅ Architecture patterns established

---

## ⏳ PHASE 2: ASSIGNMENT FEATURES (TODO)

### 2.1 Resource Assignment (HYBRID) ⏳

**Priority:** 🔴 HIGH | **Estimated:** 4-6 hours

- [ ] **DTOs:**

  - [ ] `AssignResourcesRequest.java` - Pattern: [{dayOfWeek, resourceId}]
  - [ ] `AssignResourcesResponse.java` - successCount, conflictCount, conflicts[]
  - [ ] `ResourceConflictDetail.java` - sessionId, reason, conflictingClass
  - [ ] `AssignResourcesRequestValidator.java` - Validation logic
  - [ ] `AssignResourcesResponseUtil.java` - Response processing

- [ ] **Repository:**

  - [ ] `SessionResourceRepository.bulkAssignResource()` - SQL bulk insert (Phase 1)
    ```sql
    INSERT INTO session_resource (session_id, resource_id)
    SELECT s.id, :resourceId FROM session s
    WHERE s.class_id = :classId
      AND EXTRACT(DOW FROM s.date) = :dayOfWeek
      AND NOT EXISTS (conflict check)
    RETURNING session_id;
    ```
  - [ ] `SessionRepository.findUnassignedSessionsByDayOfWeek()` - Find conflicts (Phase 2)
  - [ ] `ResourceRepository.findAvailableResources()` - Available resources query

- [ ] **Services:**

  - [ ] `ResourceAssignmentService` interface
  - [ ] `ResourceAssignmentServiceImpl`:
    - [ ] Phase 1: SQL bulk insert (90% sessions)
    - [ ] Phase 2: Java conflict analysis (10% conflicts)
    - [ ] Return `AutoPropagateResult` with detailed conflicts
  - [ ] `ClassService.assignResources()` - Wrapper method

- [ ] **Controller:**

  - [ ] `POST /api/v1/classes/{classId}/resources` - Assign resources endpoint
  - [ ] `GET /api/v1/classes/{classId}/sessions/{sessionId}/available-resources` - Query available

- [ ] **Testing:**
  - [ ] Bulk insert works for non-conflict sessions
  - [ ] Conflict detection identifies exact reason
  - [ ] Performance: <200ms for 36 sessions
  - [ ] Manual conflict resolution works

**Acceptance:**

- ✅ Assign Room 101 to Mon/Wed/Fri → 33/36 sessions successful
- ✅ 3 conflicts detected with class names
- ✅ Manual resolution assigns alternative resources

---

### 2.2 Teacher Availability Entity ⏳

**Priority:** 🔴 HIGH | **Estimated:** 2-3 hours

- [ ] **Entity Verification:**

  - [ ] `TeacherAvailability.java` exists with correct structure:
    - [ ] Composite PK: (teacher_id, time_slot_template_id, day_of_week)
    - [ ] Fields: teacher, timeSlotTemplate, dayOfWeek, effectiveDate, note
    - [ ] Timestamps: createdAt, updatedAt
  - [ ] Entity matches `schema.sql` lines 487-498

- [ ] **Repository:**

  - [ ] `TeacherAvailabilityRepository` interface
  - [ ] Methods:
    - [ ] `findByTeacherIdAndDayOfWeekAndTimeSlotTemplateId()` - Check availability
    - [ ] `findByTeacherId()` - Get all availabilities for teacher
    - [ ] `existsByTeacherIdAndDayOfWeekAndTimeSlotTemplateId()` - Quick check

- [ ] **Testing:**
  - [ ] Entity saves correctly
  - [ ] Composite PK enforced
  - [ ] Query methods work

**Acceptance:**

- ✅ Teacher availability stored correctly
- ✅ Used in PRE-CHECK query (next step)

---

### 2.3 Teacher Assignment (PRE-CHECK) ⏳

**Priority:** 🔴 HIGH | **Estimated:** 6-8 hours

- [ ] **DTOs:**

  - [ ] `AssignTeacherRequest.java` - teacherId, sessionIds (optional)
  - [ ] `AssignTeacherResponse.java` - assignedCount, needsSubstitute, remainingSessions
  - [ ] `TeacherAvailabilityDTO.java` - Full availability info
    - [ ] teacherId, fullName, skills, hasGeneralSkill
    - [ ] availabilityStatus: fully_available / partially_available / unavailable
    - [ ] totalSessions, availableSessions, availabilityPercentage
    - [ ] conflicts: {noAvailability, teachingConflict, leaveConflict}
  - [ ] `AssignTeacherRequestValidator.java` - Validation logic
  - [ ] `AssignTeacherResponseUtil.java` (if needed) - Response processing

- [ ] **Repository:**

  - [ ] `TeacherRepository.findAvailableTeachersWithPrecheck()` - CTE query
    ```sql
    WITH skill_matched_teachers AS (...),
         session_conflicts AS (
           -- Check 3 conditions for ALL sessions:
           -- 1. Teacher availability registered
           -- 2. No teaching conflict
           -- 3. No leave conflict
         )
    SELECT teacher_id, full_name, skills, has_general_skill,
           total_sessions, available_sessions, availability_percentage,
           conflict_breakdown, availability_status
    ORDER BY available_sessions DESC;
    ```
  - [ ] `TeachingSlotRepository.bulkAssignTeacher()` - Direct insert
    ```sql
    INSERT INTO teaching_slot (session_id, teacher_id, status)
    SELECT s.id, :teacherId, 'scheduled'
    FROM session s
    WHERE s.class_id = :classId
      AND skill_validation_passes
    RETURNING session_id;
    ```

- [ ] **Services:**

  - [ ] `TeacherAssignmentService` interface
  - [ ] `TeacherAssignmentServiceImpl`:
    - [ ] `queryAvailableTeachersWithPrecheck()` - Execute CTE, map to DTO
    - [ ] `assignTeacher()` - Direct bulk insert (no re-checking)
    - [ ] Handle partially available teachers (return substitute suggestions)
  - [ ] `ClassService.getAvailableTeachers()` - Wrapper for PRE-CHECK
  - [ ] `ClassService.assignTeacher()` - Wrapper for assignment

- [ ] **Controller:**

  - [ ] `GET /api/v1/classes/{classId}/available-teachers` - PRE-CHECK endpoint
  - [ ] `POST /api/v1/classes/{classId}/teachers` - Assignment endpoint

- [ ] **Key Features:**

  - [ ] 'general' skill = UNIVERSAL (can teach ANY session)
  - [ ] PRE-CHECK shows availability BEFORE user selects
  - [ ] No trial-and-error assignment
  - [ ] Detailed conflict breakdown

- [ ] **Testing:**
  - [ ] PRE-CHECK query returns correct availability status
  - [ ] 'general' skill teachers match all sessions
  - [ ] Direct assignment works without re-checking
  - [ ] Performance: <120ms for PRE-CHECK + assignment
  - [ ] Partially available teachers handled correctly

**Acceptance:**

- ✅ Query shows "Jane: 10/10 ✅, John: 7/10 ⚠️ (3 conflicts)"
- ✅ Assign Jane → all 10 sessions assigned immediately
- ✅ Assign John → 7 sessions assigned, 3 need substitute
- ✅ 'general' skill teacher can teach all session types

---

## ⏳ PHASE 3: POLISH & TESTING (TODO)

### 3.1 Error Handling ⏳

**Priority:** 🟡 MEDIUM | **Estimated:** 3-4 hours

- [ ] **Error Codes:**

  - [ ] 4000-4099 range for Class workflow
  - [ ] Specific codes for each validation failure
  - [ ] Clear error messages

- [ ] **Edge Cases:**

  - [ ] Invalid schedule days
  - [ ] Class not found
  - [ ] Unauthorized access
  - [ ] Duplicate class code
  - [ ] Course not approved
  - [ ] Invalid state transitions

- [ ] **Testing:**
  - [ ] All error scenarios tested
  - [ ] Error messages user-friendly

---

### 3.2 OpenAPI Documentation ⏳

**Priority:** 🟡 MEDIUM | **Estimated:** 2-3 hours

- [ ] **Swagger Annotations:**

  - [ ] All endpoints have @Operation
  - [ ] All parameters have @Parameter
  - [ ] All responses have @ApiResponse
  - [ ] Complete request/response examples

- [ ] **Testing:**
  - [ ] Swagger UI accessible
  - [ ] Try-it-out works
  - [ ] Examples are accurate

---

### 3.3 Performance Optimization ⏳

**Priority:** 🟢 LOW | **Estimated:** 4-5 hours

- [ ] **Benchmarking:**

  - [ ] Total workflow time measured
  - [ ] Individual operation times
  - [ ] Bottleneck identification

- [ ] **Optimizations:**

  - [ ] Database indexes
  - [ ] Query optimization
  - [ ] Caching (if needed)

- [ ] **Target:**
  - [ ] Total workflow: <500ms

---

### 3.4 Code Review & Refactoring ⏳

**Priority:** 🟡 MEDIUM | **Estimated:** 3-4 hours

- [ ] **Code Quality:**

  - [ ] SOLID principles
  - [ ] DRY (no duplication)
  - [ ] Proper exception handling
  - [ ] Comprehensive logging
  - [ ] Javadoc comments

- [ ] **Refactoring:**
  - [ ] Extract long methods
  - [ ] Remove magic numbers
  - [ ] Simplify complex logic

---

## 📋 IMPLEMENTATION CHECKLIST

### Before Starting Phase 2

- [x] Phase 1 complete and tested
- [x] DTO architecture patterns established
- [x] All Phase 1 endpoints functional
- [ ] Review Phase 2 requirements with team
- [ ] Verify entity structures in database

### During Implementation

- [ ] Write tests FIRST (TDD approach)
- [ ] Follow DTO patterns (Pure Data + Util/Validator)
- [ ] Use @Transactional appropriately
- [ ] Add comprehensive logging
- [ ] Update this checklist as you go

### Before Moving to Next Phase

- [ ] All checkboxes in current phase marked
- [ ] All tests passing
- [ ] Code review completed
- [ ] Documentation updated
- [ ] Performance targets met

---

## 🎯 SUCCESS CRITERIA

### Phase 2 Complete When:

- [ ] ✅ Resource assignment works (HYBRID approach)
- [ ] ✅ Teacher assignment works (PRE-CHECK approach)
- [ ] ✅ All 36 sessions can be fully configured
- [ ] ✅ Conflict detection accurate
- [ ] ✅ Performance targets met (<500ms total)

### Phase 3 Complete When:

- [ ] ✅ All error cases handled
- [ ] ✅ OpenAPI documentation complete
- [ ] ✅ Performance optimized
- [ ] ✅ Code review passed
- [ ] ✅ Ready for frontend integration

---

## 🚀 DAILY TRACKING

### Week 1 (Current)

**Day 1-2:** Phase 2.1 Resource Assignment (HYBRID)

- [ ] DTOs + Validators + Utils
- [ ] Repository methods (SQL bulk + conflict queries)
- [ ] Service implementation
- [ ] Controller endpoints
- [ ] Testing

**Day 3:** Phase 2.2 Teacher Availability

- [ ] Entity verification
- [ ] Repository methods
- [ ] Testing

**Day 4-5:** Phase 2.3 Teacher Assignment (PRE-CHECK)

- [ ] DTOs + Validators + Utils
- [ ] PRE-CHECK CTE query
- [ ] Service implementation
- [ ] Controller endpoints
- [ ] Testing

### Week 2

**Day 1-2:** Phase 3 Polish & Testing

- [ ] Error handling
- [ ] OpenAPI documentation
- [ ] Performance optimization
- [ ] Code review

**Day 3:** Buffer & Integration

- [ ] Bug fixes
- [ ] Final testing
- [ ] Frontend handoff preparation

---

## 📊 TIME ESTIMATES

| Phase     | Tasks                         | Estimated Time             | Priority  |
| --------- | ----------------------------- | -------------------------- | --------- |
| Phase 1   | ✅ Complete                   | 21-30 hours                | 🔴 HIGH   |
| Phase 2   | Resource + Teacher Assignment | 12-17 hours                | 🔴 HIGH   |
| Phase 3   | Polish & Testing              | 12-16 hours                | 🟡 MEDIUM |
| **Total** | **All phases**                | **45-63 hours (6-8 days)** | -         |

**Current Progress:** 40% (Phase 1 complete)

---

## 📝 NOTES

### Phase 1 Lessons Learned

1. ✅ **PostgreSQL DOW format** critical for time slot assignment
2. ✅ **Native queries** required for EXTRACT(DOW FROM date)
3. ✅ **Entity navigation paths** must be correct (cs.phase.course.id)
4. ✅ **DTO architecture** patterns prevent technical debt
5. ✅ **Separation of concerns** makes code maintainable

### Phase 2 Focus

1. **HYBRID approach** for resource assignment (speed + detail)
2. **PRE-CHECK approach** for teacher assignment (UX improvement)
3. **'general' skill** as universal skill (flexibility)
4. **Performance targets** must be met (<500ms total)

### Phase 3 Focus

1. **Error messages** must be user-friendly
2. **Documentation** must be complete for frontend
3. **Performance** must meet targets
4. **Code quality** must pass review

---

**Last Updated:** 2025-11-08  
**Next Review:** After Phase 2.1 completion  
**Status:** Phase 1 ✅ DONE | Phase 2 ⏳ READY TO START
