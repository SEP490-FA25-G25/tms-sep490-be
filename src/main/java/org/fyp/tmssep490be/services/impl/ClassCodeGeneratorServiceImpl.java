package org.fyp.tmssep490be.services.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.services.ClassCodeGeneratorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of ClassCodeGeneratorService
 * Generates unique class codes with thread-safe sequence management using PostgreSQL advisory locks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassCodeGeneratorServiceImpl implements ClassCodeGeneratorService {

    private static final int MAX_SEQUENCE = 999;
    private static final int SEQUENCE_DIGITS = 3;
    private static final Pattern CODE_PATTERN = Pattern.compile("^(.+)-(\\d{3})$");
    
    private final ClassRepository classRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public String generateClassCode(Long branchId, String branchCode, String courseCode, LocalDate startDate) {
        log.info("Generating class code for branchId={}, courseCode={}, startDate={}", 
                branchId, courseCode, startDate);
        
        try {
            // 1. Build prefix
            String normalizedCourseCode = normalizeCourseCode(courseCode);
            int year = startDate.getYear();
            String prefix = buildPrefix(normalizedCourseCode, branchCode, year);
            
            log.debug("Built prefix: {}", prefix);
            
            // 2. Acquire advisory lock for this prefix
            acquireAdvisoryLock(prefix);
            
            // 3. Find next sequence
            int nextSeq = findNextSequence(branchId, prefix);
            
            // 4. Generate final code
            String generatedCode = String.format("%s-%03d", prefix, nextSeq);
            
            log.info("Generated class code: {} (sequence={})", generatedCode, nextSeq);
            return generatedCode;
            
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate class code for branchId={}, courseCode={}", 
                    branchId, courseCode, e);
            throw new CustomException(ErrorCode.CLASS_CODE_GENERATION_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String previewClassCode(Long branchId, String branchCode, String courseCode, LocalDate startDate) {
        log.debug("Preview class code for branchId={}, courseCode={}, startDate={}", 
                branchId, courseCode, startDate);
        
        try {
            // Same logic as generate but without locking (read-only)
            String normalizedCourseCode = normalizeCourseCode(courseCode);
            int year = startDate.getYear();
            String prefix = buildPrefix(normalizedCourseCode, branchCode, year);
            
            int nextSeq = findNextSequenceReadOnly(branchId, prefix);
            
            return String.format("%s-%03d", prefix, nextSeq);
            
        } catch (Exception e) {
            log.error("Failed to preview class code for branchId={}, courseCode={}", 
                    branchId, courseCode, e);
            throw new CustomException(ErrorCode.CLASS_CODE_GENERATION_FAILED);
        }
    }

    @Override
    public String normalizeCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_CODE_INVALID_FORMAT);
        }
        
        // Remove all non-alphanumeric characters and convert to uppercase
        // Example: "IELTS-FOUND-2025-V1" -> "IELTSFOUND2025V1"
        // We'll take only the meaningful part (before year/version)
        String normalized = courseCode
                .replaceAll("[^A-Z0-9]", "")
                .toUpperCase();
        
        // Try to extract only the course name part (before year digits if any)
        // Pattern: Find the first occurrence of 4-digit year (20XX) and cut there
        Pattern yearPattern = Pattern.compile("(\\d{4})");
        Matcher matcher = yearPattern.matcher(normalized);
        
        if (matcher.find()) {
            // Cut before the year
            normalized = normalized.substring(0, matcher.start());
        }
        
        log.debug("Normalized course code: {} -> {}", courseCode, normalized);
        return normalized;
    }

    @Override
    public String buildPrefix(String normalizedCourseCode, String branchCode, int year) {
        if (normalizedCourseCode == null || branchCode == null) {
            throw new CustomException(ErrorCode.CLASS_CODE_INVALID_FORMAT);
        }
        
        // Format: COURSECODE-BRANCHCODE-YY
        String yy = String.format("%02d", year % 100);
        return String.format("%s-%s-%s", normalizedCourseCode, branchCode, yy);
    }

    /**
     * Acquire PostgreSQL advisory lock for the given prefix
     * This ensures only one transaction can generate a code for this prefix at a time
     * Lock is automatically released at transaction commit/rollback
     */
    private void acquireAdvisoryLock(String prefix) {
        log.debug("Acquiring advisory lock for prefix: {}", prefix);
        
        // Use pg_advisory_xact_lock with hashtext to convert string to bigint
        entityManager.createNativeQuery(
                "SELECT pg_advisory_xact_lock(hashtext(:prefix))"
        ).setParameter("prefix", prefix).getSingleResult();
        
        log.debug("Advisory lock acquired for prefix: {}", prefix);
    }

    /**
     * Find next sequence number with lock (for actual generation)
     */
    private int findNextSequence(Long branchId, String prefix) {
        // Build regex pattern to match exact format: PREFIX-XXX where XXX is 3 digits
        String regex = "^" + Pattern.quote(prefix) + "-[0-9]{3}$";
        
        log.debug("Finding highest sequence with regex: {}", regex);
        
        String lastCode = classRepository.findHighestCodeByPrefix(branchId, regex)
                .orElse(null);
        
        if (lastCode == null) {
            log.debug("No existing class with prefix {}, starting from 001", prefix);
            return 1;
        }
        
        log.debug("Found last code: {}", lastCode);
        
        // Extract sequence from code
        int lastSeq = extractSequence(lastCode);
        
        if (lastSeq >= MAX_SEQUENCE) {
            log.error("Sequence limit reached for prefix: {} (current={})", prefix, lastSeq);
            throw new CustomException(ErrorCode.CLASS_CODE_SEQUENCE_LIMIT_REACHED);
        }
        
        return lastSeq + 1;
    }

    /**
     * Find next sequence number without lock (for preview)
     */
    private int findNextSequenceReadOnly(Long branchId, String prefix) {
        String regex = "^" + Pattern.quote(prefix) + "-[0-9]{3}$";
        
        String lastCode = classRepository.findHighestCodeByPrefixReadOnly(branchId, regex)
                .orElse(null);
        
        if (lastCode == null) {
            return 1;
        }
        
        int lastSeq = extractSequence(lastCode);
        
        if (lastSeq >= MAX_SEQUENCE) {
            return MAX_SEQUENCE; // Preview shows limit reached
        }
        
        return lastSeq + 1;
    }

    /**
     * Extract sequence number from class code
     * Example: "IELTSFOUND-HN01-25-011" -> 11
     */
    private int extractSequence(String code) {
        Matcher matcher = CODE_PATTERN.matcher(code);
        
        if (!matcher.matches()) {
            log.error("Invalid code format for sequence extraction: {}", code);
            throw new CustomException(ErrorCode.CLASS_CODE_PARSE_ERROR);
        }
        
        String seqStr = matcher.group(2);
        
        try {
            return Integer.parseInt(seqStr);
        } catch (NumberFormatException e) {
            log.error("Failed to parse sequence from code: {}", code, e);
            throw new CustomException(ErrorCode.CLASS_CODE_PARSE_ERROR);
        }
    }
}
