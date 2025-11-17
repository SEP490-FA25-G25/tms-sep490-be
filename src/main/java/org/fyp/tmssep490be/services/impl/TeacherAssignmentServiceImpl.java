package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.TeacherDayAvailabilityDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.ClassRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.TeacherRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.TeacherAssignmentService;
import org.fyp.tmssep490be.utils.AssignTeacherResponseUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of TeacherAssignmentService with PRE-CHECK approach
 * <p>
 * Key Features:
 * <ul>
 *   <li>Executes complex CTE query for availability checking</li>
 *   <li>Maps raw SQL results to TeacherAvailabilityDTO</li>
 *   <li>Handles 'GENERAL' skill as universal skill</li>
 *   <li>Direct bulk insert for teacher assignment</li>
 *   <li>Supports partial assignment (some sessions only)</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TeacherAssignmentServiceImpl implements TeacherAssignmentService {

    private final TeacherRepository teacherRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final AssignTeacherResponseUtil responseUtil;

    /**
     * Queries available teachers with PRE-CHECK details using complex CTE query.
     * <p>
     * <b>PRE-CHECK approach:</b> Single CTE query returns all teacher availability data:
     * <ul>
     *   <li>Teacher basic info (ID, name, email, skills)</li>
     *   <li>Availability metrics (total/available sessions, percentage)</li>
     *   <li>Conflict breakdown (no availability, teaching conflict, leave, skill mismatch)</li>
     *   <li>GENERAL skill detection (universal skill that bypasses skill validation)</li>
     * </ul>
     * Results are sorted by availability percentage (descending) for easy selection.
     * </p>
     *
     * @param classId ID of the class to query teachers for
     * @return List of TeacherAvailabilityDTO with detailed availability and conflict data
     * @throws CustomException with CLASS_NOT_FOUND if class doesn't exist
     * @see TeacherAvailabilityDTO
     * @see TeacherAvailabilityDTO.AvailabilityStatus
     * @see TeacherAvailabilityDTO.ConflictBreakdown
     */
    @Override
    public List<TeacherAvailabilityDTO> queryAvailableTeachersWithPrecheck(Long classId) {
        log.info("Executing PRE-CHECK query for class {}", classId);
        long startTime = System.currentTimeMillis();

        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Execute complex CTE query
        List<Object[]> rawResults = teacherRepository.findAvailableTeachersWithPrecheck(classId);

        // Map raw results to DTOs
        List<TeacherAvailabilityDTO> teachers = rawResults.stream()
                .map(this::mapToTeacherAvailabilityDTO)
                .collect(Collectors.toList());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("PRE-CHECK query completed in {}ms. Found {} teachers", processingTime, teachers.size());

        return teachers;
    }

    /**
     * Assigns a teacher to class sessions (full or partial assignment).
     * <p>
     * <b>Full Assignment Mode</b> (sessionIds = null or empty):
     * <ul>
     *   <li>Assigns teacher to ALL sessions in the class</li>
     *   <li>Uses bulk INSERT for performance</li>
     *   <li>Response includes remaining sessions if any couldn't be assigned</li>
     * </ul>
     * </p>
     * <p>
     * <b>Partial Assignment Mode</b> (sessionIds provided):
     * <ul>
     *   <li>Assigns teacher to SPECIFIC sessions only</li>
     *   <li>Used for multi-teacher classes or conflict resolution</li>
     *   <li>Response includes remaining unassigned sessions</li>
     * </ul>
     * </p>
     *
     * @param classId ID of the class to assign teacher to
     * @param request Assignment request with teacherId and optional sessionIds list
     * @return AssignTeacherResponse with assigned count, remaining sessions, processing time
     * @throws CustomException with CLASS_NOT_FOUND if class doesn't exist
     * @throws CustomException with TEACHER_NOT_FOUND if teacher doesn't exist
     * @see AssignTeacherRequest
     * @see AssignTeacherResponse
     */
    @Override
    @Transactional
    public AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest request) {
        log.info("Assigning teacher {} to class {}", request.getTeacherId(), classId);
        long startTime = System.currentTimeMillis();

        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate teacher exists
        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // Get total sessions count
        long totalSessions = sessionRepository.countByClassEntityId(classId);

        List<Long> assignedSessionIds;
        List<Long> remainingSessionIds;

        if (request.getSessionIds() == null || request.getSessionIds().isEmpty()) {
            // Full Assignment: Assign to ALL sessions
            log.info("Full assignment mode: assigning teacher to all {} sessions", totalSessions);
            assignedSessionIds = teachingSlotRepository.bulkAssignTeacher(classId, request.getTeacherId());
            
            // Get remaining sessions (those that weren't assigned due to conflicts)
            remainingSessionIds = findRemainingSessions(classId);
        } else {
            // Partial Assignment: Assign to specific sessions only
            log.info("Partial assignment mode: assigning teacher to {} specific sessions", 
                    request.getSessionIds().size());
            assignedSessionIds = teachingSlotRepository.bulkAssignTeacherToSessions(
                    request.getSessionIds(), request.getTeacherId());
            
            // Get remaining sessions (all sessions that don't have this teacher assigned)
            remainingSessionIds = findRemainingSessions(classId);
        }

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Teacher assignment completed in {}ms. Assigned: {}, Remaining: {}", 
                processingTime, assignedSessionIds.size(), remainingSessionIds.size());

        // Build response
        return responseUtil.buildSuccessResponse(
                classId,
                teacher,
                (int) totalSessions,
                assignedSessionIds,
                remainingSessionIds,
                processingTime
        );
    }

    /**
     * Map raw SQL result to TeacherAvailabilityDTO
     * <p>
     * Object[] structure from SQL:
     * [0] Long teacherId
     * [1] String fullName
     * [2] String email
     * [3] String skills (comma-separated)
     * [4] Boolean hasGeneralSkill
     * [5] Integer totalSessions
     * [6] Integer availableSessions
     * [7] Integer noAvailabilityCount
     * [8] Integer teachingConflictCount
     * [9] Integer leaveConflictCount
     * [10] Integer skillMismatchCount
     * </p>
     */
    private TeacherAvailabilityDTO mapToTeacherAvailabilityDTO(Object[] row) {
        try {
            // Extract values with proper type conversion
            Long teacherId = convertToLong(row[0]);
            String fullName = (String) row[1];
            String email = (String) row[2];
            String skillsStr = (String) row[3];
            Boolean hasGeneralSkill = (Boolean) row[4];
            Integer totalSessions = convertToInteger(row[5]);
            Integer availableSessions = convertToInteger(row[6]);
            Integer noAvailabilityCount = convertToInteger(row[7]);
            Integer teachingConflictCount = convertToInteger(row[8]);
            Integer leaveConflictCount = convertToInteger(row[9]);
            Integer skillMismatchCount = convertToInteger(row[10]);

            // Parse skills string to List<Skill>
            List<Skill> skills = parseSkills(skillsStr);

            // Calculate availability percentage
            double availabilityPercentage = responseUtil.calculateAvailabilityPercentage(
                    availableSessions, totalSessions);

            // Determine availability status
            TeacherAvailabilityDTO.AvailabilityStatus availabilityStatus = 
                    responseUtil.calculateAvailabilityStatus(availableSessions, totalSessions);

            // Build conflict breakdown
            int totalConflicts = noAvailabilityCount + teachingConflictCount + 
                               leaveConflictCount + skillMismatchCount;

            TeacherAvailabilityDTO.ConflictBreakdown conflicts = 
                    TeacherAvailabilityDTO.ConflictBreakdown.builder()
                            .noAvailability(noAvailabilityCount)
                            .teachingConflict(teachingConflictCount)
                            .leaveConflict(leaveConflictCount)
                            .skillMismatch(skillMismatchCount)
                            .totalConflicts(totalConflicts)
                            .build();

            // Build final DTO
            return TeacherAvailabilityDTO.builder()
                    .teacherId(teacherId)
                    .fullName(fullName)
                    .email(email)
                    .skills(skills)
                    .hasGeneralSkill(hasGeneralSkill != null && hasGeneralSkill)
                    .totalSessions(totalSessions)
                    .availableSessions(availableSessions)
                    .availabilityPercentage(availabilityPercentage)
                    .availabilityStatus(availabilityStatus)
                    .conflicts(conflicts)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping teacher availability result: {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Parse comma-separated skills string to List<Skill>
     */
    private List<Skill> parseSkills(String skillsStr) {
        if (skillsStr == null || skillsStr.isEmpty()) {
            return List.of();
        }

        return Arrays.stream(skillsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Skill::valueOf)
                .collect(Collectors.toList());
    }

    /**
     * Find sessions that don't have any teacher assigned yet
     */
    private List<Long> findRemainingSessions(Long classId) {
        return sessionRepository.findSessionsWithoutTeacher(classId);
    }

    /**
     * Convert Object to Long (handles BigInteger from PostgreSQL)
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).longValue();
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    /**
     * Convert Object to Integer (handles BigInteger from PostgreSQL)
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof BigInteger) {
            return ((BigInteger) value).intValue();
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    /**
     * Query teachers available by specific days of week (for multi-teacher assignment)
     * <p>
     * Returns teachers who are available for AT LEAST ONE FULL DAY (all sessions on that day).
     * A teacher is only included for a day if they can teach ALL sessions on that day from start to end.
     * </p>
     * <p>
     * <b>Use Case:</b> "Phân công nhiều giáo viên" mode where:
     * <ul>
     *   <li>John Smith teaches all Mondays (8 sessions)</li>
     *   <li>Lisa Chen teaches all Wednesdays (8 sessions)</li>
     *   <li>Anna Martinez teaches all Fridays (8 sessions)</li>
     * </ul>
     * </p>
     * 
     * @param classId Class ID
     * @return List of teachers with available days (only fully available days)
     */
    @Override
    public List<TeacherDayAvailabilityDTO> queryTeachersAvailableByDay(Long classId) {
        log.info("Querying teachers available by day for class {}", classId);
        long startTime = System.currentTimeMillis();

        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Execute query
        List<Object[]> rawResults = teacherRepository.findAvailableTeachersByDay(classId);

        // Group results by teacher ID
        Map<Long, List<Object[]>> groupedByTeacher = rawResults.stream()
                .collect(Collectors.groupingBy(row -> convertToLong(row[0])));

        // Get total sessions for percentage calculation
        long totalSessions = sessionRepository.countByClassEntityId(classId);

        // Map to DTOs
        List<TeacherDayAvailabilityDTO> teachers = groupedByTeacher.entrySet().stream()
                .map(entry -> mapToTeacherDayAvailabilityDTO(entry.getValue(), totalSessions))
                .filter(teacher -> !teacher.getAvailableDays().isEmpty()) // Only include teachers with at least 1 available day
                .collect(Collectors.toList());

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Query completed in {}ms. Found {} teachers with day availability", processingTime, teachers.size());

        return teachers;
    }

    /**
     * Map raw SQL result rows to TeacherDayAvailabilityDTO
     * <p>
     * Expected row format from SQL query:
     * [0] teacher_id, [1] full_name, [2] email, [3] employee_code, [4] skills (comma-separated),
     * [5] has_general_skill, [6] day_of_week, [7] day_name, [8] total_sessions, [9] available_sessions,
     * [10] first_date, [11] last_date, [12] time_slot_display
     * </p>
     */
    private TeacherDayAvailabilityDTO mapToTeacherDayAvailabilityDTO(List<Object[]> rows, long totalClassSessions) {
        if (rows.isEmpty()) {
            return null;
        }

        // First row contains teacher basic info (same across all rows)
        Object[] firstRow = rows.get(0);
        Long teacherId = convertToLong(firstRow[0]);
        String fullName = (String) firstRow[1];
        String email = (String) firstRow[2];
        String employeeCode = (String) firstRow[3];
        String skillsStr = (String) firstRow[4];
        Boolean hasGeneralSkill = (Boolean) firstRow[5];

        // Parse skills
        List<Skill> skills = parseSkills(skillsStr);

        // Build day availability list from all rows
        List<TeacherDayAvailabilityDTO.DayAvailability> availableDays = rows.stream()
                .map(row -> {
                    Short dayOfWeek = ((Number) row[6]).shortValue();
                    String dayName = (String) row[7];
                    Integer totalSessions = convertToInteger(row[8]);
                    Integer availableSessions = convertToInteger(row[9]);
                    LocalDate firstDate = row[10] != null ? ((java.sql.Date) row[10]).toLocalDate() : null;
                    LocalDate lastDate = row[11] != null ? ((java.sql.Date) row[11]).toLocalDate() : null;
                    String timeSlotDisplay = (String) row[12];

                    return TeacherDayAvailabilityDTO.DayAvailability.builder()
                            .dayOfWeek(dayOfWeek)
                            .dayName(dayName)
                            .totalSessions(totalSessions)
                            .availableSessions(availableSessions)
                            .firstDate(firstDate)
                            .lastDate(lastDate)
                            .isFullyAvailable(availableSessions.equals(totalSessions))
                            .timeSlotDisplay(timeSlotDisplay)
                            .build();
                })
                .filter(day -> day.getIsFullyAvailable()) // Only include fully available days
                .collect(Collectors.toList());

        return TeacherDayAvailabilityDTO.builder()
                .teacherId(teacherId)
                .fullName(fullName)
                .email(email)
                .employeeCode(employeeCode)
                .skills(skills)
                .hasGeneralSkill(hasGeneralSkill != null && hasGeneralSkill)
                .availableDays(availableDays)
                .totalClassSessions((int) totalClassSessions)
                .build();
    }

    /**
     * Get day name in Vietnamese from day of week number
     */
    private String getDayName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Thứ Hai";
            case 2 -> "Thứ Ba";
            case 3 -> "Thứ Tư";
            case 4 -> "Thứ Năm";
            case 5 -> "Thứ Sáu";
            case 6 -> "Thứ Bảy";
            case 7 -> "Chủ Nhật";
            default -> "Unknown";
        };
    }
}

