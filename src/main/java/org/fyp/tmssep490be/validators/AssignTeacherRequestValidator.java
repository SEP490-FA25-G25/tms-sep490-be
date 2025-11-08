package org.fyp.tmssep490be.validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validator for AssignTeacherRequest
 * <p>
 * Validates:
 * <ul>
 *   <li>Class exists and is in DRAFT status</li>
 *   <li>Teacher exists and belongs to same branch</li>
 *   <li>Session IDs exist and belong to the class (for partial assignment)</li>
 *   <li>Sessions have time slots assigned</li>
 *   <li>Teacher has required skills (unless has GENERAL skill)</li>
 * </ul>
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AssignTeacherRequestValidator {

    private final ClassRepository classRepository;
    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;
    private final TeacherSkillRepository teacherSkillRepository;

    /**
     * Validate assign teacher request
     *
     * @param classId Class ID
     * @param request Assign teacher request
     * @throws CustomException If validation fails
     */
    public void validate(Long classId, AssignTeacherRequest request) {
        log.info("Validating assign teacher request for class {}, teacher {}", classId, request.getTeacherId());

        // 1. Validate class exists and is in DRAFT status
        ClassEntity classEntity = validateClassStatus(classId);

        // 2. Validate teacher exists
        Teacher teacher = validateTeacherExists(request.getTeacherId());

        // 3. Validate session IDs (if partial assignment)
        List<Long> sessionIds = request.getSessionIds();
        if (sessionIds != null && !sessionIds.isEmpty()) {
            validateSessionIds(sessionIds, classId);
        } else {
            // Full assignment - validate all sessions have time slots
            validateAllSessionsHaveTimeSlots(classId);
        }

        // 4. Validate teacher has required skills
        validateTeacherSkills(teacher, classId);

        log.info("Validation passed for assign teacher request");
    }

    /**
     * Validate class exists and is in DRAFT status
     */
    private ClassEntity validateClassStatus(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        if (classEntity.getStatus() != ClassStatus.DRAFT) {
            throw new CustomException(ErrorCode.CLASS_INVALID_STATUS);
        }

        return classEntity;
    }

    /**
     * Validate teacher exists
     */
    private Teacher validateTeacherExists(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // TODO: Add branch validation if needed
        // Note: Teacher-branch relationship is through user_branches (many-to-many)
        
        return teacher;
    }

    /**
     * Validate session IDs exist and belong to the class
     */
    private void validateSessionIds(List<Long> sessionIds, Long classId) {
        // Check for duplicates
        Set<Long> uniqueSessionIds = new HashSet<>(sessionIds);
        if (uniqueSessionIds.size() != sessionIds.size()) {
            throw new CustomException(ErrorCode.DUPLICATE_SESSION_IDS);
        }

        // Check all sessions exist and belong to class
        List<Session> sessions = sessionRepository.findAllById(sessionIds);
        if (sessions.size() != sessionIds.size()) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }

        // Check all sessions belong to the class
        boolean allBelongToClass = sessions.stream()
                .allMatch(session -> session.getClassEntity().getId().equals(classId));
        if (!allBelongToClass) {
            throw new CustomException(ErrorCode.SESSION_NOT_IN_CLASS);
        }

        // Check all sessions have time slots
        boolean allHaveTimeSlots = sessions.stream()
                .allMatch(session -> session.getTimeSlotTemplate() != null);
        if (!allHaveTimeSlots) {
            throw new CustomException(ErrorCode.TIME_SLOT_NOT_ASSIGNED);
        }
    }

    /**
     * Validate all sessions in class have time slots assigned
     */
    private void validateAllSessionsHaveTimeSlots(Long classId) {
        long sessionsWithoutTimeSlots = sessionRepository.countSessionsWithoutTimeSlots(classId);
        if (sessionsWithoutTimeSlots > 0) {
            throw new CustomException(ErrorCode.TIME_SLOT_NOT_ASSIGNED);
        }
    }

    /**
     * Validate teacher has required skills for class sessions
     * <p>
     * If teacher has GENERAL skill → Can teach ANY session (skip validation)
     * Otherwise → Must have at least one matching skill for each session type
     * </p>
     */
    private void validateTeacherSkills(Teacher teacher, Long classId) {
        // Get teacher's skills
        List<TeacherSkill> teacherSkills = teacherSkillRepository.findByTeacherId(teacher.getId());
        Set<Skill> skillSet = teacherSkills.stream()
                .map(ts -> ts.getId().getSkill())
                .collect(Collectors.toSet());

        // If teacher has GENERAL skill → Can teach any session
        if (skillSet.contains(Skill.GENERAL)) {
            log.info("Teacher {} has GENERAL skill - can teach any session type", teacher.getId());
            return;
        }

        // Get all unique skills required by class sessions
        List<String> requiredSkillNames = sessionRepository.findDistinctSkillNamesByClassId(classId);

        // Convert String names to Skill enum
        Set<Skill> requiredSkills = requiredSkillNames.stream()
                .map(Skill::valueOf)
                .collect(Collectors.toSet());

        // Check if teacher has all required skills
        Set<Skill> missingSkills = new HashSet<>();
        for (Skill requiredSkill : requiredSkills) {
            if (!skillSet.contains(requiredSkill)) {
                missingSkills.add(requiredSkill);
            }
        }

        if (!missingSkills.isEmpty()) {
            log.error("Teacher {} missing skills: {}. Teacher's skills: {}", 
                    teacher.getId(), missingSkills, skillSet);
            throw new CustomException(ErrorCode.TEACHER_SKILL_MISMATCH);
        }

        log.info("Teacher {} has all required skills for class {}", teacher.getId(), classId);
    }
}
