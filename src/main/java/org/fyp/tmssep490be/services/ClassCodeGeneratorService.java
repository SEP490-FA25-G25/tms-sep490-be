package org.fyp.tmssep490be.services;

import java.time.LocalDate;

/**
 * Service for generating unique class codes following pattern: COURSECODE-BRANCHCODE-YY-SEQ
 * Thread-safe sequence generation with PostgreSQL advisory locks
 */
public interface ClassCodeGeneratorService {

    /**
     * Generate unique class code for a class
     * Pattern: COURSECODE-BRANCHCODE-YY-SEQ
     * Example: IELTSFOUND-HN01-25-001
     *
     * @param branchId   The branch ID where the class will be created
     * @param branchCode The branch code (e.g., "HN01", "HCM01")
     * @param courseCode The full course code (e.g., "IELTS-FOUND-2025-V1")
     * @param startDate  The class start date (for extracting YY)
     * @return Generated unique class code
     * @throws org.fyp.tmssep490be.exceptions.CustomException if sequence limit reached or generation fails
     */
    String generateClassCode(Long branchId, String branchCode, String courseCode, LocalDate startDate);

    /**
     * Preview what the next class code would be (without locking or incrementing)
     * Used for UI preview before actual submission
     *
     * @param branchId   The branch ID
     * @param branchCode The branch code
     * @param courseCode The course code
     * @param startDate  The class start date
     * @return Preview of the next class code (may change when actually submitting)
     */
    String previewClassCode(Long branchId, String branchCode, String courseCode, LocalDate startDate);

    /**
     * Extract and normalize course code from full course code
     * Example: "IELTS-FOUND-2025-V1" -> "IELTSFOUND"
     *
     * @param courseCode The full course code
     * @return Normalized course code (uppercase, alphanumeric only)
     */
    String normalizeCourseCode(String courseCode);

    /**
     * Build prefix from components
     * Example: "IELTSFOUND", "HN01", 2025 -> "IELTSFOUND-HN01-25"
     *
     * @param normalizedCourseCode Normalized course code
     * @param branchCode           Branch code
     * @param year                 Full year (e.g., 2025)
     * @return Prefix string
     */
    String buildPrefix(String normalizedCourseCode, String branchCode, int year);
}
