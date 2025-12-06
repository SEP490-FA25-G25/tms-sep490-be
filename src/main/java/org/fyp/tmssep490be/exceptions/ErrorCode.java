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

    // Enrollment errors (1200-1299)

    // Enrollment Import errors (1205-1219)

    // Class errors (4000-4099)
    CLASS_NOT_FOUND(4000, "Không tìm thấy lớp học"),

    // Create Class Workflow errors (4010-4099)

    // Class Code Generation errors (4032-4039)

    // Teacher errors (3000-3099)

    // Teacher Request errors (5000-5099)

    // Student Request errors (4100-4199)

    // Make-up Request specific errors (4112-4119)

    // Attendance errors (1300-1399)

    // Common errors (9000-9999)
    ;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
