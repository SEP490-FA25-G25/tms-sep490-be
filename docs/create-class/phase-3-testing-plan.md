# PHASE 3: TESTING & POLISH - IMPLEMENTATION PLAN

**Date:** 2025-11-08  
**Status:** ⏳ TODO  
**Estimated Time:** 12-17 hours (core tests only)

---

## 📊 OVERVIEW

**Goal:** Ensure code quality and reliability through comprehensive testing

**Strategy:** Focus on **Unit Tests + API Tests** first, defer Integration Tests

**Rationale:**
- Unit + API tests provide 80% confidence coverage
- Can deploy to production with core tests only
- Integration tests add value but require more setup (Testcontainers)
- Performance optimization only needed if bottlenecks identified

---

## 🔴 PHASE 3.1: UNIT TESTS (Service Layer)

**Priority:** 🔴 HIGH | **Estimated:** 4-5 hours

### ResourceAssignmentServiceImpl Tests

- [ ] **Test HYBRID Phase 1 (Bulk Insert):**
  - [ ] Test successful bulk insert (90% case)
  - [ ] Test partial insert with some conflicts
  - [ ] Mock SessionResourceRepository.bulkInsertResourcesForDayOfWeek()
  - [ ] Verify correct successCount returned

- [ ] **Test HYBRID Phase 2 (Conflict Analysis):**
  - [ ] Test conflict detection for CLASS_BOOKING
  - [ ] Test conflict detection for MAINTENANCE
  - [ ] Test conflict detection for INSUFFICIENT_CAPACITY
  - [ ] Test conflict detection for UNAVAILABLE
  - [ ] Mock SessionResourceRepository.findSessionsWithResourceConflict()
  - [ ] Verify detailed conflict information returned

- [ ] **Test Performance Tracking:**
  - [ ] Verify processingTimeMs calculated correctly
  - [ ] Test with different session counts (10, 36, 100 sessions)

- [ ] **Test Error Scenarios:**
  - [ ] Test with empty dayOfWeekResourceMap
  - [ ] Test with invalid classId
  - [ ] Test with non-existent resourceId

**Test Pattern:**
```java
@SpringBootTest
@ActiveProfiles("test")
class ResourceAssignmentServiceImplTest {
    @Autowired private ResourceAssignmentService service;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    
    @Test
    void shouldAssignResourcesWithNoConflicts() {
        // Given
        Long classId = 1L;
        Map<Integer, Long> dayOfWeekResourceMap = Map.of(1, 10L, 3, 10L, 5, 10L);
        when(sessionResourceRepository.bulkInsertResourcesForDayOfWeek(eq(classId), anyInt(), anyLong()))
            .thenReturn(12); // 36 sessions / 3 days = 12 per day
        when(sessionResourceRepository.findSessionsWithResourceConflict(anyLong(), anyInt(), anyLong()))
            .thenReturn(Collections.emptyList());
        
        // When
        AssignResourcesResponse response = service.assignResources(classId, dayOfWeekResourceMap);
        
        // Then
        assertThat(response.getSuccessCount()).isEqualTo(36);
        assertThat(response.getConflictCount()).isEqualTo(0);
        assertThat(response.getConflicts()).isEmpty();
        assertThat(response.getProcessingTimeMs()).isGreaterThan(0L);
    }
    
    @Test
    void shouldDetectConflictsAndProvideDetails() {
        // Given - When - Then
        // Test conflict detection logic
    }
}
```

---

### TeacherAssignmentServiceImpl Tests

- [ ] **Test PRE-CHECK Query Execution:**
  - [ ] Test queryAvailableTeachersWithPrecheck() with valid classId
  - [ ] Test with multiple teachers (10+ teachers)
  - [ ] Mock TeacherRepository.findAvailableTeachersWithPrecheck()
  - [ ] Verify Object[] to DTO mapping works correctly

- [ ] **Test Object[] to DTO Mapping (11 fields):**
  - [ ] Test mapToTeacherAvailabilityDTO() with all fields populated
  - [ ] Test with null/empty skills
  - [ ] Test with GENERAL skill
  - [ ] Verify all 11 fields mapped correctly

- [ ] **Test Type Conversions:**
  - [ ] Test convertToLong() with BigInteger
  - [ ] Test convertToLong() with Long (pass-through)
  - [ ] Test convertToInteger() with BigDecimal
  - [ ] Test convertToInteger() with Integer (pass-through)
  - [ ] Test with null values
  - [ ] Test parseSkills() with comma-separated string
  - [ ] Test parseSkills() with single skill
  - [ ] Test parseSkills() with empty/null string

- [ ] **Test Full Assignment Mode (sessionIds = null):**
  - [ ] Test assignTeacher() with sessionIds = null
  - [ ] Mock TeachingSlotRepository.bulkAssignTeacher()
  - [ ] Verify all unassigned sessions get assigned
  - [ ] Verify needsSubstitute = false
  - [ ] Verify remainingSessions = 0

- [ ] **Test Partial Assignment Mode (specific sessions):**
  - [ ] Test assignTeacher() with specific sessionIds
  - [ ] Mock TeachingSlotRepository.bulkAssignTeacherToSessions()
  - [ ] Verify only specified sessions assigned
  - [ ] Verify needsSubstitute = true (if sessions remain)
  - [ ] Verify correct remainingSessions count

- [ ] **Test GENERAL Skill Bypass Logic:**
  - [ ] Test teacher with GENERAL skill bypasses validation
  - [ ] Verify skill check not performed for GENERAL
  - [ ] Test in parseSkills() method

- [ ] **Test needsSubstitute Flag Calculation:**
  - [ ] Test needsSubstitute = false (all sessions assigned)
  - [ ] Test needsSubstitute = true (some sessions remain)
  - [ ] Verify remainingSessions count correct

**Test Pattern:**
```java
@SpringBootTest
@ActiveProfiles("test")
class TeacherAssignmentServiceImplTest {
    @Autowired private TeacherAssignmentService service;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private SessionRepository sessionRepository;
    
    @Test
    void shouldQueryAvailableTeachersWithPrecheck() {
        // Given
        Long classId = 1L;
        Object[] row = new Object[]{
            BigInteger.valueOf(45L),              // teacher_id
            "John Doe",                           // full_name
            "IELTS,TOEFL,GENERAL",               // skills
            5,                                    // years_of_experience
            BigDecimal.valueOf(36),              // total_sessions
            BigDecimal.valueOf(36),              // available_sessions
            BigDecimal.ZERO,                     // total_no_availability
            BigDecimal.ZERO,                     // total_teaching_conflict
            BigDecimal.ZERO,                     // total_leave_conflict
            BigDecimal.ZERO,                     // total_skill_mismatch
            BigDecimal.ZERO                      // total_conflicts
        };
        when(teacherRepository.findAvailableTeachersWithPrecheck(classId))
            .thenReturn(List.of(row));
        
        // When
        List<TeacherAvailabilityDTO> teachers = service.queryAvailableTeachersWithPrecheck(classId);
        
        // Then
        assertThat(teachers).hasSize(1);
        TeacherAvailabilityDTO teacher = teachers.get(0);
        assertThat(teacher.getTeacherId()).isEqualTo(45L);
        assertThat(teacher.getTeacherName()).isEqualTo("John Doe");
        assertThat(teacher.getSkills()).contains(Skill.IELTS, Skill.TOEFL, Skill.GENERAL);
        assertThat(teacher.getYearsOfExperience()).isEqualTo(5);
        assertThat(teacher.getTotalSessions()).isEqualTo(36);
        assertThat(teacher.getAvailableSessions()).isEqualTo(36);
        assertThat(teacher.getAvailabilityPercentage()).isEqualTo(100.0);
        assertThat(teacher.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.FULLY_AVAILABLE);
    }
    
    @Test
    void shouldHandleFullAssignmentMode() {
        // Given
        Long classId = 1L;
        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(null); // Full assignment
        
        Teacher teacher = TestDataBuilder.buildTeacher().id(45L).build();
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.bulkAssignTeacher(classId, 45L))
            .thenReturn(List.of(1L, 2L, 3L)); // 3 sessions assigned
        when(sessionRepository.findSessionsWithoutTeacher(classId))
            .thenReturn(Collections.emptyList()); // No remaining sessions
        
        // When
        AssignTeacherResponse response = service.assignTeacher(classId, request);
        
        // Then
        assertThat(response.getAssignedCount()).isEqualTo(3);
        assertThat(response.getNeedsSubstitute()).isFalse();
        assertThat(response.getRemainingSessions()).isEqualTo(0);
        assertThat(response.getRemainingSessionIds()).isEmpty();
    }
}
```

---

### Validator Tests

- [ ] **AssignResourcesRequestValidator:**
  - [ ] Test validate() with valid input
  - [ ] Test with empty dayOfWeekResourceMap
  - [ ] Test with null values
  - [ ] Test with invalid dayOfWeek (outside 0-6)
  - [ ] Test with non-existent resourceId
  - [ ] Test with resource branch mismatch
  - [ ] Test with class not in DRAFT status
  - [ ] Verify correct ErrorCode returned

- [ ] **AssignTeacherRequestValidator:**
  - [ ] Test validate() with valid full assignment
  - [ ] Test validate() with valid partial assignment
  - [ ] Test with null teacherId
  - [ ] Test with non-existent teacherId
  - [ ] Test with non-existent sessionIds
  - [ ] Test with sessions not belonging to class
  - [ ] Test with teacher skill mismatch (without GENERAL)
  - [ ] Test with teacher having GENERAL skill (should pass)
  - [ ] Test with class not in DRAFT status
  - [ ] Verify correct ErrorCode returned

**Test Pattern:**
```java
@SpringBootTest
@ActiveProfiles("test")
class AssignTeacherRequestValidatorTest {
    @Autowired private AssignTeacherRequestValidator validator;
    @MockitoBean private ClassRepository classRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    
    @Test
    void shouldValidateSuccessfully() {
        // Given - When - Then
        // Test successful validation
    }
    
    @Test
    void shouldThrowExceptionForInvalidTeacher() {
        // Given - When - Then
        // Test validation failure
    }
}
```

---

### Util Tests

- [ ] **AssignResourcesResponseUtil:**
  - [ ] Test buildResponse() with all parameters
  - [ ] Test isFullySuccessful() - true case
  - [ ] Test isFullySuccessful() - false case (conflicts exist)
  - [ ] Test calculateSuccessRate()
  - [ ] Test groupConflictsByReason()
  - [ ] Test all 15+ utility methods

- [ ] **AssignTeacherResponseUtil:**
  - [ ] Test buildSuccessResponse() with full assignment
  - [ ] Test buildSuccessResponse() with partial assignment
  - [ ] Test calculateAvailabilityStatus() - FULLY_AVAILABLE (100%)
  - [ ] Test calculateAvailabilityStatus() - PARTIALLY_AVAILABLE (>0%, <100%)
  - [ ] Test calculateAvailabilityStatus() - UNAVAILABLE (0%)
  - [ ] Test calculateAvailabilityPercentage()
  - [ ] Test hasGeneralSkill() - true case
  - [ ] Test hasGeneralSkill() - false case

**Test Pattern:**
```java
@SpringBootTest
@ActiveProfiles("test")
class AssignTeacherResponseUtilTest {
    @Test
    void shouldCalculateFullyAvailableStatus() {
        // Given
        Integer availableSessions = 36;
        Integer totalSessions = 36;
        
        // When
        AvailabilityStatus status = AssignTeacherResponseUtil.calculateAvailabilityStatus(
            availableSessions, totalSessions);
        
        // Then
        assertThat(status).isEqualTo(AvailabilityStatus.FULLY_AVAILABLE);
    }
    
    @Test
    void shouldCalculateAvailabilityPercentage() {
        // Given - When - Then
        Double percentage = AssignTeacherResponseUtil.calculateAvailabilityPercentage(28, 36);
        assertThat(percentage).isEqualTo(77.78);
    }
}
```

---

## 🔴 PHASE 3.2: API TESTS (Controller Layer)

**Priority:** 🔴 HIGH | **Estimated:** 3-4 hours

### Resource Assignment Endpoints

- [ ] **POST /api/v1/classes/{classId}/resources - Success Cases:**
  - [ ] Test successful assignment (no conflicts)
  - [ ] Test partial success (with conflicts)
  - [ ] Verify response format (ResponseObject<AssignResourcesResponse>)
  - [ ] Verify success message format

- [ ] **POST /api/v1/classes/{classId}/resources - Validation Errors:**
  - [ ] Test with empty request body
  - [ ] Test with invalid dayOfWeek
  - [ ] Test with non-existent resourceId
  - [ ] Test with class not in DRAFT status
  - [ ] Verify 400 BAD_REQUEST returned
  - [ ] Verify error message format

- [ ] **POST /api/v1/classes/{classId}/resources - Authorization:**
  - [ ] Test with ACADEMIC_AFFAIR role (should succeed)
  - [ ] Test with TEACHER role (should fail - 403)
  - [ ] Test with no authentication (should fail - 401)
  - [ ] Test with user not having access to branch (should fail)

- [ ] **POST /api/v1/classes/{classId}/resources - Not Found Errors:**
  - [ ] Test with non-existent classId (404)
  - [ ] Verify error response format

**Test Pattern:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class ClassControllerResourceAssignmentIT {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(username = "academic", roles = "ACADEMIC_AFFAIR")
    void shouldAssignResourcesSuccessfully() throws Exception {
        // Given
        Long classId = 1L;
        AssignResourcesRequest request = new AssignResourcesRequest();
        request.setDayOfWeekResourceMap(Map.of(1, 10L, 3, 10L, 5, 10L));
        
        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", classId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.successCount").exists())
            .andExpect(jsonPath("$.data.conflictCount").exists())
            .andExpect(jsonPath("$.data.processingTimeMs").exists());
    }
    
    @Test
    @WithMockUser(username = "teacher", roles = "TEACHER")
    void shouldReturn403ForUnauthorizedRole() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/resources", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden());
    }
}
```

---

### Teacher Assignment Endpoints

- [ ] **GET /api/v1/classes/{classId}/available-teachers - Success Cases:**
  - [ ] Test successful query with multiple teachers
  - [ ] Test with empty result (no teachers available)
  - [ ] Verify response format (ResponseObject<List<TeacherAvailabilityDTO>>)
  - [ ] Verify teachers sorted by availability DESC

- [ ] **GET /api/v1/classes/{classId}/available-teachers - Authorization:**
  - [ ] Test with ACADEMIC_AFFAIR role (should succeed)
  - [ ] Test with unauthorized role (should fail - 403)
  - [ ] Test with user not having access to branch (should fail)

- [ ] **POST /api/v1/classes/{classId}/teachers - Full Assignment:**
  - [ ] Test full assignment (sessionIds = null)
  - [ ] Verify all sessions assigned
  - [ ] Verify needsSubstitute = false
  - [ ] Verify success message format

- [ ] **POST /api/v1/classes/{classId}/teachers - Partial Assignment:**
  - [ ] Test partial assignment (specific sessionIds)
  - [ ] Verify only specified sessions assigned
  - [ ] Verify needsSubstitute = true
  - [ ] Verify remainingSessions count correct

- [ ] **POST /api/v1/classes/{classId}/teachers - GENERAL Skill:**
  - [ ] Test teacher with GENERAL skill bypasses validation
  - [ ] Test teacher without required skills fails (without GENERAL)

- [ ] **POST /api/v1/classes/{classId}/teachers - Validation Errors:**
  - [ ] Test with null teacherId (400)
  - [ ] Test with non-existent teacherId (404)
  - [ ] Test with non-existent sessionIds (400)
  - [ ] Test with class not in DRAFT status (400)
  - [ ] Test with teacher skill mismatch (400)

- [ ] **POST /api/v1/classes/{classId}/teachers - Authorization:**
  - [ ] Test with ACADEMIC_AFFAIR role (should succeed)
  - [ ] Test with unauthorized role (should fail - 403)

**Test Pattern:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class ClassControllerTeacherAssignmentIT {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(username = "academic", roles = "ACADEMIC_AFFAIR")
    void shouldGetAvailableTeachers() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/classes/{classId}/available-teachers", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].teacherId").exists())
            .andExpect(jsonPath("$.data[0].availabilityStatus").exists())
            .andExpect(jsonPath("$.data[0].conflictBreakdown").exists());
    }
    
    @Test
    @WithMockUser(username = "academic", roles = "ACADEMIC_AFFAIR")
    void shouldAssignTeacherFullMode() throws Exception {
        // Given
        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(null); // Full assignment
        
        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.assignedCount").exists())
            .andExpect(jsonPath("$.data.needsSubstitute").value(false))
            .andExpect(jsonPath("$.data.remainingSessions").value(0));
    }
    
    @Test
    @WithMockUser(username = "academic", roles = "ACADEMIC_AFFAIR")
    void shouldAssignTeacherPartialMode() throws Exception {
        // Given
        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(List.of(1L, 2L, 3L)); // Partial assignment
        
        // When & Then
        mockMvc.perform(post("/api/v1/classes/{classId}/teachers", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.needsSubstitute").value(true));
    }
}
```

---

## 🟡 PHASE 3.4: ERROR HANDLING & EDGE CASES

**Priority:** 🟡 MEDIUM | **Estimated:** 2-3 hours

### Error Scenarios (Covered in Unit/API Tests)

- [ ] **Invalid Input:**
  - [ ] Invalid schedule days (outside 0-6)
  - [ ] Null/empty required fields
  - [ ] Invalid data types

- [ ] **Not Found:**
  - [ ] Class not found (404)
  - [ ] Teacher not found (404)
  - [ ] Resource not found (404)
  - [ ] Session not found (404)

- [ ] **Unauthorized:**
  - [ ] User not authenticated (401)
  - [ ] User lacks required role (403)
  - [ ] User not assigned to branch (403)

- [ ] **Business Logic:**
  - [ ] Duplicate class code (400)
  - [ ] Course not approved (400)
  - [ ] Invalid state transitions (400)
  - [ ] Resource branch mismatch (400)
  - [ ] Teacher skill mismatch (400)
  - [ ] Insufficient resource capacity (400)

### Error Message Validation

- [ ] All error messages user-friendly (not technical)
- [ ] Error codes consistent (4000-4099 range for Class workflow)
- [ ] ResponseObject format correct for all errors
- [ ] Stack traces not exposed to clients

---

## 🟡 PHASE 3.5: CODE REVIEW & REFACTORING

**Priority:** 🟡 MEDIUM | **Estimated:** 2-3 hours

### Code Quality Checks

- [ ] **SOLID Principles:**
  - [ ] Single Responsibility - Each class has one purpose
  - [ ] Open/Closed - Extensible without modification
  - [ ] Liskov Substitution - Subtypes replaceable
  - [ ] Interface Segregation - Small, focused interfaces
  - [ ] Dependency Inversion - Depend on abstractions

- [ ] **DRY (Don't Repeat Yourself):**
  - [ ] No code duplication
  - [ ] Extract common logic to utilities
  - [ ] Reusable components

- [ ] **Exception Handling:**
  - [ ] Consistent error handling pattern
  - [ ] Appropriate error codes
  - [ ] GlobalExceptionHandler catches all exceptions

- [ ] **Logging:**
  - [ ] Info level for important operations
  - [ ] Warn level for recoverable errors
  - [ ] Error level for exceptions
  - [ ] Debug level for detailed info (if needed)
  - [ ] No sensitive data logged

- [ ] **Javadoc:**
  - [ ] All public methods documented
  - [ ] Complex logic explained
  - [ ] Parameters and return values described

### Refactoring Checklist

- [ ] **Extract Long Methods:**
  - [ ] Methods > 50 lines should be split
  - [ ] Complex logic extracted to private methods
  - [ ] Nested logic flattened

- [ ] **Remove Magic Numbers:**
  - [ ] Extract to constants
  - [ ] Use enums where appropriate
  - [ ] Document meaning

- [ ] **Simplify Complex Logic:**
  - [ ] Reduce nesting (early returns)
  - [ ] Extract conditions to methods
  - [ ] Use streams where appropriate

- [ ] **Naming Conventions:**
  - [ ] Clear, descriptive names
  - [ ] Consistent naming patterns
  - [ ] No abbreviations (unless well-known)

---

## 🟢 PHASE 3.6: OPENAPI DOCUMENTATION REVIEW

**Priority:** 🟢 LOW | **Estimated:** 1-2 hours

**Note:** Most documentation already complete in Phase 2. Just need review.

### Review Existing Docs

- [ ] **All Endpoints Have @Operation:**
  - [ ] Summary clear and concise
  - [ ] Description explains approach (HYBRID/PRE-CHECK)
  - [ ] Use cases documented

- [ ] **All Parameters Have @Parameter:**
  - [ ] Description explains purpose
  - [ ] Examples provided where helpful
  - [ ] Required/optional clearly marked

- [ ] **Request/Response Examples:**
  - [ ] Success examples accurate
  - [ ] Error examples provided
  - [ ] Edge cases documented

- [ ] **Error Response Examples:**
  - [ ] 400 BAD_REQUEST examples
  - [ ] 401 UNAUTHORIZED examples
  - [ ] 403 FORBIDDEN examples
  - [ ] 404 NOT_FOUND examples

### Testing

- [ ] **Swagger UI:**
  - [ ] Accessible at `/swagger-ui.html`
  - [ ] All endpoints visible
  - [ ] Grouped logically

- [ ] **Try-it-out:**
  - [ ] Works for all GET endpoints
  - [ ] Works for all POST endpoints
  - [ ] Examples auto-populate correctly

- [ ] **Examples Accuracy:**
  - [ ] Response format matches actual responses
  - [ ] Field names correct
  - [ ] Data types correct

---

## 🟢 PHASE 3.3: INTEGRATION TESTS (Repository Layer) ⏸️

**Priority:** 🟢 DEFERRED | **Estimated:** 4-5 hours

**Note:** Deferred to later phase. Focus on Unit + API tests first.

### Why Defer Integration Tests?

1. **Setup Complexity:** Requires Testcontainers, PostgreSQL, seed data
2. **Execution Time:** Slower than unit tests (seconds vs milliseconds)
3. **Coverage:** Unit + API tests already cover most scenarios
4. **Value:** Adds 20% extra confidence vs 80% from Unit/API
5. **Timing:** Can be done after deployment if needed

### Integration Tests Plan (When Doing Later)

- [ ] **Repository Tests with Testcontainers:**
  - [ ] `TeacherRepository.findAvailableTeachersWithPrecheck()` - CTE query
  - [ ] `SessionResourceRepository.bulkInsertResourcesForDayOfWeek()` - Bulk insert
  - [ ] `TeachingSlotRepository.bulkAssignTeacher()` - Full assignment
  - [ ] `TeachingSlotRepository.bulkAssignTeacherToSessions()` - Partial assignment

- [ ] **Performance Benchmarks:**
  - [ ] PRE-CHECK query < 100ms for 10 teachers × 36 sessions
  - [ ] Bulk insert < 50ms for 36 sessions
  - [ ] Resource assignment < 200ms total
  - [ ] Teacher assignment < 150ms total

**Test Pattern:**
```java
@DataJpaTest
@Testcontainers
class TeacherRepositoryIT extends AbstractRepositoryTest {
    @Autowired private TeacherRepository repository;
    
    @Test
    void shouldFindAvailableTeachersWithPrecheck() {
        // Given - Setup test data
        // When - Execute CTE query
        // Then - Verify results
    }
}
```

---

## 🟢 PHASE 3.7: PERFORMANCE OPTIMIZATION ⏸️

**Priority:** 🟢 DEFERRED | **Estimated:** 3-4 hours

**Note:** Deferred until performance issues identified.

### When to Do Performance Optimization?

1. **After Benchmarking:** Only optimize if targets not met
2. **After Production Load:** Real usage patterns reveal bottlenecks
3. **After Profiling:** Identify actual slow queries

### Performance Optimization Plan (If Needed)

- [ ] **Benchmarking:**
  - [ ] Measure total workflow time
  - [ ] Measure individual operation times
  - [ ] Identify bottlenecks (CTE query? Bulk insert? Java logic?)

- [ ] **Database Optimizations:**
  - [ ] Add indexes on frequently queried columns:
    - [ ] `session (class_id, date)` - For session queries
    - [ ] `session_resource (session_id, resource_id)` - For conflict checks
    - [ ] `teaching_slot (session_id, teacher_id)` - For teacher queries
    - [ ] `teacher_availability (teacher_id, time_slot_template_id, day_of_week)` - Already composite PK
  - [ ] Analyze CTE query execution plan (EXPLAIN ANALYZE)
  - [ ] Optimize CROSS JOIN in teacher_session_checks

- [ ] **Query Optimizations:**
  - [ ] Review CTE steps for redundancy
  - [ ] Consider materialized views for teacher availability
  - [ ] Split complex queries if needed

- [ ] **Caching Strategies (If Needed):**
  - [ ] Cache teacher skills (rarely change)
  - [ ] Cache resource availability patterns
  - [ ] Cache time slot templates

---

## 🎯 SUCCESS CRITERIA

### Phase 3 Complete When:

- [ ] ✅ Unit tests passing (Service + Validator + Util layers)
- [ ] ✅ API tests passing (All controller endpoints)
- [ ] ✅ Error handling verified (Edge cases covered)
- [ ] ✅ Code review passed (Quality standards met)
- [ ] ✅ OpenAPI documentation reviewed
- [ ] ✅ Ready for frontend integration
- [ ] ⏸️ Integration tests (Deferred - can be done later)
- [ ] ⏸️ Performance optimization (Deferred - optimize when needed)

---

## 📊 TIME BREAKDOWN

| Phase      | Task                        | Priority  | Estimated | Status |
| ---------- | --------------------------- | --------- | --------- | ------ |
| Phase 3.1  | Unit Tests                  | 🔴 HIGH   | 4-5 hours | ⏳     |
| Phase 3.2  | API Tests                   | 🔴 HIGH   | 3-4 hours | ⏳     |
| Phase 3.4  | Error Handling              | 🟡 MEDIUM | 2-3 hours | ⏳     |
| Phase 3.5  | Code Review                 | 🟡 MEDIUM | 2-3 hours | ⏳     |
| Phase 3.6  | OpenAPI Review              | 🟢 LOW    | 1-2 hours | ⏳     |
| **Core**   | **Total (Must Have)**       | -         | **12-17h**| -      |
| Phase 3.3  | ⏸️ Integration Tests       | 🟢 DEFER  | 4-5 hours | ⏸️     |
| Phase 3.7  | ⏸️ Performance Optimize    | 🟢 DEFER  | 3-4 hours | ⏸️     |
| **Defer**  | **Total (Nice to Have)**    | -         | **7-9h**  | -      |
| **Total**  | **All Tests**               | -         | **19-26h**| -      |

**Recommended:** Focus on Core Tests (12-17h) first

---

## 🚀 RECOMMENDED WORKFLOW

### Option 1: Full Core Tests (Recommended) ✅

```
Day 1: Phase 3.1 - Unit Tests (4-5h)
  ├─ Morning: ResourceAssignmentServiceImpl tests
  ├─ Afternoon: TeacherAssignmentServiceImpl tests
  └─ Evening: Validator + Util tests

Day 2: Phase 3.2 - API Tests (3-4h)
  ├─ Morning: Resource assignment endpoints
  └─ Afternoon: Teacher assignment endpoints

Day 3: Polish & Review (5-8h)
  ├─ Morning: Error handling verification (2-3h)
  ├─ Afternoon: Code review & refactoring (2-3h)
  └─ Evening: OpenAPI documentation review (1-2h)

Total: 12-17 hours over 3 days
```

### Option 2: Minimal Viable (Fastest) ⚡

```
Day 1: Phase 3.1 - Unit Tests Only (4-5h)
  └─ Focus on critical service tests

Manual Testing:
  └─ Test API endpoints with Postman/curl

Commit & Push
  └─ Deploy with unit tests only

Later: API + Integration tests
```

### Option 3: Skip All Tests (Not Recommended) ⚠️

```
Manual Testing Only:
  └─ Test critical flows manually

Commit & Push:
  └─ Deploy without automated tests

Risk:
  └─ Bugs may reach production
  └─ Regression issues when making changes
```

---

## 📝 TESTING BEST PRACTICES

### General Principles

1. **AAA Pattern:** Arrange - Act - Assert
2. **One Assert Per Test:** Test one thing at a time
3. **Descriptive Names:** `shouldAssignTeacherWhenAllSessionsAvailable()`
4. **Test Data Builders:** Use `TestDataBuilder` for consistent test data
5. **Mock External Dependencies:** Mock repositories, don't use real database in unit tests
6. **Clean Tests:** Tests should be independent and repeatable

### Spring Boot Testing Patterns

1. **Use @SpringBootTest:** Load full Spring context
2. **Use @ActiveProfiles("test"):** Separate test configuration
3. **Use @MockitoBean:** Mock dependencies in Spring context
4. **Use @WithMockUser:** Mock authentication for controller tests
5. **Use AssertJ:** Fluent assertions for better readability

### Common Pitfalls to Avoid

1. ❌ Don't use `@Mock` + `@InjectMocks` (bypasses Spring)
2. ❌ Don't test implementation details (test behavior)
3. ❌ Don't share state between tests (use `@BeforeEach`)
4. ❌ Don't ignore test failures (fix or remove)
5. ❌ Don't skip edge cases (null, empty, boundary values)

---

## 🔗 RELATED DOCUMENTS

- **Phase 1-2 Implementation:** `/docs/create-class/create-class-implementation-checklist-v2.md`
- **Phase 2.3 Summary:** `/docs/create-class/review/phase-2.3-teacher-assignment-summary.md`
- **Testing README:** `/src/test/README.md`
- **Architecture Guide:** `/AGENTS.md`, `/CLAUDE.md`

---

**Last Updated:** 2025-11-08  
**Status:** ⏳ READY TO START  
**Next Step:** Phase 3.1 - Unit Tests (4-5 hours)
