package org.fyp.tmssep490be.entities.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced QA Report Status enum with robust parsing
 */
public enum QAReportStatus {
    DRAFT("draft", "Bản nháp"),
    SUBMITTED("submitted", "Đã nộp");

    private final String value;
    private final String displayName;

    private static final Map<String, QAReportStatus> VALUE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(QAReportStatus::getValue, type -> type));

    private static final Map<String, QAReportStatus> DISPLAY_NAME_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(QAReportStatus::getDisplayName, type -> type));

    QAReportStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static QAReportStatus fromString(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("QAReportStatus cannot be null or empty");
        }

        String trimmedInput = input.trim();

        // Strategy 1: Exact value match
        QAReportStatus exactValueMatch = VALUE_MAP.get(trimmedInput.toLowerCase());
        if (exactValueMatch != null) {
            return exactValueMatch;
        }

        // Strategy 2: Display name match
        QAReportStatus displayNameMatch = DISPLAY_NAME_MAP.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(trimmedInput))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (displayNameMatch != null) {
            return displayNameMatch;
        }

        // Strategy 3: Enum name match
        try {
            return QAReportStatus.valueOf(trimmedInput.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Continue to error
        }

        throw new IllegalArgumentException(String.format(
                "Invalid QAReportStatus: '%s'. Valid options are:%n" +
                "- Database values: %s%n" +
                "- Display names: %s%n" +
                "- Enum constants: %s",
                trimmedInput,
                VALUE_MAP.keySet().stream().collect(Collectors.joining(", ")),
                DISPLAY_NAME_MAP.keySet().stream().collect(Collectors.joining(", ")),
                Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "))
        ));
    }

    public static boolean isValidValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static QAReportStatus getDefaultValue() {
        return DRAFT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
