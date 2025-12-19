package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchRepository branchRepository;
    private final LevelRepository levelRepository;
    private final EmailService emailService;
    private final ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PolicyService policyService;
    private final ExcelParserService excelParserService;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public CreateStudentResponse createStudent(CreateStudentRequest request, Long currentUserId) {
        log.info("Creating new student with email: {} by user: {}", request.getEmail(), currentUserId);

        Optional<UserAccount> existingUserOpt = userAccountRepository.findByEmail(request.getEmail());
        if (existingUserOpt.isPresent()) {
            log.info("User with email {} already exists, attempting smart sync to branch", request.getEmail());
            return handleExistingStudentSync(existingUserOpt.get(), request, currentUserId);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userAccountRepository.existsByPhone(request.getPhone())) {
            throw new CustomException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
        }

        // Kiểm tra giáo vụ lúc tạo student thì có quyền truy cập chi nhánh này hay không
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(request.getBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Validate lại vì những skill này được fix cứng từ frontend
        if (request.getSkillAssessments() != null && !request.getSkillAssessments().isEmpty()) {
            for (SkillAssessmentInput assessment : request.getSkillAssessments()) {
                if (!levelRepository.existsById(assessment.getLevelId())) {
                    throw new CustomException(ErrorCode.LEVEL_NOT_FOUND);
                }
            }
        }

        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setFacebookUrl(request.getFacebookUrl());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setDob(request.getDob());
        user.setStatus(UserStatus.ACTIVE);

        // Lấy mật khẩu mặc định từ policy
        String defaultPassword = policyService.getGlobalString("student.default_password", "12345678");
        user.setPasswordHash(passwordEncoder.encode(defaultPassword));
        log.debug("Created user with default password for student: {}", request.getEmail());

        UserAccount savedUser = userAccountRepository.save(user);
        log.debug("Created user_account with ID: {}", savedUser.getId());

        Student student = new Student();
        student.setUserAccount(savedUser);
        student.setStudentCode(generateStudentCode(
                request.getBranchId(),
                request.getFullName(),
                request.getEmail()
        ));

        Student savedStudent = studentRepository.save(student);
        log.info("Created student with ID: {} and code: {}", savedStudent.getId(), savedStudent.getStudentCode());

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

        UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
        userBranchId.setUserId(savedUser.getId());
        userBranchId.setBranchId(request.getBranchId());

        UserBranches userBranch = new UserBranches();
        userBranch.setId(userBranchId);
        userBranch.setUserAccount(savedUser);
        userBranch.setBranch(branch);

        UserAccount assignedBy = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userBranch.setAssignedBy(assignedBy);
        userBranchesRepository.save(userBranch);
        log.debug("Assigned user {} to branch {}", savedUser.getId(), request.getBranchId());

        // Biến này để trả lại giáo vụ xem đã tạo bao nhiêu bài kiểm tra
        int assessmentsCreated = 0;
        if (request.getSkillAssessments() != null && !request.getSkillAssessments().isEmpty()) {
            for (SkillAssessmentInput input : request.getSkillAssessments()) {
                Level level = levelRepository.findById(input.getLevelId())
                        .orElseThrow(() -> new CustomException(ErrorCode.LEVEL_NOT_FOUND));

                ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
                assessment.setStudent(savedStudent);
                assessment.setSkill(input.getSkill());
                assessment.setLevel(level);
                assessment.setScore(input.getScore());
                assessment.setAssessmentDate(LocalDate.now());
                assessment.setAssessmentType("manual_creation");
                assessment.setNote(input.getNote());

                // Set người chấm điểm student này
                Long assessorId = input.getAssessedByUserId() != null ? input.getAssessedByUserId() : currentUserId;
                UserAccount assessedBy = userAccountRepository.findById(assessorId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                assessment.setAssessedBy(assessedBy);

                replacementSkillAssessmentRepository.save(assessment);
                assessmentsCreated++;
                log.debug("Created {} assessment for student {} at level {} with score {}",
                        input.getSkill(), savedStudent.getId(), level.getCode(), input.getScore());
            }
        }

        UserAccount creator = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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
                .defaultPassword(defaultPassword)
                .skillAssessmentsCreated(assessmentsCreated)
                .createdAt(savedUser.getCreatedAt())
                .createdBy(CreateStudentResponse.CreatedByInfo.builder()
                        .userId(creator.getId())
                        .fullName(creator.getFullName())
                        .build())
                .build();

        log.info("Successfully created student: {} with {} skill assessments",
                savedStudent.getStudentCode(), assessmentsCreated);

        emailService.sendNewStudentCredentialsAsync(
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedStudent.getStudentCode(),
                savedUser.getEmail(),
                defaultPassword,
                branch.getName()
        );

        log.info("Sent welcome email with credentials to: {}", savedUser.getEmail());

        return response;
    }

    @Transactional
    private CreateStudentResponse handleExistingStudentSync(
            UserAccount existingUser, CreateStudentRequest request, Long currentUserId
    ) {
        log.info("Handling existing user {} for branch sync", existingUser.getEmail());

        // Find the student entity
        Student student = studentRepository.findByUserAccountId(existingUser.getId())
                .orElseThrow(() -> {
                    log.error("User {} exists but no student record found", existingUser.getEmail());
                    return new CustomException(ErrorCode.STUDENT_NOT_FOUND);
                });

        // Validate branch access
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(request.getBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Check if already in this branch
        boolean alreadyInBranch = userBranchesRepository.existsByUserAccountIdAndBranchId(
                existingUser.getId(), request.getBranchId()
        );

        if (alreadyInBranch) {
            log.warn("Student {} already in branch {}", student.getStudentCode(), request.getBranchId());
            throw new CustomException(ErrorCode.STUDENT_ALREADY_IN_BRANCH);
        }

        // Add to new branch
        addStudentToBranch(existingUser.getId(), request.getBranchId(), currentUserId);
        log.info("Added existing student {} to branch {}", student.getStudentCode(), request.getBranchId());

        // Add new skill assessments if provided
        int assessmentsCreated = 0;
        if (request.getSkillAssessments() != null && !request.getSkillAssessments().isEmpty()) {
            for (SkillAssessmentInput input : request.getSkillAssessments()) {
                Level level = levelRepository.findById(input.getLevelId())
                        .orElseThrow(() -> new CustomException(ErrorCode.LEVEL_NOT_FOUND));

                ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
                assessment.setStudent(student);
                assessment.setSkill(input.getSkill());
                assessment.setLevel(level);
                assessment.setScore(input.getScore());
                assessment.setAssessmentDate(LocalDate.now());
                assessment.setAssessmentType("existing_student_sync");
                assessment.setNote(input.getNote());

                Long assessorId = input.getAssessedByUserId() != null ? input.getAssessedByUserId() : currentUserId;
                UserAccount assessedBy = userAccountRepository.findById(assessorId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                assessment.setAssessedBy(assessedBy);

                replacementSkillAssessmentRepository.save(assessment);
                assessmentsCreated++;
            }
        }

        UserAccount syncer = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Return response with isExistingStudent = true
        return CreateStudentResponse.builder()
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .userAccountId(existingUser.getId())
                .email(existingUser.getEmail())
                .fullName(existingUser.getFullName())
                .phone(existingUser.getPhone())
                .gender(existingUser.getGender())
                .dob(existingUser.getDob())
                .branchId(branch.getId())
                .branchName(branch.getName())
                .status(existingUser.getStatus())
                .defaultPassword(null) // Don't return password for existing students
                .skillAssessmentsCreated(assessmentsCreated)
                .createdAt(existingUser.getCreatedAt())
                .createdBy(CreateStudentResponse.CreatedByInfo.builder()
                        .userId(syncer.getId())
                        .fullName(syncer.getFullName())
                        .build())
                .isExistingStudent(true) // Flag to indicate this was a sync, not a new creation
                .build();
    }

    /**
     * Get students list with filters for AA operations (search, on-behalf requests)
```     */
    public Page<StudentListItemDTO> getStudents(
            List<Long> branchIds,
            String search,
            UserStatus status,
            Gender gender,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting students for user {} with filters: branchIds={}, search={}, status={}, gender={}",
                userId, branchIds, search, status, gender);

        // SECURITY: Validate and get branch IDs
        List<Long> userBranches = userBranchesRepository.findBranchIdsByUserId(userId);
        
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = userBranches;
        } else {
            // Validate provided branches are accessible
            if (!userBranches.containsAll(branchIds)) {
                throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
            }
        }

        // Map sort field
        pageable = mapStudentSortField(pageable);

        Page<Student> students = studentRepository.findStudentsWithFilters(branchIds, search, status, gender, pageable);

        return students.map(this::convertToStudentListItemDTO);
    }

    /**
     * Map sort fields from DTO field names to entity paths
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

        // If trying to sort by nested fields, use default (studentCode)
        if (hasNestedSort) {
            log.warn("Sorting by UserAccount fields not supported. Using default sort by studentCode.");
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
                        default -> "studentCode";
                    };
                    return new Sort.Order(order.getDirection(), mappedProperty);
                })
                .toList();

        Sort mappedSort = Sort.by(mappedOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
    }

    private StudentListItemDTO convertToStudentListItemDTO(Student student) {
        UserAccount user = student.getUserAccount();
        Branch branch = user.getUserBranches().isEmpty() ? null : user.getUserBranches().stream().findFirst().get().getBranch();

        // Get enrollment counts
        int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                student.getId(), EnrollmentStatus.ENROLLED);

        Enrollment latestEnrollment = enrollmentRepository.findLatestEnrollmentByStudent(student.getId());

        return StudentListItemDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .branchName(branch != null ? branch.getName() : null)
                .branchId(branch != null ? branch.getId() : null)
                .activeEnrollments((long) activeEnrollments)
                .lastEnrollmentDate(latestEnrollment != null && latestEnrollment.getEnrolledAt() != null
                        ? latestEnrollment.getEnrolledAt().toLocalDate()
                        : null)
                .canEnroll(activeEnrollments < policyService.getGlobalInt("student.max_concurrent_enrollments", 3))
                .build();
    }

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    public StudentDetailDTO getStudentDetail(Long studentId, Long userId) {
        log.debug("Getting student detail for student {} by user {}", studentId, userId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        return convertToStudentDetailDTO(student);
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

    private StudentDetailDTO convertToStudentDetailDTO(Student student) {
        UserAccount user = student.getUserAccount();

        // Get branch info
        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            UserBranches firstBranch = user.getUserBranches().stream().findFirst().orElse(null);
            if (firstBranch != null) {
                branchId = firstBranch.getBranch().getId();
                branchName = firstBranch.getBranch().getName();
            }
        }

        java.util.List<StudentActiveClassDTO> currentClasses = student.getEnrollments().stream()
                .map(this::convertToStudentActiveClassDTO)
                .collect(Collectors.toList());

        java.util.List<StudentDetailDTO.SkillAssessmentDetailDTO> skillAssessments = 
                student.getReplacementSkillAssessments().stream()
                .sorted((a, b) -> b.getAssessmentDate().compareTo(a.getAssessmentDate()))
                .map(this::convertToSkillAssessmentDetailDTO)
                .collect(Collectors.toList());

        return StudentDetailDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dateOfBirth(user.getDob())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .facebookUrl(user.getFacebookUrl())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .branchName(branchName)
                .branchId(branchId)
                .currentClasses(currentClasses)
                .skillAssessments(skillAssessments)
                .build();
    }

    private StudentDetailDTO.SkillAssessmentDetailDTO convertToSkillAssessmentDetailDTO(ReplacementSkillAssessment assessment) {
        return StudentDetailDTO.SkillAssessmentDetailDTO.builder()
                .id(assessment.getId())
                .skill(assessment.getSkill() != null ? assessment.getSkill().name() : null)
                .levelCode(assessment.getLevel() != null ? assessment.getLevel().getCode() : null)
                .levelName(assessment.getLevel() != null ? assessment.getLevel().getName() : null)
                .score(assessment.getScore())
                .assessmentDate(assessment.getAssessmentDate())
                .assessmentType(assessment.getAssessmentType())
                .note(assessment.getNote())
                .assessedBy(assessment.getAssessedBy() != null ?
                        StudentDetailDTO.AssessedByDTO.builder()
                                .userId(assessment.getAssessedBy().getId())
                                .fullName(assessment.getAssessedBy().getFullName())
                                .build() : null)
                .build();
    }

    private StudentActiveClassDTO convertToStudentActiveClassDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();

        return StudentActiveClassDTO.builder()
                .id(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getSubject().getName())
                .branchName(classEntity.getBranch().getName())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                .attendanceRate(null)
                .averageScore(null)
                .build();
    }

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

    public byte[] generateStudentImportTemplate() {
        log.info("Generating student import template");

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Students");

            // Header style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            // Create header row
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Họ và tên (*)", "Email (*)", "Số điện thoại", "Facebook URL", "Địa chỉ", "Giới tính (*)", "Ngày sinh"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            org.apache.poi.ss.usermodel.Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Nguyễn Văn A");
            sampleRow.createCell(1).setCellValue("nguyenvana@email.com");
            sampleRow.createCell(2).setCellValue("0912345678");
            sampleRow.createCell(3).setCellValue("https://facebook.com/nguyenvana");
            sampleRow.createCell(4).setCellValue("123 Đường ABC, Quận 1, TP.HCM");
            sampleRow.createCell(5).setCellValue("MALE");
            sampleRow.createCell(6).setCellValue("2000-01-15");

            org.apache.poi.ss.usermodel.Row instructionRow = sheet.createRow(2);
            instructionRow.createCell(0).setCellValue("Nguyễn Thị B");
            instructionRow.createCell(1).setCellValue("nguyenthib@email.com");
            instructionRow.createCell(2).setCellValue("0987654321");
            instructionRow.createCell(3).setCellValue("");
            instructionRow.createCell(4).setCellValue("456 Đường XYZ, Quận 2, TP.HCM");
            instructionRow.createCell(5).setCellValue("FEMALE");
            instructionRow.createCell(6).setCellValue("15/03/1999");

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate student import template", e);
            throw new CustomException(ErrorCode.EXCEL_GENERATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public StudentImportPreview previewStudentImport(Long branchId, org.springframework.web.multipart.MultipartFile file, Long currentUserId) {
        log.info("Previewing student import for branch {} by user {}", branchId, currentUserId);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(branchId)) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        List<StudentImportData> parsedData = excelParserService.parseStudentImport(file);

        if (parsedData.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
        }

        log.info("Parsed {} students from Excel", parsedData.size());

        // 3. Resolve each student (FOUND/CREATE/ERROR)
        resolveStudentsForImport(parsedData, branchId);

        // 4. Calculate counts
        int foundCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentImportData.StudentImportStatus.FOUND)
                .count();
        int createCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentImportData.StudentImportStatus.CREATE)
                .count();
        int errorCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentImportData.StudentImportStatus.ERROR)
                .count();

        log.info("Import preview: {} found, {} to create, {} errors", foundCount, createCount, errorCount);

        // 5. Build warnings and errors
        List<String> warnings = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();

        if (foundCount > 0) {
            warnings.add(String.format("%d học viên đã tồn tại trong hệ thống và sẽ bị bỏ qua", foundCount));
        }
        if (errorCount > 0) {
            errors.add(String.format("%d học viên có lỗi dữ liệu", errorCount));
        }

        return StudentImportPreview.builder()
                .branchId(branchId)
                .branchName(branch.getName())
                .students(parsedData)
                .foundCount(foundCount)
                .createCount(createCount)
                .errorCount(errorCount)
                .totalValid(foundCount + createCount)
                .warnings(warnings)
                .errors(errors)
                .build();
    }

    @Transactional
    public StudentImportResult executeStudentImport(StudentImportExecuteRequest request, Long currentUserId) {
        log.info("=== executeStudentImport STARTED ===");
        log.info("Executing student import for branch {} by user {}", request.getBranchId(), currentUserId);
        log.info("Total students in request: {}, selectedIndices: {}", 
                request.getStudents().size(), request.getSelectedIndices());

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(request.getBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Filter students to create (only CREATE status)
        List<StudentImportData> studentsToCreate;
        List<StudentImportData> studentsToSync; // FOUND students with needsBranchSync
        
        if (request.getSelectedIndices() != null && !request.getSelectedIndices().isEmpty()) {
            // Only process selected students
            studentsToCreate = new java.util.ArrayList<>();
            studentsToSync = new java.util.ArrayList<>();
            for (Integer index : request.getSelectedIndices()) {
                if (index >= 0 && index < request.getStudents().size()) {
                    StudentImportData student = request.getStudents().get(index);
                    log.debug("Student at index {}: status={}, needsBranchSync={}, existingId={}", 
                            index, student.getStatus(), student.isNeedsBranchSync(), student.getExistingStudentId());
                    if (student.getStatus() == StudentImportData.StudentImportStatus.CREATE) {
                        studentsToCreate.add(student);
                    } else if (student.getStatus() == StudentImportData.StudentImportStatus.FOUND && student.isNeedsBranchSync()) {
                        studentsToSync.add(student);
                        log.info("Added student {} to sync list", student.getExistingStudentCode());
                    }
                }
            }
        } else {
            // Process all CREATE status students
            studentsToCreate = request.getStudents().stream()
                    .filter(s -> s.getStatus() == StudentImportData.StudentImportStatus.CREATE)
                    .collect(Collectors.toList());
            // Process all FOUND students with needsBranchSync
            studentsToSync = request.getStudents().stream()
                    .filter(s -> s.getStatus() == StudentImportData.StudentImportStatus.FOUND && s.isNeedsBranchSync())
                    .collect(Collectors.toList());
        }

        // Validate: must have at least one student to process (create or sync)
        if (studentsToCreate.isEmpty() && studentsToSync.isEmpty()) {
            throw new CustomException(ErrorCode.NO_STUDENTS_TO_IMPORT);
        }

        log.info("Processing {} students: {} to create, {} to sync", 
                studentsToCreate.size() + studentsToSync.size(), 
                studentsToCreate.size(), 
                studentsToSync.size());
        
        // Sync FOUND students who need branch sync
        int syncedCount = 0;
        for (StudentImportData data : studentsToSync) {
            try {
                // Get UserAccount ID from Student ID
                Student student = studentRepository.findById(data.getExistingStudentId())
                        .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));
                
                addStudentToBranch(student.getUserAccount().getId(), request.getBranchId(), currentUserId);
                syncedCount++;
                log.debug("Synced student {} to branch {}", data.getExistingStudentCode(), request.getBranchId());
            } catch (Exception e) {
                log.error("Failed to sync student {}: {}", data.getExistingStudentCode(), e.getMessage());
            }
        }
        
        if (syncedCount > 0) {
            log.info("Synced {} existing students to branch {}", syncedCount, request.getBranchId());
        }

        // If no students to create, skip creation logic
        if (studentsToCreate.isEmpty()) {
            log.info("No new students to create, only synced existing students");
            int skippedExisting = (int) request.getStudents().stream()
                    .filter(s -> s.getStatus() == StudentImportData.StudentImportStatus.FOUND && !s.isNeedsBranchSync())
                    .count();

            return StudentImportResult.builder()
                    .branchId(request.getBranchId())
                    .branchName(branch.getName())
                    .totalAttempted(studentsToSync.size())
                    .successfulCreations(0)
                    .skippedExisting(skippedExisting)
                    .syncedToBranch(syncedCount)
                    .failedCreations(0)
                    .createdStudents(new java.util.ArrayList<>())
                    .importedBy(currentUserId)
                    .importedAt(OffsetDateTime.now())
                    .build();
        }

        // Get default password from policy
        String defaultPassword = policyService.getGlobalString("student.default_password", "12345678");

        // Get STUDENT role
        Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_ROLE_NOT_FOUND));

        // Get current user for assignedBy
        UserAccount assignedBy = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Create students
        List<StudentImportResult.CreatedStudentInfo> createdStudents = new java.util.ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (StudentImportData data : studentsToCreate) {
            try {
                // Double-check email uniqueness (in case of race condition)
                if (userAccountRepository.findByEmail(data.getEmail()).isPresent()) {
                    log.warn("Email {} already exists, skipping", data.getEmail());
                    failedCount++;
                    continue;
                }

                // Create UserAccount
                UserAccount user = new UserAccount();
                user.setEmail(data.getEmail());
                user.setFullName(data.getFullName());
                user.setPhone(data.getPhone());
                user.setFacebookUrl(data.getFacebookUrl());
                user.setAddress(data.getAddress());
                user.setGender(data.getGender());
                user.setDob(data.getDob());
                user.setStatus(UserStatus.ACTIVE);
                user.setPasswordHash(passwordEncoder.encode(defaultPassword));
                UserAccount savedUser = userAccountRepository.save(user);

                // Create Student
                Student student = new Student();
                student.setUserAccount(savedUser);
                student.setStudentCode(generateStudentCode(request.getBranchId(), data.getFullName(), data.getEmail()));
                Student savedStudent = studentRepository.save(student);

                // Assign STUDENT role
                UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
                userRoleId.setUserId(savedUser.getId());
                userRoleId.setRoleId(studentRole.getId());

                UserRole userRole = new UserRole();
                userRole.setId(userRoleId);
                userRole.setUserAccount(savedUser);
                userRole.setRole(studentRole);
                userRoleRepository.save(userRole);

                // Assign to branch
                UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
                userBranchId.setUserId(savedUser.getId());
                userBranchId.setBranchId(request.getBranchId());

                UserBranches userBranch = new UserBranches();
                userBranch.setId(userBranchId);
                userBranch.setUserAccount(savedUser);
                userBranch.setBranch(branch);
                userBranch.setAssignedBy(assignedBy);
                userBranchesRepository.save(userBranch);

                // Add to result
                createdStudents.add(StudentImportResult.CreatedStudentInfo.builder()
                        .studentId(savedStudent.getId())
                        .studentCode(savedStudent.getStudentCode())
                        .fullName(savedUser.getFullName())
                        .email(savedUser.getEmail())
                        .defaultPassword(defaultPassword)
                        .build());

                successCount++;
                log.debug("Created student: {} ({})", savedStudent.getStudentCode(), data.getEmail());

                // Send welcome email (async)
                emailService.sendNewStudentCredentialsAsync(
                        savedUser.getEmail(),
                        savedUser.getFullName(),
                        savedStudent.getStudentCode(),
                        savedUser.getEmail(),
                        defaultPassword,
                        branch.getName()
                );

            } catch (Exception e) {
                log.error("Failed to create student {}: {}", data.getEmail(), e.getMessage());
                failedCount++;
            }
        }

        log.info("Student import completed: {} created, {} synced, {} failed", successCount, syncedCount, failedCount);

        int skippedExisting = (int) request.getStudents().stream()
                .filter(s -> s.getStatus() == StudentImportData.StudentImportStatus.FOUND && !s.isNeedsBranchSync())
                .count();

        return StudentImportResult.builder()
                .branchId(request.getBranchId())
                .branchName(branch.getName())
                .totalAttempted(studentsToCreate.size())
                .successfulCreations(successCount)
                .skippedExisting(skippedExisting)
                .syncedToBranch(syncedCount)
                .failedCreations(failedCount)
                .createdStudents(createdStudents)
                .importedBy(currentUserId)
                .importedAt(OffsetDateTime.now())
                .build();
    }

    private void resolveStudentsForImport(List<StudentImportData> parsedData, Long branchId) {
        java.util.Set<String> seenEmails = new java.util.HashSet<>();

        for (StudentImportData data : parsedData) {
            // Skip if already has error from parsing
            if (data.getStatus() == StudentImportData.StudentImportStatus.ERROR) {
                continue;
            }

            // Validate required fields
            if (data.getEmail() == null || data.getEmail().isBlank()) {
                data.setStatus(StudentImportData.StudentImportStatus.ERROR);
                data.setErrorMessage("Email là bắt buộc");
                continue;
            }
            if (data.getFullName() == null || data.getFullName().isBlank()) {
                data.setStatus(StudentImportData.StudentImportStatus.ERROR);
                data.setErrorMessage("Họ tên là bắt buộc");
                continue;
            }
            if (data.getGender() == null) {
                data.setStatus(StudentImportData.StudentImportStatus.ERROR);
                data.setErrorMessage("Giới tính là bắt buộc");
                continue;
            }

            // Validate email format
            if (!isValidEmail(data.getEmail())) {
                data.setStatus(StudentImportData.StudentImportStatus.ERROR);
                data.setErrorMessage("Email không đúng định dạng");
                continue;
            }

            // Check duplicate trong file Excel
            String emailLower = data.getEmail().toLowerCase();
            if (seenEmails.contains(emailLower)) {
                data.setStatus(StudentImportData.StudentImportStatus.ERROR);
                data.setErrorMessage("Email trùng lặp trong file Excel");
                continue;
            }
            seenEmails.add(emailLower);

            // Check if email already exists in system
            Optional<UserAccount> existingUser = userAccountRepository.findByEmail(data.getEmail());
            if (existingUser.isPresent()) {
                Optional<Student> existingStudent = studentRepository.findByUserAccountId(existingUser.get().getId());
                if (existingStudent.isPresent()) {
                    data.setStatus(StudentImportData.StudentImportStatus.FOUND);
                    data.setExistingStudentId(existingStudent.get().getId());
                    data.setExistingStudentCode(existingStudent.get().getStudentCode());
                    
                    // Check if student is in target branch
                    boolean inBranch = userBranchesRepository.existsByUserAccountIdAndBranchId(
                            existingUser.get().getId(), branchId);
                    
                    data.setNeedsBranchSync(!inBranch);
                    if (!inBranch) {
                        data.setNote("Học viên từ chi nhánh khác, sẽ được tự động thêm vào chi nhánh này");
                    }
                    
                    log.debug("Found existing student by email: {} -> {}, needsSync: {}", 
                            data.getEmail(), existingStudent.get().getStudentCode(), !inBranch);
                    continue;
                }
            }

            data.setStatus(StudentImportData.StudentImportStatus.CREATE);
            log.debug("Student will be created: {}", data.getEmail());
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        // Match EnrollmentService regex - require TLD (.com, .vn, etc.)
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public CheckStudentExistenceResponse checkStudentExistence(
            String type, String value, Long currentBranchId, Long currentUserId
    ) {
        log.debug("Checking student existence: type={}, value={}, branchId={}", type, value, currentBranchId);

        // Validate type
        if (!type.equalsIgnoreCase("EMAIL") && !type.equalsIgnoreCase("PHONE")) {
            throw new CustomException(ErrorCode.INVALID_EXISTENCE_CHECK_TYPE);
        }

        // Validate branch access
        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(currentBranchId)) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        Optional<UserAccount> userOpt;
        if (type.equalsIgnoreCase("EMAIL")) {
            userOpt = userAccountRepository.findByEmail(value);
        } else {
            userOpt = userAccountRepository.findByPhone(value);
        }

        if (userOpt.isEmpty()) {
            return CheckStudentExistenceResponse.builder()
                    .exists(false)
                    .canAddToCurrentBranch(true)
                    .build();
        }

        UserAccount user = userOpt.get();
        Optional<Student> studentOpt = studentRepository.findByUserAccountId(user.getId());

        if (studentOpt.isEmpty()) {
            // User exists but not a student (maybe teacher, AA, etc.)
            // Get user's primary role for display (using role code like TEACHER, QA, etc.)
            String roleDisplay = user.getUserRoles().stream()
                    .findFirst()
                    .map(userRole -> userRole.getRole().getCode())
                    .orElse("USER");
            
            return CheckStudentExistenceResponse.builder()
                    .exists(false) // Not a student
                    .isUserAccount(true) // But is an existing user
                    .userRole(roleDisplay)
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .canAddToCurrentBranch(false) // Cannot use this email/phone
                    .build();
        }

        Student student = studentOpt.get();

        // Get all branches this student belongs to
        List<Long> studentBranchIds = userBranchesRepository.findBranchIdsByUserId(user.getId());
        List<Branch> studentBranches = branchRepository.findAllById(studentBranchIds);

        List<CheckStudentExistenceResponse.BranchInfo> branchInfos = studentBranches.stream()
                .map(branch -> CheckStudentExistenceResponse.BranchInfo.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .code(branch.getCode())
                        .build())
                .collect(Collectors.toList());

        boolean canAdd = !studentBranchIds.contains(currentBranchId);

        return CheckStudentExistenceResponse.builder()
                .exists(true)
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .currentBranches(branchInfos)
                .canAddToCurrentBranch(canAdd)
                .build();
    }

    @Transactional
    public SyncToBranchResponse syncStudentToBranch(
            Long studentId, SyncToBranchRequest request, Long currentUserId
    ) {
        log.info("Syncing student {} to branch {} by user {}",
                studentId, request.getTargetBranchId(), currentUserId);

        // Validate student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        UserAccount user = student.getUserAccount();

        // Validate branch exists and user has access
        Branch targetBranch = branchRepository.findById(request.getTargetBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(request.getTargetBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Check if student already in this branch
        boolean alreadyInBranch = userBranchesRepository.existsByUserAccountIdAndBranchId(
                user.getId(), request.getTargetBranchId()
        );

        if (alreadyInBranch) {
            throw new CustomException(ErrorCode.STUDENT_ALREADY_IN_BRANCH);
        }

        // Update user info if provided
        boolean userUpdated = false;
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && !request.getPhone().equals(user.getPhone())) {
            // Check phone not taken by another user
            if (userAccountRepository.existsByPhone(request.getPhone())) {
                Optional<UserAccount> existingUserWithPhone = userAccountRepository.findByPhone(request.getPhone());
                if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(user.getId())) {
                    throw new CustomException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
                }
            }
            user.setPhone(request.getPhone());
            userUpdated = true;
        }

        if (request.getAddress() != null && !request.getAddress().isBlank()
                && !request.getAddress().equals(user.getAddress())) {
            user.setAddress(request.getAddress());
            userUpdated = true;
        }

        if (userUpdated) {
            userAccountRepository.save(user);
            log.debug("Updated user info for student {}", studentId);
        }

        // Add to new branch
        UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
        userBranchId.setUserId(user.getId());
        userBranchId.setBranchId(request.getTargetBranchId());

        UserBranches userBranch = new UserBranches();
        userBranch.setId(userBranchId);
        userBranch.setUserAccount(user);
        userBranch.setBranch(targetBranch);

        UserAccount assignedBy = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userBranch.setAssignedBy(assignedBy);
        userBranchesRepository.save(userBranch);

        log.info("Added student {} to branch {}", studentId, request.getTargetBranchId());

        // Add new skill assessments if provided
        int assessmentsCreated = 0;
        if (request.getNewSkillAssessments() != null && !request.getNewSkillAssessments().isEmpty()) {
            for (SkillAssessmentInput input : request.getNewSkillAssessments()) {
                Level level = levelRepository.findById(input.getLevelId())
                        .orElseThrow(() -> new CustomException(ErrorCode.LEVEL_NOT_FOUND));

                ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
                assessment.setStudent(student);
                assessment.setSkill(input.getSkill());
                assessment.setLevel(level);
                assessment.setScore(input.getScore());
                assessment.setAssessmentDate(LocalDate.now());
                assessment.setAssessmentType("branch_sync");
                assessment.setNote(input.getNote());

                Long assessorId = input.getAssessedByUserId() != null ? input.getAssessedByUserId() : currentUserId;
                UserAccount assessedBy = userAccountRepository.findById(assessorId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
                assessment.setAssessedBy(assessedBy);

                replacementSkillAssessmentRepository.save(assessment);
                assessmentsCreated++;
                log.debug("Created {} assessment for student {} at level {}",
                        input.getSkill(), studentId, level.getCode());
            }
        }

        // Get all branches student now belongs to
        List<Long> allBranchIds = userBranchesRepository.findBranchIdsByUserId(user.getId());
        List<Branch> allBranches = branchRepository.findAllById(allBranchIds);

        List<SyncToBranchResponse.BranchInfo> allBranchInfos = allBranches.stream()
                .map(branch -> SyncToBranchResponse.BranchInfo.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .code(branch.getCode())
                        .build())
                .collect(Collectors.toList());

        return SyncToBranchResponse.builder()
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .userAccountId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dob(user.getDob())
                .status(user.getStatus())
                .allBranches(allBranchInfos)
                .newlyAddedBranch(SyncToBranchResponse.BranchInfo.builder()
                        .id(targetBranch.getId())
                        .name(targetBranch.getName())
                        .code(targetBranch.getCode())
                        .build())
                .newSkillAssessmentsCreated(assessmentsCreated)
                .syncedAt(OffsetDateTime.now())
                .syncedBy(SyncToBranchResponse.SyncedByInfo.builder()
                        .userId(assignedBy.getId())
                        .fullName(assignedBy.getFullName())
                        .build())
                .build();
    }

    @Transactional
    public void addStudentToBranch(Long userId, Long branchId, Long assignedByUserId) {
        log.debug("Adding user {} to branch {} by user {}", userId, branchId, assignedByUserId);

        // Check if already in branch
        boolean alreadyInBranch = userBranchesRepository.existsByUserAccountIdAndBranchId(userId, branchId);
        if (alreadyInBranch) {
            log.debug("User {} already in branch {}, skipping", userId, branchId);
            return;
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));

        UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
        userBranchId.setUserId(userId);
        userBranchId.setBranchId(branchId);

        UserBranches userBranch = new UserBranches();
        userBranch.setId(userBranchId);
        userBranch.setUserAccount(user);
        userBranch.setBranch(branch);

        UserAccount assignedBy = userAccountRepository.findById(assignedByUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userBranch.setAssignedBy(assignedBy);

        userBranchesRepository.save(userBranch);
        log.info("Added user {} to branch {}", userId, branchId);
    }

    // Student tự lấy profile của mình
    public StudentDetailDTO getStudentProfileByUserId(Long userId) {
        log.debug("Getting student profile for user {}", userId);
        
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        return convertToStudentDetailDTO(student);
    }

    // Student tự cập nhật profile của mình
    @Transactional
    public StudentDetailDTO updateStudentProfileByUserId(Long userId, org.fyp.tmssep490be.dtos.user.UpdateProfileRequest request) {
        log.debug("Updating student profile for user {}", userId);
        
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));
        
        UserAccount user = student.getUserAccount();
        
        // Update phone if provided
        if (request.getPhone() != null) {
            if (!request.getPhone().isEmpty()) {
                // Check phone not taken by another user
                Optional<UserAccount> existingUserWithPhone = userAccountRepository.findByPhone(request.getPhone());
                if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(userId)) {
                    throw new CustomException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
                }
            }
            user.setPhone(request.getPhone().isEmpty() ? null : request.getPhone());
        }
        
        // Update other fields
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().isEmpty() ? null : request.getAddress());
        }
        if (request.getFacebookUrl() != null) {
            user.setFacebookUrl(request.getFacebookUrl().isEmpty() ? null : request.getFacebookUrl());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().isEmpty() ? null : request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        
        userAccountRepository.save(user);
        log.info("Updated student profile for user {}", userId);
        
        return convertToStudentDetailDTO(student);
    }

    @Transactional
    public StudentDetailDTO updateStudent(Long studentId, UpdateStudentRequest request, Long currentUserId) {
        log.info("Updating student {} by user {}", studentId, currentUserId);

        // 1. Find student and validate access
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));
        validateStudentAccess(student, currentUserId);

        UserAccount user = student.getUserAccount();

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            userAccountRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(user.getId())) {
                            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
                        }
                    });
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String currentPhone = user.getPhone();
            if (currentPhone == null || !currentPhone.equals(request.getPhone())) {
                if (userAccountRepository.existsByPhone(request.getPhone())) {
                    throw new CustomException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
                }
            }
        }

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setFacebookUrl(request.getFacebookUrl());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setDob(request.getDateOfBirth());

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        userAccountRepository.save(user);
        log.debug("Updated user account fields for student {}", studentId);

        if (request.getSkillAssessments() != null && !request.getSkillAssessments().isEmpty()) {
            for (SkillAssessmentUpdateInput assessmentInput : request.getSkillAssessments()) {
                if (assessmentInput.getId() != null) {
                    updateExistingSkillAssessment(student, assessmentInput, currentUserId);
                } else {
                    createNewSkillAssessment(student, assessmentInput, currentUserId);
                }
            }
        }

        log.info("Successfully updated student {} by user {}", studentId, currentUserId);
        return convertToStudentDetailDTO(student);
    }

    private void updateExistingSkillAssessment(Student student, SkillAssessmentUpdateInput input, Long currentUserId) {
        ReplacementSkillAssessment assessment = replacementSkillAssessmentRepository.findById(input.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SKILL_ASSESSMENT_NOT_FOUND));

        if (!assessment.getStudent().getId().equals(student.getId())) {
            throw new CustomException(ErrorCode.SKILL_ASSESSMENT_NOT_FOUND);
        }

        Level level = levelRepository.findById(input.getLevelId())
                .orElseThrow(() -> new CustomException(ErrorCode.LEVEL_NOT_FOUND));

        assessment.setSkill(input.getSkill());
        assessment.setLevel(level);
        assessment.setScore(input.getScore());
        assessment.setAssessmentDate(input.getAssessmentDate() != null ? input.getAssessmentDate() : assessment.getAssessmentDate());
        assessment.setAssessmentType(input.getAssessmentType());
        assessment.setNote(input.getNote());

        if (input.getAssessedByUserId() != null) {
            UserAccount assessedBy = userAccountRepository.findById(input.getAssessedByUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            assessment.setAssessedBy(assessedBy);
        }

        replacementSkillAssessmentRepository.save(assessment);
        log.debug("Updated skill assessment {} for student {}", input.getId(), student.getId());
    }

    private void createNewSkillAssessment(Student student, SkillAssessmentUpdateInput input, Long currentUserId) {
        Level level = levelRepository.findById(input.getLevelId())
                .orElseThrow(() -> new CustomException(ErrorCode.LEVEL_NOT_FOUND));

        ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
        assessment.setStudent(student);
        assessment.setSkill(input.getSkill());
        assessment.setLevel(level);
        assessment.setScore(input.getScore());
        assessment.setAssessmentDate(input.getAssessmentDate() != null ? input.getAssessmentDate() : LocalDate.now());
        assessment.setAssessmentType(input.getAssessmentType() != null ? input.getAssessmentType() : "manual_update");
        assessment.setNote(input.getNote());

        Long assessorId = input.getAssessedByUserId() != null ? input.getAssessedByUserId() : currentUserId;
        UserAccount assessedBy = userAccountRepository.findById(assessorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        assessment.setAssessedBy(assessedBy);

        replacementSkillAssessmentRepository.save(assessment);
        log.debug("Created new skill assessment for student {} with skill {}", student.getId(), input.getSkill());
    }

    @Transactional
    public void deleteSkillAssessment(Long studentId, Long assessmentId, Long currentUserId) {
        log.info("Deleting skill assessment {} for student {} by user {}", assessmentId, studentId, currentUserId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));
        validateStudentAccess(student, currentUserId);

        ReplacementSkillAssessment assessment = replacementSkillAssessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SKILL_ASSESSMENT_NOT_FOUND));

        if (!assessment.getStudent().getId().equals(studentId)) {
            throw new CustomException(ErrorCode.SKILL_ASSESSMENT_NOT_FOUND);
        }

        replacementSkillAssessmentRepository.delete(assessment);
        log.info("Successfully deleted skill assessment {} for student {}", assessmentId, studentId);
    }

}

