package org.fyp.tmssep490be.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Branch errors (2000-2099)
    BRANCH_NOT_FOUND(2000, "Không tìm thấy chi nhánh"),
    BRANCH_ACCESS_DENIED(2004, "Từ chối truy cập: Chi nhánh không thuộc quyền quản lý của bạn"),

    // TimeSlot errors (2100-2199)

    // Resource errors (2200-2299)

    // Subject errors (1200-1219)

    // Level errors (1220-1239)
    LEVEL_NOT_FOUND(1221, "Không tìm thấy cấp độ"),

    // Course errors (1240-1269)

    // CoursePhase errors (1270-1289)

    // CourseSession errors (1290-1309)

    // PLO errors (1310-1329)

    // CLO errors (1330-1349)

    // PLO-CLO Mapping errors (1350-1369)

    // Course Material errors (1370-1389)

    // User errors (1000-1099)
    USER_NOT_FOUND(1000, "Không tìm thấy người dùng"),
    USER_PHONE_ALREADY_EXISTS(1002, "Số điện thoại đã tồn tại"),

    // Student errors (1100-1199)
    EMAIL_ALREADY_EXISTS(1103, "Email đã tồn tại"),
    STUDENT_ROLE_NOT_FOUND(1105, "Vai trò HỌC VIÊN chưa được cấu hình trong hệ thống"),
    STUDENT_NOT_FOUND(1100, "Không tìm thấy học viên"),
    STUDENT_SESSION_NOT_FOUND(1106, "Không tìm thấy buổi học của học viên hoặc học viên chưa đăng ký buổi học này"),
    STUDENT_NOT_ENROLLED_IN_CLASS(1107, "Học viên chưa đăng ký lớp này"),
    STUDENT_ACCESS_DENIED(1108, "Từ chối truy cập: Học viên không thuộc quyền quản lý của bạn"),
    STUDENT_ALREADY_IN_BRANCH(1109, "Học viên đã thuộc chi nhánh này"),
    INVALID_EXISTENCE_CHECK_TYPE(1110, "Loại kiểm tra không hợp lệ, chỉ chấp nhận EMAIL hoặc PHONE"),

    // Enrollment errors (1200-1299)
    EXCEL_GENERATION_FAILED(1216, "Tạo tệp Excel thất bại"),
    EXCEL_FILE_EMPTY(1205, "Tệp Excel trống hoặc định dạng không hợp lệ"),
    INVALID_FILE_TYPE_XLSX(1214, "Chỉ hỗ trợ tệp Excel (.xlsx)"),
    EXCEL_PARSE_FAILED(1206, "Phân tích tệp Excel thất bại"),
    NO_STUDENTS_TO_IMPORT(1217, "Không có học viên nào để nhập"),
    CLASS_NOT_APPROVED(1207, "Lớp học phải được phê duyệt trước khi đăng ký"),
    CLASS_INVALID_STATUS(1208, "Lớp học phải ở trạng thái 'đã lên lịch' hoặc 'đang diễn ra' để đăng ký"),
    NO_FUTURE_SESSIONS(1209, "Không còn buổi học nào trong tương lai để đăng ký"),

    // Enrollment Import errors (1205-1219)
    CLASS_CAPACITY_EXCEEDED(1202, "Lớp học đã đủ sĩ số"),
    OVERRIDE_REASON_REQUIRED(1210, "Yêu cầu lý do ghi đè (tối thiểu 20 ký tự)"),
    OVERRIDE_REASON_TOO_SHORT(1215, "Lý do ghi đè phải có ít nhất 20 ký tự"),
    ENROLLMENT_ALREADY_EXISTS(1201, "Học viên đã đăng ký vào lớp này"),
    INVALID_ENROLLMENT_STRATEGY(1211, "Chiến lược đăng ký không hợp lệ"),
    SELECTED_STUDENTS_EXCEED_CAPACITY(1213, "Số lượng học viên đã chọn vượt quá sức chứa"),
    PARTIAL_STRATEGY_MISSING_IDS(1212, "Cần chọn danh sách ID học viên cho chiến lược PARTIAL"),

    // Class errors (4000-4099)
    CLASS_NOT_FOUND(4000, "Không tìm thấy lớp học"),
    CLASS_NO_BRANCH_ACCESS(4004, "Người dùng không có quyền truy cập vào bất kỳ chi nhánh nào"),

    // Create Class Workflow errors (4010-4099)
    INVALID_SCHEDULE_DAYS(4010, "Lịch học không hợp lệ"),
    START_DATE_NOT_IN_SCHEDULE_DAYS(4011, "Ngày bắt đầu không khớp với lịch học"),
    CLASS_CODE_DUPLICATE(4012, "Mã lớp đã tồn tại trong chi nhánh"),
    SUBJECT_NOT_APPROVED(4013, "Môn học chưa được phê duyệt"),
    SUBJECT_NOT_FOUND(4014, "Không tìm thấy môn học"),

    // Class Code Generation errors (4032-4039)
    CLASS_CODE_GENERATION_FAILED(4032, "Tạo mã lớp thất bại"),
    CLASS_CODE_INVALID_FORMAT(4033, "Định dạng mã lớp không hợp lệ"),
    CLASS_CODE_SEQUENCE_LIMIT_REACHED(4034, "Đã đạt giới hạn số thứ tự mã lớp"),
    CLASS_CODE_PARSE_ERROR(4035, "Lỗi phân tích mã lớp"),

    // Teacher errors (3000-3099)
    TEACHER_NOT_FOUND(3000, "Không tìm thấy giáo viên"),
    TEACHER_ACCESS_DENIED(3001, "Từ chối truy cập: Giáo viên không thuộc quyền quản lý của bạn"),

    // Teacher Request errors (5000-5099)
    TEACHER_REQUEST_NOT_FOUND(5000, "Không tìm thấy yêu cầu giáo viên"),
    TEACHER_REQUEST_NOT_PENDING(5001, "Yêu cầu không ở trạng thái chờ duyệt"),
    RESOURCE_NOT_FOUND(5002, "Không tìm thấy tài nguyên"),
    TIMESLOT_NOT_FOUND(5003, "Không tìm thấy khung giờ"),
    ASSESSMENT_NOT_FOUND(5004, "Không tìm thấy bài kiểm tra"),

    // Student Request errors (4100-4199)

    // Make-up Request specific errors (4112-4119)

    // Attendance errors (1300-1399)
    ATTENDANCE_RECORDS_EMPTY(1300, "Danh sách điểm danh trống"),
    SESSION_ALREADY_DONE(1301, "Buổi học đã khóa, không thể sửa"),
    HOMEWORK_STATUS_INVALID_NO_PREVIOUS_HOMEWORK(1302, "Buổi trước không có bài, chỉ được chọn NO_HOMEWORK"),
    HOMEWORK_STATUS_INVALID_HAS_PREVIOUS_HOMEWORK(1303, "Buổi trước có bài tập, không được chọn NO_HOMEWORK"),

    // Common errors (9000-9999)
    INVALID_INPUT(9000, "Dữ liệu đầu vào không hợp lệ"),
    UNAUTHORIZED(9401, "Truy cập trái phép"),
    FORBIDDEN(9403, "Không có quyền truy cập"),
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
