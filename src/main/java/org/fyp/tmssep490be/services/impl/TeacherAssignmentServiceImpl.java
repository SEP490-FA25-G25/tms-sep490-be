package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
}
