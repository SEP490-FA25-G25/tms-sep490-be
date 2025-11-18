# Makeup Request Implementation Guide

**Version:** 2.0
**Date:** 2025-11-10
**Request Type:** MAKEUP
**Flow Support:** Dual (Self-Service + On-Behalf)

---

## Overview

**Purpose:** Student requests makeup class for missed session
**Complexity:** Medium
**Business Impact:** Course completion rate, content mastery
**Key Features:**
- Makeup ANY absent session (excused + unexcused) within 4 weeks
- Cross-class, cross-branch, cross-modality support
- Smart ranking by branch, modality, date, capacity
- Dual flow: Student self-service + AA on-behalf

---

## Flow Types

### Flow 1: Self-Service (Student)
Student creates makeup request → AA reviews → Approve/Reject

### Flow 2: On-Behalf (Academic Affairs)
AA creates makeup request for student → Auto-APPROVED (no review)

---

## Business Rules

| Rule ID | Description | Enforcement |
|---------|-------------|-------------|
| BR-MKP-001 | Makeup allowed for ANY absent session within 4 weeks | Blocking |
| BR-MKP-002 | `course_session_id` must match (same content) | Blocking |
| BR-MKP-003 | Makeup session must have capacity | Blocking |
| BR-MKP-004 | Cross-class, cross-branch, cross-modality allowed | Feature |
| BR-MKP-005 | No duplicate makeup request for same target session | Blocking |
| BR-MKP-006 | Reason required, min 10 chars | Blocking |
| BR-MKP-007 | No schedule conflict with student's other sessions | Blocking |
| BR-MKP-008 | Target session must have `attendanceStatus = 'ABSENT'` | Blocking |
| BR-MKP-009 | Both excused and unexcused absences can be made up | Feature |
| BR-MKP-010 | AA on-behalf requests auto-approved | Feature |

**Configuration:**
```yaml
makeup_request:
  eligible_weeks_lookback: 4
  max_concurrent_pending: 3
  reason_min_length: 10
  priority_scoring:
    same_branch_weight: 10
    same_modality_weight: 5
    soonest_date_weight: 3
  on_behalf:
    auto_approve: true
    allowed_roles: [ROLE_ACADEMIC_AFFAIR]
```

---

## 📱 Flow 1: Student Self-Service

### UX Flow
```
My Requests Page
  ↓
[+ New Request] → Choose Type Modal → [MAKEUP]
  ↓
Step 1: Select Missed Session (any ABSENT session in 4 weeks)
  ↓
Step 2: Select Makeup Session (ranked recommendations)
  ↓
Step 3: Fill Form (Reason) + Submit → PENDING
  ↓
Success → AA Reviews → APPROVED/REJECTED
```

### Step 1: Select Missed Session

**API:** `GET /api/v1/students/me/missed-sessions?weeksBack=4&excludeRequested=true`

**Logic:**
- Show ALL `attendanceStatus = 'ABSENT'` sessions in last 4 weeks
- Include excused (approved absence) + unexcused absences
- Exclude sessions with existing makeup request (if `excludeRequested=true`)
- Sort by most recent first

**Visual Indicators:**
- 🟢 Excused Absence (has approved absence request)
- 🔴 Unexcused Absence (no absence request or rejected)
- ⚠️ Already has makeup (disabled)

**Response:**
```json
{
  "success": true,
  "data": {
    "totalCount": 4,
    "sessions": [
      {
        "sessionId": 1012,
        "date": "2025-11-03",
        "daysAgo": 5,
        "courseSessionNumber": 12,
        "courseSessionTitle": "Grammar",
        "courseSessionId": 120,
        "class": { "id": 101, "code": "CHN-A1-01" },
        "timeSlot": { "startTime": "08:00:00", "endTime": "10:00:00" },
        "attendanceStatus": "ABSENT",
        "absenceRequestId": 42,
        "absenceRequestStatus": "APPROVED",
        "hasExistingMakeupRequest": false,
        "isExcusedAbsence": true
      }
    ]
  }
}
```

---

### Step 2: Select Makeup Session (Smart Ranking)

**API:** `GET /api/v1/student-requests/makeup-options?targetSessionId={sessionId}`

**Ranking Algorithm:**
```
Priority Score:
- Same branch: +10 points
- Same modality: +5 points
- Soonest date: +3 points per week closer
- More capacity: +1 point per 5 slots

Priority Levels:
- HIGH (🏆): >= 15 points (same branch + modality)
- MEDIUM: 8-14 points (same branch OR modality)
- LOW: < 8 points (cross-branch, cross-modality)
```

**Logic:**
- Find all sessions with same `courseSessionId` (same content)
- Only `status = 'PLANNED'` and future dates
- Exclude student's own class
- Group by priority for UI

**Response:**
```json
{
  "success": true,
  "data": {
    "targetSession": {
      "sessionId": 1012,
      "courseSessionId": 120,
      "class": { "branchId": 1, "modality": "OFFLINE" }
    },
    "makeupOptions": [
      {
        "sessionId": 2012,
        "date": "2025-11-12",
        "courseSessionId": 120,
        "class": {
          "id": 102,
          "code": "CHN-A1-02",
          "branchId": 1,
          "modality": "OFFLINE",
          "availableSlots": 5
        },
        "timeSlot": { "startTime": "14:00:00", "endTime": "16:00:00" },
        "matchScore": {
          "branchMatch": true,
          "modalityMatch": true,
          "priority": "HIGH"
        }
      }
    ]
  }
}
```

---

### Step 3: Submit Request

**API:** `POST /api/v1/student-requests`

**Request:**
```json
{
  "requestType": "MAKEUP",
  "currentClassId": 101,
  "targetSessionId": 1012,
  "makeupSessionId": 2012,
  "requestReason": "I missed Session 12 due to illness. I want to make up the content."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Makeup request submitted successfully",
  "data": {
    "id": 43,
    "requestType": "MAKEUP",
    "status": "PENDING",
    "targetSession": { "sessionId": 1012, "courseSessionNumber": 12 },
    "makeupSession": { "sessionId": 2012, "class": { "code": "CHN-A1-02" } },
    "submittedAt": "2025-11-07T16:20:00+07:00"
  }
}
```

---

## 👔 Flow 2: AA On-Behalf

### UX Flow
```
AA Portal → Student Management
  ↓
Select Student → [+ Create Request On-Behalf]
  ↓
Choose Type → [MAKEUP]
  ↓
Step 1: Select Missed Session (for selected student)
  ↓
Step 2: Select Makeup Session
  ↓
Step 3: Confirm & Submit → Auto-APPROVED
  ↓
Success → Student notified
```

### Key Differences from Self-Service
- AA selects student first
- Same 3-step flow but with student context
- **Auto-approved** (no pending review)
- Email sent to student immediately

---

### AA API: Get Missed Sessions for Student

**API:** `GET /api/v1/students/{studentId}/missed-sessions?weeksBack=4`

**Authorization:** `ROLE_ACADEMIC_AFFAIR` only

**Response:** Same structure as student endpoint but for specified student

---

### AA API: Get Makeup Options for Student

**API:** `GET /api/v1/student-requests/makeup-options?targetSessionId={sessionId}&studentId={studentId}`

**Authorization:** `ROLE_ACADEMIC_AFFAIR` only

**Logic:** Same as student flow but validates schedule conflict for specified student

---

### AA API: Submit On-Behalf Request

**API:** `POST /api/v1/student-requests/on-behalf`

**Authorization:** `ROLE_ACADEMIC_AFFAIR` only

**Request:**
```json
{
  "studentId": 123,
  "requestType": "MAKEUP",
  "currentClassId": 101,
  "targetSessionId": 1012,
  "makeupSessionId": 2012,
  "requestReason": "AA created on behalf: Student was sick and unable to submit request",
  "note": "Created by AA staff after phone consultation"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Makeup request created and auto-approved",
  "data": {
    "id": 44,
    "student": { "id": 123, "fullName": "John Doe" },
    "requestType": "MAKEUP",
    "status": "APPROVED",
    "targetSession": { "sessionId": 1012 },
    "makeupSession": { "sessionId": 2012 },
    "submittedBy": { "id": 789, "fullName": "AA Staff Nguyen", "role": "ACADEMIC_AFFAIR" },
    "submittedAt": "2025-11-07T16:20:00+07:00",
    "decidedBy": { "id": 789, "fullName": "AA Staff Nguyen" },
    "decidedAt": "2025-11-07T16:20:00+07:00"
  }
}
```

**Side Effects:**
1. Request created with `status = 'APPROVED'`
2. `submitted_by` = AA user ID
3. `decided_by` = AA user ID (same as submitter)
4. Original `student_session` updated with makeup link
5. New `student_session` created for makeup with `is_makeup = TRUE`
6. Email sent to student
7. Email sent to makeup class teacher

---

## Common APIs (Both Flows)

### View All Requests (Student)

**API:** `GET /api/v1/students/me/requests?requestType=MAKEUP&status={status}`

Shows all requests (self-created + AA on-behalf)

---

### View Pending Requests (AA)

**API:** `GET /api/v1/student-requests/pending?requestType=MAKEUP&branchId={branchId}`

Shows only **PENDING** requests (from student self-service flow)

**Note:** On-behalf requests are auto-approved, won't appear in pending list

---

### Approve Request (AA)

**API:** `PUT /api/v1/student-requests/{id}/approve`

**Request:**
```json
{
  "note": "Approved. Same branch and valid reason."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Makeup request approved and student added to makeup session",
  "data": {
    "request": { "id": 43, "status": "APPROVED", "decidedAt": "2025-11-07T17:00:00+07:00" },
    "studentSession": {
      "studentId": 123,
      "sessionId": 2012,
      "attendanceStatus": "PLANNED",
      "isMakeup": true,
      "makeupSessionId": 2012,
      "originalSessionId": 1012
    }
  }
}
```

**Side Effects:**
1. Update request `status = 'APPROVED'`
2. Update original `student_session.makeup_session_id`
3. Create new `student_session` for makeup with `is_makeup = TRUE`
4. Send email to student
5. Send email to teacher

---

### Reject Request (AA)

**API:** `PUT /api/v1/student-requests/{id}/reject`

**Request:**
```json
{
  "rejectionReason": "Makeup session is now full. Please select another session."
}
```

**Note:** Only applies to **PENDING** requests (self-service flow)

---

## Database Schema

### Table: `student_request`

```sql
CREATE TABLE student_request (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    request_type request_type_enum NOT NULL,
    current_class_id BIGINT NOT NULL REFERENCES class(id),
    target_session_id BIGINT NOT NULL REFERENCES session(id),
    makeup_session_id BIGINT REFERENCES session(id),
    request_reason TEXT NOT NULL,
    note TEXT,
    status request_status_enum NOT NULL DEFAULT 'pending',
    submitted_by BIGINT NOT NULL REFERENCES user(id),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_by BIGINT REFERENCES user(id),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_makeup_valid CHECK (
        request_type != 'MAKEUP' OR (
            target_session_id IS NOT NULL AND
            makeup_session_id IS NOT NULL
        )
    )
);

CREATE INDEX idx_student_request_makeup_session ON student_request(makeup_session_id);
CREATE INDEX idx_student_request_status ON student_request(status, request_type);
```

### Table: `student_session` (Enhanced)

```sql
CREATE TABLE student_session (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    session_id BIGINT NOT NULL REFERENCES session(id),
    attendance_status attendance_status_enum NOT NULL DEFAULT 'planned',
    is_makeup BOOLEAN NOT NULL DEFAULT FALSE,
    makeup_session_id BIGINT REFERENCES session(id), -- Forward link
    original_session_id BIGINT REFERENCES session(id), -- Backlink
    note TEXT,
    recorded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(student_id, session_id)
);

CREATE INDEX idx_student_session_makeup ON student_session(is_makeup, makeup_session_id);
CREATE INDEX idx_student_session_original ON student_session(original_session_id);
```

**Bidirectional Tracking:**
- Original session: `makeup_session_id` → points to makeup session
- Makeup session: `original_session_id` → points back to original session

---

## Backend Validation (Submit)

```java
public StudentRequestResponseDTO submitMakeupRequest(MakeupRequestDTO dto, Long submittedByUserId, boolean autoApprove) {
    // 1. Validate target session exists and is absent
    StudentSession targetStudentSession = studentSessionRepository
        .findByStudentIdAndSessionId(dto.getStudentId(), dto.getTargetSessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

    if (!targetStudentSession.getAttendanceStatus().equals(AttendanceStatus.ABSENT)) {
        throw new BusinessRuleException("Can only makeup absent sessions");
    }

    // 2. Check eligible timeframe (within 4 weeks)
    Session targetSession = targetStudentSession.getSession();
    long weeksAgo = ChronoUnit.WEEKS.between(targetSession.getDate(), LocalDate.now());
    if (weeksAgo > 4) {
        throw new BusinessRuleException("Session too old for makeup (limit: 4 weeks)");
    }

    // 3. Validate makeup session
    Session makeupSession = sessionRepository.findById(dto.getMakeupSessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Makeup session not found"));

    if (!makeupSession.getStatus().equals(SessionStatus.PLANNED)) {
        throw new BusinessRuleException("Makeup session must be PLANNED");
    }

    if (makeupSession.getDate().isBefore(LocalDate.now())) {
        throw new BusinessRuleException("Makeup session must be in the future");
    }

    // 4. CRITICAL: Validate course session match
    if (!targetSession.getCourseSession().getId().equals(makeupSession.getCourseSession().getId())) {
        throw new BusinessRuleException("Makeup session must have same content (courseSessionId)");
    }

    // 5. Check capacity
    int enrolledCount = studentSessionRepository.countBySessionId(makeupSession.getId());
    if (enrolledCount >= makeupSession.getClassEntity().getMaxCapacity()) {
        throw new BusinessRuleException("Makeup session is full");
    }

    // 6. Check schedule conflict
    List<Session> studentSessions = sessionRepository.findByStudentIdAndDate(
        dto.getStudentId(), makeupSession.getDate());

    for (Session existing : studentSessions) {
        if (hasTimeOverlap(existing.getTimeSlot(), makeupSession.getTimeSlot())) {
            throw new BusinessRuleException("Schedule conflict with other classes");
        }
    }

    // 7. Check duplicate request
    boolean hasDuplicate = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
        dto.getStudentId(), dto.getTargetSessionId(), RequestType.MAKEUP,
        List.of(RequestStatus.PENDING, RequestStatus.APPROVED));

    if (hasDuplicate) {
        throw new BusinessRuleException("Duplicate makeup request for this session");
    }

    // 8. Create request
    StudentRequest request = StudentRequest.builder()
        .student(studentRepository.getReferenceById(dto.getStudentId()))
        .requestType(RequestType.MAKEUP)
        .currentClass(classRepository.getReferenceById(dto.getCurrentClassId()))
        .targetSession(targetSession)
        .makeupSession(makeupSession)
        .requestReason(dto.getRequestReason())
        .note(dto.getNote())
        .status(autoApprove ? RequestStatus.APPROVED : RequestStatus.PENDING)
        .submittedBy(userRepository.getReferenceById(submittedByUserId))
        .submittedAt(LocalDateTime.now())
        .build();

    // 9. If auto-approve (AA on-behalf)
    if (autoApprove) {
        request.setDecidedBy(userRepository.getReferenceById(submittedByUserId));
        request.setDecidedAt(LocalDateTime.now());
    }

    request = studentRequestRepository.save(request);

    // 10. If auto-approved, execute approval logic
    if (autoApprove) {
        executeApproval(request);
    } else {
        // Send notification to AA
        notificationService.notifyAcademicAffair(request);
    }

    return mapper.toResponseDTO(request);
}
```

---

## Backend Approval Transaction

```java
@Transactional
public void executeApproval(StudentRequest request) {
    // 1. Re-validate capacity (race condition check)
    int currentEnrolled = studentSessionRepository.countBySessionId(
        request.getMakeupSession().getId());

    if (currentEnrolled >= request.getMakeupSession().getClassEntity().getMaxCapacity()) {
        throw new BusinessRuleException("Makeup session became full");
    }

    // 2. Update original student_session
    StudentSession originalStudentSession = studentSessionRepository
        .findByStudentIdAndSessionId(
            request.getStudent().getId(),
            request.getTargetSession().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Original session not found"));

    originalStudentSession.setMakeupSessionId(request.getMakeupSession().getId());
    originalStudentSession.setNote(String.format(
        "Makeup approved: Session %d on %s. Request ID: %d",
        request.getMakeupSession().getCourseSession().getCourseSessionNumber(),
        request.getMakeupSession().getDate(),
        request.getId()));
    studentSessionRepository.save(originalStudentSession);

    // 3. Create NEW student_session for makeup
    StudentSession makeupStudentSession = StudentSession.builder()
        .student(request.getStudent())
        .session(request.getMakeupSession())
        .attendanceStatus(AttendanceStatus.PLANNED)
        .isMakeup(true)
        .makeupSessionId(request.getMakeupSession().getId())
        .originalSessionId(request.getTargetSession().getId())
        .note("Makeup student from " + request.getCurrentClass().getCode())
        .build();

    studentSessionRepository.save(makeupStudentSession);

    // 4. Send notifications
    notificationService.notifyStudent(request, "approved");
    notificationService.notifyTeacher(request.getMakeupSession(), makeupStudentSession);
}
```

---

## Teacher View (Makeup Student Badge)

**Query:**
```sql
SELECT
    ss.id,
    s.student_code,
    s.full_name,
    ss.attendance_status,
    ss.is_makeup,
    ss.original_session_id,
    orig_class.code AS original_class_code
FROM student_session ss
JOIN student s ON ss.student_id = s.id
LEFT JOIN session orig_session ON ss.original_session_id = orig_session.id
LEFT JOIN class orig_class ON orig_session.class_id = orig_class.id
WHERE ss.session_id = ?
ORDER BY ss.is_makeup ASC, s.full_name ASC;
```

**UI Display:**
```
┌──────────────────────────────────────────────────┐
│ 1. Alice Nguyen      [Present] [Absent]          │
│ 2. Bob Tran          [Present] [Absent]          │
│ ...                                              │
│ 15. Zoe Le           [Present] [Absent]          │
│ 16. 🏷️ John Doe      [Present] [Absent]         │
│     (Makeup from CHN-A1-01)                      │
└──────────────────────────────────────────────────┘
```

---

## Status State Machine

### Flow 1: Self-Service
```
[Student submits] → PENDING → [AA approves] → APPROVED → [Execute approval logic]
                             → [AA rejects]  → REJECTED
```

### Flow 2: On-Behalf
```
[AA submits on-behalf] → APPROVED → [Execute approval logic immediately]
```

**Key Difference:**
- Self-service: Creates PENDING request, requires AA review
- On-behalf: Creates APPROVED request, executes immediately

---

## Notifications

### Email to Student (Approved)
```
Subject: Your Makeup Request has been Approved

Dear {student_name},

Your makeup request has been approved!

Makeup Session:
- Class: {makeup_class_code}
- Date: {date} ({day})
- Time: {start_time} - {end_time}
- Location: {branch} ({modality})
- Teacher: {teacher_name}

Important:
- Join on time
- You'll be marked as "Makeup Student"

Best regards,
Academic Affairs Team
```

### Email to Teacher (New Makeup Student)
```
Subject: New Makeup Student in Session {number}

Dear {teacher_name},

New makeup student in your session:

Session: {number} - {title}
Date: {date} {time}

Makeup Student:
- Name: {student_name} ({student_code})
- From: {original_class_code}

The student will appear in your attendance list with a "Makeup" badge.

Thank you!
Academic Affairs Team
```

---

## Key Implementation Points

1. **courseSessionId Matching:** CRITICAL - ensures same content
2. **Bidirectional Tracking:** `original_session.makeup_session_id` ↔ `makeup_session.original_session_id`
3. **Capacity Check:** Validate twice (submit + approve) for race conditions
4. **Schedule Conflict:** Check student's other sessions on same date/time
5. **Auto-Approval Logic:** AA on-behalf requests bypass pending review
6. **Dual Flow Auth:** Student endpoints require `ROLE_STUDENT`, AA endpoints require `ROLE_ACADEMIC_AFFAIR`
7. **Cross-Flexibility:** Allow different branch/modality for max convenience

---

**End of Makeup Request Guide**
