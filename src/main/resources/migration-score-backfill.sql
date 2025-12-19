-- Migration Script: Backfill Score Records for Existing Enrollments
-- Purpose: Tạo score records cho students đã enrolled nhưng chưa có score
-- Run this ONCE after deploying the enrollment fix
-- Date: 2025-12-20

-- Insert missing score records for enrolled students
-- Logic: Với mỗi student enrolled vào class, tạo score record cho tất cả assessments của class đó
INSERT INTO score (assessment_id, student_id, score, feedback, graded_by, graded_at, created_at, updated_at)
SELECT 
    a.id AS assessment_id,
    e.student_id,
    NULL AS score,           -- Chưa chấm điểm
    NULL AS feedback,
    NULL AS graded_by,
    NULL AS graded_at,
    CURRENT_TIMESTAMP AS created_at,
    CURRENT_TIMESTAMP AS updated_at
FROM enrollment e
INNER JOIN assessment a ON a.class_id = e.class_id
WHERE 
    e.status = 'ENROLLED'
    AND NOT EXISTS (
        -- Chỉ insert nếu chưa có score record
        SELECT 1 
        FROM score s 
        WHERE s.assessment_id = a.id 
        AND s.student_id = e.student_id
    )
ORDER BY e.class_id, e.student_id, a.id;

-- Verify kết quả
-- Expected: Số score records = số enrolled students × số assessments per class
SELECT 
    'Total score records created' AS metric,
    COUNT(*) AS count
FROM score
WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '5 minutes';

-- Breakdown by class
SELECT 
    c.code AS class_code,
    c.name AS class_name,
    COUNT(DISTINCT e.student_id) AS enrolled_students,
    COUNT(DISTINCT a.id) AS assessments,
    COUNT(s.id) AS total_scores,
    COUNT(DISTINCT e.student_id) * COUNT(DISTINCT a.id) AS expected_scores
FROM class c
LEFT JOIN enrollment e ON e.class_id = c.id AND e.status = 'ENROLLED'
LEFT JOIN assessment a ON a.class_id = c.id
LEFT JOIN score s ON s.assessment_id = a.id AND s.student_id = e.student_id
WHERE c.status IN ('ONGOING', 'SCHEDULED')
GROUP BY c.id, c.code, c.name
ORDER BY c.code;
