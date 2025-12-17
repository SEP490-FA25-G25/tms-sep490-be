package org.fyp.tmssep490be.entities.enums;

/**
 * Trạng thái cửa sổ đăng ký dạy lớp (real-time, không lưu DB)
 */
public enum RegistrationWindowStatus {
    NOT_OPENED, // Chưa được AA mở đăng ký (registrationOpenDate is null)
    PENDING_OPEN, // Đã được AA mở nhưng chưa đến ngày mở (NOW < registrationOpenDate)
    OPEN, // Đang trong thời gian đăng ký (registrationOpenDate <= NOW <=
          // registrationCloseDate)
    CLOSED // Đã hết hạn đăng ký (NOW > registrationCloseDate)
}
