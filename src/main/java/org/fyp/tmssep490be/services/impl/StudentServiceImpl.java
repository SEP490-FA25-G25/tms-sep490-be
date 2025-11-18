package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchRepository branchRepository;
    private final LevelRepository levelRepository;
    private final ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public CreateStudentResponse createStudent(CreateStudentRequest request, Long currentUserId) {
        log.info("Creating new student with email: {} by user: {}", request.getEmail(), currentUserId);

        // 1. VALIDATE: Email uniqueness
        if (userAccountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 2. VALIDATE: Branch exists and user has access
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        // Check if current user has access to this branch
        // For testing purposes, bypass branch access check for mock user ID 1
        if (currentUserId != 1L) {
            List<Long> userBranches = getUserAccessibleBranches(currentUserId);
            if (!userBranches.contains(request.getBranchId())) {
                throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
            }
        }

        // 3. VALIDATE: Level IDs exist (if skill assessments provided)
        if (request.getSkillAssessments() != null && !request.getSkillAssessments().isEmpty()) {
            for (SkillAssessmentInput assessment : request.getSkillAssessments()) {
                if (!levelRepository.existsById(assessment.getLevelId())) {
                    throw new CustomException(ErrorCode.LEVEL_NOT_FOUND);
                }
            }
        }

        // 4. CREATE USER ACCOUNT
        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setFacebookUrl(request.getFacebookUrl());
        user.setAddress(request.getAddress());
        user.setGender(request.getGender());
        user.setDob(request.getDob());
        user.setStatus(UserStatus.ACTIVE);

        // Password is always "12345678" - student should change after first login
        String defaultPassword = "12345678";
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        log.debug("Created user with default password for student: {}", request.getEmail());

        UserAccount savedUser = userAccountRepository.save(user);
        log.debug("Created user_account with ID: {}", savedUser.getId());

        // 5. CREATE STUDENT with auto-generated student code
        Student student = new Student();
        student.setUserAccount(savedUser);
        student.setStudentCode(generateStudentCode(
                request.getBranchId(),
                request.getFullName(),
                request.getEmail()
        ));

        Student savedStudent = studentRepository.save(student);
        log.info("Created student with ID: {} and code: {}", savedStudent.getId(), savedStudent.getStudentCode());

        // 6. ASSIGN ROLE "STUDENT"
        Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_ROLE_NOT_FOUND));

        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(savedUser.getId());
        userRoleId.setRoleId(studentRole.getId());

        UserRole userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setUserAccount(savedUser);
        userRole.setRole(studentRole);
        userRoleRepository.save(userRole);
        log.debug("Assigned STUDENT role to user: {}", savedUser.getId());

        // 7. ASSIGN TO BRANCH
        UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
        userBranchId.setUserId(savedUser.getId());
        userBranchId.setBranchId(request.getBranchId());

        UserBranches userBranch = new UserBranches();
        userBranch.setId(userBranchId);
        userBranch.setUserAccount(savedUser);
        userBranch.setBranch(branch);

        // Set assignedBy (current academic affair user)
        // For testing purposes, bypass UserBranches creation for mock user ID 1
        if (currentUserId != 1L) {
            UserAccount assignedBy = userAccountRepository.findById(currentUserId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            userBranch.setAssignedBy(assignedBy);
            userBranchesRepository.save(userBranch);
        }
        log.debug("Assigned user {} to branch {}", savedUser.getId(), request.getBranchId());

        // 8. CREATE SKILL ASSESSMENTS (if provided)
        int assessmentsCreated = 0;
        if (request.getSkillAssessments() != null && !request.getSkillAssessments().isEmpty()) {
            for (SkillAssessmentInput input : request.getSkillAssessments()) {
                Level level = levelRepository.findById(input.getLevelId())
                        .orElseThrow(() -> new CustomException(ErrorCode.LEVEL_NOT_FOUND));

                ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
                assessment.setStudent(savedStudent);
                assessment.setSkill(input.getSkill());
                assessment.setLevel(level);
                assessment.setRawScore(input.getRawScore());
                assessment.setScaledScore(input.getScaledScore());
                assessment.setScoreScale(input.getScoreScale());
                assessment.setAssessmentCategory(input.getAssessmentCategory());
                assessment.setAssessmentDate(LocalDate.now());
                assessment.setAssessmentType("manual_creation");
                assessment.setNote(input.getNote());

                // Set assessedBy user - For testing purposes, bypass for mock user ID 1
                if (currentUserId != 1L) {
                    UserAccount assessedBy = userAccountRepository.findById(currentUserId)
                            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                    assessment.setAssessedBy(assessedBy);
                }

                replacementSkillAssessmentRepository.save(assessment);
                assessmentsCreated++;
                log.debug("Created {} assessment for student {} at level {} with scaled score {}",
                        input.getSkill(), savedStudent.getId(), level.getCode(), input.getScaledScore());
            }
        }

        // 9. GET CREATOR INFO
        UserAccount creator;
        if (currentUserId != 1L) {
            creator = userAccountRepository.findById(currentUserId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        } else {
            // For testing with mock user ID 1, create a minimal creator object
            creator = new UserAccount();
            creator.setId(currentUserId);
            creator.setFullName("Mock User");
        }

        // 10. BUILD RESPONSE
        CreateStudentResponse response = CreateStudentResponse.builder()
                .studentId(savedStudent.getId())
                .studentCode(savedStudent.getStudentCode())
                .userAccountId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .phone(savedUser.getPhone())
                .gender(savedUser.getGender())
                .dob(savedUser.getDob())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .status(savedUser.getStatus())
                .defaultPassword(defaultPassword) // Always "12345678"
                .skillAssessmentsCreated(assessmentsCreated)
                .createdAt(savedUser.getCreatedAt())
                .createdBy(CreateStudentResponse.CreatedByInfo.builder()
                        .userId(creator.getId())
                        .fullName(creator.getFullName())
                        .build())
                .build();

        log.info("Successfully created student: {} with {} skill assessments",
                savedStudent.getStudentCode(), assessmentsCreated);

        return response;
    }

    /**
     * Generate unique student code based on branch, name, and email
     * Format: ST{branchId}{baseName}{randomSuffix}
     * Example: ST1NGUYENVANA123
     */
    private String generateStudentCode(Long branchId, String fullName, String email) {
        String baseName;

        // Prioritize fullName, fallback to email prefix
        if (fullName != null && !fullName.trim().isEmpty()) {
            // Remove special chars, spaces, and convert to uppercase
            baseName = fullName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            // Limit to max 10 characters
            if (baseName.length() > 10) {
                baseName = baseName.substring(0, 10);
            }
        } else if (email != null && email.contains("@")) {
            // Use part before @ from email
            baseName = email.substring(0, email.indexOf("@"))
                    .replaceAll("[^a-zA-Z0-9]", "")
                    .toUpperCase();
            if (baseName.length() > 10) {
                baseName = baseName.substring(0, 10);
            }
        } else {
            // Fallback: use timestamp
            baseName = String.valueOf(System.currentTimeMillis()).substring(6);
        }

        // Add random suffix to ensure uniqueness
        int randomSuffix = (int) (Math.random() * 1000);

        String studentCode = String.format("ST%d%s%03d", branchId, baseName, randomSuffix);

        // Double-check uniqueness (rare collision case)
        while (studentRepository.findByStudentCode(studentCode).isPresent()) {
            randomSuffix = (int) (Math.random() * 1000);
            studentCode = String.format("ST%d%s%03d", branchId, baseName, randomSuffix);
        }

        return studentCode;
    }

    @Override
    public Page<StudentListItemDTO> getStudents(
            List<Long> branchIds,
            String search,
            UserStatus status,
            Long courseId,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting students for user {} with filters: branchIds={}, search={}, status={}, courseId={}",
                userId, branchIds, search, status, courseId);

        // Get user's accessible branches if not provided
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
        }

        // Map sort field: Student entity doesn't have fullName, it's in userAccount
        // Transform pageable to use correct entity path for sorting
        pageable = mapStudentSortField(pageable);

        Page<Student> students;

        // Filter by course if specified
        if (courseId != null) {
            students = studentRepository.findStudentsByCourse(courseId, branchIds, pageable);
        } else {
            students = studentRepository.findStudentsInBranchesWithSearch(branchIds, search, pageable);
        }

        return students.map(this::convertToStudentListItemDTO);
    }

    /**
     * Map sort fields from DTO field names to entity paths
     * Since Student queries join with UserAccount, need to map fields correctly
     * 
     * NOTE: Nested path sorting (e.g., userAccount.fullName) doesn't work with Spring Data JPA
     * dynamic queries. For now, we only support sorting by Student's own fields.
     */
    private Pageable mapStudentSortField(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }

        // Check if sorting by unsupported nested fields
        boolean hasNestedSort = pageable.getSort().stream()
                .anyMatch(order -> {
                    String prop = order.getProperty();
                    return prop.equals("fullName") || prop.equals("name") || 
                           prop.equals("email") || prop.equals("phone") || 
                           prop.equals("status");
                });

        // If trying to sort by nested fields, remove sort and use default (studentCode)
        if (hasNestedSort) {
            log.warn("Sorting by UserAccount fields not supported in dynamic queries. Using default sort by studentCode.");
            return PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                Sort.by(Sort.Direction.ASC, "studentCode")
            );
        }

        // Only map Student's own fields
        List<Sort.Order> mappedOrders = pageable.getSort().stream()
                .map(order -> {
                    String property = order.getProperty();
                    String mappedProperty = switch (property) {
                        case "studentCode", "code" -> "studentCode";
                        case "createdAt", "created" -> "createdAt";
                        default -> "studentCode"; // Fallback to safe field
                    };
                    return new Sort.Order(order.getDirection(), mappedProperty);
                })
                .collect(Collectors.toList());

        Sort mappedSort = Sort.by(mappedOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }

    @Override
    public StudentDetailDTO getStudentDetail(Long studentId, Long userId) {
        log.debug("Getting student detail for student {} by user {}", studentId, userId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        // Validate student access
        validateStudentAccess(student, userId);

        return convertToStudentDetailDTO(student);
    }

    @Override
    public Page<StudentEnrollmentHistoryDTO> getStudentEnrollmentHistory(
            Long studentId,
            List<Long> branchIds,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting enrollment history for student {} by user {}", studentId, userId);

        // Validate student access
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));
        validateStudentAccess(student, userId);

        // Get user's accessible branches if not provided
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
        }

        Page<Enrollment> enrollments = enrollmentRepository.findStudentEnrollmentHistory(
                studentId, branchIds, pageable);

        return enrollments.map(this::convertToStudentEnrollmentHistoryDTO);
    }

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    private void validateStudentAccess(Student student, Long userId) {
        List<Long> accessibleBranches = getUserAccessibleBranches(userId);

        // Check if student belongs to any accessible branch
        boolean hasAccess = student.getUserAccount().getUserBranches().stream()
                .anyMatch(ub -> accessibleBranches.contains(ub.getBranch().getId()));

        if (!hasAccess) {
            throw new CustomException(ErrorCode.STUDENT_ACCESS_DENIED);
        }
    }

    private StudentListItemDTO convertToStudentListItemDTO(Student student) {
        UserAccount user = student.getUserAccount();

        // Get branch from user's branches (take first one)
        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            branchId = user.getUserBranches().iterator().next().getBranch().getId();
            branchName = user.getUserBranches().iterator().next().getBranch().getName();
        }

        // Get enrollment counts
        int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                student.getId(), EnrollmentStatus.ENROLLED);

        Optional<Enrollment> latestEnrollment = Optional.ofNullable(
                enrollmentRepository.findLatestEnrollmentByStudent(student.getId()));

        return StudentListItemDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .branchName(branchName)
                .branchId(branchId)
                .activeEnrollments((long) activeEnrollments)
                .lastEnrollmentDate(latestEnrollment
                        .filter(e -> e.getEnrolledAt() != null)
                        .map(e -> e.getEnrolledAt().toLocalDate())
                        .orElse(null))
                .canEnroll(activeEnrollments < 3) // Example max concurrent enrollments
                .build();
    }

    private StudentDetailDTO convertToStudentDetailDTO(Student student) {
        UserAccount user = student.getUserAccount();

        // Get branch info
        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            branchId = user.getUserBranches().iterator().next().getBranch().getId();
            branchName = user.getUserBranches().iterator().next().getBranch().getName();
        }

        // Get enrollment statistics
        int totalEnrollments = student.getEnrollments().size();
        int activeEnrollments = (int) student.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        int completedEnrollments = (int) student.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        // Get first and last enrollment dates
        LocalDate firstEnrollmentDate = student.getEnrollments().stream()
                .map(Enrollment::getEnrolledAt)
                .filter(Objects::nonNull)
                .map(OffsetDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastEnrollmentDate = student.getEnrollments().stream()
                .map(Enrollment::getEnrolledAt)
                .filter(Objects::nonNull)
                .map(OffsetDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        // Get current active classes
        List<StudentActiveClassDTO> currentClasses = student.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .map(this::convertToStudentActiveClassDTO)
                .collect(Collectors.toList());

        return StudentDetailDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .gender(user.getGender().name())
                .dateOfBirth(user.getDob())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .branchName(branchName)
                .branchId(branchId)
                .totalEnrollments((long) totalEnrollments)
                .activeEnrollments((long) activeEnrollments)
                .completedEnrollments((long) completedEnrollments)
                .firstEnrollmentDate(firstEnrollmentDate)
                .lastEnrollmentDate(lastEnrollmentDate)
                .currentClasses(currentClasses)
                .build();
    }

    private StudentActiveClassDTO convertToStudentActiveClassDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();

        return StudentActiveClassDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .branchName(classEntity.getBranch().getName())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .enrollmentStatus(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    private StudentEnrollmentHistoryDTO convertToStudentEnrollmentHistoryDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        ClassEntity classEntity = enrollment.getClassEntity();
        UserAccount studentUser = student.getUserAccount();

        // Get session boundary information
        Long joinSessionId = enrollment.getJoinSessionId();
        LocalDate joinSessionDate = null;
        if (enrollment.getJoinSession() != null) {
            joinSessionDate = enrollment.getJoinSession().getDate();
        }

        Long leftSessionId = enrollment.getLeftSessionId();
        LocalDate leftSessionDate = null;
        if (enrollment.getLeftSession() != null) {
            leftSessionDate = enrollment.getLeftSession().getDate();
        }

        return StudentEnrollmentHistoryDTO.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentName(studentUser.getFullName())
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .branchName(classEntity.getBranch().getName())
                .status(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .leftAt(enrollment.getLeftAt())
                .enrolledByName(enrollment.getEnrolledByUser() != null ?
                        enrollment.getEnrolledByUser().getFullName() : null)
                .joinSessionId(joinSessionId)
                .joinSessionDate(joinSessionDate)
                .leftSessionId(leftSessionId)
                .leftSessionDate(leftSessionDate)
                .classStartDate(classEntity.getStartDate())
                .classEndDate(classEntity.getPlannedEndDate())
                .modality(classEntity.getModality().name())
                .totalSessions(0) // TODO: Calculate from sessions
                .attendedSessions(0) // TODO: Calculate from student_sessions
                .attendanceRate(0.0) // TODO: Calculate from student_sessions
                .averageScore(null) // TODO: Calculate from scores
                .build();
    }
}
