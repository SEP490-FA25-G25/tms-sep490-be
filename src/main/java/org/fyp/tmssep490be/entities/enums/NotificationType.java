package org.fyp.tmssep490be.entities.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    REQUEST_APPROVAL("Yêu cầu duyệt"),
    CLASS_REMINDER("Nhắc nhở lớp học"),
    LICENSE_WARNING("Cảnh báo giấy phép"),
    FEEDBACK_REMINDER("Nhắc nhở phản hồi"),
    SYSTEM_ALERT("Cảnh báo hệ thống"),
    GRADE_NOTIFICATION("Thông báo điểm số"),
    ASSIGNMENT_DEADLINE("Hạn nộp bài tập");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }
}