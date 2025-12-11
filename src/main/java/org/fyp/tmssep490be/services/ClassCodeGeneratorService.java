package org.fyp.tmssep490be.services;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
@Slf4j
public class ClassCodeGeneratorService {

    private static final int MAX_SEQUENCE = 999;
    private static final Pattern CODE_PATTERN = Pattern.compile("^(.+)-(\\d{3})$");

    private final ClassRepository classRepository;
    private final EntityManager entityManager;

    // Sinh mã lớp mới với lock
    public String generateClassCode(Long branchId, String branchCode, String courseCode, LocalDate startDate) {
        log.info("Sinh mã lớp cho branchId={}, courseCode={}, startDate={}", branchId, courseCode, startDate);

        try {
            String normalizedCourseCode = normalizeCourseCode(courseCode);
            String prefix = buildPrefix(normalizedCourseCode, branchCode, startDate.getYear());

            // Lấy lock để tránh race condition
            acquireAdvisoryLock(prefix);

            int nextSeq = findNextSequence(branchId, prefix);
            String generatedCode = String.format("%s-%03d", prefix, nextSeq);

            log.info("Đã sinh mã lớp: {}", generatedCode);
            return generatedCode;

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi sinh mã lớp cho branchId={}, courseCode={}", branchId, courseCode, e);
            throw new CustomException(ErrorCode.CLASS_CODE_GENERATION_FAILED);
        }
    }

    // Preview mã lớp (không lock)
    @Transactional(readOnly = true)
    public String previewClassCode(Long branchId, String branchCode, String courseCode, LocalDate startDate) {
        try {
            String normalizedCourseCode = normalizeCourseCode(courseCode);
            String prefix = buildPrefix(normalizedCourseCode, branchCode, startDate.getYear());
            int nextSeq = findNextSequenceReadOnly(branchId, prefix);
            return String.format("%s-%03d", prefix, nextSeq);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi preview mã lớp", e);
            throw new CustomException(ErrorCode.CLASS_CODE_GENERATION_FAILED);
        }
    }

    // Chuẩn hóa mã khóa học: "IELTS-FOUND-2025-V1" -> "IELTSFOUND"
    public String normalizeCourseCode(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_CODE_INVALID_FORMAT);
        }

        String normalized = courseCode.replaceAll("[^A-Z0-9]", "").toUpperCase();

        // Cắt bỏ phần năm nếu có (ví dụ: 2025)
        Pattern yearPattern = Pattern.compile("(\\d{4})");
        Matcher matcher = yearPattern.matcher(normalized);
        if (matcher.find()) {
            normalized = normalized.substring(0, matcher.start());
        }

        return normalized;
    }

    // Tạo prefix: COURSECODE-BRANCHCODE-YY
    public String buildPrefix(String normalizedCourseCode, String branchCode, int year) {
        if (normalizedCourseCode == null || branchCode == null) {
            throw new CustomException(ErrorCode.CLASS_CODE_INVALID_FORMAT);
        }
        String yy = String.format("%02d", year % 100);
        return String.format("%s-%s-%s", normalizedCourseCode, branchCode, yy);
    }

    // Lấy PostgreSQL advisory lock
    private void acquireAdvisoryLock(String prefix) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:prefix))")
                .setParameter("prefix", prefix)
                .getSingleResult();
    }

    // Tìm sequence tiếp theo (có lock)
    private int findNextSequence(Long branchId, String prefix) {
        String lastCode = classRepository.findHighestCodeByPrefixReadOnly(branchId, prefix).orElse(null);

        if (lastCode == null) {
            return 1;
        }

        int lastSeq = extractSequence(lastCode);
        if (lastSeq >= MAX_SEQUENCE) {
            throw new CustomException(ErrorCode.CLASS_CODE_SEQUENCE_LIMIT_REACHED);
        }

        return lastSeq + 1;
    }

    // Tìm sequence tiếp theo (read-only, cho preview)
    private int findNextSequenceReadOnly(Long branchId, String prefix) {
        String lastCode = classRepository.findHighestCodeByPrefixReadOnly(branchId, prefix).orElse(null);

        if (lastCode == null) {
            return 1;
        }

        int lastSeq = extractSequence(lastCode);
        return lastSeq >= MAX_SEQUENCE ? MAX_SEQUENCE : lastSeq + 1;
    }

    // Trích xuất sequence từ mã lớp: "IELTSFOUND-HN01-25-011" -> 11
    private int extractSequence(String code) {
        Matcher matcher = CODE_PATTERN.matcher(code);

        if (!matcher.matches()) {
            throw new CustomException(ErrorCode.CLASS_CODE_PARSE_ERROR);
        }

        try {
            return Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.CLASS_CODE_PARSE_ERROR);
        }
    }
}
