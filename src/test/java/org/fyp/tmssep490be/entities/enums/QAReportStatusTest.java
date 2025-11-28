package org.fyp.tmssep490be.entities.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QAReportStatusTest {

    @Test
    void testDisplayName() {
        QAReportStatus status = QAReportStatus.DRAFT;
        assertEquals("Bản nháp", status.getDisplayName());
        assertEquals("Bản nháp", status.toString());
    }

    @Test
    void testFromStringValid() {
        // Test parsing enum name
        QAReportStatus result = QAReportStatus.fromString("DRAFT");
        assertEquals(QAReportStatus.DRAFT, result);

        // Test case insensitive parsing
        QAReportStatus result2 = QAReportStatus.fromString("draft");
        assertEquals(QAReportStatus.DRAFT, result2);
    }

    @Test
    void testFromStringInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            QAReportStatus.fromString("invalid_status");
        });

        assertTrue(exception.getMessage().contains("QAReportStatus không hợp lệ"));
    }

    @Test
    void testFromStringNull() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            QAReportStatus.fromString(null);
        });

        assertTrue(exception.getMessage().contains("QAReportStatus không được để trống"));
    }

    @Test
    void testFromStringEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            QAReportStatus.fromString("");
        });

        assertTrue(exception.getMessage().contains("QAReportStatus không được để trống"));
    }

    @Test
    void testGetDefaultValue() {
        assertEquals(QAReportStatus.DRAFT, QAReportStatus.getDefaultValue());
    }

    @Test
    void testAllEnumValues() {
        QAReportStatus[] statuses = QAReportStatus.values();
        assertEquals(2, statuses.length);

        // Test that all display names are in Vietnamese
        for (QAReportStatus status : statuses) {
            assertNotNull(status.getDisplayName());
            assertTrue(status.getDisplayName().length() > 0);
        }
    }
}