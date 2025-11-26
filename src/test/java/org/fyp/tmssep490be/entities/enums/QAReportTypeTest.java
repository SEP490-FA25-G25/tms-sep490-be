package org.fyp.tmssep490be.entities.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QAReportTypeTest {

    @Test
    void testDisplayName() {
        QAReportType type = QAReportType.PHASE_REVIEW;
        assertEquals("Đánh giá giai đoạn", type.getDisplayName());
        assertEquals("Đánh giá giai đoạn", type.toString());
    }

    @Test
    void testFromStringValid() {
        // Test parsing enum name
        QAReportType result = QAReportType.fromString("PHASE_REVIEW");
        assertEquals(QAReportType.PHASE_REVIEW, result);

        // Test case insensitive parsing
        QAReportType result2 = QAReportType.fromString("phase_review");
        assertEquals(QAReportType.PHASE_REVIEW, result2);
    }

    @Test
    void testFromStringInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            QAReportType.fromString("invalid_type");
        });

        assertTrue(exception.getMessage().contains("QAReportType không hợp lệ"));
    }

    @Test
    void testFromStringNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            QAReportType.fromString(null);
        });

        assertTrue(exception.getMessage().contains("QAReportType không được để trống"));
    }

    @Test
    void testFromStringEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            QAReportType.fromString("");
        });

        assertTrue(exception.getMessage().contains("QAReportType không được để trống"));
    }

    @Test
    void testGetDefaultValue() {
        assertEquals(QAReportType.CLASSROOM_OBSERVATION, QAReportType.getDefaultValue());
    }

    @Test
    void testAllEnumValues() {
        QAReportType[] types = QAReportType.values();
        assertEquals(6, types.length);

        // Test that all display names are in Vietnamese
        for (QAReportType type : types) {
            assertNotNull(type.getDisplayName());
            assertTrue(type.getDisplayName().length() > 0);
        }
    }
}