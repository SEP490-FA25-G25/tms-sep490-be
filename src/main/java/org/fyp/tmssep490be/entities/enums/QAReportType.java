package org.fyp.tmssep490be.entities.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced QA Report Type enum with robust parsing and display support
 *
 * Storage format: lowercase_with_underscores (database/API)
 * Display format: Title Case (UI)
 * Enum format: UPPERCASE_WITH_UNDERSCORES (Java constant)
 */
public enum QAReportType {
    CLASSROOM_OBSERVATION("classroom_observation", "Classroom Observation"),
    PHASE_REVIEW("phase_review", "Phase Review"),
    CLO_ACHIEVEMENT_ANALYSIS("clo_achievement_analysis", "CLO Achievement Analysis"),
    STUDENT_FEEDBACK_ANALYSIS("student_feedback_analysis", "Student Feedback Analysis"),
    ATTENDANCE_ENGAGEMENT_REVIEW("attendance_engagement_review", "Attendance & Engagement Review"),
    TEACHING_QUALITY_ASSESSMENT("teaching_quality_assessment", "Teaching Quality Assessment");

    private final String value;
    private final String displayName;

    // Static lookup maps for performance
    private static final Map<String, QAReportType> VALUE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(QAReportType::getValue, type -> type));

    private static final Map<String, QAReportType> DISPLAY_NAME_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(QAReportType::getDisplayName, type -> type));

    QAReportType(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    /**
     * Gets the database/API storage value
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the UI display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Robust parsing with multiple fallback strategies
     *
     * @param input String input from API, database, or UI
     * @return Matching QAReportType
     * @throws IllegalArgumentException if no match found
     */
    public static QAReportType fromString(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("QAReportType cannot be null or empty");
        }

        String trimmedInput = input.trim();

        // Strategy 1: Exact value match (database format)
        QAReportType exactValueMatch = VALUE_MAP.get(trimmedInput.toLowerCase());
        if (exactValueMatch != null) {
            return exactValueMatch;
        }

        // Strategy 2: Exact display name match (UI format)
        QAReportType displayNameMatch = DISPLAY_NAME_MAP.get(trimmedInput);
        if (displayNameMatch != null) {
            return displayNameMatch;
        }

        // Strategy 3: Case-insensitive display name match
        QAReportType caseInsensitiveMatch = DISPLAY_NAME_MAP.entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(trimmedInput))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (caseInsensitiveMatch != null) {
            return caseInsensitiveMatch;
        }

        // Strategy 4: Exact enum name match (Java constant)
        try {
            return QAReportType.valueOf(trimmedInput.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Continue to fuzzy matching
        }

        // Strategy 5: Fuzzy matching with normalization
        String normalizedInput = normalizeString(trimmedInput);
        for (QAReportType type : QAReportType.values()) {
            if (normalizeString(type.getValue()).equals(normalizedInput) ||
                normalizeString(type.getDisplayName()).equals(normalizedInput)) {
                return type;
            }
        }

        // If all strategies fail, provide helpful error message
        throw new IllegalArgumentException(String.format(
                "Invalid QAReportType: '%s'. Valid options are:%n" +
                "- Database values: %s%n" +
                "- Display names: %s%n" +
                "- Enum constants: %s",
                trimmedInput,
                VALUE_MAP.keySet().stream().collect(Collectors.joining(", ")),
                DISPLAY_NAME_MAP.keySet().stream().collect(Collectors.joining(", ")),
                Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "))
        ));
    }

    /**
     * Helper method to normalize strings for fuzzy matching
     */
    private static String normalizeString(String input) {
        return input.toLowerCase()
                .replaceAll("[_\\s-]+", "")
                .replaceAll("&", "and");
    }

    /**
     * Get all available values for select options
     */
    public static QAReportType[] getValues() {
        return values();
    }

    /**
     * Check if a string value is valid
     */
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

    /**
     * Get default value (first enum)
     */
    public static QAReportType getDefaultValue() {
        return CLASSROOM_OBSERVATION;
    }

    @Override
    public String toString() {
        return displayName; // Return display name for UI display
    }
}
