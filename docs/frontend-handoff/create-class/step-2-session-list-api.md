# Step 2: Session List API - Frontend Handoff

## API Endpoint
```
GET /api/v1/classes/{classId}/sessions
```

**Mục đích:** Xem danh sách buổi học đã được tự động generate sau khi tạo class (dùng cho màn hình "Xem lại buổi học")

---

## Response Example

## Response Example

```json
{
  "success": true,
  "message": "Sessions retrieved successfully",
  "data": {
    "classId": 6,
    "classCode": "IELTSFOUND-HN01-25-006",
    "totalSessions": 24,
    "dateRange": {
      "startDate": "2025-11-17",
      "endDate": "2026-01-09"
    },
    "sessions": [
      {
        "sessionId": 425,
        "sequenceNumber": 1,
        "date": "2025-11-17",
        "dayOfWeek": "Thứ Hai",
        "courseSessionName": "Introduction to IELTS & Basic Listening",
        "hasTimeSlot": false,
        "hasResource": false,
        "hasTeacher": false,
        "timeSlotInfo": null
      }
    ],
    "groupedByWeek": [
      {
        "weekNumber": 1,
        "weekRange": "2025-11-17 - 2025-11-23",
        "sessionCount": 3,
        "sessionIds": [425, 426, 427]
      }
    ]
  }
}
```

---

## Key Fields

- **sessions**: Flat list của tất cả sessions - dùng cho table/list view
- **groupedByWeek**: Sessions đã nhóm theo tuần - dùng cho timeline view  
- **dayOfWeek**: Tên thứ tiếng Việt đã format sẵn ("Thứ Hai", "Thứ Ba", ...)
- **hasTimeSlot/hasResource/hasTeacher**: Boolean flags để show trạng thái gán (✓/✗)
- **timeSlotInfo**: Có giá trị khi đã gán khung giờ, null khi chưa gán

---

## Frontend Usage

### 1. Timeline View (Recommended)
```typescript
{data.groupedByWeek.map(week => (
  <WeekSection key={week.weekNumber}>
    <h3>Tuần {week.weekNumber}: {week.weekRange}</h3>
    {week.sessionIds.map(id => {
      const session = data.sessions.find(s => s.sessionId === id);
      return <SessionCard session={session} />;
    })}
  </WeekSection>
))}
```

### 2. Table View
```typescript
<Table>
  {data.sessions.map(session => (
    <tr key={session.sessionId}>
      <td>{session.sequenceNumber}</td>
      <td>{session.date}</td>
      <td>{session.dayOfWeek}</td>
      <td>{session.courseSessionName}</td>
      <td>{session.hasTimeSlot ? '✓' : '✗'}</td>
    </tr>
  ))}
</Table>
```

### 3. Header Summary
```typescript
<h2>Tự động tạo {data.totalSessions} buổi học</h2>
<p>Từ {data.dateRange.startDate} đến {data.dateRange.endDate}</p>
```

---

## TypeScript Types

```typescript
interface SessionListResponse {
  classId: number;
  classCode: string;
  totalSessions: number;
  dateRange: { startDate: string; endDate: string };
  sessions: Session[];
  groupedByWeek: WeekGroup[];
}

interface Session {
  sessionId: number;
  sequenceNumber: number;
  date: string;
  dayOfWeek: string;  // "Thứ Hai", "Thứ Ba", ...
  courseSessionName: string;
  hasTimeSlot: boolean;
  hasResource: boolean;
  hasTeacher: boolean;
  timeSlotInfo: { startTime: string; endTime: string; displayName: string } | null;
}

interface WeekGroup {
  weekNumber: number;
  weekRange: string;
  sessionCount: number;
  sessionIds: number[];
}
```

---

## Test Command

```bash
curl -X GET "http://localhost:8080/api/v1/classes/6/sessions" | jq
```

**Expected:** 24 sessions grouped into 8 weeks
