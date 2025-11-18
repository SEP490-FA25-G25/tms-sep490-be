# Student Request System - Documentation Index

**Version:** 1.0  
**Date:** 2025-11-07  
**Status:** Implementation Ready

---

## Overview

This documentation covers the **3 types of student requests** in the Training Management System (TMS):

1. **Absence Request** - Student xin nghỉ
2. **Makeup Request** - Student xin học bù
3. **Transfer Request** - Student xin chuyển lớp

Each request type has been broken down into a separate implementation guide for clarity and ease of development.

---

## Quick Navigation

### 📄 [Absence Request](./absence-request.md)
- **Purpose:** Student xin nghỉ buổi học chưa diễn ra
- **Complexity:** Low
- **Key Features:**
  - Future sessions only
  - Lead time warnings
  - Absence threshold tracking
  - Automatic attendance update on approval

**Read this first if:** You need to implement basic request workflow

---

### 📄 [Makeup Request](./makeup-request.md)
- **Purpose:** Student xin học bù cho buổi đã nghỉ
- **Complexity:** Medium
- **Key Features:**
  - Cross-class, cross-branch, cross-modality support
  - `courseSessionId` content matching
  - Smart recommendation algorithm
  - Bidirectional session tracking
  - Teacher notification with makeup badge

**Read this if:** You understand absence requests and need makeup logic

---

### 📄 [Transfer Request](./transfer-request.md)
- **Purpose:** Student chuyển lớp
- **Complexity:** High
- **Key Features:**
  - **ONE transfer per course** (hard limit)
  - Tier 1 (Self-Service) vs Tier 2 (Consultation Required)
  - Content gap analysis
  - Auto-execution of enrollment changes
  - Session migration logic
  - Multi-stakeholder notifications

**Read this last:** Most complex, requires understanding of enrollments and sessions

---

## Common Patterns Across All Requests

### Dual Flow Support

**Luồng 1: Student Self-Service**
```
Student creates → Status: PENDING → AA reviews → APPROVED/REJECTED
```

**Luồng 2: On-Behalf Creation**
```
Student contacts AA → AA creates → Status: WAITING_CONFIRM 
→ Student confirms → Status: PENDING → AA reviews → APPROVED/REJECTED
```

### Status State Machine

```
WAITING_CONFIRM (Luồng 2 only)
    ↓ (student confirms)
PENDING (all requests)
    ↓ (AA decision)
APPROVED / REJECTED
    ↓ (if approved)
AUTO-EXECUTE (transaction)
```

### Core Business Rules

| Rule | Absence | Makeup | Transfer |
|------|---------|--------|----------|
| **Future Only** | ✅ | ❌ (past sessions) | ✅ (effective date) |
| **Content Matching** | Session-level | `courseSessionId` | `courseId` |
| **Capacity Check** | ❌ | ✅ | ✅ |
| **Cross-Class** | N/A | ✅ | ✅ |
| **Quota Limit** | None | None | **1 per course** |
| **Reason Required** | ✅ (10 chars) | ✅ (10 chars) | ✅ (20 chars) |

---

## Database Schema Overview

### Core Tables

```sql
-- Main request table (all 3 types)
student_request
  ├── student_id
  ├── request_type (ABSENCE | MAKEUP | TRANSFER)
  ├── current_class_id
  ├── target_session_id (absence, makeup)
  ├── makeup_session_id (makeup only)
  ├── target_class_id (transfer only)
  ├── effective_date (transfer only)
  ├── status (WAITING_CONFIRM | PENDING | APPROVED | REJECTED)
  └── audit fields (submitted_by, decided_by, timestamps)

-- Attendance tracking
student_session
  ├── student_id
  ├── session_id
  ├── attendance_status
  ├── is_makeup (for makeup students)
  ├── makeup_session_id (forward reference)
  ├── original_session_id (backlink)
  └── note

-- Enrollment tracking (transfer impact)
enrollment
  ├── student_id
  ├── class_id
  ├── status (ENROLLED | TRANSFERRED | ...)
  ├── transfer_count (max 1 per course)
  ├── left_at, left_session_id
  └── join_session_id
```

---

## API Endpoint Patterns

### Common Endpoints

```http
# Submit request (all types)
POST /api/v1/student-requests
{
  "requestType": "ABSENCE" | "MAKEUP" | "TRANSFER",
  ...
}

# Approve request
PUT /api/v1/student-requests/{id}/approve
{
  "note": "Approval reason"
}

# Reject request
PUT /api/v1/student-requests/{id}/reject
{
  "rejectionReason": "Reason for rejection"
}

# Student confirms (Luồng 2 only)
PUT /api/v1/student-requests/{id}/confirm
{
  "confirmed": true
}
```

### Type-Specific Endpoints

```http
# Absence: Get available sessions
GET /api/v1/students/me/classes/sessions?date={date}

# Makeup: Get missed sessions
GET /api/v1/students/me/missed-sessions?weeksBack=4

# Makeup: Get makeup options
GET /api/v1/student-requests/makeup-options?targetSessionId={id}

# Transfer: Check eligibility
GET /api/v1/students/me/transfer-eligibility

# Transfer: Get transfer options (Tier 1)
GET /api/v1/student-requests/transfer-options/tier1?currentClassId={id}
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
1. ✅ Database schema setup
2. ✅ Enum definitions (`request_type`, `request_status`, etc.)
3. ✅ Entity models (`StudentRequest`, `StudentSession`, `Enrollment`)
4. ✅ Basic CRUD repositories

### Phase 2: Absence Request (Week 3)
1. ✅ Absence submission endpoint
2. ✅ Validation logic (future sessions, duplicates)
3. ✅ AA approval/rejection workflow
4. ✅ Attendance status update on approval
5. ✅ Email notifications

**Deliverable:** Students can request absences, AA can approve/reject

---

### Phase 3: Makeup Request (Week 4-5)
1. ✅ Missed sessions query
2. ✅ Makeup options finder (courseSessionId matching)
3. ✅ Scoring/ranking algorithm
4. ✅ Makeup submission with schedule conflict check
5. ✅ Create makeup `student_session` on approval
6. ✅ Teacher notification with makeup badge
7. ✅ Bidirectional tracking

**Deliverable:** Students can makeup missed sessions across classes

---

### Phase 4: Transfer Request (Week 6-8)
1. ✅ Transfer eligibility check (quota validation)
2. ✅ Tier 1: Self-service transfer
3. ✅ Content gap analysis algorithm
4. ✅ Approval auto-execution (enrollment + sessions)
5. ✅ Tier 2: Consultation flow
6. ✅ Counselor on-behalf creation
7. ✅ Student confirmation workflow
8. ✅ Multi-stakeholder notifications

**Deliverable:** Complete transfer system with Tier 1 + Tier 2 support

---

### Phase 5: Polish & Testing (Week 9)
1. ✅ Integration tests for all flows
2. ✅ Performance optimization (indexes, caching)
3. ✅ Dashboard analytics (turnaround time, approval rate)
4. ✅ Edge case handling
5. ✅ Documentation updates

---

## Testing Strategy

### Unit Tests (Per Request Type)
```java
// Absence
- validateFutureSessionOnly()
- checkDuplicateRequest()
- calculateAbsenceRate()
- statusTransitions()

// Makeup
- validateCourseSessionIdMatch()
- checkCapacity()
- detectScheduleConflict()
- scoreMakeupOptions()

// Transfer
- enforceTransferQuota()
- validateSameCourse()
- analyzeContentGap()
- determineTransferTier()
```

### Integration Tests
```java
// End-to-end flows
- submitAbsenceRequest_andApprove()
- submitMakeupRequest_crossClass()
- submitTransferRequest_tier1()
- submitTransferRequest_tier2_withConfirmation()

// Edge cases
- rejectRequestAfterSessionPassed()
- approveTransferWhenCapacityFull()
- timeoutTier2Confirmation()
```

---

## Configuration Reference

```yaml
# application.yml
student_requests:
  absence:
    lead_time_days: 1
    absence_threshold_percent: 20
    reason_min_length: 10
  
  makeup:
    eligible_weeks_lookback: 4
    max_concurrent_pending: 3
    reason_min_length: 10
    priority_scoring:
      same_branch_weight: 10
      same_modality_weight: 5
      soonest_date_weight: 3
  
  transfer:
    transfers_per_course: 1
    tier2_confirmation_hours: 48
    tier2_counselor_response_hours: 24
    max_content_gap_sessions: 3
    reason_min_length: 20
    allow_cross_branch: true
    allow_cross_modality: true
```

---

## Key Success Metrics

| Metric | Target | Purpose |
|--------|--------|---------|
| **Request Turnaround Time** | < 24 hours | AA efficiency |
| **Approval Rate** | > 85% | Process effectiveness |
| **Self-Service Rate** | > 80% | Automation success |
| **Makeup Completion Rate** | > 90% | Student follow-through |
| **Transfer Content Gap** | < 2 sessions (avg) | Transfer quality |
| **Student Satisfaction** | > 4.5/5 | Overall UX |

---

## Critical Design Decisions

### 1. Why Separate Tables? (student_request vs student_session)
- **Separation of Concerns:** Request = workflow, StudentSession = actual data
- **Audit Trail:** Keep request history even after execution
- **Query Performance:** Different query patterns for requests vs attendance

### 2. Why Bidirectional Tracking (Makeup)?
- **Query Efficiency:** "What did student makeup?" and "Who's making up today?"
- **Data Integrity:** Cross-reference validation
- **Reporting:** Complete audit trail for compliance

### 3. Why ONE Transfer Limit?
- **Business Rule:** Prevent class-hopping abuse
- **Data Integrity:** Enforced at database level (CHECK constraint)
- **Student Commitment:** Encourage thoughtful transfer decisions

### 4. Why Tier 1 vs Tier 2?
- **Efficiency:** Simple schedule changes don't need counselor
- **Quality:** Branch/modality changes benefit from consultation
- **Scalability:** Reduce counselor workload for routine transfers

### 5. Why Auto-Execution on Approval?
- **Consistency:** No manual steps = no human error
- **Atomicity:** ACID transaction ensures all-or-nothing
- **Audit Trail:** Timestamps track exactly when changes occurred

---

## Development Tips

1. **Start Simple:** Implement Absence first, it's the foundation
2. **Test Transactions:** Use `@Transactional` tests to verify rollback behavior
3. **Mock Notifications:** Don't let email issues block development
4. **Use DTOs:** Keep API contracts clean and versioned
5. **Validate Twice:** Client-side + server-side validation
6. **Log Everything:** Audit trail is critical for debugging
7. **Index Smartly:** Add indexes after you know query patterns
8. **Cache Wisely:** Invalidate cache on approval/rejection

---

## Troubleshooting Guide

### Common Issues

**Issue:** Duplicate request created despite validation
- **Cause:** Race condition in concurrent submissions
- **Fix:** Add unique constraint on (student_id, target_session_id, request_type, status)

**Issue:** Makeup session full error after approval
- **Cause:** Multiple concurrent approvals
- **Fix:** Re-check capacity in approval transaction with row-level locking

**Issue:** Transfer auto-execution partial failure
- **Cause:** Missing `@Transactional` annotation
- **Fix:** Ensure entire approval method is in single transaction

**Issue:** Content gap calculation incorrect
- **Cause:** Not filtering by courseSessionId
- **Fix:** Use courseSessionId as primary matching key, not session dates

---

## Support & References

- **Original Journey Doc:** `student-request-journey.md` (comprehensive, detailed)
- **PRD:** `docs/prd.md`
- **Business Flow:** `docs/business-flow-usecase.md`
- **Testing Guide:** `src/test/README.md`
- **Project Guide:** `AGENTS.md` (for coding agents)

---

## Quick Start for Coding Agents

```bash
# 1. Read this index first (you are here)
# 2. Choose implementation order:
#    - Start: absence-request.md
#    - Next: makeup-request.md
#    - Last: transfer-request.md
# 3. Each guide contains:
#    - Complete API specs
#    - Database schema
#    - Backend logic (Java code)
#    - Validation rules
#    - Transaction boundaries
# 4. Copy-paste code snippets directly
# 5. Adapt to existing codebase patterns
```

---

**Ready to implement? Start with:** [absence-request.md](./absence-request.md)

**Document End**
