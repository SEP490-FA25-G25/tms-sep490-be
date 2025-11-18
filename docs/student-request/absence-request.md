# Absence Request Implementation Guide

**Version:** 1.0  
**Date:** 2025-11-07  
**Request Type:** ABSENCE  

---

## Overview

**Purpose:** Student xin nghỉ buổi học chưa diễn ra  
**Complexity:** Low  
**Flow Support:** Dual (Self-Service + On-Behalf)  
**Business Impact:** Attendance tracking, excused absences  

---

## 📱 Student UX Flow

### UX Principle
> **Content-First & Progressive Disclosure:** Show only what's needed at each step. Guide student naturally from date selection → class/session → form submission.

### Flow Diagram
```
My Requests Page → [+ New Request] 
  → Modal: Choose Type (Absence/Makeup/Transfer)
  → Step 1: Select Date (Calendar)
  → Step 2: Select Class & Session (Radio)
  → Step 3: Fill Form (Reason + Note)
  → Submit → Success Message
```

---

### 🖥️ Screen 1: Select Date

**Purpose:** Let student pick which day they need to be absent

**UI Components:** `Dialog`, `Calendar`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Absence Request                       [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 1 of 3                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ When do you need to be absent?                          │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │           November 2025                         │   │
│ │   S   M   T   W   T   F   S                     │   │
│ │                       1   2                     │   │
│ │   3   4   5   6   7   8   9                     │   │
│ │  10  11  12  13  14  15  16   ← Today: 7       │   │
│ │  17  18  19  20  21  22  23                     │   │
│ │  24  25  26  27  28  29  30                     │   │
│ │                                                 │   │
│ │  • Grey: Past dates (disabled)                  │   │
│ │  • Blue: Has classes (selectable)               │   │
│ │  • White: No classes                            │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Student opens modal and sees calendar
2. Past dates are disabled (grey)
3. Dates with classes are highlighted (blue)
4. Click date → Load classes/sessions for that date
5. Click [Next] → Go to Step 2

**API Call Trigger:** When student selects a date

---

### 🖥️ Screen 2: Select Class & Session

**Purpose:** Choose which specific session to request absence for

**UI Components:** `RadioGroup`, `Card`, `Badge`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Absence Request                       [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 2 of 3                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ You have 2 classes on November 10, 2025                │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ CHN-A1-01 • Chinese A1 - Morning Class          │   │
│ │ Central Branch • Offline                        │   │
│ │                                                 │   │
│ │ ┌─────────────────────────────────────────┐    │   │
│ │ │ ○ Session 12: Grammar                   │    │   │
│ │ │   08:00 - 10:00                         │    │   │
│ │ └─────────────────────────────────────────┘    │   │
│ │                                                 │   │
│ │ ┌─────────────────────────────────────────┐    │   │
│ │ │ ○ Session 13: Vocabulary                │    │   │
│ │ │   10:15 - 12:15                         │    │   │
│ │ └─────────────────────────────────────────┘    │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ENG-B2-03 • English B2 - Evening Class          │   │
│ │ Central Branch • Online                         │   │
│ │                                                 │   │
│ │ ┌─────────────────────────────────────────┐    │   │
│ │ │ ○ Session 8: Listening Practice         │    │   │
│ │ │   18:00 - 20:00                         │    │   │
│ │ └─────────────────────────────────────────┘    │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. See all classes with sessions on selected date
2. Select one session (radio button)
3. Click [Next] → Go to Step 3

**Data Source:** Use data from previous API call (no new API needed)

---

### 🖥️ Screen 3: Fill Form & Submit

**Purpose:** Provide reason for absence and submit request

**UI Components:** `Textarea`, `Alert`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Absence Request                       [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 3 of 3                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Session Details (read-only)                             │
│ ┌─────────────────────────────────────────────────┐   │
│ │ Class: CHN-A1-01 • Chinese A1 - Morning Class   │   │
│ │ Session: Session 12 - Grammar                   │   │
│ │ Date: November 10, 2025 (Monday)                │   │
│ │ Time: 08:00 - 10:00                             │   │
│ │ Teacher: Mr. Nguyen Van A                       │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ⚠️ You have 4/20 absences (20% absence rate)          │
│                                                         │
│ Reason for absence *                                    │
│ ┌─────────────────────────────────────────────────┐   │
│ │ I have a medical appointment at the hospital    │   │
│ │ that cannot be rescheduled.                     │   │
│ │                                                 │   │
│ └─────────────────────────────────────────────────┘   │
│ Minimum 10 characters (45/10)                           │
│                                                         │
│ Additional notes (optional)                             │
│ ┌─────────────────────────────────────────────────┐   │
│ │ I will review the materials...                  │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│                                    [Cancel] [Submit]    │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Review session details (read-only)
2. See absence rate warning (if applicable)
3. Enter reason (minimum 10 characters)
4. Optionally enter additional notes
5. Click [Submit] → API call to create request

**Client-Side Validation:**
- Reason must be ≥ 10 characters
- Show character counter in real-time
- Disable [Submit] until valid

**API Call Trigger:** When student clicks [Submit]

---

### 🖥️ Screen 4: Success State

**Purpose:** Confirm submission and guide next steps

**UI Components:** `Dialog`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│                         ✓                               │
│                                                         │
│          Request Submitted Successfully                 │
│                                                         │
│     Your absence request has been sent to               │
│     Academic Affairs for review.                        │
│                                                         │
│     Request ID: #042                                    │
│     Status: Pending                                     │
│                                                         │
│     You'll receive an email notification once           │
│     your request is reviewed.                           │
│                                                         │
│                            [View My Requests]           │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. See success confirmation
2. Note the Request ID
3. Click [View My Requests] → Navigate to list page

---

## Business Rules

| Rule ID | Description | Enforcement |
|---------|-------------|-------------|
| BR-ABS-001 | Chỉ request cho future sessions (`date >= CURRENT_DATE`) | Blocking |
| BR-ABS-002 | Session must be `status = 'PLANNED'` | Blocking |
| BR-ABS-003 | No duplicate requests (same student + session) | Blocking |
| BR-ABS-004 | Lead time policy: Request X days trước | Warning only |
| BR-ABS-005 | Absence threshold: Warning khi > Y% | Warning only |
| BR-ABS-006 | Reason required, min 10 chars | Blocking |

**Configuration:**
```yaml
absence_request:
  lead_time_days: 1
  absence_threshold_percent: 20
  reason_min_length: 10
```

---

## API Endpoints

### 1. Get My Requests (Student)

**When to Call:** When student opens "My Requests" page

**Purpose:** List all requests submitted by the current student

**Request:**
```http
GET /api/v1/students/me/requests?requestType={type}&status={status}&page={page}&size={size}&sort={sort}
Authorization: Bearer {access_token}
```

**Query Parameters:**
- `requestType` (optional): Filter by type - `ABSENCE`, `MAKEUP`, `TRANSFER`
- `status` (optional): Filter by status - `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 10)
- `sort` (optional): Sort criteria (default: `submittedAt,desc`)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "requestType": "ABSENCE",
        "status": "PENDING",
        "currentClass": {
          "id": 101,
          "code": "CHN-A1-01",
          "name": "Chinese A1 - Morning Class"
        },
        "targetSession": {
          "id": 1012,
          "date": "2025-11-10",
          "courseSessionNumber": 12,
          "courseSessionTitle": "Grammar",
          "timeSlot": {
            "startTime": "08:00:00",
            "endTime": "10:00:00"
          }
        },
        "requestReason": "I have a medical appointment.",
        "note": "Will review materials.",
        "submittedAt": "2025-11-07T14:30:00+07:00",
        "decidedAt": null,
        "decidedBy": null,
        "rejectionReason": null
      },
      {
        "id": 38,
        "requestType": "ABSENCE",
        "status": "APPROVED",
        "currentClass": {
          "id": 101,
          "code": "CHN-A1-01",
          "name": "Chinese A1 - Morning Class"
        },
        "targetSession": {
          "id": 1008,
          "date": "2025-11-05",
          "courseSessionNumber": 11,
          "courseSessionTitle": "Listening",
          "timeSlot": {
            "startTime": "08:00:00",
            "endTime": "10:00:00"
          }
        },
        "requestReason": "Family emergency.",
        "note": "Approved due to valid reason.",
        "submittedAt": "2025-11-03T10:00:00+07:00",
        "decidedAt": "2025-11-03T15:30:00+07:00",
        "decidedBy": {
          "id": 789,
          "fullName": "AA Staff Nguyen",
          "email": "aa.staff@example.com"
        },
        "rejectionReason": null
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": {
        "sorted": true,
        "unsorted": false
      }
    },
    "totalElements": 15,
    "totalPages": 2,
    "last": false,
    "first": true
  }
}
```

**Frontend Usage:**
```typescript
// Load student's requests
const loadMyRequests = async (filters: RequestFilters) => {
  const params = new URLSearchParams({
    ...(filters.type && { requestType: filters.type }),
    ...(filters.status && { status: filters.status }),
    page: filters.page.toString(),
    size: '10',
    sort: 'submittedAt,desc'
  });

  const response = await fetch(
    `/api/v1/students/me/requests?${params}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  return response.json();
};
```

---

### 2. Get Request Details (Student)

**When to Call:** When student clicks on a request to view details

**Request:**
```http
GET /api/v1/students/me/requests/{id}
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "requestType": "ABSENCE",
    "status": "PENDING",
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Chinese A1 - Morning Class",
      "branch": {
        "id": 1,
        "name": "Central Branch"
      }
    },
    "targetSession": {
      "id": 1012,
      "date": "2025-11-10",
      "courseSessionNumber": 12,
      "courseSessionTitle": "Grammar",
      "timeSlot": {
        "startTime": "08:00:00",
        "endTime": "10:00:00"
      },
      "teacher": {
        "id": 456,
        "fullName": "Mr. Nguyen Van A"
      }
    },
    "requestReason": "I have a medical appointment at the hospital.",
    "note": "I will review materials and catch up.",
    "submittedAt": "2025-11-07T14:30:00+07:00",
    "submittedBy": {
      "id": 123,
      "fullName": "John Doe",
      "email": "john.doe@example.com"
    },
    "decidedAt": null,
    "decidedBy": null,
    "rejectionReason": null
  }
}
```

---

### 3. Cancel Request (Student)

**When to Call:** When student wants to cancel a pending request

**Request:**
```http
PUT /api/v1/students/me/requests/{id}/cancel
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "message": "Request cancelled successfully",
  "data": {
    "id": 42,
    "status": "CANCELLED"
  }
}
```

**Business Rules:**
- Only `PENDING` requests can be cancelled
- Cannot cancel after decision made (`APPROVED`/`REJECTED`)

---

### 4. Get Pending Requests for Review (Academic Affairs)

**When to Call:** When AA staff opens "Requests Management" page

**Purpose:** List all pending requests that need review at AA's center(s)

**Request:**
```http
GET /api/v1/student-requests/pending?branchId={branchId}&requestType={type}&studentName={name}&classCode={code}&sessionDateFrom={from}&sessionDateTo={to}&page={page}&size={size}&sort={sort}
Authorization: Bearer {access_token}
```

**Query Parameters:**
- `branchId` (optional): Filter by branch (AA can see requests from their assigned branches)
- `requestType` (optional): Filter by type - `ABSENCE`, `MAKEUP`, `TRANSFER`
- `studentName` (optional): Search by student name
- `classCode` (optional): Search by class code
- `sessionDateFrom` (optional): Filter sessions from date (format: `YYYY-MM-DD`)
- `sessionDateTo` (optional): Filter sessions to date
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort criteria (default: `submittedAt,asc` - oldest first)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "requestType": "ABSENCE",
        "status": "PENDING",
        "student": {
          "id": 123,
          "studentCode": "STU2024001",
          "fullName": "John Doe",
          "email": "john.doe@example.com",
          "phone": "0123456789"
        },
        "currentClass": {
          "id": 101,
          "code": "CHN-A1-01",
          "name": "Chinese A1 - Morning Class",
          "branch": {
            "id": 1,
            "name": "Central Branch"
          }
        },
        "targetSession": {
          "id": 1012,
          "date": "2025-11-10",
          "courseSessionNumber": 12,
          "courseSessionTitle": "Grammar",
          "timeSlot": {
            "startTime": "08:00:00",
            "endTime": "10:00:00"
          },
          "teacher": {
            "id": 456,
            "fullName": "Mr. Nguyen Van A"
          }
        },
        "requestReason": "I have a medical appointment.",
        "note": "Will review materials.",
        "submittedAt": "2025-11-07T14:30:00+07:00",
        "submittedBy": {
          "id": 123,
          "fullName": "John Doe"
        },
        "daysUntilSession": 3,
        "studentAbsenceRate": 15.5
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 45,
    "totalPages": 3,
    "summary": {
      "totalPending": 45,
      "needsUrgentReview": 12,
      "absenceRequests": 30,
      "makeupRequests": 10,
      "transferRequests": 5
    }
  }
}
```

**Notes:**
- `daysUntilSession`: Number of days until the session (for urgency)
- `studentAbsenceRate`: Student's current absence rate in percentage
- `needsUrgentReview`: Requests with sessions happening in next 2 days

**Frontend Usage:**
```typescript
// Load pending requests for AA review
const loadPendingRequests = async (filters: AARequestFilters) => {
  const params = new URLSearchParams({
    ...(filters.branchId && { branchId: filters.branchId.toString() }),
    ...(filters.type && { requestType: filters.type }),
    ...(filters.studentName && { studentName: filters.studentName }),
    ...(filters.classCode && { classCode: filters.classCode }),
    page: filters.page.toString(),
    size: '20',
    sort: 'submittedAt,asc'
  });

  const response = await fetch(
    `/api/v1/student-requests/pending?${params}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );

  return response.json();
};
```

---

### 5. Get All Requests History (Academic Affairs)

**When to Call:** When AA staff wants to view all requests history (approved/rejected/cancelled)

**Request:**
```http
GET /api/v1/student-requests?branchId={branchId}&status={status}&requestType={type}&studentName={name}&classCode={code}&decidedBy={userId}&sessionDateFrom={from}&sessionDateTo={to}&submittedDateFrom={from}&submittedDateTo={to}&page={page}&size={size}&sort={sort}
Authorization: Bearer {access_token}
```

**Query Parameters:**
- `branchId` (optional): Filter by branch
- `status` (optional): Filter by status - `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`
- `requestType` (optional): Filter by type
- `studentName` (optional): Search by student name
- `classCode` (optional): Search by class code
- `decidedBy` (optional): Filter by who decided (AA user ID)
- `sessionDateFrom` / `sessionDateTo` (optional): Filter by session date range
- `submittedDateFrom` / `submittedDateTo` (optional): Filter by submission date range
- `page`, `size`, `sort`: Pagination

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 38,
        "requestType": "ABSENCE",
        "status": "APPROVED",
        "student": {
          "id": 123,
          "studentCode": "STU2024001",
          "fullName": "John Doe"
        },
        "currentClass": {
          "id": 101,
          "code": "CHN-A1-01",
          "name": "Chinese A1 - Morning Class"
        },
        "targetSession": {
          "id": 1008,
          "date": "2025-11-05",
          "courseSessionNumber": 11,
          "courseSessionTitle": "Listening"
        },
        "requestReason": "Family emergency.",
        "submittedAt": "2025-11-03T10:00:00+07:00",
        "submittedBy": {
          "id": 123,
          "fullName": "John Doe"
        },
        "decidedAt": "2025-11-03T15:30:00+07:00",
        "decidedBy": {
          "id": 789,
          "fullName": "AA Staff Nguyen"
        },
        "note": "Approved due to valid reason."
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 250
  }
}
```

---

### 6. Get Request Details (Academic Affairs)

**When to Call:** When AA staff clicks on a request to view full details before deciding

**Request:**
```http
GET /api/v1/student-requests/{id}
Authorization: Bearer {access_token}
```

**Response:** Same structure as Student's "Get Request Details" but with additional AA-specific data:
```json
{
  "success": true,
  "data": {
    "id": 42,
    "requestType": "ABSENCE",
    "status": "PENDING",
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe",
      "email": "john.doe@example.com",
      "phone": "0123456789"
    },
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Chinese A1 - Morning Class",
      "branch": {
        "id": 1,
        "name": "Central Branch"
      },
      "teacher": {
        "id": 456,
        "fullName": "Mr. Nguyen Van A",
        "email": "teacher.a@example.com"
      }
    },
    "targetSession": {
      "id": 1012,
      "date": "2025-11-10",
      "dayOfWeek": "MONDAY",
      "courseSessionNumber": 12,
      "courseSessionTitle": "Grammar",
      "timeSlot": {
        "startTime": "08:00:00",
        "endTime": "10:00:00"
      },
      "status": "PLANNED"
    },
    "requestReason": "I have a medical appointment at the hospital.",
    "note": "I will review materials and catch up.",
    "submittedAt": "2025-11-07T14:30:00+07:00",
    "submittedBy": {
      "id": 123,
      "fullName": "John Doe",
      "email": "john.doe@example.com"
    },
    "decidedAt": null,
    "decidedBy": null,
    "rejectionReason": null,
    "additionalInfo": {
      "daysUntilSession": 3,
      "studentAbsenceStats": {
        "totalAbsences": 4,
        "totalSessions": 20,
        "absenceRate": 20.0,
        "excusedAbsences": 2,
        "unexcusedAbsences": 2
      },
      "previousRequests": {
        "totalRequests": 3,
        "approvedRequests": 2,
        "rejectedRequests": 0,
        "cancelledRequests": 1
      }
    }
  }
}
```

**Notes:**
- `additionalInfo` provides context for AA to make informed decisions
- Includes student's absence statistics and request history

---

### 7. Get Available Sessions for Date

**When to Call:** When student selects a date in Step 1

**Purpose:** Load all classes and sessions for the selected date

**Request:**
```http
GET /api/v1/students/me/classes/sessions?date={date}&requestType=ABSENCE
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "classId": 101,
      "classCode": "CHN-A1-01",
      "className": "Chinese A1 - Morning Class",
      "courseId": 10,
      "courseName": "Chinese A1",
      "branchId": 1,
      "branchName": "Central Branch",
      "modality": "OFFLINE",
      "sessionCount": 2,
      "sessions": [
        {
          "sessionId": 1012,
          "date": "2025-11-10",
          "courseSessionNumber": 12,
          "courseSessionTitle": "Grammar",
          "timeSlot": {
            "startTime": "08:00:00",
            "endTime": "10:00:00"
          },
          "status": "PLANNED",
          "type": "CLASS"
        }
      ]
    }
  ]
}
```

**Frontend Usage:**
```typescript
// Step 1: When student selects date
const handleDateSelect = async (date: Date) => {
  const response = await fetch(
    `/api/v1/students/me/classes/sessions?date=${formatDate(date)}&requestType=ABSENCE`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const data = await response.json();
  
  // Show Step 2 with classes/sessions
  setClasses(data.data);
  goToStep(2);
};
```

---

### 8. Submit Absence Request

**When to Call:** When student clicks [Submit] in Step 3

**Request:**
```http
POST /api/v1/student-requests
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "requestType": "ABSENCE",
  "currentClassId": 101,
  "targetSessionId": 1012,
  "requestReason": "I have a medical appointment at the hospital.",
  "note": "I will review materials and catch up."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Absence request submitted successfully",
  "data": {
    "id": 42,
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "requestType": "ABSENCE",
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Chinese A1 - Morning Class"
    },
    "targetSession": {
      "id": 1012,
      "date": "2025-11-10",
      "courseSessionNumber": 12,
      "courseSessionTitle": "Grammar",
      "timeSlot": {
        "startTime": "08:00:00",
        "endTime": "10:00:00"
      }
    },
    "requestReason": "I have a medical appointment at the hospital.",
    "note": "I will review materials and catch up.",
    "status": "PENDING",
    "submittedAt": "2025-11-07T14:30:00+07:00",
    "submittedBy": {
      "id": 456,
      "email": "john.doe@example.com",
      "fullName": "John Doe"
    }
  }
}
```

**Frontend Usage:**
```typescript
// Step 3: When student clicks Submit
const handleSubmit = async (formData: AbsenceFormData) => {
  try {
    const response = await fetch('/api/v1/student-requests', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requestType: 'ABSENCE',
        currentClassId: selectedClass.id,
        targetSessionId: selectedSession.id,
        requestReason: formData.reason,
        note: formData.note
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      // Show success screen
      showSuccessDialog(result.data.id);
    }
  } catch (error) {
    // Show error toast
    showError(error.message);
  }
};
```

---

### 9. Approve Request (Academic Affairs Only)

**When to Call:** When AA staff approves a request

**Request:**
```http
PUT /api/v1/student-requests/{id}/approve
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "note": "Approved due to valid medical reason."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Absence request approved successfully",
  "data": {
    "id": 42,
    "status": "APPROVED",
    "decidedBy": {
      "id": 789,
      "email": "aa.staff@example.com",
      "fullName": "AA Staff Nguyen"
    },
    "decidedAt": "2025-11-07T15:45:00+07:00"
  }
}
```

**Side Effects:**
- Updates `student_session.attendance_status = 'ABSENT'`
- Sends email notification to student

---

### 10. Reject Request (Academic Affairs Only)

**When to Call:** When AA staff rejects a request

**Request:**
```http
PUT /api/v1/student-requests/{id}/reject
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "rejectionReason": "Insufficient lead time. Submit 24 hours in advance."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Absence request rejected",
  "data": {
    "id": 42,
    "status": "REJECTED",
    "rejectionReason": "Insufficient lead time. Submit 24 hours in advance.",
    "decidedBy": {
      "id": 789,
      "email": "aa.staff@example.com",
      "fullName": "AA Staff Nguyen"
    },
    "decidedAt": "2025-11-07T15:45:00+07:00"
  }
}
```

**Side Effects:**
- Sends email notification to student with rejection reason

---

## Database Schema

### Table: `student_request`

```sql
CREATE TABLE student_request (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    request_type request_type_enum NOT NULL, -- 'ABSENCE'
    current_class_id BIGINT NOT NULL REFERENCES class(id),
    target_session_id BIGINT NOT NULL REFERENCES session(id),
    request_reason TEXT NOT NULL,
    note TEXT,
    status request_status_enum NOT NULL DEFAULT 'pending',
    submitted_by BIGINT NOT NULL REFERENCES user(id),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_by BIGINT REFERENCES user(id),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_absence_valid CHECK (
        request_type != 'ABSENCE' OR (
            target_session_id IS NOT NULL AND
            makeup_session_id IS NULL AND
            target_class_id IS NULL
        )
    )
);

CREATE INDEX idx_student_request_status ON student_request(status, request_type);
CREATE INDEX idx_student_request_student ON student_request(student_id, status);
```

### Update: `student_session`

```sql
-- When approved:
UPDATE student_session
SET 
    attendance_status = 'ABSENT',
    note = CONCAT('Excused absence approved on ', NOW(), '. Request ID: ', {request_id})
WHERE student_id = {student_id}
  AND session_id = {target_session_id};
```

---

## Backend Transaction Logic

### Validation (Submit)

```java
// StudentRequestService.java
public StudentRequestResponseDTO submitAbsenceRequest(AbsenceRequestDTO dto) {
    // 1. Validate session exists and is future
    Session session = sessionRepository.findById(dto.getTargetSessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    if (session.getDate().isBefore(LocalDate.now())) {
        throw new BusinessRuleException("Cannot request absence for past sessions");
    }
    
    if (!session.getStatus().equals(SessionStatus.PLANNED)) {
        throw new BusinessRuleException("Session must be in PLANNED status");
    }
    
    // 2. Validate student enrollment
    Enrollment enrollment = enrollmentRepository
        .findByStudentIdAndClassIdAndStatus(
            getCurrentUserId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED)
        .orElseThrow(() -> new BusinessRuleException("Not enrolled in this class"));
    
    // 3. Check duplicate request
    boolean hasDuplicate = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
        getCurrentUserId(), dto.getTargetSessionId(), RequestType.ABSENCE, 
        List.of(RequestStatus.PENDING, RequestStatus.APPROVED));
    
    if (hasDuplicate) {
        throw new BusinessRuleException("Duplicate absence request for this session");
    }
    
    // 4. Check lead time (warning only)
    long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), session.getDate());
    if (daysUntil < configProperties.getAbsence().getLeadTimeDays()) {
        // Log warning, don't block
        log.warn("Absence request submitted with insufficient lead time: {} days", daysUntil);
    }
    
    // 5. Check absence threshold (warning only)
    double absenceRate = calculateAbsenceRate(getCurrentUserId(), dto.getCurrentClassId());
    if (absenceRate > configProperties.getAbsence().getThresholdPercent()) {
        // Log warning, don't block
        log.warn("Student absence rate {}% exceeds threshold", absenceRate);
    }
    
    // 6. Create request
    StudentRequest request = StudentRequest.builder()
        .student(studentRepository.getReferenceById(getCurrentUserId()))
        .requestType(RequestType.ABSENCE)
        .currentClass(classRepository.getReferenceById(dto.getCurrentClassId()))
        .targetSession(session)
        .requestReason(dto.getRequestReason())
        .note(dto.getNote())
        .status(RequestStatus.PENDING)
        .submittedBy(userRepository.getReferenceById(getCurrentUserId()))
        .submittedAt(LocalDateTime.now())
        .build();
    
    request = studentRequestRepository.save(request);
    
    // 7. Send notification
    notificationService.notifyAcademicAffair(request);
    
    return mapper.toResponseDTO(request);
}
```

### Approval Transaction

```java
@Transactional
public StudentRequestResponseDTO approveAbsenceRequest(Long requestId, ApprovalDTO dto) {
    // 1. Load request
    StudentRequest request = studentRequestRepository.findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    
    if (!request.getRequestType().equals(RequestType.ABSENCE)) {
        throw new BusinessRuleException("Not an absence request");
    }
    
    if (!request.getStatus().equals(RequestStatus.PENDING)) {
        throw new BusinessRuleException("Request not in PENDING status");
    }
    
    // 2. Update request status
    request.setStatus(RequestStatus.APPROVED);
    request.setDecidedBy(userRepository.getReferenceById(getCurrentUserId()));
    request.setDecidedAt(LocalDateTime.now());
    request.setNote(dto.getNote());
    request = studentRequestRepository.save(request);
    
    // 3. Update student_session attendance
    StudentSession studentSession = studentSessionRepository
        .findByStudentIdAndSessionId(request.getStudent().getId(), request.getTargetSession().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Student session not found"));
    
    studentSession.setAttendanceStatus(AttendanceStatus.ABSENT);
    studentSession.setNote(String.format("Excused absence approved on %s. Request ID: %d", 
        LocalDateTime.now(), requestId));
    studentSessionRepository.save(studentSession);
    
    // 4. Send notifications
    notificationService.notifyStudent(request, "approved");
    
    return mapper.toResponseDTO(request);
}
```

---

## Status State Machine

```
[Student submits] → PENDING → [AA reviews] → APPROVED → [Auto-execute]
                                            → REJECTED
```

**Status Transitions:**
- `null → PENDING`: Student submits (Luồng 1) OR AA submits (Luồng 2 → requires confirmation first)
- `PENDING → APPROVED`: AA approves
- `PENDING → REJECTED`: AA rejects
- `PENDING → CANCELLED`: Student cancels before decision

---

## Notifications

### Email Templates

**To Student (Approved):**
```
Subject: Your Absence Request has been Approved

Dear {student_name},

Your absence request has been approved:

Session: {session_title}
Date: {date} {time}
Class: {class_code}

Note from Academic Affairs:
{approval_note}

Your attendance will be marked as "Excused Absence".
If you wish to make up this session, submit a Makeup Request.

Best regards,
Academic Affairs Team
```

**To Student (Rejected):**
```
Subject: Your Absence Request has been Rejected

Dear {student_name},

Your absence request has been rejected.

Session: {session_title}
Date: {date}

Reason:
{rejection_reason}

Please contact Academic Affairs if you have questions.

Best regards,
Academic Affairs Team
```

**To Academic Affairs (New Request):**
```
Subject: New Absence Request from {student_name} ({class_code})

A new absence request requires your review:

Student: {student_name} ({student_code})
Class: {class_code}
Session: {session_title}
Date: {date} {time}
Reason: {request_reason}

View Request: {link}
```

---

## UI Flow

1. **Select Date** → Calendar widget
2. **Load Classes** → API call with `date` param
3. **Select Class** → Expand sessions
4. **Select Session** → Show details
5. **Fill Reason** → Textarea validation (min 10 chars)
6. **Submit** → Validation + API call
7. **Success** → Redirect to "My Requests"

---

## Testing Scenarios

### Unit Tests
- ✅ Validate future session only
- ✅ Validate PLANNED status
- ✅ Check duplicate request
- ✅ Calculate absence rate
- ✅ Status transitions

### Integration Tests
- ✅ Submit request → DB record created
- ✅ Approve → `student_session` updated
- ✅ Reject → Status updated, no side effects
- ✅ Notification sent to AA and student

---

## Performance Considerations

**Indexes:**
```sql
CREATE INDEX idx_session_date_status ON session(date, status);
CREATE INDEX idx_student_session_attendance ON student_session(student_id, attendance_status);
```

**Query Optimization:**
```sql
-- Get sessions for date
SELECT s.*, c.* 
FROM session s
JOIN class c ON s.class_id = c.id
JOIN enrollment e ON e.class_id = c.id
WHERE e.student_id = ?
  AND s.date = ?
  AND s.status = 'PLANNED'
  AND e.status = 'ENROLLED';
```

---

## Key Points for Implementation

1. **Validation Order:** Session exists → Future date → Enrollment → Duplicate → Warnings
2. **Transaction Boundary:** Request creation + Notification sending
3. **Audit Trail:** Track `submitted_by`, `submitted_at`, `decided_by`, `decided_at`
4. **No Deletion:** Status updates only, preserve history
5. **Warnings vs Blockers:** Lead time and absence threshold are warnings, not blockers

---

**End of Absence Request Guide**
