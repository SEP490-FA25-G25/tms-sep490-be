package org.fyp.tmssep490be.integration;

import org.fyp.tmssep490be.entities.enums.QAReportType;
import org.fyp.tmssep490be.entities.enums.QAReportStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EnumParsingIntegrationTest {

    @Test
    void testPhaseReviewEnumParsing() {
        // Test the specific problematic case that was causing the original error
        QAReportType result = QAReportType.fromString("phase_review");
        assertEquals(QAReportType.PHASE_REVIEW, result);
        assertEquals("phase_review", result.getValue());
        assertEquals("Phase Review", result.getDisplayName());
    }

    @Test
    void testEnumDatabaseConsistency() {
        // Verify that our enums match the database values
        assertEquals("phase_review", QAReportType.PHASE_REVIEW.getValue());
        assertEquals("classroom_observation", QAReportType.CLASSROOM_OBSERVATION.getValue());

        assertEquals("draft", QAReportStatus.DRAFT.getValue());
        assertEquals("submitted", QAReportStatus.SUBMITTED.getValue());
    }

    @Test
    void testInvalidEnumHandling() {
        // Verify that invalid values throw appropriate exceptions
        assertThrows(IllegalArgumentException.class, () -> {
            QAReportType.fromString("invalid_type");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            QAReportStatus.fromString("invalid_status");
        });
    }

    @Test
    void testEnumValidationHelpers() {
        assertTrue(QAReportType.isValidValue("phase_review"));
        assertTrue(QAReportType.isValidValue("Phase Review"));
        assertFalse(QAReportType.isValidValue("invalid"));

        assertTrue(QAReportStatus.isValidValue("draft"));
        assertTrue(QAReportStatus.isValidValue("submitted"));
        assertFalse(QAReportStatus.isValidValue("invalid"));
    }
}