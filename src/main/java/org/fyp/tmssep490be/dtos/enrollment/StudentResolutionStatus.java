package org.fyp.tmssep490be.dtos.enrollment;

public enum StudentResolutionStatus {
    FOUND, // Student đã tồn tại trong DB → sẽ enroll

    CREATE, // Student mới → sẽ tạo mới rồi enroll

    ALREADY_ENROLLED, // Student đã đăng ký vào lớp này → UI sẽ disable

    DUPLICATE, // Trùng trong file Excel (error)

    ERROR // Validation lỗi (email invalid, missing fields...)
}
