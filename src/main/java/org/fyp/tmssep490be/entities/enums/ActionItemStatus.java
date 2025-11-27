package org.fyp.tmssep490be.entities.enums;

public enum ActionItemStatus {
    PENDING("Chờ xử lý"),
    IN_PROGRESS("Đang thực hiện"),
    COMPLETED("Đã hoàn thành"),
    CANCELLED("Đã hủy");

    private final String displayName;

    ActionItemStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}