package org.fyp.tmssep490be.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.enrollment.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.EnrollmentService;
import org.fyp.tmssep490be.services.ExcelParserService;
import org.fyp.tmssep490be.services.NotificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final ExcelParserService excelParserService;
    private final PasswordEncoder passwordEncoder;
    private final ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    private final LevelRepository levelRepository;
    private final NotificationService notificationService;

    @Override
    public ClassEnrollmentImportPreview previewClassEnrollmentImport(
            Long classId,
            MultipartFile file,
            Long enrolledBy
    ) {
        log.info("Previewing enrollment import for class ID: {}", classId);

        // 1. Validate class exists và đủ điều kiện enroll
        ClassEntity classEntity = validateClassForEnrollment(classId);

        // 2. Parse Excel file
        List<StudentEnrollmentData> parsedData = excelParserService.parseStudentEnrollment(file);

        if (parsedData.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
        }

        log.info("Parsed {} students from Excel", parsedData.size());

        // 3. Resolve từng student (FOUND/CREATE/ERROR)
        resolveStudents(parsedData);

        // 4. Calculate capacity
        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                classId, EnrollmentStatus.ENROLLED
        );
        int maxCapacity = classEntity.getMaxCapacity();
        int availableSlots = maxCapacity - currentEnrolled;

        int validStudentsCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentResolutionStatus.FOUND
                        || d.getStatus() == StudentResolutionStatus.CREATE)
                .count();

        int errorCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentResolutionStatus.ERROR
                        || d.getStatus() == StudentResolutionStatus.DUPLICATE)
                .count();

        boolean exceedsCapacity = validStudentsCount > availableSlots;
        int exceededBy = exceedsCapacity ? (validStudentsCount - availableSlots) : 0;

        log.info("Capacity check: {}/{} enrolled, {} valid students, {} available slots",
                currentEnrolled, maxCapacity, validStudentsCount, availableSlots);

        // 5. Determine recommendation
        EnrollmentRecommendation recommendation = determineRecommendation(
                validStudentsCount,
                availableSlots,
                maxCapacity,
                currentEnrolled
        );

        // 6. Build warnings and errors
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (exceedsCapacity) {
            warnings.add(String.format(
                    "Import will exceed capacity by %d students (%d enrolled + %d new = %d/%d)",
                    exceededBy, currentEnrolled, validStudentsCount,
                    currentEnrolled + validStudentsCount, maxCapacity
            ));
        }
        if (errorCount > 0) {
            errors.add(String.format("%d students have validation errors or duplicates", errorCount));
        }

        // 7. Return preview
        return ClassEnrollmentImportPreview.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .students(parsedData)
                .foundCount((int) parsedData.stream().filter(d -> d.getStatus() == StudentResolutionStatus.FOUND).count())
                .createCount((int) parsedData.stream().filter(d -> d.getStatus() == StudentResolutionStatus.CREATE).count())
                .errorCount(errorCount)
                .totalValid(validStudentsCount)
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .exceedsCapacity(exceedsCapacity)
                .exceededBy(exceededBy)
                .warnings(warnings)
                .errors(errors)
                .recommendation(recommendation)
                .build();
    }

    @Override
    @Transactional
    public EnrollmentResult executeClassEnrollmentImport(
            ClassEnrollmentImportExecuteRequest request,
            Long enrolledBy
    ) {
        log.info("Executing enrollment import for class ID: {} with strategy: {}",
                request.getClassId(), request.getStrategy());

        // 1. Lock class để đảm bảo consistency (tránh race condition)
        ClassEntity classEntity = classRepository.findByIdWithLock(request.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + request.getClassId()));

        // 2. Re-validate capacity (double-check for race condition)
        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                request.getClassId(), EnrollmentStatus.ENROLLED
        );

        log.info("Class locked. Current enrollment: {}/{}", currentEnrolled, classEntity.getMaxCapacity());

        // 3. Filter students theo strategy
        List<StudentEnrollmentData> studentsToEnroll = filterStudentsByStrategy(
                request,
                classEntity,
                currentEnrolled,
                enrolledBy
        );

        log.info("Filtered {} students to enroll based on strategy", studentsToEnroll.size());

        // 4. Create new students nếu cần
        List<Long> allStudentIds = new ArrayList<>();
        int studentsCreated = 0;

        for (StudentEnrollmentData data : studentsToEnroll) {
            if (data.getStatus() == StudentResolutionStatus.CREATE) {
                Student newStudent = createStudentQuick(data, classEntity.getBranch().getId(), enrolledBy);
                allStudentIds.add(newStudent.getId());
                studentsCreated++;
                log.info("Created new student: {} ({})", newStudent.getId(), data.getEmail());
            } else if (data.getStatus() == StudentResolutionStatus.FOUND) {
                allStudentIds.add(data.getResolvedStudentId());
            }
        }

        log.info("Created {} new students, total {} students to enroll", studentsCreated, allStudentIds.size());

        // 5. Determine if this is capacity override
        boolean isOverride = request.getStrategy() == EnrollmentStrategy.OVERRIDE;
        String overrideReason = isOverride ? request.getOverrideReason() : null;

        // 6. Batch enroll all students
        EnrollmentResult result = enrollStudents(
                request.getClassId(),
                allStudentIds,
                enrolledBy,
                isOverride,
                overrideReason
        );
        result.setStudentsCreated(studentsCreated);

        log.info("Enrollment completed. Enrolled: {}, Created: {}, Sessions per student: {}",
                result.getEnrolledCount(), studentsCreated, result.getSessionsGeneratedPerStudent());

        return result;
    }

    /**
     * Resolve từng student: tìm trong DB hoặc mark as CREATE
     */
    private void resolveStudents(List<StudentEnrollmentData> parsedData) {
        Set<String> seenEmails = new HashSet<>();

        for (StudentEnrollmentData data : parsedData) {
            // Skip if already has error from parsing
            if (data.getStatus() == StudentResolutionStatus.ERROR) {
                continue;
            }

            // Validate required fields
            if (data.getEmail() == null || data.getEmail().isBlank()) {
                data.setStatus(StudentResolutionStatus.ERROR);
                data.setErrorMessage("Email is required");
                continue;
            }
            if (data.getFullName() == null || data.getFullName().isBlank()) {
                data.setStatus(StudentResolutionStatus.ERROR);
                data.setErrorMessage("Full name is required");
                continue;
            }

            // Check duplicate trong file Excel
            String emailLower = data.getEmail().toLowerCase();
            if (seenEmails.contains(emailLower)) {
                data.setStatus(StudentResolutionStatus.DUPLICATE);
                data.setErrorMessage("Duplicate email in Excel file");
                continue;
            }
            seenEmails.add(emailLower);

            // REMOVED: Student code search logic
            // Student codes are now auto-generated by the system
            // We only search by email for existing students

            // Try to find by email
            Optional<UserAccount> userByEmail = userAccountRepository.findByEmail(data.getEmail());
            if (userByEmail.isPresent()) {
                Optional<Student> student = studentRepository.findByUserAccountId(userByEmail.get().getId());
                if (student.isPresent()) {
                    data.setStatus(StudentResolutionStatus.FOUND);
                    data.setResolvedStudentId(student.get().getId());
                    log.debug("Found student by email: {} -> ID: {}", data.getEmail(), student.get().getId());
                    continue;
                }
            }

            // Mark as CREATE (student mới)
            data.setStatus(StudentResolutionStatus.CREATE);
            log.debug("Student will be created: {}", data.getEmail());
        }
    }

    /**
     * Determine recommendation based on capacity
     */
    private EnrollmentRecommendation determineRecommendation(
            int toEnroll,
            int available,
            int maxCapacity,
            int currentEnrolled
    ) {
        if (toEnroll <= available) {
            // Case 1: Capacity đủ
            return EnrollmentRecommendation.builder()
                    .type(RecommendationType.OK)
                    .message("Sufficient capacity. All students can be enrolled.")
                    .suggestedEnrollCount(toEnroll)
                    .build();
        }

        int exceededBy = toEnroll - available;
        double exceededPercentage = (double) exceededBy / maxCapacity * 100;

        if (exceededPercentage <= 20) {
            // Case 2: Vượt <= 20% → suggest override
            return EnrollmentRecommendation.builder()
                    .type(RecommendationType.OVERRIDE_AVAILABLE)
                    .message(String.format(
                            "Exceeds capacity by %d students (%.1f%%). You can override with approval reason.",
                            exceededBy, exceededPercentage
                    ))
                    .suggestedEnrollCount(null)
                    .build();
        }

        if (available > 0) {
            // Case 3: Vượt > 20% nhưng vẫn còn slots → suggest partial
            return EnrollmentRecommendation.builder()
                    .type(RecommendationType.PARTIAL_SUGGESTED)
                    .message(String.format(
                            "Exceeds capacity significantly (%.1f%%). Recommend enrolling only %d students (available slots).",
                            exceededPercentage, available
                    ))
                    .suggestedEnrollCount(available)
                    .build();
        }

        // Case 4: Class đã full
        return EnrollmentRecommendation.builder()
                .type(RecommendationType.BLOCKED)
                .message("Class is full. Cannot enroll any students without capacity override.")
                .suggestedEnrollCount(0)
                .build();
    }

    /**
     * Filter students theo enrollment strategy
     */
    private List<StudentEnrollmentData> filterStudentsByStrategy(
            ClassEnrollmentImportExecuteRequest request,
            ClassEntity classEntity,
            int currentEnrolled,
            Long enrolledBy
    ) {
        List<StudentEnrollmentData> studentsToEnroll;

        switch (request.getStrategy()) {
            case ALL:
                // Enroll tất cả valid students
                studentsToEnroll = request.getStudents().stream()
                        .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                                || s.getStatus() == StudentResolutionStatus.CREATE)
                        .collect(Collectors.toList());

                // Validate capacity
                if (currentEnrolled + studentsToEnroll.size() > classEntity.getMaxCapacity()) {
                    throw new CustomException(ErrorCode.CLASS_CAPACITY_EXCEEDED);
                }
                break;

            case PARTIAL:
                // Enroll chỉ selected students
                if (request.getSelectedStudentIds() == null || request.getSelectedStudentIds().isEmpty()) {
                    throw new CustomException(ErrorCode.PARTIAL_STRATEGY_MISSING_IDS);
                }

                Set<Long> selectedIds = new HashSet<>(request.getSelectedStudentIds());

                // FIXED: Use index-based matching for CREATE students instead of unreliable email hash
                Map<Integer, StudentEnrollmentData> createStudentsByIndex = new HashMap<>();
                for (int i = 0; i < request.getStudents().size(); i++) {
                    StudentEnrollmentData student = request.getStudents().get(i);
                    if (student.getStatus() == StudentResolutionStatus.CREATE) {
                        createStudentsByIndex.put(i, student); // zero-based index to match frontend
                    }
                }

                studentsToEnroll = new ArrayList<>();

                // Add FOUND students by resolvedStudentId
                for (StudentEnrollmentData student : request.getStudents()) {
                    if (student.getStatus() == StudentResolutionStatus.FOUND
                            && selectedIds.contains(student.getResolvedStudentId())) {
                        studentsToEnroll.add(student);
                    }
                }

                // Add CREATE students by index (frontend sends zero-based indices)
                for (Long selectedIndex : selectedIds) {
                    int index = selectedIndex.intValue();
                    if (createStudentsByIndex.containsKey(index)) {
                        studentsToEnroll.add(createStudentsByIndex.get(index));
                    }
                }

                log.debug("PARTIAL strategy: {} selected IDs, {} students to enroll",
                    selectedIds.size(), studentsToEnroll.size());

                // Validate capacity
                if (currentEnrolled + studentsToEnroll.size() > classEntity.getMaxCapacity()) {
                    throw new CustomException(ErrorCode.SELECTED_STUDENTS_EXCEED_CAPACITY);
                }
                break;

            case OVERRIDE:
                // Override capacity và enroll tất cả
                if (request.getOverrideReason() == null || request.getOverrideReason().length() < 20) {
                    throw new CustomException(ErrorCode.OVERRIDE_REASON_REQUIRED);
                }

                studentsToEnroll = request.getStudents().stream()
                        .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                                || s.getStatus() == StudentResolutionStatus.CREATE)
                        .collect(Collectors.toList());

                log.warn("CAPACITY_OVERRIDE: Class {} will enroll {} students (capacity: {}). Reason: {}. Approved by user {}",
                        request.getClassId(), studentsToEnroll.size(), classEntity.getMaxCapacity(),
                        request.getOverrideReason(), enrolledBy);
                break;

            default:
                throw new CustomException(ErrorCode.INVALID_ENROLLMENT_STRATEGY);
        }

        return studentsToEnroll;
    }

    /**
     * Core enrollment logic - batch enroll students vào class
     *
     * @param classId Class ID
     * @param studentIds List of student IDs to enroll
     * @param enrolledBy User ID performing enrollment
     * @param capacityOverride True if this enrollment exceeds capacity
     * @param overrideReason Reason for override (required if capacityOverride = true)
     */
    @Transactional
    public EnrollmentResult enrollStudents(
            Long classId,
            List<Long> studentIds,
            Long enrolledBy,
            boolean capacityOverride,
            String overrideReason
    ) {
        log.info("Enrolling {} students into class {} (override: {})", studentIds.size(), classId, capacityOverride);

        // 1. Validate class
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));

        // 2. Get all future sessions của class
        List<Session> futureSessions = sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                classId,
                LocalDate.now(),
                SessionStatus.PLANNED
        );

        if (futureSessions.isEmpty()) {
            throw new CustomException(ErrorCode.NO_FUTURE_SESSIONS);
        }

        log.info("Found {} future sessions for class", futureSessions.size());

        // 3. Batch insert enrollments
        List<Enrollment> enrollments = new ArrayList<>();
        for (Long studentId : studentIds) {
            // Check duplicate enrollment
            boolean alreadyEnrolled = enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                    classId, studentId, EnrollmentStatus.ENROLLED
            );
            if (alreadyEnrolled) {
                log.warn("Student {} is already enrolled in class {}", studentId, classId);
                throw new CustomException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }

            Enrollment enrollment = Enrollment.builder()
                    .classId(classId)
                    .studentId(studentId)
                    .status(EnrollmentStatus.ENROLLED)
                    .enrolledAt(OffsetDateTime.now())
                    .enrolledBy(enrolledBy)
                    .capacityOverride(capacityOverride)
                    .overrideReason(overrideReason)
                    .build();

            // Mid-course enrollment: track join_session_id
            if (LocalDate.now().isAfter(classEntity.getStartDate())) {
                Session firstFutureSession = futureSessions.get(0);
                enrollment.setJoinSessionId(firstFutureSession.getId());
                log.debug("Mid-course enrollment for student {}. Join session: {}",
                        studentId, firstFutureSession.getId());
            }

            enrollments.add(enrollment);
        }
        enrollmentRepository.saveAll(enrollments);

        log.info("Saved {} enrollment records", enrollments.size());

        // 4. Auto-generate student_session records
        List<StudentSession> studentSessions = new ArrayList<>();
        
        // Fetch all students to avoid lazy loading issues
        List<Long> enrolledStudentIds = enrollments.stream()
                .map(Enrollment::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Student> studentMap = studentRepository.findAllById(enrolledStudentIds).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        
        for (Enrollment enrollment : enrollments) {
            Student student = studentMap.get(enrollment.getStudentId());
            if (student == null) {
                log.error("Student not found: {}", enrollment.getStudentId());
                continue;
            }
            
            for (Session session : futureSessions) {
                // IMPORTANT: When using @MapsId:
                // 1. Create empty composite key object (JPA needs it to exist)
                // 2. Set the relationships - @MapsId will auto-populate the key fields
                StudentSession ss = new StudentSession();
                ss.setId(new StudentSession.StudentSessionId()); // Empty id - JPA will populate it
                ss.setStudent(student);  // @MapsId("studentId") extracts from this
                ss.setSession(session);  // @MapsId("sessionId") extracts from this
                ss.setAttendanceStatus(AttendanceStatus.PLANNED);
                ss.setIsMakeup(false);
                studentSessions.add(ss);
            }
        }
        studentSessionRepository.saveAll(studentSessions);

        log.info("Generated {} student_session records ({} sessions per student)",
                studentSessions.size(), futureSessions.size());

        // 5. Send notifications cho students và Academic Affairs
        sendEnrollmentNotifications(enrollments, classEntity);

        // 6. Send welcome emails (async) - COMMENTED OUT for future implementation
        // for (Long studentId : studentIds) {
        //     emailService.sendEnrollmentConfirmation(studentId, classId);
        // }
        log.info("Email sending skipped (not implemented yet)");

        // 7. Return result
        List<String> warnings = new ArrayList<>();
        if (LocalDate.now().isAfter(classEntity.getStartDate())) {
            warnings.add("Mid-course enrollment: Students will only be enrolled in future sessions");
        }

        return EnrollmentResult.builder()
                .enrolledCount(enrollments.size())
                .sessionsGeneratedPerStudent(futureSessions.size())
                .totalStudentSessionsCreated(studentSessions.size())
                .warnings(warnings)
                .build();
    }

    /**
     * Create student nhanh từ Excel data
     */
    private Student createStudentQuick(StudentEnrollmentData data, Long branchId, Long enrolledBy) {
        log.info("Creating new student: {}", data.getEmail());

        // 1. Create user_account
        UserAccount user = new UserAccount();
        user.setEmail(data.getEmail());
        user.setFullName(data.getFullName());
        user.setPhone(data.getPhone());
        user.setFacebookUrl(data.getFacebookUrl());
        user.setAddress(data.getAddress());
        user.setGender(data.getGender());
        user.setDob(data.getDob());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        UserAccount savedUser = userAccountRepository.save(user);

        log.debug("Created user_account: ID {}", savedUser.getId());

        // 2. Create student
        Student student = new Student();
        student.setUserAccount(savedUser);
        student.setStudentCode(generateStudentCode(branchId, data.getFullName(), data.getEmail()));
        Student savedStudent = studentRepository.save(student);

        log.debug("Created student: ID {}, Code {}", savedStudent.getId(), savedStudent.getStudentCode());

        // 3. Assign STUDENT role
        Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new EntityNotFoundException("STUDENT role not found"));

        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(savedUser.getId());
        userRoleId.setRoleId(studentRole.getId());

        UserRole userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setUserAccount(savedUser);
        userRole.setRole(studentRole);
        userRoleRepository.save(userRole);

        log.debug("Assigned STUDENT role to user {}", savedUser.getId());

        // 4. Assign to branch
        Branch branch = new Branch();
        branch.setId(branchId);

        UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
        userBranchId.setUserId(savedUser.getId());
        userBranchId.setBranchId(branchId);

        UserBranches userBranch = new UserBranches();
        userBranch.setId(userBranchId);
        userBranch.setUserAccount(savedUser);
        userBranch.setBranch(branch);
        // Note: assignedBy should be the enrolledBy user, but we don't have it here
        // This is acceptable as the enrollment record tracks enrolled_by
        userBranchesRepository.save(userBranch);

        log.debug("Assigned user {} to branch {}", savedUser.getId(), branchId);

        // REMOVED: Skill assessment creation from Excel data
        // Assessments are now handled separately through dedicated individual student creation workflow
        log.debug("Student creation completed without assessment data for student {}", savedStudent.getId());

        return savedStudent;
    }

    /**
     * Validate class có đủ điều kiện để enroll không
     */
    private ClassEntity validateClassForEnrollment(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));

        if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.CLASS_NOT_APPROVED);
        }

        if (!classEntity.getStatus().equals(ClassStatus.SCHEDULED)) {
            throw new CustomException(ErrorCode.CLASS_INVALID_STATUS);
        }

        return classEntity;
    }

    /**
     * Generate temporary password cho student mới
     */
    private String generateTemporaryPassword() {
        // Generate random 8-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Generate student code
     * Format: ST{branchId}{timestamp}
     */
    private String generateStudentCode(Long branchId, String fullName, String email) {
        String baseName;

        // Ưu tiên dùng fullName, nếu không có thì dùng email
        if (fullName != null && !fullName.trim().isEmpty()) {
            // Loại bỏ ký tự đặc biệt, spaces và chuyển thành uppercase
            baseName = fullName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            // Giới hạn độ dài tối đa 10 ký tự
            if (baseName.length() > 10) {
                baseName = baseName.substring(0, 10);
            }
        } else if (email != null && email.contains("@")) {
            // Lấy phần trước @ của email
            baseName = email.substring(0, email.indexOf("@")).replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            // Giới hạn độ dài tối đa 10 ký tự
            if (baseName.length() > 10) {
                baseName = baseName.substring(0, 10);
            }
        } else {
            // Fallback: dùng timestamp
            baseName = String.valueOf(System.currentTimeMillis()).substring(6);
        }

        // Thêm random number để đảm bảo uniqueness
        int randomSuffix = (int) (Math.random() * 1000);

        return String.format("ST%d%s%d", branchId, baseName, randomSuffix);
    }

    /**
     * Create initial skill assessment for new student
     * Only creates if level is provided and valid
     */
    private void createInitialSkillAssessment(Student student, String levelCode, Long assessedBy) {
        if (levelCode == null || levelCode.trim().isEmpty()) {
            log.debug("No level provided for student {}, skipping skill assessment creation", student.getId());
            return;
        }

        try {
            // Find level by code (assuming level code is unique across subjects for simplicity)
            // In real implementation, you might need subject ID as well
            Level level = levelRepository.findByCodeIgnoreCase(levelCode.trim())
                    .orElse(null);

            if (level == null) {
                log.warn("Level with code '{}' not found, skipping skill assessment creation for student {}",
                        levelCode, student.getId());
                return;
            }

            // Create initial skill assessment with GENERAL skill
            ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
            assessment.setStudent(student);
            assessment.setSkill(Skill.GENERAL);
            assessment.setLevel(level);
            assessment.setRawScore(BigDecimal.ZERO);
            assessment.setScaledScore(BigDecimal.ZERO);
            assessment.setScoreScale("0-100");
            assessment.setAssessmentCategory("PLACEMENT");
            assessment.setAssessmentDate(LocalDate.now());
            assessment.setAssessmentType("enrollment_initial");
            assessment.setNote("Initial assessment created during student enrollment");

            // Set assessed by user if provided, otherwise null
            if (assessedBy != null) {
                UserAccount assessedByUser = new UserAccount();
                assessedByUser.setId(assessedBy);
                assessment.setAssessedBy(assessedByUser);
            }

            replacementSkillAssessmentRepository.save(assessment);
            log.debug("Created initial skill assessment for student {} at level {}", student.getId(), levelCode);

        } catch (Exception e) {
            // Don't fail student creation if skill assessment creation fails
            log.error("Failed to create initial skill assessment for student {}: {}",
                    student.getId(), e.getMessage(), e);
        }
    }

    // REMOVED: Assessment-related methods from Excel enrollment workflow
// - createSkillAssessmentsFromExcelData()
// - parseSkillAssessmentData()
// - createSingleAssessment()
//
// Assessments are now handled separately through dedicated individual student creation workflow
// using CreateStudentRequest with SkillAssessmentInput DTOs
// This separation ensures Excel enrollment focuses on basic student information only

    /**
     * Enroll existing students vào class (Tab 1: Select Existing Students)
     * Multi-select manual enrollment cho ad-hoc enrollment
     *
     * @param request Request chứa classId, studentIds, và capacity override info
     * @param enrolledBy User ID của Academic Affair (for audit)
     * @return Enrollment result với counts
     */
    @Override
    @Transactional
    public EnrollmentResult enrollExistingStudents(
            EnrollExistingStudentsRequest request,
            Long enrolledBy
    ) {
        log.info("Enrolling {} existing students into class {} by user {}",
                request.getStudentIds().size(), request.getClassId(), enrolledBy);

        // 1. Validate class exists và đủ điều kiện enroll
        ClassEntity classEntity = validateClassForEnrollment(request.getClassId());

        // 2. Remove duplicates from studentIds
        List<Long> uniqueStudentIds = request.getStudentIds().stream()
                .distinct()
                .collect(Collectors.toList());

        if (uniqueStudentIds.size() < request.getStudentIds().size()) {
            log.warn("Removed {} duplicate student IDs from request",
                    request.getStudentIds().size() - uniqueStudentIds.size());
        }

        // 3. Validate all students exist
        List<Student> students = studentRepository.findAllById(uniqueStudentIds);
        if (students.size() != uniqueStudentIds.size()) {
            List<Long> foundIds = students.stream().map(Student::getId).collect(Collectors.toList());
            List<Long> missingIds = uniqueStudentIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            log.error("Students not found: {}", missingIds);
            throw new EntityNotFoundException("Some students not found: " + missingIds);
        }

        // 4. Check capacity
        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                request.getClassId(), EnrollmentStatus.ENROLLED
        );
        int maxCapacity = classEntity.getMaxCapacity();
        int availableSlots = maxCapacity - currentEnrolled;
        int requestedCount = uniqueStudentIds.size();

        log.info("Capacity check: {}/{} enrolled, {} requested, {} available",
                currentEnrolled, maxCapacity, requestedCount, availableSlots);

        // 5. Validate capacity override
        boolean needsOverride = requestedCount > availableSlots;
        boolean hasOverride = Boolean.TRUE.equals(request.getOverrideCapacity());

        if (needsOverride && !hasOverride) {
            log.error("Class capacity exceeded. Current: {}, Requested: {}, Max: {}",
                    currentEnrolled, requestedCount, maxCapacity);
            throw new CustomException(ErrorCode.CLASS_CAPACITY_EXCEEDED);
        }

        if (hasOverride) {
            if (request.getOverrideReason() == null || request.getOverrideReason().trim().isEmpty()) {
                throw new CustomException(ErrorCode.OVERRIDE_REASON_REQUIRED);
            }
            if (request.getOverrideReason().length() < 20) {
                throw new CustomException(ErrorCode.OVERRIDE_REASON_TOO_SHORT);
            }
            log.warn("CAPACITY_OVERRIDE: Class {} will enroll {} students (capacity: {}). Reason: '{}'. Approved by user {}",
                    request.getClassId(), requestedCount, maxCapacity,
                    request.getOverrideReason(), enrolledBy);
        }

        // 6. Execute enrollment using core logic
        EnrollmentResult result = enrollStudents(
                request.getClassId(),
                uniqueStudentIds,
                enrolledBy,
                hasOverride,
                request.getOverrideReason()
        );

        log.info("Successfully enrolled {} students into class {}. Total student_sessions: {}",
                result.getEnrolledCount(), request.getClassId(), result.getTotalStudentSessionsCreated());

        return result;
    }

    // ==================== NOTIFICATION METHODS ====================

    /**
     * Gửi notification khi student được enroll vào class
     */
    private void sendEnrollmentNotifications(List<Enrollment> enrollments, ClassEntity classEntity) {
        try {
            if (enrollments.isEmpty()) {
                log.debug("Không có enrollments nào để gửi notification");
                return;
            }

            // Gửi notification cho từng student
            for (Enrollment enrollment : enrollments) {
                sendEnrollmentNotificationToStudent(enrollment, classEntity);
            }

            // Gửi notification cho Academic Affairs của branch tương ứng
            sendEnrollmentNotificationToAcademicAffairs(enrollments, classEntity);

            log.info("Đã gửi notifications cho {} enrollments trong class {}",
                    enrollments.size(), classEntity.getId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi enrollment notifications cho class {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Gửi enrollment confirmation notification cho student
     */
    private void sendEnrollmentNotificationToStudent(Enrollment enrollment, ClassEntity classEntity) {
        try {
            Long studentUserId = enrollment.getStudent().getUserAccount().getId();

            String title = String.format("Xác nhận đăng ký lớp: %s", classEntity.getCode());
            String message = String.format(
                    "Bạn đã được đăng ký thành công vào lớp học %s (%s). " +
                    "Khóa học sẽ bắt đầu vào ngày %s. " +
                    "Lịch học: %s",
                    classEntity.getCode(),
                    classEntity.getCourse().getName(),
                    classEntity.getStartDate() != null ? classEntity.getStartDate().toString() : "sớm",
                    buildScheduleDisplay(classEntity)
            );

            notificationService.createNotificationWithReference(
                    studentUserId,
                    NotificationType.CLASS_REMINDER,
                    title,
                    message,
                    "Enrollment",
                    enrollment.getId()
            );

            log.debug("Đã gửi enrollment confirmation notification cho student {}", enrollment.getStudentId());
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho student {} về enrollment {}: {}",
                    enrollment.getStudentId(), enrollment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Gửi notification cho Academic Affairs khi có enrollment mới
     */
    private void sendEnrollmentNotificationToAcademicAffairs(List<Enrollment> enrollments, ClassEntity classEntity) {
        try {
            // Lấy branch của class để gửi cho Academic Affairs tương ứng
            Long branchId = classEntity.getBranch().getId();

            List<UserAccount> academicAffairsUsers = userAccountRepository.findByRoleCodeAndBranches(
                    "ACADEMIC_AFFAIRS", List.of(branchId));

            if (!academicAffairsUsers.isEmpty()) {
                String title = String.format("Đăng ký mới: %d học viên", enrollments.size());
                // Lấy số lượng enrolled students hiện tại
                Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                        classEntity.getId(), EnrollmentStatus.ENROLLED);

                String message = String.format(
                        "Có %d học viên mới đã được đăng ký vào lớp %s (%s). " +
                        "Tổng sĩ số hiện tại: %d/%d",
                        enrollments.size(),
                        classEntity.getCode(),
                        classEntity.getCourse().getName(),
                        currentEnrolled != null ? currentEnrolled : 0,
                        classEntity.getMaxCapacity()
                );

                List<Long> recipientIds = academicAffairsUsers.stream()
                        .map(UserAccount::getId)
                        .collect(Collectors.toList());

                notificationService.sendBulkNotificationsWithReference(
                        recipientIds,
                        NotificationType.SYSTEM_ALERT,
                        title,
                        message,
                        "Class",
                        classEntity.getId()
                );

                log.info("Đã gửi enrollment notification cho {} Academic Affairs users về class {}",
                        academicAffairsUsers.size(), classEntity.getId());
            } else {
                log.warn("Không tìm thấy Academic Affairs user nào cho branch {}", branchId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi notification cho Academic Affairs về enrollments trong class {}: {}",
                    classEntity.getId(), e.getMessage(), e);
        }
    }

    /**
     * Xây dựng chuỗi hiển thị lịch học từ class entity
     */
    private String buildScheduleDisplay(ClassEntity classEntity) {
        if (classEntity.getScheduleDays() == null || classEntity.getScheduleDays().length == 0) {
            return "Chưa có lịch cụ thể";
        }

        // Convert schedule days to day names
        String[] dayNames = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};
        StringBuilder schedule = new StringBuilder();

        for (int i = 0; i < classEntity.getScheduleDays().length; i++) {
            if (i > 0) schedule.append(", ");
            int dayIndex = classEntity.getScheduleDays()[i];
            if (dayIndex >= 0 && dayIndex < dayNames.length) {
                schedule.append(dayNames[dayIndex]);
            }
        }

        return schedule.toString();
    }
}
