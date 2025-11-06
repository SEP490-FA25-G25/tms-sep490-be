# CREATE CLASS WORKFLOW - TEST PLAN

**Status:** 📋 PLANNED
**Created:** 2025-11-05
**Reference:**
- `create-class-implementation-plan.md`
- `create-class-implementation-checklist.md`
- Enrollment test pattern (`EnrollmentServiceImplTest.java`, `EnrollmentRepositoryTest.java`)

---

## TESTING STRATEGY OVERVIEW

### Test Pyramid
```
           ╱╲
          ╱  ╲  Integration Tests (Controllers)
         ╱────╲  ~15% (30-40 tests)
        ╱      ╲
       ╱────────╲ Unit Tests (Services)
      ╱          ╲ ~70% (140-160 tests)
     ╱────────────╲
    ╱──────────────╲ Repository Tests
   ╱                ╲ ~15% (30-40 tests)
  ╱──────────────────╲
```

**Total Estimated Tests:** 200-240 test cases

---

## TEST FRAMEWORK & PATTERNS

### Modern Spring Boot 3.5.7 Testing Approach

**Service Tests:** `@SpringBootTest + @MockitoBean`
```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SessionGenerationService Unit Tests")
class SessionGenerationServiceImplTest {
    @MockitoBean private SessionRepository sessionRepository;
    @Autowired private SessionGenerationService sessionGenerationService;

    @Test
    @DisplayName("Should generate 36 sessions for Mon/Wed/Fri schedule")
    void shouldGenerateSessionsForMonWedFriSchedule() {
        // AAA pattern (Arrange-Act-Assert)
    }
}
```

**Repository Tests:** `@DataJpaTest + Testcontainers`
```java
@DataJpaTest
@DisplayName("ClassRepository Integration Tests")
class ClassRepositoryTest extends AbstractRepositoryTest {
    @Autowired private ClassRepository classRepository;

    @Test
    @DisplayName("Should find class by branch and code")
    void shouldFindClassByBranchAndCode() {
        // Test with real PostgreSQL via Testcontainers
    }
}
```

**Controller Tests:** `@SpringBootTest + @AutoConfigureMockMvc + @Transactional`
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ClassController - Create Class Integration Tests")
class ClassControllerIT {
    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/classes - Should create class with session generation")
    void shouldCreateClassWithSessionGeneration() throws Exception {
        mockMvc.perform(post("/api/v1/classes")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

---

## PHASE 1: SERVICE LAYER TESTS (140-160 tests)

### 1.1 SessionGenerationServiceImplTest (6 tests)

**File:** `src/test/java/org/fyp/tmssep490be/services/impl/SessionGenerationServiceImplTest.java`

**Test Cases:**

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SessionGenerationService Unit Tests")
class SessionGenerationServiceImplTest {

    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private CourseSessionRepository courseSessionRepository;
    @Autowired private SessionGenerationService sessionGenerationService;

    // ==================== Happy Path Tests ====================

    @Test
    @DisplayName("Should generate 36 sessions for Mon/Wed/Fri schedule")
    void shouldGenerateSessionsForMonWedFriSchedule() {
        // Given: Class with Mon/Wed/Fri schedule, course with 36 sessions
        // When: generateSessionsForClass()
        // Then: Returns 36 sessions with correct dates (cycling Mon/Wed/Fri)
    }

    @Test
    @DisplayName("Should generate 24 sessions for Tue/Thu schedule")
    void shouldGenerateSessionsForTueThurSchedule() {
        // Given: Class with Tue/Thu schedule, course with 24 sessions
        // When: generateSessionsForClass()
        // Then: Returns 24 sessions with correct dates (cycling Tue/Thu)
    }

    @Test
    @DisplayName("Should link sessions to course session templates")
    void shouldLinkToCourseSessionTemplate() {
        // Given: Class with course containing course_sessions
        // When: generateSessionsForClass()
        // Then: Each session has course_session_id pointing to template
    }

    @Test
    @DisplayName("Should set default session status to PLANNED")
    void shouldSetDefaultSessionStatus() {
        // Given: New class
        // When: generateSessionsForClass()
        // Then: All sessions have status = PLANNED, type = CLASS
    }

    // ==================== Date Calculation Tests ====================

    @Test
    @DisplayName("Should calculate dates correctly with week rollover")
    void shouldHandleWeekRollover() {
        // Given: Class starting on Monday with Mon/Wed/Fri schedule
        // When: Generate 6 sessions
        // Then: Week 1: Mon(1), Wed(3), Fri(5), Week 2: Mon(8), Wed(10), Fri(12)
    }

    @Test
    @DisplayName("Should handle schedule days in ISODOW format (1=Mon, 7=Sun)")
    void shouldHandleIsodowDayOfWeek() {
        // Given: scheduleDays = [1, 3, 5] (Mon, Wed, Fri)
        // When: generateSessionsForClass()
        // Then: Dates correspond to Monday, Wednesday, Friday
    }
}
```

**Estimated Time:** 3-4 hours

---

### 1.2 ClassServiceImplTest - Create Class (9 tests)

**File:** `src/test/java/org/fyp/tmssep490be/services/impl/ClassServiceImplTest.java`

**Test Cases:**

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ClassService - Create Class Unit Tests")
class ClassServiceImplTest {

    @MockitoBean private ClassRepository classRepository;
    @MockitoBean private CourseRepository courseRepository;
    @MockitoBean private BranchRepository branchRepository;
    @MockitoBean private SessionGenerationService sessionGenerationService;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private UserBranchesRepository userBranchesRepository;
    @Autowired private ClassService classService;

    // ==================== Happy Path Tests ====================

    @Test
    @DisplayName("Should create class successfully with session generation")
    void shouldCreateClassSuccessfully() {
        // Given: Valid CreateClassRequest, approved course, user has branch access
        // When: createClass()
        // Then: Class saved with status=DRAFT, approvalStatus=PENDING, 36 sessions generated
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("Should throw exception when class code already exists for branch")
    void shouldFailWhenClassCodeDuplicate() {
        // Given: Class with code "ENG-A1-001" already exists for branch
        // When: createClass() with same code
        // Then: Throws CustomException(CLASS_CODE_DUPLICATE)
    }

    @Test
    @DisplayName("Should throw exception when course not approved")
    void shouldFailWhenCourseNotApproved() {
        // Given: Course with approval_status = PENDING
        // When: createClass()
        // Then: Throws CustomException(COURSE_NOT_APPROVED)
    }

    @Test
    @DisplayName("Should throw exception when start date not in schedule days")
    void shouldFailWhenStartDateNotInScheduleDays() {
        // Given: start_date = Tuesday, schedule_days = [1, 3, 5] (Mon/Wed/Fri)
        // When: createClass()
        // Then: Throws CustomException(START_DATE_NOT_IN_SCHEDULE_DAYS)
    }

    @Test
    @DisplayName("Should throw exception when schedule days invalid (0, 8, null)")
    void shouldFailWhenInvalidScheduleDays() {
        // Given: schedule_days = [0, 8, 9] (invalid ISODOW)
        // When: createClass()
        // Then: Throws CustomException(INVALID_SCHEDULE_DAYS)
    }

    @Test
    @DisplayName("Should throw exception when user has no access to branch")
    void shouldFailWhenUserNoAccessToBranch() {
        // Given: User not assigned to branch
        // When: createClass()
        // Then: Throws CustomException(UNAUTHORIZED_ACCESS)
    }

    @Test
    @DisplayName("Should throw exception when course not found")
    void shouldFailWhenCourseNotFound() {
        // Given: courseId = 999 (non-existent)
        // When: createClass()
        // Then: Throws EntityNotFoundException("Course not found")
    }

    @Test
    @DisplayName("Should throw exception when branch not found")
    void shouldFailWhenBranchNotFound() {
        // Given: branchId = 999 (non-existent)
        // When: createClass()
        // Then: Throws EntityNotFoundException("Branch not found")
    }

    // ==================== Transaction Tests ====================

    @Test
    @DisplayName("Should rollback class creation if session generation fails")
    void shouldRollbackOnSessionGenerationFailure() {
        // Given: Valid class data but sessionGenerationService throws exception
        // When: createClass()
        // Then: Transaction rolled back, no class or sessions saved
    }
}
```

**Estimated Time:** 5-6 hours

---

### 1.3 ResourceAssignmentServiceImplTest - HYBRID (6 tests)

**File:** `src/test/java/org/fyp/tmssep490be/services/impl/ResourceAssignmentServiceImplTest.java`

**Test Cases:**

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ResourceAssignmentService - HYBRID Auto-propagation Tests")
class ResourceAssignmentServiceImplTest {

    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @Autowired private ResourceAssignmentService resourceAssignmentService;

    // ==================== Happy Path Tests ====================

    @Test
    @DisplayName("Should bulk assign resources successfully (no conflicts)")
    void shouldBulkAssignResourcesSuccessfully() {
        // Given: 36 sessions, Room 101 available for all
        // When: assignResourcesWithPropagation(classId, {1: Room101, 3: Room101, 5: Room101})
        // Then: 36 sessions assigned, successCount=36, conflictCount=0
    }

    @Test
    @DisplayName("Should detect resource conflicts with another class")
    void shouldDetectResourceConflicts() {
        // Given: 36 sessions, Room 101 booked on 3 dates
        // When: assignResourcesWithPropagation()
        // Then: successCount=33, conflictCount=3, conflicts list has details
    }

    @Test
    @DisplayName("Should handle multiple rooms per schedule (Mon: R101, Wed: R102, Fri: R103)")
    void shouldHandleMultipleRoomsPerSchedule() {
        // Given: Different rooms for different days
        // When: assignResourcesWithPropagation({1: R101, 3: R102, 5: R103})
        // Then: Sessions assigned to correct rooms per day
    }

    // ==================== Conflict Analysis Tests ====================

    @Test
    @DisplayName("Should provide conflict details (conflicting class code)")
    void shouldProvideConflictDetails() {
        // Given: Room 101 booked by "ENG-B1-002" on 2025-01-15
        // When: Assign Room 101 to session on 2025-01-15
        // Then: Conflict reason = "Room booked by Class ENG-B1-002"
    }

    // ==================== Query Available Resources Tests ====================

    @Test
    @DisplayName("Should query available resources filtered by capacity")
    void shouldQueryAvailableResources() {
        // Given: Class max_capacity = 20
        // When: queryAvailableResources(sessionId)
        // Then: Only rooms with capacity >= 20 returned
    }

    @Test
    @DisplayName("Should exclude conflicting resources from available list")
    void shouldExcludeConflictingResources() {
        // Given: Room 101 booked on 2025-01-15 at 08:00-10:00
        // When: queryAvailableResources(session on 2025-01-15 at 08:00-10:00)
        // Then: Room 101 not in available list
    }
}
```

**Estimated Time:** 5-6 hours

---

### 1.4 TeacherAssignmentServiceImplTest - PRE-CHECK (9 tests)

**File:** `src/test/java/org/fyp/tmssep490be/services/impl/TeacherAssignmentServiceImplTest.java`

**Test Cases:**

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TeacherAssignmentService - PRE-CHECK Tests")
class TeacherAssignmentServiceImplTest {

    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private TeacherAvailabilityRepository teacherAvailabilityRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @Autowired private TeacherAssignmentService teacherAssignmentService;

    // ==================== PRE-CHECK Query Tests ====================

    @Test
    @DisplayName("Should query teachers with full availability (10/10)")
    void shouldReturnFullyAvailableTeachers() {
        // Given: Teacher Jane has availability for all 10 sessions, no conflicts
        // When: queryAvailableTeachersWithPrecheck(classId, skillSet)
        // Then: Jane: availabilityStatus=FULLY_AVAILABLE, totalSessions=10, availableSessions=10
    }

    @Test
    @DisplayName("Should query teachers with partial availability (7/10)")
    void shouldReturnPartiallyAvailableTeachers() {
        // Given: Teacher John has 3 teaching conflicts
        // When: queryAvailableTeachersWithPrecheck(classId, skillSet)
        // Then: John: availabilityStatus=PARTIALLY_AVAILABLE, totalSessions=10, availableSessions=7, conflicts=[3 teaching]
    }

    @Test
    @DisplayName("Should detect no availability conflicts")
    void shouldDetectNoAvailabilityConflict() {
        // Given: Teacher Bob has no registered availability for Mon 08:00
        // When: queryAvailableTeachersWithPrecheck(classId)
        // Then: Bob: conflict breakdown includes "no_availability_count=5"
    }

    @Test
    @DisplayName("Should detect teaching conflicts")
    void shouldDetectTeachingConflict() {
        // Given: Teacher Alice teaching "ENG-B1-002" on Mon 08:00
        // When: queryAvailableTeachersWithPrecheck(classId)
        // Then: Alice: conflict breakdown includes "teaching_conflict_count=3"
    }

    @Test
    @DisplayName("Should detect leave conflicts")
    void shouldDetectLeaveConflict() {
        // Given: Teacher Charlie on approved leave 2025-01-15 to 2025-01-20
        // When: queryAvailableTeachersWithPrecheck(classId)
        // Then: Charlie: conflict breakdown includes "leave_conflict_count=2"
    }

    // ==================== 'general' Skill Tests ====================

    @Test
    @DisplayName("Should match teacher with 'general' skill to ANY session")
    void shouldMatchGeneralSkillToAnySessions() {
        // Given: Teacher Eve has skill='general'
        // When: queryAvailableTeachersWithPrecheck(classId with Listening/Speaking/Writing sessions)
        // Then: Eve can teach ALL sessions (totalSessions=36)
    }

    @Test
    @DisplayName("Should match teacher with specific skills to matching sessions")
    void shouldMatchSpecificSkills() {
        // Given: Teacher Frank has skills=['listening', 'reading']
        // When: queryAvailableTeachersWithPrecheck(classId with 10 Listening + 10 Reading sessions)
        // Then: Frank: totalSessions=20 (only Listening + Reading)
    }

    // ==================== Direct Assignment Tests ====================

    @Test
    @DisplayName("Should assign teacher to all available sessions successfully")
    void shouldAssignTeacherSuccessfully() {
        // Given: Teacher Jane fully available for 10 sessions
        // When: assignTeacher(classId, teacherId=Jane, sessionIds=null)
        // Then: 10 teaching_slots created, assignedCount=10
    }

    @Test
    @DisplayName("Should assign teacher to specific session IDs only")
    void shouldAssignTeacherToSpecificSessions() {
        // Given: Teacher John partially available (7/10), conflicts on 3 sessions
        // When: assignTeacher(classId, teacherId=John, sessionIds=[1,2,3,4,5,6,7])
        // Then: 7 teaching_slots created for available sessions only
    }
}
```

**Estimated Time:** 7-8 hours

---

### 1.5 ValidationServiceImplTest (6 tests)

**File:** `src/test/java/org/fyp/tmssep490be/services/impl/ValidationServiceImplTest.java`

**Test Cases:**

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ValidationService Unit Tests")
class ValidationServiceImplTest {

    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ClassRepository classRepository;
    @Autowired private ValidationService validationService;

    // ==================== Happy Path Tests ====================

    @Test
    @DisplayName("Should pass validation when class complete")
    void shouldPassValidationWhenComplete() {
        // Given: All 36 sessions have timeslot + resource + teacher
        // When: validateClassComplete(classId)
        // Then: isValid=true, canSubmit=true, errors=[], warnings=[]
    }

    // ==================== Error Detection Tests ====================

    @Test
    @DisplayName("Should fail when sessions missing timeslot")
    void shouldFailWhenMissingTimeslots() {
        // Given: 5 sessions without time_slot_template_id
        // When: validateClassComplete(classId)
        // Then: isValid=false, errors=["5 sessions missing timeslot"]
    }

    @Test
    @DisplayName("Should fail when sessions missing resource")
    void shouldFailWhenMissingResources() {
        // Given: 3 sessions without session_resource entry
        // When: validateClassComplete(classId)
        // Then: isValid=false, errors=["3 sessions missing resource"]
    }

    @Test
    @DisplayName("Should fail when sessions missing teacher")
    void shouldFailWhenMissingTeachers() {
        // Given: 8 sessions without teaching_slot entry
        // When: validateClassComplete(classId)
        // Then: isValid=false, errors=["8 sessions missing teacher"]
    }

    // ==================== Warning Tests ====================

    @Test
    @DisplayName("Should warn when using multiple teachers for same skill group")
    void shouldWarnMultipleTeachers() {
        // Given: Teacher A teaches Listening sessions 1-5, Teacher B teaches Listening 6-10
        // When: validateClassComplete(classId)
        // Then: warnings=["Using multiple teachers for 1 skill groups"]
    }

    @Test
    @DisplayName("Should warn when start date is in the past")
    void shouldWarnPastStartDate() {
        // Given: Class start_date = 2025-01-01 (past)
        // When: validateClassComplete(classId)
        // Then: warnings=["Start date is in the past"]
    }
}
```

**Estimated Time:** 4-5 hours

---

### 1.6 ApprovalServiceImplTest (6 tests)

**File:** `src/test/java/org/fyp/tmssep490be/services/impl/ApprovalServiceImplTest.java`

**Test Cases:**

```java
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ApprovalService Unit Tests")
class ApprovalServiceImplTest {

    @MockitoBean private ClassRepository classRepository;
    @MockitoBean private ValidationService validationService;
    @Autowired private ApprovalService approvalService;

    // ==================== Submit Tests ====================

    @Test
    @DisplayName("Should submit class for approval when validation passes")
    void shouldSubmitClassForApproval() {
        // Given: Class DRAFT + complete validation
        // When: submitForApproval(classId, userId)
        // Then: submitted_at set, approval_status remains PENDING
    }

    @Test
    @DisplayName("Should throw exception when submitting incomplete class")
    void shouldNotSubmitIncompleteClass() {
        // Given: Class with incomplete sessions (validation fails)
        // When: submitForApproval(classId, userId)
        // Then: Throws CustomException(CLASS_INCOMPLETE_CANNOT_SUBMIT)
    }

    // ==================== Approve Tests ====================

    @Test
    @DisplayName("Should approve class successfully")
    void shouldApproveClass() {
        // Given: Class with submitted_at != null
        // When: approveClass(classId, approverUserId)
        // Then: status=SCHEDULED, approval_status=APPROVED, approved_by, approved_at set
    }

    @Test
    @DisplayName("Should throw exception when approving unsubmitted class")
    void shouldNotApproveUnsubmittedClass() {
        // Given: Class with submitted_at = null
        // When: approveClass(classId, approverUserId)
        // Then: Throws CustomException(CLASS_NOT_SUBMITTED)
    }

    // ==================== Reject Tests ====================

    @Test
    @DisplayName("Should reject class with reason")
    void shouldRejectClass() {
        // Given: Class with submitted_at != null, reason provided
        // When: rejectClass(classId, reason, rejecterUserId)
        // Then: status=DRAFT, approval_status=REJECTED, rejection_reason set, submitted_at reset
    }

    @Test
    @DisplayName("Should throw exception when rejection reason too short")
    void shouldRequireRejectionReason() {
        // Given: reason = "Bad" (< 20 chars)
        // When: rejectClass(classId, reason, rejecterUserId)
        // Then: Throws CustomException(REJECTION_REASON_REQUIRED)
    }
}
```

**Estimated Time:** 4-5 hours

---

## PHASE 2: REPOSITORY LAYER TESTS (30-40 tests)

### 2.1 ClassRepositoryTest (8 tests)

**File:** `src/test/java/org/fyp/tmssep490be/repositories/ClassRepositoryTest.java`

**Test Cases:**

```java
@DataJpaTest
@DisplayName("ClassRepository Integration Tests")
class ClassRepositoryTest extends AbstractRepositoryTest {

    @Autowired private ClassRepository classRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CourseRepository courseRepository;

    @Test
    @DisplayName("Should find class by branch and code")
    void shouldFindClassByBranchAndCode() {
        // Given: Class "ENG-A1-001" for branch 1
        // When: findByBranchIdAndCode(1, "ENG-A1-001")
        // Then: Returns class
    }

    @Test
    @DisplayName("Should return empty when class code not found")
    void shouldReturnEmptyWhenCodeNotFound() {
        // Given: No class with code "NONEXISTENT"
        // When: findByBranchIdAndCode(1, "NONEXISTENT")
        // Then: Returns Optional.empty()
    }

    @Test
    @DisplayName("Should count sessions without timeslot")
    void shouldCountSessionsWithoutTimeslot() {
        // Given: 5 sessions without time_slot_template_id
        // When: countSessionsWithoutTimeslot(classId)
        // Then: Returns 5
    }

    @Test
    @DisplayName("Should count sessions without resource")
    void shouldCountSessionsWithoutResource() {
        // Given: 3 sessions without session_resource entry
        // When: countSessionsWithoutResource(classId)
        // Then: Returns 3
    }

    @Test
    @DisplayName("Should count sessions without teacher")
    void shouldCountSessionsWithoutTeacher() {
        // Given: 8 sessions without teaching_slot entry
        // When: countSessionsWithoutTeacher(classId)
        // Then: Returns 8
    }

    @Test
    @DisplayName("Should enforce unique constraint on (branch_id, code)")
    void shouldEnforceUniqueBranchCode() {
        // Given: Class "ENG-A1-001" already exists for branch 1
        // When: Save another class with same code for branch 1
        // Then: Throws DataIntegrityViolationException
    }

    @Test
    @DisplayName("Should allow same code for different branches")
    void shouldAllowSameCodeForDifferentBranches() {
        // Given: Class "ENG-A1-001" for branch 1
        // When: Save class "ENG-A1-001" for branch 2
        // Then: Success (different branches can have same code)
    }

    @Test
    @DisplayName("Should save approval fields correctly")
    void shouldSaveApprovalFields() {
        // Given: Class with submitted_at, approved_by, approved_at
        // When: Save class
        // Then: Fields persisted correctly
    }
}
```

**Estimated Time:** 4-5 hours

---

### 2.2 SessionRepositoryTest (6 tests)

**File:** `src/test/java/org/fyp/tmssep490be/repositories/SessionRepositoryTest.java`

**Test Cases:**

```java
@DataJpaTest
@DisplayName("SessionRepository Integration Tests")
class SessionRepositoryTest extends AbstractRepositoryTest {

    @Autowired private SessionRepository sessionRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private TimeSlotTemplateRepository timeSlotTemplateRepository;

    @Test
    @DisplayName("Should bulk update time slots by day of week (ISODOW)")
    void shouldBulkUpdateTimeSlotsByDayOfWeek() {
        // Given: 36 sessions (12 Mon, 12 Wed, 12 Fri)
        // When: updateTimeSlotByDayOfWeek(classId, dayOfWeek=1, timeSlotId=5)
        // Then: 12 Monday sessions updated, returns 12
    }

    @Test
    @DisplayName("Should find unassigned sessions by day of week")
    void shouldFindUnassignedSessionsByDayOfWeek() {
        // Given: 12 Monday sessions, 3 have resources assigned
        // When: findUnassignedSessionsByDayOfWeek(classId, dayOfWeek=1)
        // Then: Returns 9 unassigned sessions
    }

    @Test
    @DisplayName("Should find conflicting session (same date + timeslot)")
    void shouldFindConflictingSession() {
        // Given: Room 101 assigned to session on 2025-01-15 08:00-10:00
        // When: findConflictingSession(resourceId=101, date=2025-01-15, timeSlotId=5)
        // Then: Returns conflicting session
    }

    @Test
    @DisplayName("Should return empty when no conflicting session")
    void shouldReturnEmptyWhenNoConflict() {
        // Given: No session on 2025-01-20 08:00-10:00
        // When: findConflictingSession(resourceId=101, date=2025-01-20, timeSlotId=5)
        // Then: Returns Optional.empty()
    }

    @Test
    @DisplayName("Should delete sessions by class ID")
    void shouldDeleteSessionsByClassId() {
        // Given: 36 sessions for class 1
        // When: deleteByClassEntityId(1)
        // Then: All 36 sessions deleted
    }

    @Test
    @DisplayName("Should handle ISODOW extraction correctly (1=Mon, 7=Sun)")
    void shouldHandleIsodowExtraction() {
        // Given: Session on 2025-01-06 (Monday)
        // When: Query EXTRACT(ISODOW FROM date)
        // Then: Returns 1
    }
}
```

**Estimated Time:** 4-5 hours

---

### 2.3 SessionResourceRepositoryTest (4 tests)

**File:** `src/test/java/org/fyp/tmssep490be/repositories/SessionResourceRepositoryTest.java`

**Test Cases:**

```java
@DataJpaTest
@DisplayName("SessionResourceRepository Integration Tests")
class SessionResourceRepositoryTest extends AbstractRepositoryTest {

    @Autowired private SessionResourceRepository sessionResourceRepository;
    @Autowired private SessionRepository sessionRepository;

    @Test
    @DisplayName("Should bulk assign resources and return session IDs")
    void shouldBulkAssignResourcesWithReturning() {
        // Given: 12 Monday sessions available
        // When: bulkAssignResource(classId, dayOfWeek=1, resourceId=101, resourceType='room')
        // Then: Returns list of 12 session IDs
    }

    @Test
    @DisplayName("Should skip sessions with conflicts during bulk assign")
    void shouldSkipConflictingSessionsDuringBulkAssign() {
        // Given: 12 Monday sessions, Room 101 booked on 3 dates
        // When: bulkAssignResource(classId, dayOfWeek=1, resourceId=101)
        // Then: Returns 9 session IDs (3 skipped)
    }

    @Test
    @DisplayName("Should enforce unique constraint on (session_id, resource_id)")
    void shouldEnforceUniqueSessionResource() {
        // Given: SessionResource (session=1, resource=101) exists
        // When: Insert duplicate (session=1, resource=101)
        // Then: Throws DataIntegrityViolationException
    }

    @Test
    @DisplayName("Should cascade delete session resources when session deleted")
    void shouldCascadeDeleteSessionResources() {
        // Given: Session 1 with 2 session_resource entries
        // When: Delete session 1
        // Then: Session resources automatically deleted (ON DELETE CASCADE)
    }
}
```

**Estimated Time:** 3-4 hours

---

### 2.4 TeacherRepositoryTest (5 tests)

**File:** `src/test/java/org/fyp/tmssep490be/repositories/TeacherRepositoryTest.java`

**Test Cases:**

```java
@DataJpaTest
@DisplayName("TeacherRepository - PRE-CHECK Query Tests")
class TeacherRepositoryTest extends AbstractRepositoryTest {

    @Autowired private TeacherRepository teacherRepository;
    @Autowired private TeacherAvailabilityRepository teacherAvailabilityRepository;

    @Test
    @DisplayName("Should execute PRE-CHECK CTE query successfully")
    void shouldExecutePrecheckQuery() {
        // Given: Class with 10 sessions, 3 teachers with various skills
        // When: findAvailableTeachersWithPrecheck(classId)
        // Then: Returns Object[] with teacher info + availability breakdown
    }

    @Test
    @DisplayName("Should calculate availability percentage correctly")
    void shouldCalculateAvailabilityPercentage() {
        // Given: Teacher with 7/10 available sessions
        // When: findAvailableTeachersWithPrecheck(classId)
        // Then: availability_percentage = 70
    }

    @Test
    @DisplayName("Should order teachers by contract_type, available_sessions DESC")
    void shouldOrderTeachersByContractAndAvailability() {
        // Given: FULL_TIME teachers first, then PART_TIME, sorted by availability
        // When: findAvailableTeachersWithPrecheck(classId)
        // Then: Order: [FULL_TIME 10/10, FULL_TIME 8/10, PART_TIME 9/10, ...]
    }

    @Test
    @DisplayName("Should prioritize teachers with 'general' skill")
    void shouldPrioritizeGeneralSkill() {
        // Given: Teacher A with 'general', Teacher B with specific skills
        // When: findAvailableTeachersWithPrecheck(classId)
        // Then: Order considers has_general_skill (TRUE before FALSE)
    }

    @Test
    @DisplayName("Should map Object[] to TeacherAvailability DTO correctly")
    void shouldMapObjectArrayToDTO() {
        // Given: CTE query returns Object[] with 15+ fields
        // When: Service layer maps to TeacherAvailability DTO
        // Then: All fields correctly mapped (teacherId, fullName, skills, conflicts, etc.)
    }
}
```

**Estimated Time:** 4-5 hours

---

### 2.5 TeachingSlotRepositoryTest (3 tests)

**File:** `src/test/java/org/fyp/tmssep490be/repositories/TeachingSlotRepositoryTest.java`

**Test Cases:**

```java
@DataJpaTest
@DisplayName("TeachingSlotRepository Integration Tests")
class TeachingSlotRepositoryTest extends AbstractRepositoryTest {

    @Autowired private TeachingSlotRepository teachingSlotRepository;
    @Autowired private TeacherRepository teacherRepository;

    @Test
    @DisplayName("Should bulk assign teacher with 'general' skill validation")
    void shouldBulkAssignTeacherWithGeneralSkill() {
        // Given: Teacher with 'general' skill, 36 sessions
        // When: bulkAssignTeacher(classId, teacherId, sessionIds=null)
        // Then: 36 teaching_slots created
    }

    @Test
    @DisplayName("Should bulk assign teacher to specific sessions only")
    void shouldBulkAssignToSpecificSessions() {
        // Given: Teacher with skill='listening', sessionIds=[1,2,3,4,5]
        // When: bulkAssignTeacher(classId, teacherId, sessionIds=[1,2,3,4,5])
        // Then: 5 teaching_slots created
    }

    @Test
    @DisplayName("Should validate skill match when assigning teacher")
    void shouldValidateSkillMatch() {
        // Given: Teacher with skills=['listening'], session requires 'writing'
        // When: bulkAssignTeacher(classId, teacherId, sessionIds=[writingSessionId])
        // Then: Returns empty list (no assignments)
    }
}
```

**Estimated Time:** 3-4 hours

---

### 2.6 TimeSlotTemplateRepositoryTest (2 tests)

**File:** `src/test/java/org/fyp/tmssep490be/repositories/TimeSlotTemplateRepositoryTest.java`

**Test Cases:**

```java
@DataJpaTest
@DisplayName("TimeSlotTemplateRepository Integration Tests")
class TimeSlotTemplateRepositoryTest extends AbstractRepositoryTest {

    @Autowired private TimeSlotTemplateRepository timeSlotTemplateRepository;

    @Test
    @DisplayName("Should find time slot templates by branch ordered by start time")
    void shouldFindByBranchOrderedByStartTime() {
        // Given: 5 time slots for branch 1 (08:00, 10:00, 14:00, 16:00, 18:00)
        // When: findByBranchIdOrderByStartTimeAsc(1)
        // Then: Returns 5 time slots in ascending order
    }

    @Test
    @DisplayName("Should return empty list when no time slots for branch")
    void shouldReturnEmptyListWhenNoTimeSlots() {
        // Given: No time slots for branch 999
        // When: findByBranchIdOrderByStartTimeAsc(999)
        // Then: Returns empty list
    }
}
```

**Estimated Time:** 2 hours

---

## PHASE 3: CONTROLLER/INTEGRATION TESTS (30-40 tests)

### 3.1 ClassControllerIT - Create Class (9 tests)

**File:** `src/test/java/org/fyp/tmssep490be/controllers/ClassControllerIT.java`

**Test Cases:**

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ClassController - Create Class Integration Tests")
class ClassControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String academicAffairToken;

    @BeforeEach
    void setUp() {
        // Create ACADEMIC_AFFAIR user with branch access
        // Generate JWT token
    }

    // ==================== Happy Path Tests ====================

    @Test
    @DisplayName("POST /api/v1/classes - Should create class with session generation")
    void shouldCreateClassWithSessionGeneration() throws Exception {
        // Given: Valid CreateClassRequest
        // When: POST /api/v1/classes
        // Then: 201 Created, response contains classId, code, sessionsGenerated=36
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("POST /api/v1/classes - Should return 400 when class code duplicate")
    void shouldReturn400WhenClassCodeDuplicate() throws Exception {
        // Given: Class "ENG-A1-001" already exists
        // When: POST /api/v1/classes with same code
        // Then: 400 Bad Request, error code CLASS_CODE_DUPLICATE
    }

    @Test
    @DisplayName("POST /api/v1/classes - Should return 400 when course not approved")
    void shouldReturn400WhenCourseNotApproved() throws Exception {
        // Given: Course with approval_status = PENDING
        // When: POST /api/v1/classes
        // Then: 400 Bad Request, error code COURSE_NOT_APPROVED
    }

    @Test
    @DisplayName("POST /api/v1/classes - Should return 400 when start date not in schedule days")
    void shouldReturn400WhenStartDateNotInScheduleDays() throws Exception {
        // Given: start_date = Tuesday, schedule_days = [1, 3, 5]
        // When: POST /api/v1/classes
        // Then: 400 Bad Request, error code START_DATE_NOT_IN_SCHEDULE_DAYS
    }

    @Test
    @DisplayName("POST /api/v1/classes - Should return 400 when invalid schedule days")
    void shouldReturn400WhenInvalidScheduleDays() throws Exception {
        // Given: schedule_days = [0, 8] (invalid)
        // When: POST /api/v1/classes
        // Then: 400 Bad Request, error code INVALID_SCHEDULE_DAYS
    }

    @Test
    @DisplayName("POST /api/v1/classes - Should return 400 when missing required fields")
    void shouldReturn400WhenMissingRequiredFields() throws Exception {
        // Given: Request missing branchId
        // When: POST /api/v1/classes
        // Then: 400 Bad Request, validation error
    }

    // ==================== Authorization Tests ====================

    @Test
    @DisplayName("POST /api/v1/classes - Should return 401 when no JWT token")
    void shouldReturn401WhenNoToken() throws Exception {
        // Given: No Authorization header
        // When: POST /api/v1/classes
        // Then: 401 Unauthorized
    }

    @Test
    @DisplayName("POST /api/v1/classes - Should return 403 when wrong role")
    void shouldReturn403WhenWrongRole() throws Exception {
        // Given: User with STUDENT role (not ACADEMIC_AFFAIR)
        // When: POST /api/v1/classes
        // Then: 403 Forbidden
    }

    @Test
    @DisplayName("POST /api/v1/classes - Should return 403 when no access to branch")
    void shouldReturn403WhenNoAccessToBranch() throws Exception {
        // Given: User not assigned to branch 2
        // When: POST /api/v1/classes with branchId=2
        // Then: 403 Forbidden
    }
}
```

**Estimated Time:** 6-7 hours

---

### 3.2 ClassControllerIT - Assign Time Slots (4 tests)

**Test Cases:**

```java
@Test
@DisplayName("POST /api/v1/classes/{classId}/time-slots - Should assign time slots")
void shouldAssignTimeSlotsSuccessfully() throws Exception {
    // Given: Class with 36 sessions, request assigns Mon: TS1, Wed: TS2, Fri: TS3
    // When: POST /api/v1/classes/1/time-slots
    // Then: 200 OK, response shows 36 sessions assigned
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/time-slots - Should return 404 when class not found")
void shouldReturn404WhenClassNotFound() throws Exception {
    // Given: classId = 999 (non-existent)
    // When: POST /api/v1/classes/999/time-slots
    // Then: 404 Not Found
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/time-slots - Should return 400 when invalid day of week")
void shouldReturn400WhenInvalidDayOfWeek() throws Exception {
    // Given: Request with dayOfWeek = 0 (invalid)
    // When: POST /api/v1/classes/1/time-slots
    // Then: 400 Bad Request
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/time-slots - Should return 403 when user has no access")
void shouldReturn403WhenNoAccessToClass() throws Exception {
    // Given: User not assigned to class's branch
    // When: POST /api/v1/classes/1/time-slots
    // Then: 403 Forbidden
}
```

**Estimated Time:** 3-4 hours

---

### 3.3 ClassControllerIT - Assign Resources (4 tests)

**Test Cases:**

```java
@Test
@DisplayName("POST /api/v1/classes/{classId}/resources - Should assign resources with auto-propagation")
void shouldAssignResourcesWithAutoPropagation() throws Exception {
    // Given: Class with 36 sessions, Room 101 available
    // When: POST /api/v1/classes/1/resources
    // Then: 200 OK, response shows successCount=36, conflictCount=0
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/resources - Should return conflicts")
void shouldReturnConflictsWhenResourceBooked() throws Exception {
    // Given: Room 101 booked on 3 dates
    // When: POST /api/v1/classes/1/resources
    // Then: 200 OK, response shows conflictCount=3, conflicts array with details
}

@Test
@DisplayName("GET /api/v1/classes/{classId}/available-resources - Should return available resources")
void shouldReturnAvailableResources() throws Exception {
    // Given: Class with 36 sessions
    // When: GET /api/v1/classes/1/available-resources?sessionId=5
    // Then: 200 OK, list of available resources (capacity >= class max_capacity)
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/resources - Should validate time slots assigned first")
void shouldReturn400WhenTimeSlotsNotAssigned() throws Exception {
    // Given: Class with sessions missing time_slot_template_id
    // When: POST /api/v1/classes/1/resources
    // Then: 400 Bad Request (time slots required before resource assignment)
}
```

**Estimated Time:** 3-4 hours

---

### 3.4 ClassControllerIT - Assign Teachers (5 tests)

**Test Cases:**

```java
@Test
@DisplayName("GET /api/v1/classes/{classId}/available-teachers - Should return PRE-CHECK results")
void shouldReturnAvailableTeachersWithPrecheck() throws Exception {
    // Given: Class with 10 sessions
    // When: GET /api/v1/classes/1/available-teachers?skillSet=listening,reading
    // Then: 200 OK, list of teachers with availabilityStatus, totalSessions, availableSessions, conflicts
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/teachers - Should assign teacher successfully")
void shouldAssignTeacherSuccessfully() throws Exception {
    // Given: Teacher Jane fully available
    // When: POST /api/v1/classes/1/teachers with teacherId=Jane
    // Then: 200 OK, response shows assignedCount=10
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/teachers - Should assign to specific sessions")
void shouldAssignToSpecificSessions() throws Exception {
    // Given: Teacher John partially available, sessionIds=[1,2,3,4,5]
    // When: POST /api/v1/classes/1/teachers with sessionIds
    // Then: 200 OK, assignedCount=5
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/teachers - Should return 400 when skill mismatch")
void shouldReturn400WhenSkillMismatch() throws Exception {
    // Given: Teacher with 'listening' skill only, sessions require 'writing'
    // When: POST /api/v1/classes/1/teachers
    // Then: 400 Bad Request, no sessions assigned
}

@Test
@DisplayName("GET /api/v1/classes/{classId}/available-teachers - Should handle 'general' skill")
void shouldHandleGeneralSkill() throws Exception {
    // Given: Teacher Eve with 'general' skill
    // When: GET /api/v1/classes/1/available-teachers
    // Then: Eve can teach ALL sessions (totalSessions=36)
}
```

**Estimated Time:** 4-5 hours

---

### 3.5 ClassControllerIT - Validation & Approval (6 tests)

**Test Cases:**

```java
@Test
@DisplayName("POST /api/v1/classes/{classId}/validate - Should pass validation when complete")
void shouldPassValidationWhenComplete() throws Exception {
    // Given: Class with all sessions fully assigned
    // When: POST /api/v1/classes/1/validate
    // Then: 200 OK, isValid=true, canSubmit=true
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/validate - Should fail when incomplete")
void shouldFailValidationWhenIncomplete() throws Exception {
    // Given: Class with 5 sessions missing teachers
    // When: POST /api/v1/classes/1/validate
    // Then: 200 OK, isValid=false, errors=["5 sessions missing teacher"]
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/submit - Should submit class for approval")
void shouldSubmitClassForApproval() throws Exception {
    // Given: Class DRAFT + complete
    // When: POST /api/v1/classes/1/submit
    // Then: 200 OK, submitted_at set
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/submit - Should return 400 when incomplete")
void shouldReturn400WhenSubmittingIncomplete() throws Exception {
    // Given: Class with incomplete sessions
    // When: POST /api/v1/classes/1/submit
    // Then: 400 Bad Request, error code CLASS_INCOMPLETE_CANNOT_SUBMIT
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/approve - Should approve class (CENTER_HEAD)")
void shouldApproveClass() throws Exception {
    // Given: Class with submitted_at != null, user with CENTER_HEAD role
    // When: POST /api/v1/classes/1/approve
    // Then: 200 OK, status=SCHEDULED, approval_status=APPROVED
}

@Test
@DisplayName("POST /api/v1/classes/{classId}/reject - Should reject class with reason")
void shouldRejectClass() throws Exception {
    // Given: Class submitted, user with CENTER_HEAD role
    // When: POST /api/v1/classes/1/reject with reason
    // Then: 200 OK, status=DRAFT, approval_status=REJECTED, rejection_reason set
}
```

**Estimated Time:** 5-6 hours

---

## TEST DATA BUILDERS & UTILITIES

### TestDataBuilder Enhancements

**File:** `src/test/java/org/fyp/tmssep490be/utils/TestDataBuilder.java`

**New Builders Needed:**

```java
public class TestDataBuilder {

    // Existing builders...

    // New builders for Create Class Workflow
    public static ClassEntity.ClassEntityBuilder buildClassEntity() {
        return ClassEntity.builder()
            .code("CLASS-TEST-001")
            .name("Test Class")
            .modality(Modality.OFFLINE)
            .startDate(LocalDate.now().plusDays(7))
            .scheduleDays(new Short[]{1, 3, 5}) // Mon, Wed, Fri
            .maxCapacity(20)
            .status(ClassStatus.DRAFT)
            .approvalStatus(ApprovalStatus.PENDING);
    }

    public static Session.SessionBuilder buildSession() {
        return Session.builder()
            .date(LocalDate.now().plusDays(1))
            .type(SessionType.CLASS)
            .status(SessionStatus.PLANNED);
    }

    public static SessionResource.SessionResourceBuilder buildSessionResource() {
        return SessionResource.builder()
            .resourceType(ResourceType.ROOM);
    }

    public static TeachingSlot.TeachingSlotBuilder buildTeachingSlot() {
        return TeachingSlot.builder()
            .status(TeachingSlotStatus.SCHEDULED)
            .teachingRole(TeachingRole.PRIMARY);
    }

    public static TeacherAvailability.TeacherAvailabilityBuilder buildTeacherAvailability() {
        return TeacherAvailability.builder()
            .dayOfWeek(1); // Monday
    }

    public static TimeSlotTemplate.TimeSlotTemplateBuilder buildTimeSlotTemplate() {
        return TimeSlotTemplate.builder()
            .name("Morning Slot")
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(10, 0))
            .durationMin(120);
    }
}
```

---

## TESTING BEST PRACTICES

### 1. AAA Pattern (Arrange-Act-Assert)

```java
@Test
@DisplayName("Should generate 36 sessions for Mon/Wed/Fri schedule")
void shouldGenerateSessionsForMonWedFriSchedule() {
    // Arrange
    ClassEntity classEntity = TestDataBuilder.buildClassEntity()
        .scheduleDays(new Short[]{1, 3, 5})
        .build();
    Course course = createCourseWith36Sessions();
    when(courseSessionRepository.findByCourseId(courseId))
        .thenReturn(course.getCourseSessions());

    // Act
    List<Session> sessions = sessionGenerationService.generateSessionsForClass(
        classEntity, course
    );

    // Assert
    assertThat(sessions).hasSize(36);
    assertThat(sessions.get(0).getDate().getDayOfWeek().getValue()).isEqualTo(1); // Monday
    assertThat(sessions.get(1).getDate().getDayOfWeek().getValue()).isEqualTo(3); // Wednesday
    assertThat(sessions.get(2).getDate().getDayOfWeek().getValue()).isEqualTo(5); // Friday
}
```

### 2. AssertJ Fluent Assertions

```java
// ✅ GOOD - Fluent and readable
assertThat(preview.getClassCode()).isEqualTo("ENG-A1-001");
assertThat(preview.isExceedsCapacity()).isFalse();
assertThat(preview.getRecommendation().getType()).isEqualTo(RecommendationType.OK);
assertThat(sessions).hasSize(36)
    .allMatch(s -> s.getStatus() == SessionStatus.PLANNED);

// ❌ BAD - JUnit assertions (less readable)
assertEquals("ENG-A1-001", preview.getClassCode());
assertFalse(preview.isExceedsCapacity());
```

### 3. @DisplayName Annotations

```java
// ✅ GOOD - Descriptive test names
@Test
@DisplayName("Should generate 36 sessions for Mon/Wed/Fri schedule")
void shouldGenerateSessionsForMonWedFriSchedule() { }

@Test
@DisplayName("Should throw exception when class code already exists for branch")
void shouldFailWhenClassCodeDuplicate() { }

// ❌ BAD - Generic names
@Test
void testGenerateSessions() { }

@Test
void testDuplicateCode() { }
```

### 4. Use TestDataBuilder for Consistency

```java
// ✅ GOOD - Reusable, consistent test data
ClassEntity classEntity = TestDataBuilder.buildClassEntity()
    .code("ENG-A1-001")
    .scheduleDays(new Short[]{1, 3, 5})
    .build();

// ❌ BAD - Manual construction (repetitive, error-prone)
ClassEntity classEntity = new ClassEntity();
classEntity.setCode("ENG-A1-001");
classEntity.setName("Test Class");
classEntity.setModality(Modality.OFFLINE);
// ... 10+ more setters
```

### 5. Test Isolation with @Transactional

```java
// Repository and Controller tests automatically rollback after each test
@DataJpaTest // Auto-transactional
class ClassRepositoryTest {
    @Test
    void shouldFindClassByCode() {
        classRepository.save(testClass); // Rolled back after test
    }
}
```

---

## TEST COVERAGE TARGETS

| Layer | Target Coverage | Priority |
|-------|----------------|----------|
| **Service Layer** | 90%+ | 🔴 HIGH |
| **Repository Layer** | 70%+ | 🟡 MEDIUM |
| **Controller Layer** | 80%+ | 🔴 HIGH |
| **Overall** | 80%+ | 🔴 HIGH |

**Coverage Report:**
```bash
mvn clean verify jacoco:report
open target/site/jacoco/index.html
```

---

## TEST EXECUTION STRATEGY

### Local Development

```bash
# Run all Create Class tests
mvn test -Dtest=*Class*,*Session*,*Resource*,*Teacher*,*Validation*,*Approval*

# Run specific test class
mvn test -Dtest=SessionGenerationServiceImplTest

# Run with coverage
mvn clean verify jacoco:report
```

### CI/CD Pipeline

```yaml
# GitHub Actions / GitLab CI
- name: Run Tests
  run: mvn clean verify

- name: Check Coverage
  run: |
    mvn jacoco:check
    # Fail if coverage < 80%
```

---

## TEST DATA REQUIREMENTS

### Database Seed Data for Tests

**File:** `src/test/resources/test-data/create-class-test-data.sql`

```sql
-- Centers
INSERT INTO center (code, name, email, phone, address) VALUES
('CT001', 'Test Center', 'center@test.com', '0123456789', 'Test Address');

-- Branches
INSERT INTO branch (code, name, address, phone, center_id) VALUES
('BR001', 'Test Branch 1', 'Branch 1 Address', '0987654321', 1),
('BR002', 'Test Branch 2', 'Branch 2 Address', '0987654322', 1);

-- Subjects and Levels
INSERT INTO subject (code, name, description) VALUES
('ENG', 'English', 'English Language');

INSERT INTO level (code, name, subject_id, sort_order) VALUES
('A1', 'Beginner A1', 1, 1),
('B1', 'Intermediate B1', 1, 3);

-- Courses
INSERT INTO course (code, name, subject_id, level_id, total_sessions, approval_status) VALUES
('ENG-A1-V1', 'English A1', 1, 1, 36, 'approved'),
('ENG-B1-V1', 'English B1', 1, 2, 48, 'approved');

-- Course Sessions (36 sessions for ENG-A1-V1)
INSERT INTO course_session (course_id, phase, sequence, skill_set) VALUES
(1, 1, 1, ARRAY['listening', 'reading']),
(1, 1, 2, ARRAY['listening', 'reading']),
-- ... (34 more sessions)
(1, 6, 6, ARRAY['general']);

-- Time Slot Templates
INSERT INTO time_slot_template (branch_id, name, start_time, end_time, duration_min) VALUES
(1, 'Morning 1', '08:00', '10:00', 120),
(1, 'Morning 2', '10:00', '12:00', 120),
(1, 'Afternoon 1', '14:00', '16:00', 120),
(1, 'Afternoon 2', '16:00', '18:00', 120);

-- Resources
INSERT INTO resource (branch_id, code, name, resource_type, capacity) VALUES
(1, 'R101', 'Room 101', 'room', 25),
(1, 'R102', 'Room 102', 'room', 30),
(1, 'R103', 'Room 103', 'room', 20);

-- Teachers
INSERT INTO user_account (email, full_name, gender, status, password_hash) VALUES
('jane@teacher.com', 'Jane Doe', 'female', 'active', 'hashed_password'),
('john@teacher.com', 'John Smith', 'male', 'active', 'hashed_password');

INSERT INTO teacher (user_account_id, employee_code, contract_type) VALUES
(1, 'T001', 'full_time'),
(2, 'T002', 'part_time');

-- Teacher Skills
INSERT INTO teacher_skill (teacher_id, skill, level) VALUES
(1, 'general', 5), -- Jane can teach anything
(2, 'listening', 4),
(2, 'reading', 4);

-- Teacher Availability
INSERT INTO teacher_availability (teacher_id, day_of_week, time_slot_template_id) VALUES
(1, 1, 1), -- Jane available Monday 08:00
(1, 1, 2), -- Jane available Monday 10:00
(1, 3, 1), -- Jane available Wednesday 08:00
(1, 3, 2), -- Jane available Wednesday 10:00
(1, 5, 1), -- Jane available Friday 08:00
(1, 5, 2); -- Jane available Friday 10:00
```

---

## PERFORMANCE BENCHMARKS FOR TESTS

| Test Suite | Target Time | Max Acceptable |
|------------|-------------|----------------|
| **Service Tests (140-160 tests)** | < 2 minutes | 3 minutes |
| **Repository Tests (30-40 tests)** | < 1 minute | 2 minutes |
| **Controller Tests (30-40 tests)** | < 2 minutes | 4 minutes |
| **Total (200-240 tests)** | < 5 minutes | 10 minutes |

**Optimization Tips:**
- Use `@MockitoBean` instead of real beans when possible (faster)
- Testcontainers PostgreSQL instance is shared across tests (singleton)
- `@Transactional` rollback is faster than database cleanup
- Parallel execution: `mvn -T 1C clean verify` (1 thread per CPU core)

---

## TEST DOCUMENTATION TEMPLATE

### Service Test Documentation

```java
/**
 * Service layer tests for SessionGenerationService.
 *
 * <p>Uses modern Spring Boot 3.5.7 @SpringBootTest with @MockitoBean pattern.
 * Tests session auto-generation business logic in Spring context.
 *
 * <p><b>Test Coverage:</b>
 * <ul>
 *   <li>Happy path: Generate sessions for Mon/Wed/Fri and Tue/Thu schedules</li>
 *   <li>Date calculation: ISODOW week rollover logic</li>
 *   <li>Template linking: Verify course_session_id mapping</li>
 *   <li>Default values: Session status and type</li>
 * </ul>
 *
 * <p><b>Related Components:</b>
 * <ul>
 *   <li>{@link SessionGenerationService} - Service interface</li>
 *   <li>{@link SessionGenerationServiceImpl} - Implementation under test</li>
 *   <li>{@link SessionRepository} - Mocked dependency</li>
 * </ul>
 *
 * @see SessionGenerationService
 * @see SessionGenerationServiceImpl
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SessionGenerationService Unit Tests")
class SessionGenerationServiceImplTest {
    // Test implementation...
}
```

---

## NEXT STEPS

### Implementation Order

1. ✅ **Phase 1.1**: SessionGenerationServiceImplTest (6 tests) - **3-4 hours**
2. ✅ **Phase 1.2**: ClassServiceImplTest - Create Class (9 tests) - **5-6 hours**
3. ✅ **Phase 2.1**: ClassRepositoryTest (8 tests) - **4-5 hours**
4. ✅ **Phase 2.2**: SessionRepositoryTest (6 tests) - **4-5 hours**
5. ✅ **Phase 3.1**: ClassControllerIT - Create Class (9 tests) - **6-7 hours**
6. ✅ **Phase 1.3**: ResourceAssignmentServiceImplTest (6 tests) - **5-6 hours**
7. ✅ **Phase 1.4**: TeacherAssignmentServiceImplTest (9 tests) - **7-8 hours**
8. ✅ **Phase 1.5**: ValidationServiceImplTest (6 tests) - **4-5 hours**
9. ✅ **Phase 1.6**: ApprovalServiceImplTest (6 tests) - **4-5 hours**
10. ✅ **Phase 3.2-3.5**: Remaining Controller Tests (19 tests) - **15-20 hours**
11. ✅ **Phase 2.3-2.6**: Remaining Repository Tests (14 tests) - **12-15 hours**

**Total Estimated Time:** 70-90 hours (9-12 days)

---

## SUCCESS CRITERIA

**Definition of Done for Testing:**

- ✅ All 200-240 test cases implemented and passing
- ✅ Service layer coverage ≥ 90%
- ✅ Repository layer coverage ≥ 70%
- ✅ Controller layer coverage ≥ 80%
- ✅ Overall coverage ≥ 80%
- ✅ All tests follow modern Spring Boot 3.5.7 patterns
- ✅ AAA pattern used consistently
- ✅ AssertJ fluent assertions used
- ✅ @DisplayName annotations for all tests
- ✅ TestDataBuilder patterns for all entities
- ✅ Zero failing tests in CI/CD pipeline
- ✅ Test execution time < 10 minutes

---

## CHANGELOG

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-11-05 | 1.0.0 | Initial test plan created based on implementation plan and enrollment test patterns | Claude |

---

**Ready to start testing? Let's begin with SessionGenerationServiceImplTest! 🚀**
