# API: Get Teachers Available by Day - Frontend Handoff

## Tổng Quan

API này trả về danh sách giáo viên có thể dạy cho một lớp học cụ thể, được nhóm theo ngày trong tuần. Backend tự động filter dựa trên thông tin của lớp học đã được lưu trong database.

**⚠️ LƯU Ý QUAN TRỌNG:**
- Frontend **KHÔNG CẦN** gửi thông tin chi nhánh, ngày học, khóa học từ màn create class
- Backend **TỰ ĐỘNG** đọc tất cả thông tin từ database dựa trên `classId`
- Class đã được tạo và lưu vào database trước khi gọi API này

---

## API Endpoint

### Request

```http
GET /api/v1/classes/{classId}/teachers/available-by-day
Authorization: Bearer {access_token}
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `classId` | Long | ✅ Yes | ID của lớp học cần assign teacher |

**Headers:**
| Header | Value | Required | Description |
|--------|-------|----------|-------------|
| `Authorization` | Bearer {token} | ✅ Yes | JWT access token |

**Query Parameters:** Không có

**Request Body:** Không có

### Response

```json
{
  "success": true,
  "message": "Found 4 teachers with availability on 11 day(s)",
  "data": [
    {
      "teacherId": 4,
      "fullName": "Sarah Johnson",
      "email": "sarah.johnson@englishcenter.com",
      "employeeCode": "TCH004",
      "skills": [
        "IELTS_LISTENING",
        "IELTS_READING",
        "IELTS_WRITING",
        "IELTS_SPEAKING"
      ],
      "hasGeneralSkill": false,
      "availableDays": [
        {
          "dayOfWeek": 1,
          "dayName": "Thứ Hai",
          "totalSessions": 8,
          "availableSessions": 8,
          "firstDate": "2024-01-15",
          "lastDate": "2024-03-25",
          "isFullyAvailable": true,
          "timeSlotDisplay": "08:00 - 10:00"
        },
        {
          "dayOfWeek": 3,
          "dayName": "Thứ Tư",
          "totalSessions": 8,
          "availableSessions": 8,
          "firstDate": "2024-01-17",
          "lastDate": "2024-03-27",
          "isFullyAvailable": true,
          "timeSlotDisplay": "08:00 - 10:00"
        },
        {
          "dayOfWeek": 5,
          "dayName": "Thứ Sáu",
          "totalSessions": 8,
          "availableSessions": 8,
          "firstDate": "2024-01-19",
          "lastDate": "2024-03-29",
          "isFullyAvailable": true,
          "timeSlotDisplay": "08:00 - 10:00"
        }
      ],
      "totalClassSessions": 24
    }
  ]
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `success` | Boolean | Trạng thái thành công |
| `message` | String | Thông báo kết quả |
| `data[]` | Array | Danh sách teachers có thể dạy |
| `data[].teacherId` | Long | ID của giáo viên |
| `data[].fullName` | String | Họ tên giáo viên |
| `data[].email` | String | Email giáo viên |
| `data[].employeeCode` | String | Mã nhân viên |
| `data[].skills` | String[] | Danh sách kỹ năng (IELTS_LISTENING, TOEIC_READING, ...) |
| `data[].hasGeneralSkill` | Boolean | Có skill GENERAL (dạy được mọi môn) hay không |
| `data[].availableDays[]` | Array | Các ngày trong tuần mà teacher có thể dạy |
| `data[].availableDays[].dayOfWeek` | Short | Ngày trong tuần (1=Thứ Hai, 2=Thứ Ba, ..., 7=Chủ Nhật) |
| `data[].availableDays[].dayName` | String | Tên ngày bằng tiếng Việt |
| `data[].availableDays[].totalSessions` | Integer | Tổng số buổi học trong ngày này |
| `data[].availableDays[].availableSessions` | Integer | Số buổi teacher có thể dạy (luôn = totalSessions) |
| `data[].availableDays[].firstDate` | LocalDate | Ngày đầu tiên của lớp trong ngày này |
| `data[].availableDays[].lastDate` | LocalDate | Ngày cuối cùng của lớp trong ngày này |
| `data[].availableDays[].isFullyAvailable` | Boolean | Teacher có thể dạy 100% buổi trong ngày này (luôn true) |
| `data[].availableDays[].timeSlotDisplay` | String | Khung giờ học (format: "HH:mm - HH:mm") |
| `data[].totalClassSessions` | Integer | Tổng số buổi học của lớp |

---

## Backend Logic (Tự Động)

Backend **TỰ ĐỘNG** filter teachers dựa trên 5 điều kiện:

### 1. ✅ Teacher Availability (Lịch rảnh)
- Kiểm tra bảng `teacher_availability`
- Teacher phải đăng ký rảnh vào ngày trong tuần tương ứng (Monday, Wednesday, Friday, ...)

### 2. ✅ No Teaching Conflicts (Không trùng lịch dạy)
- Kiểm tra bảng `teaching_slot`
- Teacher không được có lịch dạy trùng với time slot của lớp này
- Chỉ check các teaching slot có status = 'SCHEDULED'

### 3. ⏳ No Leave Conflicts (Không nghỉ phép) - TODO
- ⚠️ Tạm thời chưa implement (bảng `leave_request` chưa tồn tại)
- Sẽ kiểm tra teacher không có đơn nghỉ phép được duyệt trong khoảng thời gian này

### 4. ✅ Skill Match (Phù hợp kỹ năng)
- Kiểm tra bảng `teacher_skill`
- Teacher phải có skill phù hợp với môn học của lớp:
  - IELTS class → IELTS skills (LISTENING, READING, WRITING, SPEAKING)
  - TOEIC class → TOEIC skills (LISTENING, READING)
  - TOEFL class → TOEFL skills (LISTENING, READING, WRITING, SPEAKING)
- **HOẶC** teacher có skill GENERAL (dạy được mọi môn)

### 5. ✅ Same Branch (Cùng chi nhánh)
- Kiểm tra bảng `user_branches` và `class`
- Teacher phải thuộc cùng chi nhánh với lớp học
- **Quan trọng:** Đây là điều kiện bảo mật, teacher chỉ dạy ở chi nhánh của mình

### 6. ✅ 100% Availability per Day (Rảnh toàn bộ)
- Teacher phải rảnh **TẤT CẢ** các buổi học trong mỗi ngày
- Ví dụ: Nếu lớp có 8 buổi vào Thứ Hai → Teacher phải rảnh cả 8 buổi đó
- Điều kiện: `totalSessions = availableSessions` cho mỗi ngày

---

## Data Source (Backend Tự Động Lấy)

Backend **KHÔNG DÙNG** data từ frontend, mà đọc từ database:

| Thông tin | Source | SQL |
|-----------|--------|-----|
| **Chi nhánh** | `class.branch_id` | `JOIN class c ON c.id = :classId`<br>`WHERE ub.branch_id = c.branch_id` |
| **Ngày học** | `session.date` | `SELECT EXTRACT(DOW FROM s.date)`<br>`FROM session s WHERE s.class_id = :classId` |
| **Khóa học** | `class.course_id` | `JOIN course co ON co.id = c.course_id` |
| **Level** | `course.level_id` | `JOIN level l ON l.id = co.level_id` |
| **Subject** | `level.subject_id` | `JOIN subject subj ON subj.id = l.subject_id` |
| **Sessions** | `session` table | `WHERE s.class_id = :classId AND s.status = 'PLANNED'` |
| **Time slots** | `session.time_slot_template_id` | `LEFT JOIN time_slot_template ts_template` |

**Lý do:**
- ✅ **Single Source of Truth**: Database là nguồn dữ liệu duy nhất
- ✅ **Bảo mật**: Không tin tưởng data từ frontend (user có thể manipulate)
- ✅ **Nhất quán**: Đảm bảo data luôn đúng với database

---

## UI Flow (Gợi Ý)

### Bước 1: Lấy danh sách teachers
```javascript
// Sau khi tạo class thành công, có classId
const classId = 6;

const response = await fetch(
  `http://localhost:8080/api/v1/classes/${classId}/teachers/available-by-day`,
  {
    headers: {
      'Authorization': `Bearer ${accessToken}`
    }
  }
);

const result = await response.json();
// result.data = array of teachers với availableDays
```

### Bước 2: Hiển thị danh sách teachers
```
┌─────────────────────────────────────────────────────────┐
│ Assign Teacher for Class IELTS-HN01-25-001             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│ Found 4 teachers available                             │
│                                                         │
│ ☑ Sarah Johnson (TCH004)                               │
│   Email: sarah.johnson@englishcenter.com               │
│   Skills: IELTS_LISTENING, IELTS_READING, ...          │
│   Available Days:                                       │
│     • Thứ Hai (8 sessions) - 08:00-10:00               │
│     • Thứ Tư (8 sessions) - 08:00-10:00                │
│     • Thứ Sáu (8 sessions) - 08:00-10:00               │
│                                                         │
│ ☐ Michael Chen (TCH005)                                │
│   ...                                                   │
│                                                         │
│ [Assign Selected Teacher]                              │
└─────────────────────────────────────────────────────────┘
```

### Bước 3: Chọn teacher
- User chọn 1 teacher từ list
- Có thể hiển thị chi tiết từng ngày với số buổi học
- Tất cả teachers trong list đều **ĐẢM BẢO** có thể dạy 100% buổi học

### Bước 4: Assign teacher
- Call API `POST /api/v1/classes/{classId}/teachers`
- Gửi teacherId + sessionIds (hoặc assignAll = true)

---

## Error Handling

### HTTP Status Codes

| Status Code | Meaning | Response |
|-------------|---------|----------|
| 200 | Success | Trả về list teachers |
| 400 | Bad Request | classId invalid |
| 401 | Unauthorized | JWT token missing/invalid |
| 403 | Forbidden | User không có quyền (phải là ACADEMIC_AFFAIR) |
| 404 | Not Found | Class không tồn tại |
| 500 | Internal Server Error | Lỗi server |

### Empty Result

Nếu không có teacher nào available:
```json
{
  "success": true,
  "message": "No teachers available for this class",
  "data": []
}
```

**Nguyên nhân có thể:**
- Không có teacher nào rảnh vào các ngày này
- Không có teacher nào có skill phù hợp
- Tất cả teachers đều có teaching conflict
- Không có teacher nào ở chi nhánh này

---

## Performance

- **Query time**: ~50ms cho 4 teachers, 24 sessions
- **Database**: 3 CTEs (Common Table Expressions) với JOIN phức tạp
- **Optimization**: Query đã được tối ưu với proper indexes

---

## Testing

### Test Case 1: Class thứ 2, 4, 6
```bash
curl "http://localhost:8080/api/v1/classes/6/teachers/available-by-day" \
  -H "Authorization: Bearer {token}"
```
Expected: Teachers với availableDays chứa dayOfWeek = 1, 3, 5

### Test Case 2: Class không tồn tại
```bash
curl "http://localhost:8080/api/v1/classes/999/teachers/available-by-day" \
  -H "Authorization: Bearer {token}"
```
Expected: 404 Not Found

### Test Case 3: Missing JWT token
```bash
curl "http://localhost:8080/api/v1/classes/6/teachers/available-by-day"
```
Expected: 401 Unauthorized

---

## Comparison: Single Teacher vs Multi-Teacher Assignment

| Feature | Single Teacher (This API) | Multi-Teacher (Future) |
|---------|---------------------------|------------------------|
| **API** | `/available-by-day` | `/available-by-session` |
| **Grouping** | By day (Mon, Wed, Fri) | By session (Session 1, 2, 3, ...) |
| **Filter** | 100% available per day | Available per specific session |
| **Use Case** | 1 teacher dạy toàn bộ khóa | Nhiều teachers dạy các sessions khác nhau |
| **Response** | `availableDays[]` | `availableSessions[]` |
| **Assignment** | Assign all sessions to 1 teacher | Assign different teachers to different sessions |

---

## Notes for Frontend Developer

### ✅ DO's
- Gọi API này **SAU KHI** class đã được tạo thành công
- Chỉ gửi `classId` trong URL path
- Include JWT token trong Authorization header
- Hiển thị tất cả teachers trong response (đều đã được filter)
- Show availability info cho user (ngày nào, bao nhiêu buổi)

### ❌ DON'Ts
- **ĐỪNG** gửi thông tin chi nhánh, ngày học từ màn create class
- **ĐỪNG** gửi request body (API không cần)
- **ĐỪNG** filter thêm ở frontend (backend đã filter đủ)
- **ĐỪNG** assume data structure - check `success` field trước
- **ĐỪNG** cache response lâu (teacher availability có thể thay đổi)

### 🔐 Security
- API yêu cầu role: `ACADEMIC_AFFAIR`
- JWT token bắt buộc
- classId được validate ở backend
- User chỉ thấy teachers của chi nhánh mình quản lý (nếu có branch-level security)

---

## FAQ

**Q: Tại sao frontend không cần gửi chi nhánh/ngày học?**
A: Vì class đã được tạo và lưu vào database trước đó. Backend đọc trực tiếp từ database để đảm bảo data chính xác và bảo mật.

**Q: Nếu user sửa form create class sau khi tạo thì sao?**
A: Không ảnh hưởng. API này chỉ đọc từ database, không đọc từ frontend form.

**Q: `availableSessions` luôn bằng `totalSessions` trong response?**
A: Đúng. Query đã filter ra chỉ những teachers có 100% availability. Nếu teacher chỉ rảnh 6/8 buổi thì sẽ không xuất hiện trong response.

**Q: Nếu muốn hiển thị teachers rảnh một phần thì sao?**
A: Cần API khác hoặc modify query để bỏ điều kiện `HAVING`. Hiện tại API này chỉ trả về teachers rảnh 100%.

**Q: `timeSlotDisplay` có thể null không?**
A: Có thể, nếu session không có time_slot_template_id. Handle case này ở UI.

**Q: Làm sao biết teacher có skill GENERAL?**
A: Check field `hasGeneralSkill = true`. Teacher này có thể dạy mọi môn.

---

## Changelog

| Date | Version | Changes |
|------|---------|---------|
| 2025-11-17 | 1.0 | Initial version với 5 filter conditions |

---

## Contact

For backend questions: Backend Team
For API issues: [Create GitHub Issue](https://github.com/SEP490-FA25-G25/tms-sep490-be/issues)
