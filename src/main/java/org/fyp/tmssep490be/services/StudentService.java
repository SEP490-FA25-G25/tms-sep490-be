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

        if (userAccountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
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

    /**
     * Get students list with filters for AA operations (search, on-behalf requests)
     */
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
        resolveStudentsForImport(parsedData);

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
        log.info("Executing student import for branch {} by user {}", request.getBranchId(), currentUserId);

        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new CustomException(ErrorCode.BRANCH_NOT_FOUND));
        List<Long> userBranches = getUserAccessibleBranches(currentUserId);
        if (!userBranches.contains(request.getBranchId())) {
            throw new CustomException(ErrorCode.BRANCH_ACCESS_DENIED);
        }

        // Filter students to create (only CREATE status)
        List<StudentImportData> studentsToCreate;
        if (request.getSelectedIndices() != null && !request.getSelectedIndices().isEmpty()) {
            // Only create selected students
            studentsToCreate = new java.util.ArrayList<>();
            for (Integer index : request.getSelectedIndices()) {
                if (index >= 0 && index < request.getStudents().size()) {
                    StudentImportData student = request.getStudents().get(index);
                    if (student.getStatus() == StudentImportData.StudentImportStatus.CREATE) {
                        studentsToCreate.add(student);
                    }
                }
            }
        } else {
            // Create all CREATE status students
            studentsToCreate = request.getStudents().stream()
                    .filter(s -> s.getStatus() == StudentImportData.StudentImportStatus.CREATE)
                    .collect(Collectors.toList());
        }

        if (studentsToCreate.isEmpty()) {
            throw new CustomException(ErrorCode.NO_STUDENTS_TO_IMPORT);
        }

        log.info("Creating {} new students", studentsToCreate.size());

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

        log.info("Student import completed: {} created, {} failed", successCount, failedCount);

        int skippedExisting = (int) request.getStudents().stream()
                .filter(s -> s.getStatus() == StudentImportData.StudentImportStatus.FOUND)
                .count();

        return StudentImportResult.builder()
                .branchId(request.getBranchId())
                .branchName(branch.getName())
                .totalAttempted(studentsToCreate.size())
                .successfulCreations(successCount)
                .skippedExisting(skippedExisting)
                .failedCreations(failedCount)
                .createdStudents(createdStudents)
                .importedBy(currentUserId)
                .importedAt(OffsetDateTime.now())
                .build();
    }

    private void resolveStudentsForImport(List<StudentImportData> parsedData) {
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
                    log.debug("Found existing student by email: {} -> {}", data.getEmail(), existingStudent.get().getStudentCode());
                    continue;
                }
            }

            data.setStatus(StudentImportData.StudentImportStatus.CREATE);
            log.debug("Student will be created: {}", data.getEmail());
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

}
