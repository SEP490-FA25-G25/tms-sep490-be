package org.fyp.tmssep490be.entities.enums;

import lombok.Getter;

@Getter
public enum NotificationStatus {
    UNREAD("Chưa đọc"),
    READ("Đã đọc"),
    ARCHIVED("Đã lưu trữ");

    private final String displayName;

    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }
}