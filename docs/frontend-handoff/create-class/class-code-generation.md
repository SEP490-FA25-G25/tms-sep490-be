# Class Code Generation - Frontend Handoff

## Tổng quan

Hệ thống tự động tạo mã lớp học theo pattern: `COURSECODE-BRANCHCODE-YY-SEQ`

### Pattern Format

- **COURSECODE**: Tên khóa học chuẩn hóa (VD: "IELTSFOUND" từ "IELTS-FOUND-2025-V1")
- **BRANCHCODE**: Mã chi nhánh (VD: "HN01", "HCM01")
- **YY**: 2 số cuối năm bắt đầu (VD: "25" cho 2025)
- **SEQ**: Số thứ tự 3 chữ số (001-999)

**Ví dụ**: `IELTSFOUND-HN01-25-005`

---

## API Endpoints

### 1. Preview Class Code (Optional - STEP 0)

**Endpoint**: `GET /api/v1/classes/preview-code`  
**Role**: `ACADEMIC_AFFAIR`  
**Mục đích**: Xem trước mã lớp sẽ được tạo (không lưu vào database)

#### Query Parameters

- `branchId` (required): ID chi nhánh - Long
- `courseId` (required): ID khóa học - Long
- `startDate` (required): Ngày bắt đầu - Date (format: YYYY-MM-DD)

#### Example Request

```
GET /api/v1/classes/preview-code?branchId=1&courseId=5&startDate=2025-01-15
Authorization: Bearer <access_token>
```

#### Frontend Code Example

```typescript
// Axios
const response = await axios.get("/api/v1/classes/preview-code", {
  params: {
    branchId: 1,
    courseId: 5,
    startDate: "2025-01-15",
  },
  headers: {
    Authorization: `Bearer ${accessToken}`,
  },
});

// Fetch API
const params = new URLSearchParams({
  branchId: "1",
  courseId: "5",
  startDate: "2025-01-15",
});
const response = await fetch(`/api/v1/classes/preview-code?${params}`, {
  headers: {
    Authorization: `Bearer ${accessToken}`,
  },
});
```

#### Response Success

```json
{
  "success": true,
  "message": "Class code preview generated successfully",
  "data": {
    "previewCode": "IELTSFOUND-HN01-25-005",
    "prefix": "IELTSFOUND-HN01-25",
    "nextSequence": 5,
    "warning": null
  }
}
```

#### Response với Warning (seq >= 990)

```json
{
  "success": true,
  "message": "Class code preview generated with warning",
  "data": {
    "previewCode": "IELTSFOUND-HN01-25-995",
    "prefix": "IELTSFOUND-HN01-25",
    "nextSequence": 995,
    "warning": "Approaching sequence limit (995/999). Consider using different prefix."
  }
}
```

#### Response khi đạt giới hạn (seq = 999)

```json
{
  "success": false,
  "message": "Sequence limit reached",
  "data": {
    "previewCode": "IELTSFOUND-HN01-25-999",
    "prefix": "IELTSFOUND-HN01-25",
    "nextSequence": 999,
    "warning": "Sequence limit reached (999). Cannot create more classes with this prefix."
  }
}
```

#### Use Cases

- Hiển thị trước mã lớp sẽ được tạo
- Cảnh báo sớm nếu sắp hết số thứ tự
- **Lưu ý**: Mã preview có thể thay đổi nếu có lớp khác được tạo trong lúc đó

---

### 2. Create Class - Auto-generate (STEP 1)

**Endpoint**: `POST /api/v1/classes`  
**Role**: `ACADEMIC_AFFAIR`  
**Thay đổi**: Field `code` giờ **OPTIONAL**

#### Request Body - Không cần code (Auto-generate)

```json
{
  "branchId": 1,
  "courseId": 5,
  "name": "IELTS Foundation Morning Class",
  "startDate": "2025-01-15",
  "endDate": "2025-04-15",
  "capacity": 20,
  "modality": "OFFLINE"
  // "code": null hoặc không gửi → tự động tạo
}
```

#### Request Body - Với code thủ công

```json
{
  "code": "CUSTOM-CODE-123", // Vẫn validate format nếu có
  "branchId": 1,
  "courseId": 5,
  "name": "IELTS Foundation Morning Class",
  "startDate": "2025-01-15",
  "endDate": "2025-04-15",
  "capacity": 20,
  "modality": "OFFLINE"
}
```

#### Response Success

```json
{
  "success": true,
  "message": "Class IELTSFOUND-HN01-25-005 created successfully with 36 sessions generated",
  "data": {
    "code": "IELTSFOUND-HN01-25-005", // Mã được tạo tự động
    "id": 123,
    "name": "IELTS Foundation Morning Class",
    "sessionSummary": {
      "sessionsGenerated": 36,
      "totalSessions": 36
    }
    // ... other response fields
  }
}
```

---

## UI/UX Recommendations

### 1. Trước khi tạo lớp (Optional)

```
┌─────────────────────────────────────┐
│ Preview Class Code                   │
├─────────────────────────────────────┤
│ Branch:    [Hà Nội 01 ▼]            │
│ Course:    [IELTS Foundation ▼]     │
│ Start:     [15/01/2025]              │
│                                      │
│ [Preview Code]                       │
│                                      │
│ Next Code: IELTSFOUND-HN01-25-005   │
│ ⚠️ Warning: 5 codes remaining        │ (nếu seq >= 990)
└─────────────────────────────────────┘
```

### 2. Form Create Class

```
┌─────────────────────────────────────┐
│ Create New Class                     │
├─────────────────────────────────────┤
│ Class Code: [________________]       │
│ 💡 Leave empty for auto-generation  │
│    (e.g., IELTSFOUND-HN01-25-XXX)   │
│                                      │
│ Branch:    [Hà Nội 01 ▼]            │
│ Course:    [IELTS Foundation ▼]     │
│ Name:      [________________]        │
│ Start:     [15/01/2025]              │
│ ...                                  │
│                                      │
│ [Create Class]                       │
└─────────────────────────────────────┘
```

### 3. Sau khi tạo thành công

```
┌─────────────────────────────────────┐
│ ✅ Class Created Successfully        │
├─────────────────────────────────────┤
│ Class Code: IELTSFOUND-HN01-25-005  │
│ Auto-generated from course & branch │
│                                      │
│ 36 sessions generated automatically │
│                                      │
│ [Continue to STEP 3: Time Slots]    │
└─────────────────────────────────────┘
```

### 4. Warning States

#### Approaching Limit (990-998)

```
⚠️ Warning: Only X codes remaining for this prefix
Consider creating classes in a different year or branch
```

#### Limit Reached (999)

```
❌ Error: Cannot create more classes
Sequence limit reached (999/999) for prefix IELTSFOUND-HN01-25
Please contact administrator or use different course/year
```

---

## Error Handling

### Error Codes

| Code | Constant                          | Mô tả                     |
| ---- | --------------------------------- | ------------------------- |
| 4032 | CLASS_CODE_GENERATION_FAILED      | Lỗi khi tạo mã            |
| 4033 | CLASS_CODE_SEQUENCE_LIMIT_REACHED | Đã dùng hết 999 số thứ tự |
| 4034 | CLASS_CODE_INVALID_FORMAT         | Format mã không hợp lệ    |
| 4035 | CLASS_CODE_PARSE_ERROR            | Lỗi parse mã hiện tại     |

### Xử lý Error 4033 (Sequence Limit)

```json
{
  "success": false,
  "message": "Cannot create more classes: sequence limit reached for prefix IELTSFOUND-HN01-25 (999/999)",
  "data": null
}
```

**Action**:

- Hiển thị modal/alert thông báo lỗi
- Suggest: "Try different start year or contact admin"
- Disable nút "Create" cho đến khi user thay đổi năm/khóa học

---

## Field Validation

### Class Code Field (Optional)

- **Required**: ❌ No (có thể để trống)
- **Pattern**: `^[A-Z0-9]+-[A-Z0-9]+-\d{2}-\d{3}$` (nếu user nhập)
- **Min Length**: 10
- **Max Length**: 50
- **Example**: `IELTSFOUND-HN01-25-005`

### Frontend Validation

```typescript
// Nếu user nhập code thủ công
if (classCode && classCode.trim() !== "") {
  // Validate format
  const codePattern = /^[A-Z0-9]+-[A-Z0-9]+-\d{2}-\d{3}$/;
  if (!codePattern.test(classCode)) {
    return "Invalid class code format";
  }
}
// Nếu để trống → backend tự tạo
```

---

## Thread Safety & Concurrency

### Backend Guarantees

- ✅ Không trùng mã dù tạo đồng thời (PostgreSQL advisory locks)
- ✅ Sequence tự động tăng an toàn
- ✅ Preview code có thể thay đổi nếu ai đó tạo lớp trước

### Frontend Không Cần

- ❌ Không cần handle concurrency logic
- ❌ Không cần lock UI khi preview
- ❌ Không cần retry logic

---

## Testing Scenarios

### Test Case 1: Auto-generate thành công

1. Để trống field "Class Code"
2. Chọn branch, course, start date
3. Click "Create"
4. **Expected**: Class được tạo với mã auto-generated

### Test Case 2: Manual code thành công

1. Nhập code: "CUSTOM-HN01-25-001"
2. Chọn các field khác
3. Click "Create"
4. **Expected**: Class được tạo với mã đã nhập

### Test Case 3: Preview warning

1. Chọn branch/course có seq >= 990
2. Click "Preview Code"
3. **Expected**: Hiển thị warning màu cam

### Test Case 4: Sequence limit

1. Chọn branch/course đã có 999 classes
2. Click "Create"
3. **Expected**: Error 4033, disable form

### Test Case 5: Invalid format

1. Nhập code: "invalid-format"
2. Click "Create"
3. **Expected**: Validation error trước khi gọi API

---

## API Documentation

**Swagger UI**: `http://localhost:8080/swagger-ui.html`  
**OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

Tìm endpoint:

- `POST /api/v1/classes/preview-code`
- `POST /api/v1/classes` (với note về optional code field)

---

## Migration Notes

### Breaking Changes

- ❌ Không có breaking changes
- Field `code` vẫn accept manual input (backward compatible)

### Recommended Changes

1. Update form placeholder: "Leave empty for auto-generation"
2. Add "Preview Code" button (optional feature)
3. Add warning toast khi seq >= 990
4. Handle error 4033 gracefully

---

## Support & Contact

- **Backend Dev**: [Your Name]
- **API Issues**: Check Swagger UI first
- **Error 4033**: Contact admin to reset sequence or create new course
