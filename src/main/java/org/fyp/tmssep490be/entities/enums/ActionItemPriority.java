package org.fyp.tmssep490be.entities.enums;

public enum ActionItemPriority {
    LOW("Thấp"),
    MEDIUM("Trung bình"),
    HIGH("Cao"),
    URGENT("Khẩn cấp");

    private final String displayName;

    ActionItemPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}