package org.fyp.tmssep490be.entities.enums;

import lombok.Getter;

@Getter
public enum NotificationPriority {
    LOW("Thấp"),
    MEDIUM("Trung bình"),
    HIGH("Cao"),
    URGENT("Khẩn cấp");

    private final String displayName;

    NotificationPriority(String displayName) {
        this.displayName = displayName;
    }
}