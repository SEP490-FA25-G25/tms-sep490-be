package org.fyp.tmssep490be.entities.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class QAReportTypeTest {

    @Test
    void testValueAndDisplayName() {
        QAReportType type = QAReportType.PHASE_REVIEW;
        assertEquals("phase_review", type.getValue());
        assertEquals("Phase Review", type.getDisplayName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "phase_review",           // Exact value match
            "Phase Review",            // Display name match
            "phase review",            // Case-insensitive display
            "PHASE_REVIEW",            // Enum constant
            "phase_review",            // Fuzzy match
            "PhaseReview"              // Fuzzy match
    })
    void testFromStringVariousFormats(String input) {
        QAReportType result = QAReportType.fromString(input);
        assertEquals(QAReportType.PHASE_REVIEW, result);
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            QAReportType.fromString("invalid_type");
        });
    }

    @Test
    void testIsValidValue() {
        assertTrue(QAReportType.isValidValue("phase_review"));
        assertTrue(QAReportType.isValidValue("Phase Review"));
        assertFalse(QAReportType.isValidValue("invalid"));
        assertFalse(QAReportType.isValidValue(null));
        assertFalse(QAReportType.isValidValue(""));
    }

    @Test
    void testGetDefaultValue() {
        assertEquals(QAReportType.CLASSROOM_OBSERVATION, QAReportType.getDefaultValue());
    }
}