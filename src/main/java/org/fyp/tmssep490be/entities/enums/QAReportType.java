package org.fyp.tmssep490be.entities.enums;

public enum QAReportType {
    CLASSROOM_OBSERVATION("Quan sát lớp học"),
    PHASE_REVIEW("Đánh giá giai đoạn"),
    CLO_ACHIEVEMENT_ANALYSIS("Phân tích mức độ đạt CLO"),
    STUDENT_FEEDBACK_ANALYSIS("Phân tích phản hồi học viên"),
    ATTENDANCE_ENGAGEMENT_REVIEW("Đánh giá chuyên cần và tham gia"),
    TEACHING_QUALITY_ASSESSMENT("Đánh giá chất lượng giảng dạy");

    private final String displayName;

    QAReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static QAReportType fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("QAReportType không được để trống");
        }
        for (QAReportType type : QAReportType.values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("QAReportType không hợp lệ: " + text);
    }

    public static QAReportType getDefaultValue() {
        return CLASSROOM_OBSERVATION;
    }
}
