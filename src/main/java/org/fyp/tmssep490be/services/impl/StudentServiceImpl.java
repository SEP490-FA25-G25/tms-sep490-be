package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.PolicyService;
import org.fyp.tmssep490be.services.StudentService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchRepository branchRepository;
    private final LevelRepository levelRepository;
    private final ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PolicyService policyService;

    @Override
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

        // 11. SEND WELCOME EMAIL with login credentials -- TODO

        log.info("Sent welcome email with credentials to: {}", savedUser.getEmail());

        return response;
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

}
