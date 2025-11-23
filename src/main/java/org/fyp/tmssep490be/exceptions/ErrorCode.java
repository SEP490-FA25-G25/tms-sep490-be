package org.fyp.tmssep490be.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Branch errors (2000-2099)
    BRANCH_NOT_FOUND(2000, "Branch not found"),
    BRANCH_CODE_ALREADY_EXISTS(2001, "Branch code already exists for this center"),
    CENTER_NOT_FOUND(2002, "Center not found"),
    BRANCH_HAS_ACTIVE_CLASSES(2003, "Cannot delete branch with active classes"),
    BRANCH_ACCESS_DENIED(2004, "Access denied: Branch not in your assigned branches"),

    // TimeSlot errors (2100-2199)
    TIMESLOT_NOT_FOUND(2100, "Time slot not found"),
    TIMESLOT_INVALID_TIME_RANGE(2101, "Start time must be before end time"),
    TIMESLOT_DURATION_MISMATCH(2102, "Calculated duration does not match provided duration"),
    TIMESLOT_OVERLAP(2103, "Time slot overlaps with existing time slot"),

    // Resource errors (2200-2299)
    RESOURCE_NOT_FOUND(2200, "Resource not found"),
    RESOURCE_NAME_ALREADY_EXISTS(2201, "Resource name already exists for this branch"),
    RESOURCE_CONFLICT(2202, "Resource is already booked for the specified time"),
    RESOURCE_INVALID_TYPE(2203, "Invalid resource type"),
    RESOURCE_CAPACITY_INSUFFICIENT(2204, "Resource capacity is insufficient"),

    // Subject errors (1200-1219)
    SUBJECT_NOT_FOUND(1201, "Subject not found"),
    SUBJECT_CODE_DUPLICATE(1202, "Subject code already exists"),
    SUBJECT_CODE_INVALID(1203, "Subject code format invalid (must be uppercase alphanumeric with hyphens)"),
    SUBJECT_HAS_LEVELS(1204, "Cannot delete subject with existing levels"),
    SUBJECT_HAS_COURSES(1205, "Cannot delete subject with existing courses"),

    // Level errors (1220-1239)
    LEVEL_NOT_FOUND(1221, "Level not found"),
    LEVEL_CODE_DUPLICATE(1222, "Level code already exists for this subject"),
    LEVEL_HAS_COURSES(1223, "Cannot delete level with existing courses"),
    LEVEL_INVALID_SUBJECT(1224, "Invalid subject ID"),
    LEVEL_SORT_ORDER_DUPLICATE(1225, "Sort order already exists for this subject"),

    // Course errors (1240-1269)
    COURSE_NOT_FOUND(1240, "Course not found"),
    COURSE_ALREADY_EXISTS(1241, "Course already exists with this subject, level, and version"),
    COURSE_CODE_DUPLICATE(1242, "Course code already exists"),
    COURSE_CANNOT_BE_UPDATED(1243, "Course cannot be updated (must be in draft or rejected status)"),
    COURSE_CANNOT_BE_MODIFIED(1244, "Course cannot be modified (not in draft status)"),
    COURSE_IN_USE(1245, "Cannot delete course that is being used by classes"),
    COURSE_ALREADY_SUBMITTED(1246, "Course has already been submitted for approval"),
    COURSE_NOT_SUBMITTED(1247, "Course has not been submitted for approval"),
    COURSE_NO_PHASES(1248, "Course must have at least one phase before submission"),
    INVALID_ACTION(1249, "Invalid approval action (must be 'approve' or 'reject')"),
    REJECTION_REASON_REQUIRED(1250, "Rejection reason is required when rejecting a course"),
    INVALID_TOTAL_HOURS(1251, "Total hours calculation is inconsistent with duration, sessions per week, and hours per session"),

    // CoursePhase errors (1270-1289)
    PHASE_NOT_FOUND(1270, "Course phase not found"),
    PHASE_NUMBER_DUPLICATE(1271, "Phase number already exists for this course"),
    PHASE_HAS_SESSIONS(1272, "Cannot delete phase that has course sessions"),

    // CourseSession errors (1290-1309)
    SESSION_NOT_FOUND(1290, "Course session not found"),
    SESSION_SEQUENCE_DUPLICATE(1291, "Session sequence number already exists for this phase"),
    SESSION_IN_USE(1292, "Cannot delete course session that is being used in actual sessions"),
    INVALID_SKILL_SET(1293, "Invalid skill set value(s)"),
    COURSE_SESSION_NOT_FOUND(1294, "Course session not found"),
    SESSION_ALREADY_DONE(1295, "Cannot modify attendance for a session that has already been completed"),

    // PLO errors (1310-1329)
    PLO_NOT_FOUND(1310, "PLO not found"),
    PLO_CODE_DUPLICATE(1311, "PLO code already exists for this subject"),
    PLO_HAS_MAPPINGS(1312, "Cannot delete PLO with existing CLO mappings"),

    // CLO errors (1330-1349)
    CLO_NOT_FOUND(1330, "CLO not found"),
    CLO_CODE_DUPLICATE(1331, "CLO code already exists for this course"),
    CLO_HAS_MAPPINGS(1332, "Cannot delete CLO with existing mappings"),

    // PLO-CLO Mapping errors (1350-1369)
    PLO_CLO_SUBJECT_MISMATCH(1350, "PLO and CLO must belong to the same subject"),
    PLO_CLO_MAPPING_ALREADY_EXISTS(1351, "This PLO-CLO mapping already exists"),
    CLO_SESSION_COURSE_MISMATCH(1352, "CLO and CourseSession must belong to the same course"),
    CLO_SESSION_MAPPING_ALREADY_EXISTS(1353, "This CLO-Session mapping already exists"),

    // Course Material errors (1370-1389)
    COURSE_MATERIAL_NOT_FOUND(1370, "Course material not found"),
    MATERIAL_MUST_HAVE_CONTEXT(1371, "Material must be associated with course, phase, or session"),
    INVALID_FILE_TYPE(1372, "File type not allowed"),
    FILE_TOO_LARGE(1373, "File size exceeds maximum limit"),
    FILE_UPLOAD_FAILED(1374, "Failed to upload file"),

    // User errors (1000-1099)
    USER_NOT_FOUND(1000, "User not found"),
    USER_EMAIL_ALREADY_EXISTS(1001, "Email already exists"),
    USER_PHONE_ALREADY_EXISTS(1002, "Phone number already exists"),
    USER_ALREADY_EXISTS(1003, "User already exists"),
    ROLE_NOT_FOUND(1004, "Role not found"),
    INVALID_PASSWORD(1005, "Invalid password"),
    PASSWORD_MISMATCH(1006, "Old password does not match"),

    // Student errors (1100-1199)
    STUDENT_NOT_FOUND(1100, "Student not found"),
    STUDENT_CODE_ALREADY_EXISTS(1101, "Student code already exists"),
    STUDENT_ACCESS_DENIED(1102, "Access denied: Student not in your assigned branches"),
    EMAIL_ALREADY_EXISTS(1103, "Email already exists"),
    STUDENT_ROLE_NOT_FOUND(1105, "STUDENT role not configured in system"),

    // Enrollment errors (1200-1299)
    ENROLLMENT_NOT_FOUND(1200, "Enrollment not found"),
    ENROLLMENT_ALREADY_EXISTS(1201, "Student is already enrolled in this class"),
    CLASS_CAPACITY_EXCEEDED(1202, "Class capacity exceeded"),
    CLASS_NOT_AVAILABLE(1203, "Class is not available for enrollment"),
    CANNOT_UNENROLL_COMPLETED_CLASS(1204, "Cannot remove student from completed class"),

    // Enrollment Import errors (1205-1219)
    EXCEL_FILE_EMPTY(1205, "Excel file is empty or invalid format"),
    EXCEL_PARSE_FAILED(1206, "Failed to parse Excel file"),
    CLASS_NOT_APPROVED(1207, "Class must be approved before enrollment"),
    CLASS_INVALID_STATUS(1208, "Class must be in 'scheduled' status for enrollment"),
    NO_FUTURE_SESSIONS(1209, "No future sessions available for enrollment"),
    OVERRIDE_REASON_REQUIRED(1210, "Override reason required (min 20 characters)"),
    OVERRIDE_REASON_TOO_SHORT(1215, "Override reason must be at least 20 characters"),
    INVALID_ENROLLMENT_STRATEGY(1211, "Invalid enrollment strategy"),
    PARTIAL_STRATEGY_MISSING_IDS(1212, "Selected student IDs required for PARTIAL strategy"),
    SELECTED_STUDENTS_EXCEED_CAPACITY(1213, "Selected students still exceed capacity"),
    INVALID_FILE_TYPE_XLSX(1214, "Only Excel files (.xlsx) are supported"),

    // Class errors (4000-4099)
    CLASS_NOT_FOUND(4000, "Class not found"),
    CLASS_ACCESS_DENIED(4001, "Access denied: Class not in your assigned branches"),
    CLASS_NOT_APPROVED_FOR_ENROLLMENT(4002, "Class must be approved to be accessible"),
    CLASS_NOT_SCHEDULED(4003, "Class must be in scheduled status to be accessible"),
    CLASS_NO_BRANCH_ACCESS(4004, "User does not have access to any branches"),
    UNAUTHORIZED_ACCESS(4005, "Unauthorized access: Academic staff role required"),

    // Teacher errors (3000-3099)
    TEACHER_NOT_FOUND(3000, "Teacher not found"),
    TEACHER_EMPLOYEE_CODE_ALREADY_EXISTS(3001, "Teacher employee code already exists"),
    TEACHER_SKILL_NOT_FOUND(3002, "Teacher skill not found"),
    TEACHER_AVAILABILITY_NOT_FOUND(3003, "Teacher availability not found"),
    TEACHER_AVAILABILITY_CONFLICT(3004, "Teacher availability conflicts with existing schedule"),
    TEACHER_ALREADY_ASSIGNED_TO_BRANCH(3005, "Teacher is already assigned to this branch"),
    TEACHER_NOT_ASSIGNED_TO_BRANCH(3006, "Teacher is not assigned to this branch"),
    TEACHER_SCHEDULE_NOT_FOUND(3007, "Teacher schedule not found"),
    TEACHER_WORKLOAD_EXCEEDED(3008, "Teacher workload exceeds maximum capacity"),

    // Teacher Request errors (5000-5099)
    TEACHER_REQUEST_NOT_FOUND(5000, "Teacher request not found"),
    TEACHER_DOES_NOT_OWN_SESSION(5001, "Teacher does not own this session"),
    TEACHER_REQUEST_NOT_PENDING(5002, "Request is not in pending status"),
    TEACHER_REQUEST_DUPLICATE(5003, "Duplicate teacher request already exists for this session"),
    SESSION_NOT_IN_TIME_WINDOW(5004, "Session must be within 7 days from today"),
    RESOURCE_NOT_AVAILABLE(5005, "Resource is not available at the specified time"),
    INVALID_RESOURCE_FOR_MODALITY(5006, "Invalid resource for modality change"),
    TEACHER_REQUEST_NOT_WAITING_CONFIRM(5007, "Request is not in waiting_confirm status"),
    REPLACEMENT_TEACHER_CONFLICT(5008, "Replacement teacher has a schedule conflict"),

    // Student Request errors (4100-4199)
    STUDENT_REQUEST_NOT_FOUND(4100, "Student request not found"),
    STUDENT_NOT_ENROLLED_IN_CLASS(4101, "Student is not enrolled in this class"),
    SESSION_NOT_PLANNED(4102, "Session is not in planned status"),
    SESSION_ALREADY_OCCURRED(4103, "Cannot request absence for past session"),
    ABSENCE_REQUEST_LEAD_TIME_NOT_MET(4104, "Absence request must be submitted at least {0} days before session"),
    DUPLICATE_ABSENCE_REQUEST(4105, "You already have a pending absence request for this session"),
    STUDENT_SESSION_NOT_FOUND(4106, "Student session record not found"),
    REQUEST_NOT_PENDING(4107, "Request is not in pending status"),
    REQUEST_TYPE_MISMATCH(4108, "Request type mismatch"),
    STUDENT_ABSENCE_QUOTA_EXCEEDED(4109, "Student has reached the maximum absence quota for this class"),
    CANNOT_MODIFY_APPROVED_REQUEST(4110, "Cannot modify an approved request"),
    CANNOT_CANCEL_APPROVED_REQUEST(4111, "Cannot cancel an approved request"),

    // Make-up Request specific errors (4112-4119)
    MAKEUP_COURSE_SESSION_MISMATCH(4112, "Target session and makeup session must have the same course content"),
    MAKEUP_SESSION_CAPACITY_FULL(4113, "The makeup session has reached maximum capacity"),
    SCHEDULE_CONFLICT(4114, "Student has another session scheduled at the same time"),
    MAKEUP_QUOTA_EXCEEDED(4115, "Student has exceeded the maximum number of makeup sessions allowed"),
    STUDENT_ALREADY_ENROLLED_IN_MAKEUP(4116, "Student is already enrolled in the makeup session"),
    MAKEUP_SESSION_NOW_FULL(4117, "Makeup session capacity filled between submission and approval"),
    NO_AVAILABLE_MAKEUP_SESSIONS(4118, "No available makeup sessions found for this content"),
    INVALID_ATTENDANCE_STATUS_FOR_MAKEUP(4119, "Student must be absent or have planned status to request makeup"),

    // Attendance errors (1300-1399)
    ATTENDANCE_RECORDS_EMPTY(1300, "Attendance records must not be empty"),
    HOMEWORK_STATUS_INVALID_NO_PREVIOUS_HOMEWORK(1301, "Cannot set homework status because previous session has no homework assignment"),
    HOMEWORK_STATUS_INVALID_HAS_PREVIOUS_HOMEWORK(1302, "Cannot set homework status to NO_HOMEWORK because previous session has homework assignment"),

    // Common errors (9000-9999)
    INVALID_INPUT(9000, "Invalid input provided"),
    INVALID_REQUEST(9001, "Invalid request"),
    INVALID_STATUS(9002, "Invalid status value"),
    UNAUTHORIZED(9401, "Unauthorized access"),
    FORBIDDEN(9403, "Access forbidden"),
    INTERNAL_SERVER_ERROR(9500, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
