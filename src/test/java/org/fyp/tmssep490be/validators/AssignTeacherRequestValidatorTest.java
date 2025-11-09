package org.fyp.tmssep490be.validators;

import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AssignTeacherRequestValidator
 * <p>
 * Tests validation logic for teacher assignment requests including:
 * - Class existence and status validation
 * - Teacher existence validation
 * - Session ID validation (for partial assignment)
 * - Time slot validation
 * - Teacher skill validation (with GENERAL skill bypass)
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AssignTeacherRequestValidator Tests")
class AssignTeacherRequestValidatorTest {

    @Autowired
    private AssignTeacherRequestValidator validator;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private TeacherRepository teacherRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private TeacherSkillRepository teacherSkillRepository;

    private ClassEntity validClass;
    private Teacher validTeacher;
    private AssignTeacherRequest fullAssignmentRequest;
    private AssignTeacherRequest partialAssignmentRequest;
    private List<Session> sessions;

    @BeforeEach
    void setUp() {
        // Build valid class in DRAFT status
        validClass = TestDataBuilder.buildClass()
                .id(1L)
                .status(ClassStatus.DRAFT)
                .build();

        // Build valid teacher
        validTeacher = TestDataBuilder.buildTeacher()
                .id(45L)
                .build();

        // Build sessions with time slots
        sessions = List.of(
                TestDataBuilder.buildSession().id(1L).classEntity(validClass).build(),
                TestDataBuilder.buildSession().id(2L).classEntity(validClass).build(),
                TestDataBuilder.buildSession().id(3L).classEntity(validClass).build()
        );

        // Full assignment request (all sessions)
        fullAssignmentRequest = new AssignTeacherRequest();
        fullAssignmentRequest.setTeacherId(45L);
        fullAssignmentRequest.setSessionIds(null); // null means all sessions

        // Partial assignment request (specific sessions)
        partialAssignmentRequest = new AssignTeacherRequest();
        partialAssignmentRequest.setTeacherId(45L);
        partialAssignmentRequest.setSessionIds(List.of(1L, 2L));
    }

    // ==================== Full Assignment Success Tests ====================

    @Test
    @DisplayName("Should validate full assignment request successfully")
    void shouldValidateFullAssignmentRequestSuccessfully() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Mock teacher has GENERAL skill (bypasses skill validation)
        TeacherSkill generalSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.GENERAL)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(generalSkill));

        // When & Then - Should not throw exception
        validator.validate(1L, fullAssignmentRequest);
    }

    @Test
    @DisplayName("Should validate full assignment with specific required skills")
    void shouldValidateFullAssignmentWithSpecificRequiredSkills() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Mock teacher has READING skill
        TeacherSkill ieltsSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.READING)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(ieltsSkill));

        // Mock class requires READING skill
        when(sessionRepository.findDistinctSkillNamesByClassId(1L)).thenReturn(List.of("READING"));

        // When & Then - Should not throw exception
        validator.validate(1L, fullAssignmentRequest);
    }

    // ==================== Partial Assignment Success Tests ====================

    @Test
    @DisplayName("Should validate partial assignment request successfully")
    void shouldValidatePartialAssignmentRequestSuccessfully() {
        // Given - Create sessions WITH time slots for partial assignment
        TimeSlotTemplate timeSlot = new TimeSlotTemplate();
        timeSlot.setId(1L);
        Session session1 = TestDataBuilder.buildSession().id(1L).classEntity(validClass).build();
        session1.setTimeSlotTemplate(timeSlot);
        Session session2 = TestDataBuilder.buildSession().id(2L).classEntity(validClass).build();
        session2.setTimeSlotTemplate(timeSlot);
        
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(session1, session2));

        // Mock teacher has GENERAL skill
        TeacherSkill generalSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.GENERAL)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(generalSkill));

        // When & Then - Should not throw exception
        validator.validate(1L, partialAssignmentRequest);
    }

    // ==================== Class Validation Error Tests ====================

    @Test
    @DisplayName("Should throw exception when class not found")
    void shouldThrowExceptionWhenClassNotFound() {
        // Given
        when(classRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> validator.validate(999L, fullAssignmentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLASS_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw exception when class is not in DRAFT status")
    void shouldThrowExceptionWhenClassIsNotInDraftStatus() {
        // Given
        validClass.setStatus(ClassStatus.SCHEDULED); // Not DRAFT
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, fullAssignmentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CLASS_INVALID_STATUS);
    }

    // ==================== Teacher Validation Error Tests ====================

    @Test
    @DisplayName("Should throw exception when teacher not found")
    void shouldThrowExceptionWhenTeacherNotFound() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(999L);

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEACHER_NOT_FOUND);
    }

    // ==================== Session Validation Error Tests ====================

    @Test
    @DisplayName("Should throw exception when session IDs contain duplicates")
    void shouldThrowExceptionWhenSessionIdsContainDuplicates() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(List.of(1L, 2L, 1L)); // Duplicate session ID 1

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_SESSION_IDS);
    }

    @Test
    @DisplayName("Should throw exception when session not found")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(sessions.get(0))); // Only 1 found, not 2

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(List.of(1L, 999L));

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw exception when session does not belong to class")
    void shouldThrowExceptionWhenSessionDoesNotBelongToClass() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));

        // Create session that belongs to different class
        ClassEntity otherClass = TestDataBuilder.buildClass().id(2L).build();
        Session sessionFromOtherClass = TestDataBuilder.buildSession()
                .id(999L)
                .classEntity(otherClass)
                .build();

        when(sessionRepository.findAllById(List.of(1L, 999L)))
                .thenReturn(List.of(sessions.get(0), sessionFromOtherClass));

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(List.of(1L, 999L));

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_IN_CLASS);
    }

    @Test
    @DisplayName("Should throw exception when session does not have time slot")
    void shouldThrowExceptionWhenSessionDoesNotHaveTimeSlot() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));

        // Create session without time slot
        Session sessionWithoutTimeSlot = TestDataBuilder.buildSession()
                .id(1L)
                .classEntity(validClass)
                .build();

        when(sessionRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(sessionWithoutTimeSlot));

        AssignTeacherRequest request = new AssignTeacherRequest();
        request.setTeacherId(45L);
        request.setSessionIds(List.of(1L));

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIME_SLOT_NOT_ASSIGNED);
    }

    @Test
    @DisplayName("Should throw exception when full assignment has sessions without time slots")
    void shouldThrowExceptionWhenFullAssignmentHasSessionsWithoutTimeSlots() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(5L); // 5 sessions without time slots

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, fullAssignmentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TIME_SLOT_NOT_ASSIGNED);
    }

    // ==================== Teacher Skill Validation Tests ====================

    @Test
    @DisplayName("Should bypass skill validation when teacher has GENERAL skill")
    void shouldBypassSkillValidationWhenTeacherHasGeneralSkill() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Teacher has GENERAL skill
        TeacherSkill generalSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.GENERAL)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(generalSkill));

        // Class requires READING (but GENERAL bypasses this check)
        when(sessionRepository.findDistinctSkillNamesByClassId(1L)).thenReturn(List.of("READING"));

        // When & Then - Should NOT throw exception (GENERAL skill bypasses)
        validator.validate(1L, fullAssignmentRequest);
    }

    @Test
    @DisplayName("Should throw exception when teacher lacks required skills")
    void shouldThrowExceptionWhenTeacherLacksRequiredSkills() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Teacher has only WRITING skill
        TeacherSkill toeflSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.WRITING)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(toeflSkill));

        // Class requires READING (teacher doesn't have it)
        when(sessionRepository.findDistinctSkillNamesByClassId(1L)).thenReturn(List.of("READING"));

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, fullAssignmentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEACHER_SKILL_MISMATCH);
    }

    @Test
    @DisplayName("Should validate successfully when teacher has all required skills")
    void shouldValidateSuccessfullyWhenTeacherHasAllRequiredSkills() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Teacher has READING and WRITING skills
        TeacherSkill ieltsSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.READING)
                .build();
        TeacherSkill toeflSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.WRITING)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(ieltsSkill, toeflSkill));

        // Class requires both READING and WRITING
        when(sessionRepository.findDistinctSkillNamesByClassId(1L)).thenReturn(List.of("READING", "WRITING"));

        // When & Then - Should not throw exception
        validator.validate(1L, fullAssignmentRequest);
    }

    @Test
    @DisplayName("Should throw exception when teacher has no skills and class requires skills")
    void shouldThrowExceptionWhenTeacherHasNoSkillsAndClassRequiresSkills() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Teacher has NO skills
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of());

        // Class requires READING
        when(sessionRepository.findDistinctSkillNamesByClassId(1L)).thenReturn(List.of("READING"));

        // When & Then
        assertThatThrownBy(() -> validator.validate(1L, fullAssignmentRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TEACHER_SKILL_MISMATCH);
    }

    @Test
    @DisplayName("Should validate successfully when class requires no skills")
    void shouldValidateSuccessfullyWhenClassRequiresNoSkills() {
        // Given
        when(classRepository.findById(1L)).thenReturn(Optional.of(validClass));
        when(teacherRepository.findById(45L)).thenReturn(Optional.of(validTeacher));
        when(sessionRepository.countSessionsWithoutTimeSlots(1L)).thenReturn(0L);

        // Teacher has READING skill
        TeacherSkill ieltsSkill = TestDataBuilder.buildTeacherSkill()
                .teacher(validTeacher)
                .skill(Skill.READING)
                .build();
        when(teacherSkillRepository.findByTeacherId(45L)).thenReturn(List.of(ieltsSkill));

        // Class requires NO skills (empty list)
        when(sessionRepository.findDistinctSkillNamesByClassId(1L)).thenReturn(List.of());

        // When & Then - Should not throw exception
        validator.validate(1L, fullAssignmentRequest);
    }
}
