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
        QAReportType result = QAReportType.fromString("PHASE_REVIEW");
        assertEquals(QAReportType.PHASE_REVIEW, result);
        assertEquals("Đánh giá giai đoạn", result.getDisplayName());
        assertEquals("Đánh giá giai đoạn", result.toString());
    }

    @Test
    void testEnumDatabaseConsistency() {
        // Verify that our enums display names are in Vietnamese
        assertEquals("Đánh giá giai đoạn", QAReportType.PHASE_REVIEW.getDisplayName());
        assertEquals("Quan sát lớp học", QAReportType.CLASSROOM_OBSERVATION.getDisplayName());

        assertEquals("Bản nháp", QAReportStatus.DRAFT.getDisplayName());
        assertEquals("Đã nộp", QAReportStatus.SUBMITTED.getDisplayName());
    }

    @Test
    void testInvalidEnumHandling() {
        // Verify that invalid values throw appropriate exceptions
        Exception typeException = assertThrows(IllegalArgumentException.class, () -> {
            QAReportType.fromString("invalid_type");
        });
        assertTrue(typeException.getMessage().contains("QAReportType không hợp lệ"));

        Exception statusException = assertThrows(IllegalArgumentException.class, () -> {
            QAReportStatus.fromString("invalid_status");
        });
        assertTrue(statusException.getMessage().contains("QAReportStatus không hợp lệ"));
    }

    @Test
    void testEnumParsing() {
        // Test case-insensitive parsing
        assertEquals(QAReportType.PHASE_REVIEW, QAReportType.fromString("PHASE_REVIEW"));
        assertEquals(QAReportType.PHASE_REVIEW, QAReportType.fromString("phase_review"));

        assertEquals(QAReportStatus.DRAFT, QAReportStatus.fromString("DRAFT"));
        assertEquals(QAReportStatus.DRAFT, QAReportStatus.fromString("draft"));
    }
}