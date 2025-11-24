package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignResourcesResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignSessionResourceRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignSessionResourceResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTeacherResponse;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsRequest;
import org.fyp.tmssep490be.dtos.createclass.AssignTimeSlotsResponse;
import org.fyp.tmssep490be.dtos.createclass.AvailableResourceDTO;
import org.fyp.tmssep490be.dtos.createclass.CreateClassRequest;
import org.fyp.tmssep490be.dtos.createclass.CreateClassResponse;
import org.fyp.tmssep490be.dtos.createclass.PreviewClassCodeResponse;
import org.fyp.tmssep490be.dtos.createclass.TeacherAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.TeacherDayAvailabilityDTO;
import org.fyp.tmssep490be.dtos.createclass.UpdateClassRequest;
// import org.fyp.tmssep490be.dtos.createclass.SubmitClassResponse; // Removed - now using classmanagement package
// import org.fyp.tmssep490be.dtos.createclass.ValidateClassResponse; // Removed - now using classmanagement package
import org.fyp.tmssep490be.dtos.classmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.ApprovalService;
import org.fyp.tmssep490be.services.ClassCodeGeneratorService;
import org.fyp.tmssep490be.services.ClassService;
import org.fyp.tmssep490be.services.ResourceAssignmentService;
import org.fyp.tmssep490be.services.SessionGenerationService;
import org.fyp.tmssep490be.services.TeacherAssignmentService;
import org.fyp.tmssep490be.services.ValidationService;
import org.fyp.tmssep490be.validators.AssignResourcesRequestValidator;
import org.fyp.tmssep490be.validators.AssignTeacherRequestValidator;
import org.fyp.tmssep490be.validators.CreateClassRequestValidator;
import org.fyp.tmssep490be.validators.AssignTimeSlotsRequestValidator;
import org.fyp.tmssep490be.utils.ValidateClassResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of ClassService for Academic Affairs class management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SessionRepository sessionRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentRepository studentRepository;
    private final ReplacementSkillAssessmentRepository skillAssessmentRepository;

    // Additional repositories for Create Class workflow
    private final BranchRepository branchRepository;
    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final SessionResourceRepository sessionResourceRepository;
    private final ResourceRepository resourceRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    // Services for Create Class workflow
    private final SessionGenerationService sessionGenerationService;
    private final ClassCodeGeneratorService classCodeGeneratorService;
    private final ResourceAssignmentService resourceAssignmentService;
    private final TeacherAssignmentService teacherAssignmentService;
    private final ValidationService validationService;
    private final ApprovalService approvalService;

    // Validators for Create Class workflow
    private final CreateClassRequestValidator createClassRequestValidator;
    private final AssignTimeSlotsRequestValidator assignTimeSlotsRequestValidator;
    private final AssignResourcesRequestValidator assignResourcesRequestValidator;
    private final AssignTeacherRequestValidator assignTeacherRequestValidator;
    private final ValidateClassResponseUtil validateClassResponseUtil;

    @Override
    public Page<ClassListItemDTO> getClasses(
            List<Long> branchIds,
            Long courseId,
            ClassStatus status,
            ApprovalStatus approvalStatus,
            Modality modality,
            String search,
            Pageable pageable,
            Long userId) {
        log.debug(
                "Getting classes for user {} with filters: branchIds={}, courseId={}, status={}, approvalStatus={}, modality={}, search={}",
                userId, branchIds, courseId, status, approvalStatus, modality, search);

        // Get user's branch access
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

        // Filter by provided branch IDs if any
        List<Long> finalBranchIds = branchIds != null ? branchIds : accessibleBranchIds;
        if (finalBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_NO_BRANCH_ACCESS);
        }

        // Query classes with filters (null status/approvalStatus = all)
        Page<ClassEntity> classes = classRepository.findClassesForAcademicAffairs(
                finalBranchIds,
                approvalStatus, // null = all approval statuses
                status, // null = all class statuses
                courseId,
                modality,
                search,
                pageable);

        return classes.map(this::convertToClassListItemDTO);
    }

    @Override
    public ClassDetailDTO getClassDetail(Long classId, Long userId) {
        log.debug("Getting class detail for class {} by user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch (no status restrictions for
        // detail view)
        validateClassBranchAccess(classEntity, userId);

        // Get enrollment summary
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        ClassDetailDTO.EnrollmentSummary enrollmentSummary = calculateEnrollmentSummary(
                currentEnrolled, classEntity.getMaxCapacity());

        // Get upcoming sessions (next 5)
        List<Session> upcomingSessions = sessionRepository.findUpcomingSessions(
                classId, PageRequest.of(0, 5));
        List<ClassDetailDTO.SessionDTO> sessionDTOs = upcomingSessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());

        // Get all teachers teaching this class
        List<TeacherSummaryDTO> teachers = getTeachersForClass(classId);

        return ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .course(convertToCourseDTO(classEntity.getCourse()))
                .branch(convertToBranchDTO(classEntity.getBranch()))
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .actualEndDate(classEntity.getActualEndDate())
                .scheduleDays(classEntity.getScheduleDays())
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .rejectionReason(classEntity.getRejectionReason())
                .submittedAt(classEntity.getSubmittedAt() != null ? classEntity.getSubmittedAt().toLocalDate() : null)
                .decidedAt(classEntity.getDecidedAt() != null ? classEntity.getDecidedAt().toLocalDate() : null)
                .decidedByName(classEntity.getDecidedBy() != null ? classEntity.getDecidedBy().getFullName() : null)
                .teachers(teachers)
                .scheduleSummary(formatScheduleSummary(classEntity.getScheduleDays()))
                .enrollmentSummary(enrollmentSummary)
                .upcomingSessions(sessionDTOs)
                .build();
    }

    @Override
    public Page<ClassStudentDTO> getClassStudents(
            Long classId,
            String search,
            Pageable pageable,
            Long userId) {
        log.debug("Getting students for class {} by user {} with search: {}", classId, userId, search);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch (with status restrictions for
        // enrollment operations)
        validateClassAccess(classEntity, userId);

        // Prepare search parameter with wildcards for LIKE query
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search + "%"
                : null;

        // Get enrolled students
        Page<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsByClass(
                classId, EnrollmentStatus.ENROLLED, searchPattern, pageable);

        return enrollments.map(this::convertToClassStudentDTO);
    }

    @Override
    public ClassEnrollmentSummaryDTO getClassEnrollmentSummary(Long classId, Long userId) {
        log.debug("Getting enrollment summary for class {} by user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch
        validateClassAccess(classEntity, userId);

        // Calculate enrollment data
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        Integer maxCapacity = classEntity.getMaxCapacity();
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        // Determine if enrollment is possible
        boolean canEnroll = (classEntity.getStatus() == ClassStatus.SCHEDULED
                || classEntity.getStatus() == ClassStatus.ONGOING)
                && classEntity.getApprovalStatus() == ApprovalStatus.APPROVED
                && availableSlots > 0;

        String restrictionReason = null;
        if (!canEnroll) {
            if (classEntity.getStatus() == ClassStatus.COMPLETED) {
                restrictionReason = "Class has completed";
            } else if (classEntity.getStatus() == ClassStatus.CANCELLED) {
                restrictionReason = "Class was cancelled";
            } else if (classEntity.getStatus() != ClassStatus.SCHEDULED
                    && classEntity.getStatus() != ClassStatus.ONGOING) {
                restrictionReason = "Class is not available for enrollment";
            } else if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
                restrictionReason = "Class is not approved";
            } else if (availableSlots <= 0) {
                restrictionReason = "Class is at full capacity";
            }
        }

        return ClassEnrollmentSummaryDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(restrictionReason)
                .status(classEntity.getStatus().name())
                .approvalStatus(classEntity.getApprovalStatus() != null
                        ? classEntity.getApprovalStatus().name()
                        : null)
                .startDate(classEntity.getStartDate())
                .build();
    }

    @Override
    public Page<AvailableStudentDTO> getAvailableStudentsForClass(
            Long classId,
            String search,
            Pageable pageable,
            Long userId) {
        log.debug("Getting available students for class {} by user {} with search: {}", classId, userId, search);

        // Validate class exists and user has access (with status restrictions for
        // enrollment operations)
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        validateClassAccess(classEntity, userId);

        // Get class details for skill assessment matching
        Long branchId = classEntity.getBranch().getId();
        Long classSubjectId = classEntity.getCourse().getLevel().getSubject().getId();
        Long classLevelId = classEntity.getCourse().getLevel().getId();

        // Hybrid approach: Get ALL available students without pagination for proper
        // sorting
        List<Student> allAvailableStudents = studentRepository.findAllAvailableStudentsForClass(
                classId, branchId, search);

        // Batch fetch ALL skill assessments for all students
        List<Long> studentIds = allAvailableStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        List<ReplacementSkillAssessment> allAssessments = studentIds.isEmpty() ? List.of()
                : skillAssessmentRepository.findByStudentIdIn(studentIds);

        // Group assessments by student ID
        Map<Long, List<ReplacementSkillAssessment>> assessmentsByStudent = allAssessments.stream()
                .collect(Collectors.groupingBy(assessment -> assessment.getStudent().getId()));

        // Convert to DTOs with complete assessment data and sort by matchPriority
        List<AvailableStudentDTO> allDtos = allAvailableStudents.stream()
                .map(student -> convertToAvailableStudentDTO(
                        student,
                        assessmentsByStudent.get(student.getId()),
                        classSubjectId,
                        classLevelId))
                .sorted((dto1, dto2) -> {
                    // Sort by matchPriority (ascending: 1, 2, 3)
                    // Then by student name if same priority
                    int priorityCompare = dto1.getClassMatchInfo().getMatchPriority()
                            .compareTo(dto2.getClassMatchInfo().getMatchPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    return dto1.getFullName().compareTo(dto2.getFullName());
                })
                .collect(Collectors.toList());

        // Apply pagination manually on sorted list
        int start = pageable.getPageNumber() * pageable.getPageSize();
        int end = Math.min(start + pageable.getPageSize(), allDtos.size());
        List<AvailableStudentDTO> paginatedDtos = start < allDtos.size() ? allDtos.subList(start, end) : List.of();

        // Create Page with correct pagination metadata
        return new org.springframework.data.domain.PageImpl<>(
                paginatedDtos,
                pageable,
                allDtos.size());
    }

    // Helper methods

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    /**
     * Validate user can access class from their branch only (no status
     * restrictions)
     * Used for read-only operations like viewing class details
     */
    private void validateClassBranchAccess(ClassEntity classEntity, Long userId) {
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

        if (!accessibleBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }
    }

    /**
     * Validate user can access class with status restrictions
     * Used for enrollment-related operations (viewing students, enrolling new
     * students)
     */
    private void validateClassAccess(ClassEntity classEntity, Long userId) {
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

        if (!accessibleBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }

        if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.CLASS_NOT_APPROVED_FOR_ENROLLMENT);
        }

        // Allow enrollment-related operations for SCHEDULED and ONGOING classes
        if (classEntity.getStatus() != ClassStatus.SCHEDULED
                && classEntity.getStatus() != ClassStatus.ONGOING) {
            throw new CustomException(ErrorCode.CLASS_NOT_SCHEDULED);
        }
    }

    private ClassListItemDTO convertToClassListItemDTO(ClassEntity classEntity) {
        // Get enrollment data
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                classEntity.getId(), EnrollmentStatus.ENROLLED);
        Integer maxCapacity = classEntity.getMaxCapacity();
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        // Get all teachers teaching this class
        List<TeacherSummaryDTO> teachers = getTeachersForClass(classEntity.getId());

        // Determine if enrollment is possible
        boolean canEnroll = availableSlots > 0
                && (classEntity.getStatus() == ClassStatus.SCHEDULED
                        || classEntity.getStatus() == ClassStatus.ONGOING)
                && classEntity.getApprovalStatus() == ApprovalStatus.APPROVED;

        return ClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .courseCode(classEntity.getCourse().getCode())
                .branchName(classEntity.getBranch().getName())
                .branchCode(classEntity.getBranch().getCode())
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .maxCapacity(maxCapacity)
                .currentEnrolled(currentEnrolled)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .teachers(teachers)
                .scheduleSummary(formatScheduleSummary(classEntity.getScheduleDays()))
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(canEnroll ? null
                        : availableSlots <= 0 ? "Class is full"
                                : classEntity.getStatus() == ClassStatus.COMPLETED ? "Class has completed"
                                        : classEntity.getStatus() == ClassStatus.CANCELLED ? "Class was cancelled"
                                                : "Class not available for enrollment")
                .build();
    }

    private ClassStudentDTO convertToClassStudentDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        UserAccount userAccount = student.getUserAccount();

        return ClassStudentDTO.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .branchName(student.getUserAccount().getUserBranches().iterator().next().getBranch().getName())
                .enrolledAt(enrollment.getEnrolledAt())
                .enrolledBy(enrollment.getEnrolledByUser() != null ? enrollment.getEnrolledByUser().getFullName()
                        : "System")
                .enrolledById(enrollment.getEnrolledBy())
                .status(enrollment.getStatus())
                .joinSessionId(enrollment.getJoinSessionId())
                .joinSessionDate(
                        enrollment.getJoinSession() != null ? enrollment.getJoinSession().getDate().toString() : null)
                .capacityOverride(enrollment.getCapacityOverride())
                .overrideReason(enrollment.getOverrideReason())
                .build();
    }

    private ClassDetailDTO.CourseDTO convertToCourseDTO(Course course) {
        return ClassDetailDTO.CourseDTO.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .totalHours(course.getTotalHours())
                .durationWeeks(course.getDurationWeeks())
                .sessionPerWeek(course.getSessionPerWeek())
                .build();
    }

    private ClassDetailDTO.BranchDTO convertToBranchDTO(Branch branch) {
        return ClassDetailDTO.BranchDTO.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .build();
    }

    private ClassDetailDTO.SessionDTO convertToSessionDTO(Session session) {
        return ClassDetailDTO.SessionDTO.builder()
                .id(session.getId())
                .date(session.getDate())
                .startTime(
                        session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getStartTime().toString()
                                : null)
                .endTime(session.getTimeSlotTemplate() != null ? session.getTimeSlotTemplate().getEndTime().toString()
                        : null)
                .teachers(getTeachersForSession(session))
                .room(null) // Room info not available in current entity structure
                .status(session.getStatus().name())
                .type(session.getType().name())
                .build();
    }

    private ClassDetailDTO.EnrollmentSummary calculateEnrollmentSummary(Integer currentEnrolled, Integer maxCapacity) {
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        boolean canEnroll = availableSlots > 0;
        String restrictionReason = canEnroll ? null : "Class is at full capacity";

        return ClassDetailDTO.EnrollmentSummary.builder()
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(restrictionReason)
                .build();
    }

    /**
     * Get all teachers teaching in a class with their session counts
     * Groups by teacher and counts how many sessions each teacher teaches
     */
    private List<TeacherSummaryDTO> getTeachersForClass(Long classId) {
        List<TeachingSlot> teachingSlots = teachingSlotRepository
                .findByClassEntityIdAndStatus(classId, TeachingSlotStatus.SCHEDULED);

        // Group by teacher and count sessions
        Map<Teacher, Long> teacherSessionCounts = teachingSlots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(
                        TeachingSlot::getTeacher,
                        Collectors.counting()));

        // Convert to DTOs sorted by session count (descending)
        return teacherSessionCounts.entrySet().stream()
                .map(entry -> {
                    Teacher teacher = entry.getKey();
                    UserAccount userAccount = teacher.getUserAccount();
                    return TeacherSummaryDTO.builder()
                            .id(userAccount.getId())
                            .teacherId(teacher.getId())
                            .fullName(userAccount.getFullName())
                            .email(userAccount.getEmail())
                            .phone(userAccount.getPhone())
                            .employeeCode(teacher.getEmployeeCode())
                            .sessionCount(entry.getValue().intValue())
                            .build();
                })
                .sorted(Comparator.comparing(TeacherSummaryDTO::getSessionCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all teachers teaching in a specific session
     */
    private List<TeacherSummaryDTO> getTeachersForSession(Session session) {
        return session.getTeachingSlots().stream()
                .filter(slot -> slot.getStatus() == TeachingSlotStatus.SCHEDULED)
                .filter(slot -> slot.getTeacher() != null)
                .map(slot -> {
                    Teacher teacher = slot.getTeacher();
                    UserAccount userAccount = teacher.getUserAccount();
                    return TeacherSummaryDTO.builder()
                            .id(userAccount.getId())
                            .teacherId(teacher.getId())
                            .fullName(userAccount.getFullName())
                            .email(userAccount.getEmail())
                            .phone(userAccount.getPhone())
                            .employeeCode(teacher.getEmployeeCode())
                            .sessionCount(1) // Single session context
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String formatScheduleSummary(Short[] scheduleDays) {
        if (scheduleDays == null || scheduleDays.length == 0) {
            return "Not specified";
        }

        String[] dayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        return Arrays.stream(scheduleDays)
                .filter(day -> day != null && day >= 1 && day <= 7)
                .map(day -> dayNames[day - 1])
                .collect(Collectors.joining(", "));
    }

    private AvailableStudentDTO convertToAvailableStudentDTO(
            Student student,
            List<ReplacementSkillAssessment> assessments,
            Long classSubjectId,
            Long classLevelId) {
        return convertToAvailableStudentDTO(student, assessments, classSubjectId, classLevelId, null, null);
    }

    /**
     * Convert Student to AvailableStudentDTO with assessment data
     * Overloaded method that accepts pre-computed match priority for database-first
     * approach
     */
    private AvailableStudentDTO convertToAvailableStudentDTO(
            Student student,
            List<ReplacementSkillAssessment> assessments,
            Long classSubjectId,
            Long classLevelId,
            Integer preComputedMatchPriority,
            String preComputedMatchingSkill) {
        UserAccount userAccount = student.getUserAccount();

        // Get branch info (take first branch)
        String branchName = null;
        Long branchId = null;
        if (!userAccount.getUserBranches().isEmpty()) {
            branchId = userAccount.getUserBranches().iterator().next().getBranch().getId();
            branchName = userAccount.getUserBranches().iterator().next().getBranch().getName();
        }

        // Convert all assessments to DTOs
        List<AvailableStudentDTO.SkillAssessmentDTO> assessmentDTOs = assessments != null ? assessments.stream()
                .map(this::convertToSkillAssessmentDTO)
                .sorted((a1, a2) -> a2.getAssessmentDate().compareTo(a1.getAssessmentDate())) // Most recent first
                .collect(Collectors.toList()) : List.of();

        // Use pre-computed match priority if provided, otherwise calculate it
        AvailableStudentDTO.ClassMatchInfoDTO classMatchInfo;
        if (preComputedMatchPriority != null) {
            // Use database-computed values for better performance and consistency
            ReplacementSkillAssessment matchingAssessment = findMatchingAssessment(assessments, classSubjectId,
                    classLevelId);
            classMatchInfo = AvailableStudentDTO.ClassMatchInfoDTO.builder()
                    .matchPriority(preComputedMatchPriority)
                    .matchingSkill(preComputedMatchingSkill)
                    .matchingLevel(matchingAssessment != null && matchingAssessment.getLevel() != null
                            ? convertToLevelInfoDTO(matchingAssessment.getLevel())
                            : null)
                    .matchReason(getMatchReason(preComputedMatchPriority))
                    .build();
        } else {
            // Legacy calculation method (kept for backward compatibility)
            ReplacementSkillAssessment matchingAssessment = findMatchingAssessment(assessments, classSubjectId,
                    classLevelId);
            classMatchInfo = calculateClassMatchInfo(matchingAssessment, classSubjectId, classLevelId);
        }

        // Get active enrollments count
        int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                student.getId(), EnrollmentStatus.ENROLLED);
        boolean canEnroll = activeEnrollments < 3; // Max concurrent enrollments

        return AvailableStudentDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .branchId(branchId)
                .branchName(branchName)
                .replacementSkillAssessments(assessmentDTOs)
                .classMatchInfo(classMatchInfo)
                .activeEnrollments(activeEnrollments)
                .canEnroll(canEnroll)
                .build();
    }

    private ReplacementSkillAssessment findMatchingAssessment(
            List<ReplacementSkillAssessment> assessments,
            Long classSubjectId,
            Long classLevelId) {
        if (assessments == null || assessments.isEmpty()) {
            return null;
        }

        // Find assessments matching the class subject
        List<ReplacementSkillAssessment> subjectMatches = assessments.stream()
                .filter(a -> a.getLevel() != null &&
                        a.getLevel().getSubject() != null &&
                        a.getLevel().getSubject().getId().equals(classSubjectId))
                .collect(Collectors.toList());

        if (subjectMatches.isEmpty()) {
            return null;
        }

        // Find perfect match (subject + level)
        for (ReplacementSkillAssessment assessment : subjectMatches) {
            if (assessment.getLevel().getId().equals(classLevelId)) {
                return assessment;
            }
        }

        // Return first subject match as partial match
        return subjectMatches.get(0);
    }

    private AvailableStudentDTO.ClassMatchInfoDTO calculateClassMatchInfo(
            ReplacementSkillAssessment matchingAssessment,
            Long classSubjectId,
            Long classLevelId) {
        Integer matchPriority = 3; // Default: No match
        String matchingSkill = null;
        AvailableStudentDTO.LevelInfoDTO matchingLevel = null;
        String matchReason = "No skill assessment found for this course's subject";

        if (matchingAssessment != null && matchingAssessment.getLevel() != null) {
            Long assessmentSubjectId = matchingAssessment.getLevel().getSubject().getId();
            Long assessmentLevelId = matchingAssessment.getLevel().getId();

            if (assessmentSubjectId.equals(classSubjectId)) {
                matchingSkill = matchingAssessment.getSkill().name();
                matchingLevel = convertToLevelInfoDTO(matchingAssessment.getLevel());

                if (assessmentLevelId.equals(classLevelId)) {
                    // Perfect match: Same subject AND same level
                    matchPriority = 1;
                    matchReason = "Perfect match - Assessment matches both Subject AND Level";
                } else {
                    // Partial match: Same subject, different level
                    matchPriority = 2;
                    matchReason = "Partial match - Assessment matches Subject only";
                }
            }
        }

        return AvailableStudentDTO.ClassMatchInfoDTO.builder()
                .matchPriority(matchPriority)
                .matchingSkill(matchingSkill)
                .matchingLevel(matchingLevel)
                .matchReason(matchReason)
                .build();
    }

    /**
     * Get match reason based on priority level
     */
    private String getMatchReason(Integer matchPriority) {
        return switch (matchPriority) {
            case 1 -> "Perfect match - Assessment matches both Subject AND Level";
            case 2 -> "Partial match - Assessment matches Subject only";
            case 3 -> "No skill assessment found for this course's subject";
            default -> "Unknown match priority";
        };
    }

    private AvailableStudentDTO.SkillAssessmentDTO convertToSkillAssessmentDTO(ReplacementSkillAssessment assessment) {
        return AvailableStudentDTO.SkillAssessmentDTO.builder()
                .id(assessment.getId())
                .skill(assessment.getSkill().name())
                .level(convertToLevelInfoDTO(assessment.getLevel()))
                .score(assessment.getScaledScore() != null ? assessment.getScaledScore().intValue() : 0)
                .assessmentDate(assessment.getAssessmentDate())
                .assessmentType(assessment.getAssessmentType())
                .note(assessment.getNote())
                .assessedBy(convertToAssessorDTO(assessment.getAssessedBy()))
                .build();
    }

    private AvailableStudentDTO.LevelInfoDTO convertToLevelInfoDTO(Level level) {
        if (level == null) {
            return null;
        }

        return AvailableStudentDTO.LevelInfoDTO.builder()
                .id(level.getId())
                .code(level.getCode())
                .name(level.getName())
                .subject(convertToSubjectInfoDTO(level.getSubject()))
                .expectedDurationHours(level.getExpectedDurationHours())
                .description(level.getDescription())
                .build();
    }

    private AvailableStudentDTO.SubjectInfoDTO convertToSubjectInfoDTO(Subject subject) {
        if (subject == null) {
            return null;
        }

        return AvailableStudentDTO.SubjectInfoDTO.builder()
                .id(subject.getId())
                .name(subject.getName())
                .build();
    }

    private AvailableStudentDTO.AssessorDTO convertToAssessorDTO(UserAccount assessor) {
        if (assessor == null) {
            return null;
        }

        return AvailableStudentDTO.AssessorDTO.builder()
                .id(assessor.getId())
                .fullName(assessor.getFullName())
                .build();
    }

    // Create Class Workflow implementations (STEP 0 - Optional, STEP 1, 3, 6, 7)

    @Override
    @Transactional(readOnly = true)
    public PreviewClassCodeResponse previewClassCode(Long branchId, Long courseId, LocalDate startDate, Long userId) {
        log.info("Previewing class code for branchId: {}, courseId: {}, startDate: {}", branchId, courseId, startDate);

        // Validate inputs
        if (branchId == null || courseId == null || startDate == null || userId == null) {
            log.error("Invalid input: branchId={}, courseId={}, startDate={}, userId={}",
                    branchId, courseId, startDate, userId);
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // Get entities
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> {
                    log.error("Branch not found with ID: {}", branchId);
                    return new CustomException(ErrorCode.BRANCH_NOT_FOUND);
                });

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> {
                    log.error("Course not found with ID: {}", courseId);
                    return new CustomException(ErrorCode.COURSE_NOT_FOUND);
                });

        log.debug("Found branch: code={}, course: code={}", branch.getCode(), course.getCode());

        // Call the code generator service to preview
        String previewCode = classCodeGeneratorService.previewClassCode(
                branchId,
                branch.getCode(),
                course.getCode(),
                startDate);

        // Extract prefix and sequence from preview code
        String normalizedCourseCode = classCodeGeneratorService.normalizeCourseCode(course.getCode());
        int year = startDate.getYear();
        String prefix = classCodeGeneratorService.buildPrefix(normalizedCourseCode, branch.getCode(), year);

        // Extract sequence number
        int sequence = Integer.parseInt(previewCode.substring(previewCode.lastIndexOf('-') + 1));

        // Build response with warning if sequence is getting high
        String warning = null;
        if (sequence >= 990) {
            warning = "Sequence number is approaching limit (999). Consider using a different course or branch.";
        }

        return PreviewClassCodeResponse.builder()
                .previewCode(previewCode)
                .prefix(prefix)
                .nextSequence(sequence)
                .warning(warning)
                .build();
    }

    @Override
    @Transactional
    public CreateClassResponse createClass(CreateClassRequest request, Long userId) {
        log.info("Creating new class with code: {} for user ID: {}", request.getCode(), userId);

        // Validate request
        validateCreateClassRequest(request, userId);

        // Get entities
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        UserAccount createdBy = getUserAccount(userId);

        // Validate business rules
        validateCreateClassBusinessRules(request, branch, course, userId);

        // Auto-generate class code if not provided
        String classCode = request.getCode();
        if (classCode == null || classCode.trim().isEmpty()) {
            log.info("Class code not provided - auto-generating");
            classCode = classCodeGeneratorService.generateClassCode(
                    branch.getId(),
                    branch.getCode(),
                    course.getCode(),
                    request.getStartDate());
            log.info("Auto-generated class code: {}", classCode);
        }

        // Create class entity
        ClassEntity classEntity = ClassEntity.builder()
                .branch(branch)
                .course(course)
                .code(classCode)
                .name(request.getName())
                .modality(request.getModality())
                .startDate(request.getStartDate())
                .scheduleDays(request.getScheduleDays().toArray(new Short[0]))
                .maxCapacity(request.getMaxCapacity())
                .status(ClassStatus.DRAFT)
                .approvalStatus(null) // chỉ đặt PENDING khi submit
                .createdBy(createdBy)
                .createdAt(java.time.OffsetDateTime.now())
                .updatedAt(java.time.OffsetDateTime.now())
                .build();

        // Save class first to get ID
        classEntity = classRepository.save(classEntity);
        log.info("Created class entity with ID: {}", classEntity.getId());

        // Generate sessions (STEP 2 - auto-triggered)
        List<Session> sessions = sessionGenerationService.generateSessionsForClass(classEntity, course);
        List<Session> savedSessions = sessionRepository.saveAll(sessions);

        // Calculate end date
        LocalDate endDate = sessionGenerationService.calculateEndDate(savedSessions);
        classEntity.setPlannedEndDate(endDate);
        classEntity = classRepository.save(classEntity);

        log.info("Generated and saved {} sessions for class {}", savedSessions.size(), classEntity.getCode());

        // Build response
        CreateClassResponse.SessionGenerationSummary sessionSummary = CreateClassResponse.SessionGenerationSummary
                .builder()
                .sessionsGenerated(savedSessions.size())
                .totalSessionsInCourse(sessions.size())
                .courseCode(course.getCode())
                .courseName(course.getName())
                .startDate(classEntity.getStartDate())
                .endDate(endDate)
                .scheduleDays(classEntity.getScheduleDays())
                .build();

        return CreateClassResponse.builder()
                .classId(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .createdAt(classEntity.getCreatedAt())
                .sessionSummary(sessionSummary)
                .build();
    }

    @Override
    @Transactional
    public CreateClassResponse updateClass(Long classId, UpdateClassRequest request, Long userId) {
        log.info("Updating class {} by user {}", classId, userId);

        // Fetch existing class with access check
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }

        if (classEntity.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.CLASS_ALREADY_APPROVED);
        }

        boolean isSubmittedPendingDraft = classEntity.getStatus() == ClassStatus.DRAFT
                && classEntity.getApprovalStatus() == ApprovalStatus.PENDING;
        if (isSubmittedPendingDraft) {
            throw new CustomException(ErrorCode.CLASS_NOT_EDITABLE);
        }
        if (classEntity.getStatus() == ClassStatus.SUBMITTED) {
            throw new CustomException(ErrorCode.CLASS_NOT_EDITABLE);
        }

        boolean editable = (classEntity.getStatus() == ClassStatus.DRAFT
                && classEntity.getApprovalStatus() != ApprovalStatus.PENDING)
                || classEntity.getApprovalStatus() == ApprovalStatus.REJECTED;
        if (!editable) {
            throw new CustomException(ErrorCode.CLASS_NOT_EDITABLE);
        }

        // Tự động bật regenerate nếu dữ liệu lịch thay đổi
        boolean scheduleChanged = !Objects.equals(request.getBranchId(), classEntity.getBranch().getId())
                || !Objects.equals(request.getCourseId(), classEntity.getCourse().getId())
                || request.getStartDate() != null && !request.getStartDate().equals(classEntity.getStartDate())
                || request.getScheduleDays() != null && !Arrays.equals(
                        request.getScheduleDays().toArray(new Short[0]),
                        classEntity.getScheduleDays())
                || request.getModality() != null && request.getModality() != classEntity.getModality();
        if (scheduleChanged) {
            request.setRegenerateSessions(true);
        }

        // Default plannedEndDate nếu không gửi lên:
        // - nếu scheduleChanged/regenerate: cho phép null, sẽ tính lại sau khi regenerate
        // - nếu không đổi lịch: dùng giá trị hiện có
        if (request.getPlannedEndDate() == null) {
            if (scheduleChanged || Boolean.TRUE.equals(request.getRegenerateSessions())) {
                request.setPlannedEndDate(classEntity.getStartDate()); // placeholder để qua validation
            } else {
                request.setPlannedEndDate(classEntity.getPlannedEndDate());
            }
        }

        // Basic validation reuse (sau khi đã set plannedEndDate placeholder nếu cần)
        validateCreateClassRequest(mapToCreateRequest(request), userId);

        if (request.getPlannedEndDate() != null && request.getStartDate().isAfter(request.getPlannedEndDate())) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        if (!userBranchIds.contains(branch.getId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }
        if (branch.getStatus() == BranchStatus.INACTIVE) {
            throw new CustomException(ErrorCode.BRANCH_NOT_FOUND);
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));
        if (course.getApprovalStatus() != ApprovalStatus.APPROVED || course.getStatus() == CourseStatus.INACTIVE) {
            throw new CustomException(ErrorCode.COURSE_NOT_APPROVED);
        }

        // Capacity guard
        int enrolledCount = Optional.ofNullable(classRepository.countEnrolledStudents(classId)).orElse(0);
        if (request.getMaxCapacity() < enrolledCount) {
            throw new CustomException(ErrorCode.CLASS_CAPACITY_TOO_LOW);
        }

        // Code uniqueness check (skip if unchanged or blank)
        String newCode = request.getCode() != null && !request.getCode().isBlank()
                ? request.getCode()
                : classEntity.getCode();
        Optional<ClassEntity> conflictingClass = classRepository.findByBranchIdAndCode(branch.getId(), newCode);
        if (conflictingClass.isPresent() && !conflictingClass.get().getId().equals(classEntity.getId())) {
            throw new CustomException(ErrorCode.CLASS_CODE_DUPLICATE);
        }

        // Apply updates
        classEntity.setBranch(branch);
        classEntity.setCourse(course);
        classEntity.setCode(newCode);
        classEntity.setName(request.getName());
        classEntity.setModality(request.getModality());
        classEntity.setStartDate(request.getStartDate());
        classEntity.setPlannedEndDate(request.getPlannedEndDate());
        classEntity.setMaxCapacity(request.getMaxCapacity());
        classEntity.setScheduleDays(request.getScheduleDays().toArray(new Short[0]));
        classEntity.setUpdatedAt(java.time.OffsetDateTime.now());

        CreateClassResponse.SessionGenerationSummary sessionSummary;
        if (Boolean.TRUE.equals(request.getRegenerateSessions())) {
            sessionSummary = regenerateSessions(classEntity, course);
        } else {
            classRepository.save(classEntity);
            sessionSummary = buildSessionSummary(classEntity, course);
        }

        return CreateClassResponse.builder()
                .classId(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .createdAt(classEntity.getCreatedAt())
                .sessionSummary(sessionSummary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public org.fyp.tmssep490be.dtos.createclass.SessionListResponse listSessions(Long classId, Long userId) {
        log.info("Listing sessions for class ID {} by user {}", classId, userId);

        // Get class and validate access
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to class's branch (skip if userId is null -
        // authentication disabled)
        if (userId != null) {
            List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
            if (!userBranchIds.contains(classEntity.getBranch().getId())) {
                throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
            }
        }

        // Get all sessions ordered by date
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        if (sessions.isEmpty()) {
            log.warn("No sessions found for class ID {}", classId);
            return org.fyp.tmssep490be.dtos.createclass.SessionListResponse.builder()
                    .classId(classId)
                    .classCode(classEntity.getCode())
                    .totalSessions(0)
                    .sessions(List.of())
                    .groupedByWeek(List.of())
                    .warnings(List.of())
                    .build();
        }

        // Calculate date range
        LocalDate startDate = sessions.get(0).getDate();
        LocalDate endDate = sessions.get(sessions.size() - 1).getDate();

        // Build session DTOs
        List<org.fyp.tmssep490be.dtos.createclass.SessionListResponse.SessionDTO> sessionDTOs = new java.util.ArrayList<>();
        int sequenceNumber = 1;

        for (Session session : sessions) {
            // Check if session has assignments
            boolean hasTimeSlot = session.getTimeSlotTemplate() != null;
            boolean hasResource = sessionResourceRepository.existsBySessionId(session.getId());
            boolean hasTeacher = teachingSlotRepository.existsBySessionId(session.getId());

            // Build time slot info if available
            org.fyp.tmssep490be.dtos.createclass.SessionListResponse.TimeSlotInfoDTO timeSlotInfo = null;
            Long timeSlotTemplateId = null;
            if (hasTimeSlot && session.getTimeSlotTemplate() != null) {
                TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
                timeSlotTemplateId = timeSlot.getId();
                if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
                    timeSlotInfo = org.fyp.tmssep490be.dtos.createclass.SessionListResponse.TimeSlotInfoDTO.builder()
                            .startTime(timeSlot.getStartTime().toString())
                            .endTime(timeSlot.getEndTime().toString())
                            .displayName(timeSlot.getStartTime() + " - " + timeSlot.getEndTime())
                            .build();
                }
            }

            // Get resource name
            String resourceName = null;
            Long resourceId = null;
            if (hasResource) {
                List<SessionResource> sessionResources = sessionResourceRepository.findBySessionId(session.getId());
                if (!sessionResources.isEmpty()) {
                    // Get first resource name (usually only one resource per session)
                    Resource resource = sessionResources.get(0).getResource();
                    if (resource != null) {
                        resourceName = resource.getName();
                        resourceId = resource.getId();
                    }
                }
            }

            // Get teachers assigned to this session
            List<org.fyp.tmssep490be.dtos.createclass.SessionListResponse.TeacherInfoDTO> teacherInfos = new java.util.ArrayList<>();
            String teacherName = null;
            if (hasTeacher) {
                List<TeachingSlot> teachingSlots = teachingSlotRepository.findBySessionIdAndStatus(
                        session.getId(), TeachingSlotStatus.SCHEDULED);

                teacherInfos = teachingSlots.stream()
                        .filter(slot -> slot.getTeacher() != null)
                        .map(slot -> {
                            Teacher teacher = slot.getTeacher();
                            return org.fyp.tmssep490be.dtos.createclass.SessionListResponse.TeacherInfoDTO.builder()
                                    .teacherId(teacher.getId())
                                    .fullName(teacher.getUserAccount().getFullName())
                                    .employeeCode(teacher.getEmployeeCode())
                                    .build();
                        })
                        .collect(Collectors.toList());

                // Create comma-separated teacher name string
                if (!teacherInfos.isEmpty()) {
                    teacherName = teacherInfos.stream()
                            .map(org.fyp.tmssep490be.dtos.createclass.SessionListResponse.TeacherInfoDTO::getFullName)
                            .collect(Collectors.joining(", "));
                }
            }

            // Get day of week name
            String dayOfWeek = getDayName(session.getDate().getDayOfWeek().getValue());

            sessionDTOs.add(org.fyp.tmssep490be.dtos.createclass.SessionListResponse.SessionDTO.builder()
                    .sessionId(session.getId())
                    .sequenceNumber(sequenceNumber++)
                    .date(session.getDate())
                    .dayOfWeek(dayOfWeek)
                    .dayOfWeekNumber((short) session.getDate().getDayOfWeek().getValue())
                    .courseSessionName(
                            session.getCourseSession() != null ? session.getCourseSession().getTopic() : "Unknown")
                    .status(session.getStatus().name())
                    .hasTimeSlot(hasTimeSlot)
                    .hasResource(hasResource)
                    .hasTeacher(hasTeacher)
                    .timeSlotTemplateId(timeSlotTemplateId)
                    .timeSlotInfo(timeSlotInfo)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .teacherName(teacherName)
                    .teachers(teacherInfos)
                    .teacherIds(teacherInfos.stream()
                            .map(org.fyp.tmssep490be.dtos.createclass.SessionListResponse.TeacherInfoDTO::getTeacherId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()))
                    .build());
        }

        // Group sessions by week
        List<org.fyp.tmssep490be.dtos.createclass.SessionListResponse.WeekGroupDTO> weekGroups = groupSessionsByWeek(
                sessions);

        // Build response
        return org.fyp.tmssep490be.dtos.createclass.SessionListResponse.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .totalSessions(sessions.size())
                .dateRange(org.fyp.tmssep490be.dtos.createclass.SessionListResponse.DateRangeDTO.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .build())
                .sessions(sessionDTOs)
                .groupedByWeek(weekGroups)
                .warnings(List.of()) // Future: add holiday warnings
                .build();
    }

    /**
     * Group sessions by week for better frontend display
     */
    private List<org.fyp.tmssep490be.dtos.createclass.SessionListResponse.WeekGroupDTO> groupSessionsByWeek(
            List<Session> sessions) {
        List<org.fyp.tmssep490be.dtos.createclass.SessionListResponse.WeekGroupDTO> weekGroups = new java.util.ArrayList<>();

        if (sessions.isEmpty()) {
            return weekGroups;
        }

        LocalDate firstDate = sessions.get(0).getDate();
        int weekNumber = 1;
        LocalDate currentWeekStart = firstDate;
        List<Long> currentWeekSessionIds = new java.util.ArrayList<>();

        for (Session session : sessions) {
            LocalDate sessionDate = session.getDate();

            // Check if session is in next week (7+ days from current week start)
            if (sessionDate.isAfter(currentWeekStart.plusDays(6))) {
                // Save current week group
                if (!currentWeekSessionIds.isEmpty()) {
                    LocalDate weekEnd = currentWeekStart.plusDays(6);
                    weekGroups.add(org.fyp.tmssep490be.dtos.createclass.SessionListResponse.WeekGroupDTO.builder()
                            .weekNumber(weekNumber++)
                            .weekRange(formatDateRange(currentWeekStart, weekEnd))
                            .sessionCount(currentWeekSessionIds.size())
                            .sessionIds(new java.util.ArrayList<>(currentWeekSessionIds))
                            .build());
                }

                // Start new week
                currentWeekStart = sessionDate;
                currentWeekSessionIds = new java.util.ArrayList<>();
            }

            currentWeekSessionIds.add(session.getId());
        }

        // Add last week group
        if (!currentWeekSessionIds.isEmpty()) {
            LocalDate weekEnd = sessions.get(sessions.size() - 1).getDate();
            weekGroups.add(org.fyp.tmssep490be.dtos.createclass.SessionListResponse.WeekGroupDTO.builder()
                    .weekNumber(weekNumber)
                    .weekRange(formatDateRange(currentWeekStart, weekEnd))
                    .sessionCount(currentWeekSessionIds.size())
                    .sessionIds(currentWeekSessionIds)
                    .build());
        }

        return weekGroups;
    }

    /**
     * Format date range for display
     */
    private String formatDateRange(LocalDate start, LocalDate end) {
        return start.toString() + " - " + end.toString();
    }

    /**
     * Get Vietnamese day name from day of week number
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

    @Override
    @Transactional
    public AssignTimeSlotsResponse assignTimeSlots(Long classId, AssignTimeSlotsRequest request, Long userId) {
        log.info("Assigning time slots for class ID {} by user {}", classId, userId);

        // Validate request
        if (!assignTimeSlotsRequestValidator.isValid(request)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (assignTimeSlotsRequestValidator.hasDuplicateDays(request)) {
            Short duplicateDay = assignTimeSlotsRequestValidator.getDuplicateDay(request);
            throw new CustomException(ErrorCode.DUPLICATE_TIME_SLOT_ASSIGNMENT);
        }

        // Get class and validate access
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to class's branch
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }

        // Get total sessions count
        int totalSessions = (int) sessionRepository.countByClassEntityId(classId);
        if (totalSessions == 0) {
            throw new CustomException(ErrorCode.NO_SESSIONS_FOUND_FOR_CLASS);
        }

        List<AssignTimeSlotsResponse.AssignmentDetail> assignmentDetails = new ArrayList<>();
        int totalSessionsUpdated = 0;

        // Process each time slot assignment
        for (AssignTimeSlotsRequest.TimeSlotAssignment assignment : request.getAssignments()) {
            try {
                // Validate time slot exists and belongs to class's branch
                TimeSlotTemplate timeSlot = timeSlotTemplateRepository.findById(assignment.getTimeSlotTemplateId())
                        .orElseThrow(() -> new CustomException(ErrorCode.TIME_SLOT_NOT_FOUND));

                if (!timeSlot.getBranch().getId().equals(classEntity.getBranch().getId())) {
                    throw new CustomException(ErrorCode.TIME_SLOT_NOT_IN_BRANCH);
                }

                // Update sessions for this day of week
                int sessionsUpdated = sessionRepository.updateTimeSlotByDayOfWeek(
                        classId,
                        assignment.getDayOfWeek().intValue(),
                        assignment.getTimeSlotTemplateId());

                // Create assignment detail
                AssignTimeSlotsResponse.AssignmentDetail detail = AssignTimeSlotsResponse.AssignmentDetail.builder()
                        .dayOfWeek(assignment.getDayOfWeek())
                        .dayName(getDayName(assignment.getDayOfWeek()))
                        .timeSlotTemplateId(assignment.getTimeSlotTemplateId())
                        .timeSlotName(timeSlot.getName())
                        .startTime(timeSlot.getStartTime().toString())
                        .endTime(timeSlot.getEndTime().toString())
                        .sessionsAffected(sessionsUpdated)
                        .successful(true)
                        .build();

                assignmentDetails.add(detail);
                totalSessionsUpdated += sessionsUpdated;

                log.debug("Updated {} sessions for day {} with time slot {} in class {}",
                        sessionsUpdated, assignment.getDayOfWeek(), timeSlot.getName(), classEntity.getCode());

            } catch (CustomException e) {
                // Create failed assignment detail
                AssignTimeSlotsResponse.AssignmentDetail detail = AssignTimeSlotsResponse.AssignmentDetail.builder()
                        .dayOfWeek(assignment.getDayOfWeek())
                        .dayName(getDayName(assignment.getDayOfWeek()))
                        .timeSlotTemplateId(assignment.getTimeSlotTemplateId())
                        .successful(false)
                        .errorMessage(e.getMessage())
                        .build();

                assignmentDetails.add(detail);
                log.warn("Failed to assign time slot for day {} in class {}: {}",
                        assignment.getDayOfWeek(), classEntity.getCode(), e.getMessage());
            }
        }

        // Build response
        AssignTimeSlotsResponse response = AssignTimeSlotsResponse.builder()
                .success(totalSessionsUpdated > 0)
                .message(String.format("Time slots assignment completed. %d of %d sessions updated.",
                        totalSessionsUpdated, totalSessions))
                .classId(classId)
                .classCode(classEntity.getCode())
                .totalSessions(totalSessions)
                .sessionsUpdated(totalSessionsUpdated)
                .updatedAt(classEntity.getUpdatedAt())
                .assignmentDetails(assignmentDetails)
                .build();

        log.info("Time slots assignment completed for class {}: {}/{} sessions updated",
                classEntity.getCode(), totalSessionsUpdated, totalSessions);

        return response;
    }

    @Override
    @Transactional
    public AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request, Long userId) {
        log.info("Assigning resources for class ID {} by user {}", classId, userId);

        // Validate request
        if (!assignResourcesRequestValidator.isValid(request)) {
            List<String> errors = assignResourcesRequestValidator.getValidationErrors(request);
            log.error("Invalid resource assignment request: {}", errors);
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (assignResourcesRequestValidator.hasDuplicateDays(request)) {
            Short duplicateDay = assignResourcesRequestValidator.getDuplicateDay(request);
            log.error("Duplicate day assignment found for day: {}", duplicateDay);
            throw new CustomException(ErrorCode.DUPLICATE_TIME_SLOT_ASSIGNMENT);
        }

        // Get class and validate access
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to class's branch
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(classEntity.getBranch().getId())) {
            log.error("User {} does not have access to class {} in branch {}",
                    userId, classId, classEntity.getBranch().getId());
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }

        log.debug("Delegating resource assignment to ResourceAssignmentService for class {}",
                classEntity.getCode());

        // Delegate to ResourceAssignmentService for HYBRID implementation
        AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

        log.info("Resource assignment completed for class {}: {}/{} sessions assigned, {} conflicts",
                classEntity.getCode(), response.getSuccessCount(), response.getTotalSessions(),
                response.getConflictCount());

        return response;
    }

    @Override
    @Transactional
    public AssignSessionResourceResponse assignResourceToSession(
            Long classId,
            Long sessionId,
            AssignSessionResourceRequest request,
            Long userId) {
        log.info("Quick fix assign resource {} -> session {} (class {}) by user {}", request.getResourceId(), sessionId,
                classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        validateClassBranchAccess(classEntity, userId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getClassEntity().getId().equals(classId)) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }

        resourceAssignmentService.assignResourceToSession(sessionId, request.getResourceId());

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        return AssignSessionResourceResponse.builder()
                .classId(classId)
                .sessionId(sessionId)
                .sessionDate(session.getDate() != null ? session.getDate().toString() : null)
                .resourceId(resource.getId())
                .resourceCode(resource.getCode())
                .resourceName(resource.getName())
                .resolved(true)
                .message("Resource assigned successfully")
                .build();
    }

    @Override
    public List<AvailableResourceDTO> getAvailableResourcesForSession(Long classId, Long sessionId, Long userId) {
        log.info("Fetching resource suggestions for session {} in class {}", sessionId, classId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        validateClassBranchAccess(classEntity, userId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getClassEntity().getId().equals(classId)) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }

        ResourceType resourceType = classEntity.getModality() == Modality.ONLINE
                ? ResourceType.VIRTUAL
                : ResourceType.ROOM;

        List<Resource> availableResources = resourceAssignmentService.queryAvailableResources(
                classId,
                sessionId,
                resourceType);

        return availableResources.stream()
                .map(AvailableResourceDTO::basic)
                .toList();
    }

    @Override
    public ValidateClassResponse validateClass(Long classId, Long userId) {
        log.info("Validating class ID: {} by user ID: {}", classId, userId);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            // Check user has access to this class's branch (without approval status check)
            validateClassBranchAccess(classEntity, userId);

            // Delegate to validation service for comprehensive validation
            ValidateClassResponse validationResponse = validationService.validateClassComplete(classId);

            log.info("Class validation completed for class ID: {}. Valid: {}, CanSubmit: {}",
                    classId, validateClassResponseUtil.isValid(validationResponse),
                    validateClassResponseUtil.canSubmit(validationResponse));

            return validationResponse;

        } catch (CustomException e) {
            log.error("Validation failed for class ID: {} by user ID: {}. Error: {}",
                    classId, userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during class validation for class ID: {} by user ID: {}",
                    classId, userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public SubmitClassResponse submitClass(Long classId, Long userId) {
        log.info("Submitting class ID: {} for approval by user: {}", classId, userId);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            validateClassBranchAccess(classEntity, userId);

            // Delegate to ApprovalService
            return approvalService.submitForApproval(classId, userId);

        } catch (CustomException e) {
            log.error("Error submitting class ID: {} for approval: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error submitting class ID: {} for approval", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void approveClass(Long classId, Long approverUserId) {
        log.info("Approving class ID: {} by user: {}", classId, approverUserId);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            validateClassBranchAccess(classEntity, approverUserId);

            // Delegate to ApprovalService
            approvalService.approveClass(classId, approverUserId);

        } catch (CustomException e) {
            log.error("Error approving class ID: {}: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error approving class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public RejectClassResponse rejectClass(Long classId, String reason, Long rejecterUserId) {
        log.info("Rejecting class ID: {} by user: {} with reason: {}", classId, rejecterUserId, reason);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            validateClassBranchAccess(classEntity, rejecterUserId);

            // Delegate to ApprovalService
            return approvalService.rejectClass(classId, reason, rejecterUserId);

        } catch (CustomException e) {
            log.error("Error rejecting class ID: {}: {}", classId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error rejecting class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public void deleteClass(Long classId, Long userId) {
        log.info("Deleting class ID: {} by user: {}", classId, userId);

        // Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch
        validateClassBranchAccess(classEntity, userId);

        // Validate class is in DRAFT status (only DRAFT classes can be deleted)
        if (classEntity.getStatus() != ClassStatus.DRAFT) {
            throw new CustomException(ErrorCode.CLASS_CANNOT_DELETE_NON_DRAFT);
        }

        // Check if class has any enrollments (shouldn't happen for DRAFT, but safety
        // check)
        Integer enrolledCount = classRepository.countEnrolledStudents(classId);
        if (enrolledCount != null && enrolledCount > 0) {
            throw new CustomException(ErrorCode.CLASS_HAS_ENROLLMENTS);
        }

        try {
            // Cascade delete: sessions → teaching_slots, session_resources (handled by DB
            // constraints)
            classRepository.delete(classEntity);
            log.info("Successfully deleted class ID: {} (code: {})", classId, classEntity.getCode());
        } catch (Exception e) {
            log.error("Error deleting class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // Helper methods for Create Class workflow

    private void validateCreateClassRequest(CreateClassRequest request, Long userId) {
        if (!createClassRequestValidator.isValid(request)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (createClassRequestValidator.hasDuplicateDays(request)) {
            throw new CustomException(ErrorCode.INVALID_SCHEDULE_DAYS);
        }

        if (!createClassRequestValidator.isStartDateInScheduleDays(request)) {
            throw new CustomException(ErrorCode.START_DATE_NOT_IN_SCHEDULE_DAYS);
        }
    }

    private void validateCreateClassBusinessRules(CreateClassRequest request, Branch branch, Course course,
            Long userId) {
        // Validate user has access to branch
        List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
        if (!userBranchIds.contains(branch.getId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Validate course approval status (not course status)
        if (course.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.COURSE_NOT_APPROVED);
        }

        // Validate class code uniqueness within branch (only if code is provided)
        if (request.getCode() != null && !request.getCode().isBlank()) {
            if (classRepository.findByBranchIdAndCode(branch.getId(), request.getCode()).isPresent()) {
                throw new CustomException(ErrorCode.CLASS_CODE_DUPLICATE);
            }
        }
    }

    private CreateClassRequest mapToCreateRequest(UpdateClassRequest request) {
        return CreateClassRequest.builder()
                .branchId(request.getBranchId())
                .courseId(request.getCourseId())
                .code(request.getCode())
                .name(request.getName())
                .modality(request.getModality())
                .startDate(request.getStartDate())
                .scheduleDays(request.getScheduleDays())
                .maxCapacity(request.getMaxCapacity())
                .build();
    }

    private CreateClassResponse.SessionGenerationSummary regenerateSessions(ClassEntity classEntity, Course course) {
        List<Session> existingSessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classEntity.getId());
        if (!existingSessions.isEmpty()) {
            sessionRepository.deleteAll(existingSessions);
            classEntity.getSessions().clear();
        }

        List<Session> sessions = sessionGenerationService.generateSessionsForClass(classEntity, course);
        List<Session> savedSessions = sessionRepository.saveAll(sessions);

        LocalDate endDate = sessionGenerationService.calculateEndDate(savedSessions);
        classEntity.setPlannedEndDate(endDate);
        classRepository.save(classEntity);

        return CreateClassResponse.SessionGenerationSummary.builder()
                .sessionsGenerated(savedSessions.size())
                .totalSessionsInCourse(savedSessions.size())
                .courseCode(course.getCode())
                .courseName(course.getName())
                .startDate(classEntity.getStartDate())
                .endDate(endDate)
                .scheduleDays(classEntity.getScheduleDays())
                .build();
    }

    private CreateClassResponse.SessionGenerationSummary buildSessionSummary(ClassEntity classEntity, Course course) {
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classEntity.getId());
        int sessionCount = sessions.size();

        LocalDate startDate = classEntity.getStartDate();
        LocalDate endDate = classEntity.getPlannedEndDate();
        if (!sessions.isEmpty()) {
            startDate = sessions.get(0).getDate();
            endDate = sessions.get(sessionCount - 1).getDate();
        }

        return CreateClassResponse.SessionGenerationSummary.builder()
                .sessionsGenerated(sessionCount)
                .totalSessionsInCourse(sessionCount)
                .courseCode(course.getCode())
                .courseName(course.getName())
                .startDate(startDate)
                .endDate(endDate)
                .scheduleDays(classEntity.getScheduleDays())
                .build();
    }

    private UserAccount getUserAccount(Long userId) {
        // This would typically come from UserRepository
        // For now, assuming UserAccount can be obtained from context
        // TODO: Implement proper UserAccount retrieval
        return null; // Placeholder
    }

    /**
     * Convert day of week number to readable name (ISODOW format)
     * 1=Monday, 7=Sunday
     */
    private String getDayName(Short dayOfWeek) {
        if (dayOfWeek == null)
            return "Unknown";
        switch (dayOfWeek) {
            case 1:
                return "Monday";
            case 2:
                return "Tuesday";
            case 3:
                return "Wednesday";
            case 4:
                return "Thursday";
            case 5:
                return "Friday";
            case 6:
                return "Saturday";
            case 7:
                return "Sunday";
            default:
                return "Unknown";
        }
    }

    // ==================== CREATE CLASS WORKFLOW - PHASE 2.3: TEACHER ASSIGNMENT
    // (PRE-CHECK) ====================

    @Override
    public List<TeacherAvailabilityDTO> getAvailableTeachers(Long classId, Long userId) {
        log.info("Getting available teachers for class ID: {} by user ID: {}", classId, userId);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
            if (!userBranchIds.contains(classEntity.getBranch().getId())) {
                throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
            }

            // Execute PRE-CHECK query via TeacherAssignmentService
            List<TeacherAvailabilityDTO> teachers = teacherAssignmentService
                    .queryAvailableTeachersWithPrecheck(classId);

            // Populate schedule information for UNAVAILABLE teachers
            populateScheduleInformation(teachers, classId);

            log.info("Found {} available teachers for class ID: {}", teachers.size(), classId);
            return teachers;

        } catch (CustomException e) {
            log.error("Failed to get available teachers: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting available teachers for class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public List<TeacherDayAvailabilityDTO> getTeachersAvailableByDay(Long classId, Long userId) {
        log.info("Getting teachers available by day for class ID: {} by user ID: {}", classId, userId);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
            if (!userBranchIds.contains(classEntity.getBranch().getId())) {
                throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
            }

            // Execute day-level availability query via TeacherAssignmentService
            List<TeacherDayAvailabilityDTO> teachers = teacherAssignmentService.queryTeachersAvailableByDay(classId);

            log.info("Found {} teachers with day-level availability for class ID: {}", teachers.size(), classId);
            return teachers;

        } catch (CustomException e) {
            log.error("Error getting available teachers for class ID: {}", classId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting available teachers for class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Populate schedule information for UNAVAILABLE teachers to show detailed
     * mismatch message
     * <p>
     * For teachers with noAvailability = totalSessions, populate:
     * - teacherSchedule: What teacher registered (e.g., "T3/T5/T7 Chiều")
     * - classSchedule: What class needs (e.g., "T2/T4/T6 Sáng")
     * </p>
     */
    private void populateScheduleInformation(List<TeacherAvailabilityDTO> teachers, Long classId) {
        // Get class schedule once (reuse for all teachers)
        TeacherAvailabilityDTO.ScheduleInfo classSchedule = getClassSchedule(classId);

        for (TeacherAvailabilityDTO teacher : teachers) {
            // Populate for ANY teacher with noAvailability conflicts (partial or full)
            // This includes:
            // - UNAVAILABLE (0% availability) with full noAvailability
            // - PARTIALLY_AVAILABLE (1-99%) with some noAvailability (e.g., missing Friday)
            if (teacher.getConflicts().getNoAvailability() > 0) {

                // Get teacher's registered schedule
                TeacherAvailabilityDTO.ScheduleInfo teacherSchedule = getTeacherSchedule(teacher.getTeacherId());

                if (teacherSchedule != null && classSchedule != null) {
                    teacher.setTeacherSchedule(teacherSchedule);
                    teacher.setClassSchedule(classSchedule);
                }
            }
        }
    }

    /**
     * Get teacher's registered availability schedule
     */
    private TeacherAvailabilityDTO.ScheduleInfo getTeacherSchedule(Long teacherId) {
        List<TeacherAvailability> availabilities = teacherAvailabilityRepository.findByTeacherId(teacherId);

        if (availabilities.isEmpty()) {
            return null;
        }

        // Extract unique days and time slot from teacher's availability
        List<String> days = availabilities.stream()
                .map(a -> getDayAbbreviation(a.getId().getDayOfWeek().intValue()))
                .distinct()
                .sorted(Comparator.comparingInt(this::getDayOrder))
                .collect(Collectors.toList());

        // Assume all availabilities have same time slot (teacher registers for one
        // shift)
        TimeSlotTemplate timeSlot = availabilities.get(0).getTimeSlotTemplate();

        return TeacherAvailabilityDTO.ScheduleInfo.builder()
                .days(days)
                .timeSlot(timeSlot.getName())
                .location(extractLocationFromTimeSlot(timeSlot.getName()))
                .build();
    }

    /**
     * Get class required schedule from sessions
     */
    private TeacherAvailabilityDTO.ScheduleInfo getClassSchedule(Long classId) {
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        if (sessions.isEmpty()) {
            return null;
        }

        // Extract unique days from sessions
        List<String> days = sessions.stream()
                .map(s -> getDayAbbreviation(s.getDate().getDayOfWeek().getValue()))
                .distinct()
                .sorted(Comparator.comparingInt(this::getDayOrder))
                .collect(Collectors.toList());

        // Assume all sessions have same time slot
        TimeSlotTemplate timeSlot = sessions.get(0).getTimeSlotTemplate();

        return TeacherAvailabilityDTO.ScheduleInfo.builder()
                .days(days)
                .timeSlot(timeSlot.getName())
                .location(extractLocationFromTimeSlot(timeSlot.getName()))
                .build();
    }

    /**
     * Convert day of week number to Vietnamese abbreviation
     * 
     * @param dayOfWeek 1=Monday, 2=Tuesday, ..., 7=Sunday, 0=Sunday (PostgreSQL
     *                  format)
     */
    private String getDayAbbreviation(Integer dayOfWeek) {
        Map<Integer, String> dayMap = Map.of(
                0, "CN", // Sunday (PostgreSQL format)
                1, "T2", // Monday
                2, "T3", // Tuesday
                3, "T4", // Wednesday
                4, "T5", // Thursday
                5, "T6", // Friday
                6, "T7", // Saturday
                7, "CN" // Sunday (alternative)
        );
        return dayMap.getOrDefault(dayOfWeek, "T" + dayOfWeek);
    }

    /**
     * Get day order for sorting (Monday first)
     */
    private Integer getDayOrder(String dayAbbr) {
        Map<String, Integer> orderMap = Map.of(
                "T2", 1,
                "T3", 2,
                "T4", 3,
                "T5", 4,
                "T6", 5,
                "T7", 6,
                "CN", 7);
        return orderMap.getOrDefault(dayAbbr, 99);
    }

    /**
     * Extract location from time slot name (e.g., "HN Morning 1" → "HN")
     */
    private String extractLocationFromTimeSlot(String timeSlotName) {
        if (timeSlotName == null) {
            return "";
        }
        // Time slot format: "HN Morning 1", "HCM Afternoon 1"
        String[] parts = timeSlotName.split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    @Override
    @Transactional
    public AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest request, Long userId) {
        log.info("Assigning teacher for class ID: {} by user ID: {}", classId, userId);

        try {
            // Validate class exists and user has access
            ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

            List<Long> userBranchIds = userBranchesRepository.findBranchIdsByUserId(userId);
            if (!userBranchIds.contains(classEntity.getBranch().getId())) {
                throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
            }

            // Validate request using validator
            assignTeacherRequestValidator.validate(classId, request);

            // Execute teacher assignment via TeacherAssignmentService
            AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

            log.info("Teacher assignment completed for class ID: {}. Assigned: {}, Remaining: {}",
                    classId, response.getAssignedCount(), response.getRemainingSessions());

            return response;

        } catch (CustomException e) {
            log.error("Error assigning teacher for class ID: {}", classId, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error assigning teacher for class ID: {}", classId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
