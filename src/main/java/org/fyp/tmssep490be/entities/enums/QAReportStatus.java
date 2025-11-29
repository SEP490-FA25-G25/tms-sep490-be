package org.fyp.tmssep490be.entities.enums;

public enum QAReportStatus {
    DRAFT("Bản nháp"),
    SUBMITTED("Đã nộp");

    private final String displayName;

    QAReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static QAReportStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QAReportStatus không được để trống");
        }
        for (QAReportStatus status : QAReportStatus.values()) {
            if (status.name().equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("QAReportStatus không hợp lệ: " + text);
    }

    public static QAReportStatus getDefaultValue() {
        return DRAFT;
    }
}
