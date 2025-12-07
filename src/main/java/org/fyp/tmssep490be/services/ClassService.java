package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.AvailableStudentDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassDetailDTO;
import org.fyp.tmssep490be.dtos.classmanagement.ClassListItemDTO;
import org.fyp.tmssep490be.dtos.classmanagement.TeacherSummaryDTO;
import org.fyp.tmssep490be.dtos.qa.QASessionListResponse;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClassService {

    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ReplacementSkillAssessmentRepository skillAssessmentRepository;
    private final StudentSessionRepository studentSessionRepository;

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

        // Batch query session counts for all classes in page
        List<Long> classIds = classes.getContent().stream()
                .map(ClassEntity::getId)
                .toList();
        Map<Long, int[]> sessionCountsMap = getSessionCountsForClasses(classIds);

        return classes.map(classEntity -> convertToClassListItemDTO(classEntity, sessionCountsMap));
    }

    private Map<Long, int[]> getSessionCountsForClasses(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Object[]> results = sessionRepository.countSessionsByClassIds(classIds);
        Map<Long, int[]> map = new java.util.HashMap<>();
        for (Object[] row : results) {
            Long classId = ((Number) row[0]).longValue();
            int completed = ((Number) row[1]).intValue();
            int total = ((Number) row[2]).intValue();
            map.put(classId, new int[] { completed, total });
        }
        return map;
    }

    private ClassListItemDTO convertToClassListItemDTO(ClassEntity classEntity, Map<Long, int[]> sessionCountsMap) {
        // Lấy ra số lượng học viên đang ghi danh trong lớp này
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

        // Get session progress from batch query result
        int[] sessionCounts = sessionCountsMap.getOrDefault(classEntity.getId(), new int[] { 0, 0 });
        int completedSessions = sessionCounts[0];
        int totalSessions = sessionCounts[1];

        return ClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .courseName(classEntity.getSubject().getName())
                .courseCode(classEntity.getSubject().getCode())
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
                .completedSessions(completedSessions)
                .totalSessions(totalSessions)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(canEnroll ? null
                        : availableSlots <= 0 ? "Class is full"
                        : classEntity.getStatus() == ClassStatus.COMPLETED ? "Class has completed"
                        : classEntity.getStatus() == ClassStatus.CANCELLED ? "Class was cancelled"
                        : "Class not available for enrollment")
                .build();
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

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    public ClassDetailDTO getClassDetail(Long classId, Long userId) {
        log.debug("Getting class detail for class {} by user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        validateClassBranchAccess(classEntity, userId);

        // Get enrollment summary
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        ClassDetailDTO.EnrollmentSummary enrollmentSummary = calculateEnrollmentSummary(
                currentEnrolled, classEntity.getMaxCapacity(), classEntity.getStatus(), classEntity.getApprovalStatus());

        // Get all teachers teaching this class
        List<TeacherSummaryDTO> teachers = getTeachersForClass(classId);

        return ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .subject(convertToSubjectDTO(classEntity.getSubject()))
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
                .createdByName(classEntity.getCreatedBy() != null ? classEntity.getCreatedBy().getFullName() : null)
                .createdAt(classEntity.getCreatedAt())
                .updatedAt(classEntity.getUpdatedAt())
                .teachers(teachers)
                .scheduleSummary(formatScheduleSummary(classEntity.getScheduleDays()))
                .enrollmentSummary(enrollmentSummary)
                .build();
    }

    private void validateClassBranchAccess(ClassEntity classEntity, Long userId) {
        // Managers are allowed to view all classes for monitoring purposes
        if (userId != null) {
            UserAccount user = userAccountRepository.findById(userId).orElse(null);
            if (user != null && user.getUserRoles() != null) {
                boolean isManager = user.getUserRoles().stream()
                        .anyMatch(ur -> ur.getRole() != null && "MANAGER".equals(ur.getRole().getCode()));
                if (isManager) {
                    return;
                }
            }
        }

        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);
        boolean hasBranchAccess = accessibleBranchIds.contains(classEntity.getBranch().getId());
        boolean hasTeachingAccess = hasTeacherAssignment(userId, classEntity.getId());

        if (!hasBranchAccess && !hasTeachingAccess) {
            log.warn("Access denied for user {} on class {} - branchAccess={}, teachingAccess={}",
                    userId, classEntity.getId(), hasBranchAccess, hasTeachingAccess);
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND); // Use existing error code
        }
    }

    private boolean hasTeacherAssignment(Long userId, Long classId) {
        if (userId == null) {
            return false;
        }

        // Check if user has any teaching slots for this class
        List<TeachingSlot> slots = teachingSlotRepository.findByClassEntityIdAndStatus(classId, TeachingSlotStatus.SCHEDULED);
        boolean hasUserAssignment = slots.stream()
                .anyMatch(slot -> slot.getTeacher() != null && 
                                 slot.getTeacher().getUserAccount().getId().equals(userId));
        
        if (hasUserAssignment) {
            return true;
        }

        // Check by teacher entity
        return teacherRepository.findByUserAccountId(userId)
                .map(teacher -> slots.stream()
                        .anyMatch(slot -> slot.getTeacher() != null && slot.getTeacher().getId().equals(teacher.getId())))
                .orElse(false);
    }

    private ClassDetailDTO.SubjectDTO convertToSubjectDTO(Subject subject) {
        ClassDetailDTO.LevelDTO levelDTO = null;

        if (subject.getLevel() != null) {
            Level level = subject.getLevel();
            ClassDetailDTO.CurriculumDTO curriculumDTO = null;
            
            if (level.getCurriculum() != null) {
                Curriculum curriculum = level.getCurriculum();
                curriculumDTO = ClassDetailDTO.CurriculumDTO.builder()
                        .id(curriculum.getId())
                        .code(curriculum.getCode())
                        .name(curriculum.getName())
                        .build();
            }

            levelDTO = ClassDetailDTO.LevelDTO.builder()
                    .id(level.getId())
                    .code(level.getCode())
                    .name(level.getName())
                    .curriculum(curriculumDTO)
                    .build();
        }

        return ClassDetailDTO.SubjectDTO.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .totalHours(subject.getTotalHours())
                .numberOfSessions(subject.getNumberOfSessions())
                .hoursPerSession(subject.getHoursPerSession())
                .prerequisites(subject.getPrerequisites())
                .targetAudience(subject.getTargetAudience())
                .teachingMethods(subject.getTeachingMethods())
                .level(levelDTO)
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

    private ClassDetailDTO.EnrollmentSummary calculateEnrollmentSummary(
            Integer currentEnrolled, 
            Integer maxCapacity,
            ClassStatus status,
            ApprovalStatus approvalStatus) {
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        boolean canEnroll = (status == ClassStatus.SCHEDULED || status == ClassStatus.ONGOING)
                && approvalStatus == ApprovalStatus.APPROVED
                && availableSlots > 0;

        String restrictionReason = null;
        if (!canEnroll) {
            if (status == ClassStatus.COMPLETED) {
                restrictionReason = "Class has completed";
            } else if (status == ClassStatus.CANCELLED) {
                restrictionReason = "Class was cancelled";
            } else if (status != ClassStatus.SCHEDULED && status != ClassStatus.ONGOING) {
                restrictionReason = "Class is not available for enrollment";
            } else if (approvalStatus != ApprovalStatus.APPROVED) {
                restrictionReason = "Class is not approved";
            } else if (availableSlots <= 0) {
                restrictionReason = "Class is at full capacity";
            }
        }

        return ClassDetailDTO.EnrollmentSummary.builder()
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(restrictionReason)
                .build();
    }

    // Allowed skills for matching
    private static final Set<Skill> ALLOWED_SKILLS = Set.of(
            Skill.GENERAL, Skill.READING, Skill.WRITING, Skill.SPEAKING, Skill.LISTENING
    );

    public Page<AvailableStudentDTO> getAvailableStudentsForClass(
            Long classId,
            String search,
            Pageable pageable,
            Long userId) {
        log.debug("Getting available students for class {} by user {} with search: {}", classId, userId, search);

        // Validate class exists and user has access
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        validateClassBranchAccess(classEntity, userId);

        // Get class details for skill assessment matching
        Long branchId = classEntity.getBranch().getId();
        final Long classCurriculumId;
        final Long classLevelId;
        
        if (classEntity.getSubject() != null && classEntity.getSubject().getLevel() != null) {
            classLevelId = classEntity.getSubject().getLevel().getId();
            if (classEntity.getSubject().getLevel().getCurriculum() != null) {
                classCurriculumId = classEntity.getSubject().getLevel().getCurriculum().getId();
            } else {
                classCurriculumId = null;
            }
        } else {
            classCurriculumId = null;
            classLevelId = null;
        }

        // Get ALL available students (no pagination for proper sorting)
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
                        classCurriculumId,
                        classLevelId))
                .sorted((dto1, dto2) -> {
                    // Sort by matchPriority (ascending: 1, 2, 3)
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

        return new PageImpl<>(paginatedDtos, pageable, allDtos.size());
    }

    private AvailableStudentDTO convertToAvailableStudentDTO(
            Student student,
            List<ReplacementSkillAssessment> assessments,
            Long classCurriculumId,
            Long classLevelId) {
        UserAccount userAccount = student.getUserAccount();
        List<ReplacementSkillAssessment> filteredAssessments = filterAllowedAssessments(assessments);

        // Get branch info (take first branch)
        String branchName = null;
        Long branchId = null;
        if (userAccount.getUserBranches() != null && !userAccount.getUserBranches().isEmpty()) {
            branchId = userAccount.getUserBranches().iterator().next().getBranch().getId();
            branchName = userAccount.getUserBranches().iterator().next().getBranch().getName();
        }

        // Convert all assessments to DTOs
        List<AvailableStudentDTO.SkillAssessmentDTO> assessmentDTOs = !filteredAssessments.isEmpty()
                ? filteredAssessments.stream()
                .map(this::convertToSkillAssessmentDTO)
                .sorted((a1, a2) -> a2.getAssessmentDate().compareTo(a1.getAssessmentDate()))
                .collect(Collectors.toList())
                : List.of();

        // Calculate class match info
        ReplacementSkillAssessment matchingAssessment = findMatchingAssessment(
                filteredAssessments, classCurriculumId, classLevelId);
        AvailableStudentDTO.ClassMatchInfoDTO classMatchInfo = calculateClassMatchInfo(
                matchingAssessment, classCurriculumId, classLevelId);

        // Get active enrollments count
        int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                student.getId(), EnrollmentStatus.ENROLLED);
        int maxEnrollments = 3; // Default policy
        boolean canEnroll = activeEnrollments < maxEnrollments;

        return AvailableStudentDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .avatarUrl(userAccount.getAvatarUrl())
                .branchId(branchId)
                .branchName(branchName)
                .replacementSkillAssessments(assessmentDTOs)
                .classMatchInfo(classMatchInfo)
                .activeEnrollments(activeEnrollments)
                .canEnroll(canEnroll)
                .accountStatus(userAccount.getStatus().name())
                .build();
    }

    private List<ReplacementSkillAssessment> filterAllowedAssessments(
            List<ReplacementSkillAssessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return List.of();
        }
        return assessments.stream()
                .filter(a -> a.getSkill() != null && ALLOWED_SKILLS.contains(a.getSkill()))
                .collect(Collectors.toList());
    }

    private ReplacementSkillAssessment findMatchingAssessment(
            List<ReplacementSkillAssessment> assessments,
            Long classCurriculumId,
            Long classLevelId) {
        if (assessments == null || assessments.isEmpty() || classCurriculumId == null) {
            return null;
        }

        // Find assessments matching the class curriculum
        List<ReplacementSkillAssessment> curriculumMatches = assessments.stream()
                .filter(a -> a.getLevel() != null &&
                        a.getLevel().getCurriculum() != null &&
                        a.getLevel().getCurriculum().getId().equals(classCurriculumId))
                .toList();

        if (curriculumMatches.isEmpty()) {
            return null;
        }

        // Find perfect match (curriculum + level)
        if (classLevelId != null) {
            for (ReplacementSkillAssessment assessment : curriculumMatches) {
                if (assessment.getLevel().getId().equals(classLevelId)) {
                    return assessment;
                }
            }
        }

        return curriculumMatches.get(0);
    }

    private AvailableStudentDTO.ClassMatchInfoDTO calculateClassMatchInfo(
            ReplacementSkillAssessment matchingAssessment,
            Long classCurriculumId,
            Long classLevelId) {
        Integer matchPriority = 3; // Default: No match
        String matchingSkill = null;
        AvailableStudentDTO.LevelInfoDTO matchingLevel = null;
        String matchReason = "No skill assessment found for this course's curriculum";

        if (matchingAssessment != null && matchingAssessment.getLevel() != null) {
            Long assessmentCurriculumId = matchingAssessment.getLevel().getCurriculum() != null ?
                                          matchingAssessment.getLevel().getCurriculum().getId() : null;
            Long assessmentLevelId = matchingAssessment.getLevel().getId();

            if (assessmentCurriculumId != null && assessmentCurriculumId.equals(classCurriculumId)) {
                matchingSkill = matchingAssessment.getSkill().name();
                matchingLevel = convertToLevelInfoDTO(matchingAssessment.getLevel());

                if (classLevelId != null && assessmentLevelId.equals(classLevelId)) {
                    matchPriority = 1;
                    matchReason = "Perfect match - Assessment matches both Curriculum AND Level";
                } else {
                    matchPriority = 2;
                    matchReason = "Partial match - Assessment matches Curriculum only";
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

    private AvailableStudentDTO.SkillAssessmentDTO convertToSkillAssessmentDTO(
            ReplacementSkillAssessment assessment) {
        return AvailableStudentDTO.SkillAssessmentDTO.builder()
                .id(assessment.getId())
                .skill(assessment.getSkill().name())
                .level(convertToLevelInfoDTO(assessment.getLevel()))
                .score(assessment.getScore())
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
                .subject(convertToCurriculumInfoDTO(level.getCurriculum()))
                .description(level.getDescription())
                .build();
    }

    private AvailableStudentDTO.SubjectInfoDTO convertToCurriculumInfoDTO(Curriculum curriculum) {
        if (curriculum == null) {
            return null;
        }

        return AvailableStudentDTO.SubjectInfoDTO.builder()
                .id(curriculum.getId())
                .name(curriculum.getName())
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

    public QASessionListResponse getSessionsWithMetrics(Long classId, Long userId) {
        log.info("Getting sessions with metrics for class ID {} by user {}", classId, userId);

        // Get class and validate access
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        validateClassBranchAccess(classEntity, userId);

        // Get all sessions ordered by date
        List<Session> sessions = sessionRepository.findByClassEntityIdOrderByDateAsc(classId);

        // Map to QASessionItemDTO with real metrics
        List<QASessionListResponse.QASessionItemDTO> sessionItems = sessions.stream()
                .map(s -> {
                    // Get real student session data for this session
                    List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(s.getId());

                    // Calculate real attendance metrics
                    long presentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                            .count();

                    long absentCount = studentSessions.stream()
                            .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                            .count();

                    long totalStudents = studentSessions.size();
                    double attendanceRate = totalStudents > 0 ? (presentCount * 100.0 / totalStudents) : 0.0;

                    // Calculate homework completion metrics (exclude NO_HOMEWORK)
                    long homeworkCompletedCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() == HomeworkStatus.COMPLETED)
                            .count();

                    long homeworkTotalCount = studentSessions.stream()
                            .filter(ss -> ss.getHomeworkStatus() != null
                                    && ss.getHomeworkStatus() != HomeworkStatus.NO_HOMEWORK)
                            .count();

                    double homeworkCompletionRate = homeworkTotalCount > 0
                            ? (homeworkCompletedCount * 100.0 / homeworkTotalCount)
                            : 0.0;

                    // Get QA report count
                    int qaReportCount = s.getQaReports() != null ? s.getQaReports().size() : 0;

                    // Get session info from related entities
                    Integer sequenceNumber = s.getSubjectSession() != null ? s.getSubjectSession().getSequenceNo() : null;
                    String timeSlot = s.getTimeSlotTemplate() != null ? s.getTimeSlotTemplate().getName() : "TBA";
                    String topic = s.getSubjectSession() != null ? s.getSubjectSession().getTopic() : "N/A";

                    // Get teacher from teaching slots
                    String teacherName = "TBA";
                    if (s.getTeachingSlots() != null && !s.getTeachingSlots().isEmpty()) {
                        teacherName = s.getTeachingSlots().stream()
                                .findFirst()
                                .map(ts -> ts.getTeacher() != null && ts.getTeacher().getUserAccount() != null
                                        ? ts.getTeacher().getUserAccount().getFullName()
                                        : "TBA")
                                .orElse("TBA");
                    }

                    return QASessionListResponse.QASessionItemDTO.builder()
                            .sessionId(s.getId())
                            .sequenceNumber(sequenceNumber)
                            .date(s.getDate())
                            .dayOfWeek(s.getDate() != null ? s.getDate().getDayOfWeek().name() : null)
                            .timeSlot(timeSlot)
                            .topic(topic)
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .teacherName(teacherName)
                            .totalStudents((int) totalStudents)
                            .presentCount((int) presentCount)
                            .absentCount((int) absentCount)
                            .attendanceRate(attendanceRate)
                            .homeworkCompletedCount((int) homeworkCompletedCount)
                            .homeworkCompletionRate(homeworkCompletionRate)
                            .hasQAReport(qaReportCount > 0)
                            .qaReportCount(qaReportCount)
                            .build();
                })
                .collect(Collectors.toList());

        return QASessionListResponse.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode() != null ? classEntity.getCode() : "N/A")
                .totalSessions(sessions.size())
                .sessions(sessionItems)
                .build();
    }

}
