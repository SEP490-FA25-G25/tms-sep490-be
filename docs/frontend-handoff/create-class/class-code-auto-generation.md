# Class Code Auto-Generation - Frontend Handoff

## Overview
Backend now supports **automatic class code generation** when creating a class. The `code` field is now **optional** in the create class request. If not provided, the system will automatically generate a unique code based on course, branch, and start date.

## Auto-Generated Code Format

**Pattern:** `COURSECODE-BRANCHCODE-YY-SEQ`

**Example:** `IELTSFOUND-HN01-25-005`

**Components:**
- **Course Code** (`IELTSFOUND`) - Extracted and normalized from the course code (uppercase alphanumeric only)
- **Branch Code** (`HN01`) - The branch code where the class is held
- **Year** (`25`) - Last 2 digits of the class start date year
- **Sequence** (`005`) - Auto-incremented number (001-999) unique per prefix combination

## API Changes

### Create Class API

**Endpoint:** `POST /api/v1/classes`

**Request Body Changes:**
- The `code` field is now **OPTIONAL**
- If `code` is null or empty, backend will auto-generate it
- If `code` is provided, it must follow the format pattern and be unique within the branch
- All other fields remain required: `branchId`, `courseId`, `name`, `startDate`, `modality`, `scheduleDays`, `maxCapacity`

**Important Validation Rules:**
- `startDate` MUST be a future date (not today or past)
- `scheduleDays` is an array of numbers 0-6 representing days of week (0=Sunday, 1=Monday, etc.)
- `maxCapacity` must be between 1 and 1000

**Success Response:**
The response now includes the auto-generated `code` in the data object, along with session generation summary showing how many sessions were created based on the course structure.

### Preview Class Code API

**Endpoint:** `GET /api/v1/classes/preview-code`

**Purpose:** 
Allows frontend to preview what the auto-generated class code will be BEFORE the user submits the form. This helps users understand the naming convention and verify the code looks correct.

**Query Parameters:**
- `branchId` (required) - The selected branch ID
- `courseId` (required) - The selected course ID  
- `startDate` (required) - The selected start date in YYYY-MM-DD format

**Response:**
Returns a preview of what the class code will be, including a breakdown of each component (course code, branch code, year, sequence number).

**Use Case:**
Call this endpoint whenever the user changes branch, course, or start date in the form to show them a real-time preview of the code that will be assigned.

## Frontend Implementation Recommendations

### Option 1: Pure Auto-Generation (Recommended)

**User Experience:**
- Remove the class code input field entirely from the create form
- Show a read-only preview of the auto-generated code as user fills out other fields
- Display the preview below or near the form with clear labeling (e.g., "Class Code (Auto-generated)")
- Update the preview automatically when branch, course, or start date changes
- Include a small note explaining the code will be automatically assigned

**Benefits:**
- Simpler user experience - one less field to worry about
- No validation errors related to code format or uniqueness
- Consistent naming convention across all classes

### Option 2: Auto-Generate with Manual Override

**User Experience:**
- Provide a checkbox or toggle: "Auto-generate class code"
- When checked (default): Show preview code (read-only), don't send code field in request
- When unchecked: Show text input for manual entry, validate format before submit
- If manual entry is used, the code must follow the same format pattern

**Use Case:**
For advanced users or special cases where a custom code is needed, while still encouraging auto-generation for most cases.

## Preview Behavior

**When to Fetch Preview:**
- When form loads (if branch, course, start date are pre-selected)
- When user changes branch selection
- When user changes course selection
- When user changes start date

**Debouncing:**
Recommend debouncing API calls by 500ms to avoid excessive requests while user is typing or selecting dates.

**Loading States:**
- Show loading indicator while fetching preview
- Display placeholder text like "Generating preview..." 
- Handle error states gracefully (e.g., if course has no sessions)

**Error Handling:**
If preview fails (e.g., sequence limit reached), show a clear message to the user. In this rare case, they may need to contact admin or use a different start year.

## Validation Changes

**Backend Validation (Automatic):**
- Branch ID must exist and user must have access
- Course ID must exist and be in APPROVED status
- Start date must be in the future
- Schedule days must be valid (0-6) and non-empty
- Max capacity must be between 1-1000
- If code is provided manually, it must be unique within the branch

**Frontend Validation (Recommended):**
- Validate start date is future before allowing submit
- Ensure at least one schedule day is selected
- Validate max capacity range (1-1000)
- If manual code entry is allowed, validate format pattern: uppercase letters, numbers, and hyphens only

## Common Error Scenarios

**Error: "Course must be approved before creating class"**
- Occurs when selected course is not in APPROVED approval status
- Frontend should filter course dropdown to only show approved courses

**Error: "Start date must be in the future"**
- User selected today's date or a past date
- Frontend date picker should disable past dates

**Error: "Sequence limit reached for prefix [PREFIX] (999 classes)"**
- Very rare - means 999 classes already exist for this branch/course/year combination  
- User may need to change start year or contact administrator

**Error: "Start date must fall on one of the scheduled days"**
- Start date's day of week doesn't match any selected schedule days
- Example: Start date is Monday but only Tuesday and Thursday are scheduled
- Frontend should validate this before submit or guide user to compatible dates

## Sequence Behavior

**How Sequences Work:**
- Sequences are unique per **prefix combination** (course code + branch code + year)
- First class created: gets sequence 001
- Second class created: gets sequence 002
- Sequences continue incrementing up to 999 per prefix

**Independent Sequences:**
- Different branch = new sequence starts at 001
- Different year = new sequence starts at 001  
- Same branch and year = sequence continues incrementing

**Example Scenarios:**
- `IELTSFOUND-HN01-25-001` → First IELTS Foundation class at HN01 branch in 2025
- `IELTSFOUND-HN01-25-002` → Second IELTS Foundation class at HN01 branch in 2025
- `IELTSFOUND-HCM01-25-001` → First IELTS Foundation class at HCM01 branch in 2025 (different branch)
- `IELTSFOUND-HN01-26-001` → First IELTS Foundation class at HN01 branch in 2026 (different year)

## Concurrency & Thread Safety

**Backend Implementation:**
The system uses PostgreSQL advisory locks to ensure thread-safe sequence generation. This means:
- Multiple users can create classes simultaneously
- Each will receive a unique sequence number
- No risk of duplicate codes
- No race conditions

**What This Means for Frontend:**
You don't need to worry about handling concurrent requests. The backend guarantees uniqueness even under high load.

## Migration Guide for Existing Frontend

**If you already have a create class form:**

1. **Make code field optional** - Change field definition from required to optional/nullable
2. **Remove code from submit payload** - Don't send the code field in POST request (recommended) OR send it as null/empty
3. **Add preview display** - Call preview endpoint and show result to users (optional but recommended)
4. **Update validation** - Remove any frontend validation that enforces code is required
5. **Update error handling** - Add handling for new error codes related to auto-generation

**Backward Compatibility:**
The API still accepts manual codes if provided, so existing implementations will continue to work. However, we recommend migrating to auto-generation for consistency.

## Testing Checklist

**Preview Functionality:**
- [ ] Preview displays when all required fields (branch, course, start date) are filled
- [ ] Preview updates correctly when branch changes
- [ ] Preview updates correctly when course changes  
- [ ] Preview updates correctly when start date changes
- [ ] Loading state shows during preview fetch
- [ ] Error state handled gracefully if preview fails

**Create Class Functionality:**
- [ ] Class can be created without providing code field
- [ ] Auto-generated code appears in success response
- [ ] Auto-generated code matches the preview shown before submit
- [ ] Session count in response matches course session count
- [ ] New class has DRAFT status and PENDING approval status

**Validation Testing:**
- [ ] Error shown if start date is today or in past
- [ ] Error shown if course is not approved
- [ ] Error shown if schedule days array is empty
- [ ] Error shown if max capacity is below 1 or above 1000
- [ ] Error shown if start date doesn't match schedule days

**Sequence Testing:**
- [ ] Creating multiple classes increments sequence correctly (001, 002, 003...)
- [ ] Different branches start at 001 independently
- [ ] Different years start at 001 independently

## Technical Notes

**Date Format:**
- All dates should be in `YYYY-MM-DD` format (ISO 8601)
- Example: `2025-12-01` for December 1, 2025

**Schedule Days Format:**
- Array of integers 0-6
- 0 = Sunday, 1 = Monday, 2 = Tuesday, 3 = Wednesday, 4 = Thursday, 5 = Friday, 6 = Saturday
- Example: `[2, 4, 6]` means Tuesday, Thursday, Saturday

**Modality Values:**
- `ONLINE` - Virtual classes only
- `OFFLINE` - In-person classes only  
- `HYBRID` - Mix of online and offline sessions

## Support & Resources

**For Questions:**
Contact the backend team for clarification on implementation details or edge cases.

**API Documentation:**
Full API specs available at: `http://localhost:8080/swagger-ui.html`

**Related Documentation:**
- Create Class Workflow: `/docs/create-class/create-class-workflow-final.md`
- Implementation Checklist: `/docs/create-class/create-class-implementation-checklist-v2.md`

