package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UpdateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.UserAccountService;
import org.fyp.tmssep490be.services.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * User account service implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Validate email uniqueness
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create user account
        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setFacebookUrl(request.getFacebookUrl());
        user.setDob(request.getDob());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setStatus(request.getStatus());

        // Save user
        user = userAccountRepository.save(user);
        log.info("User account created with ID: {}", user.getId());

        // Assign roles
        Set<UserRole> userRoles = new HashSet<>();
        for (Long roleId : request.getRoleIds()) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

            UserRole userRole = new UserRole();
            UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
            userRoleId.setUserId(user.getId());
            userRoleId.setRoleId(role.getId());
            userRole.setId(userRoleId);
            userRole.setUserAccount(user);
            userRole.setRole(role);

            userRoles.add(userRoleRepository.save(userRole));
        }
        user.setUserRoles(userRoles);

        // Assign branches if provided
        Set<Long> assignedBranchIds = new HashSet<>();
        if (request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
            Set<UserBranches> userBranches = new HashSet<>();
            for (Long branchId : request.getBranchIds()) {
                Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

                UserBranches userBranch = new UserBranches();
                UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
                userBranchId.setUserId(user.getId());
                userBranchId.setBranchId(branch.getId());
                userBranch.setId(userBranchId);
                userBranch.setUserAccount(user);
                userBranch.setBranch(branch);

                userBranches.add(userBranchesRepository.save(userBranch));
                assignedBranchIds.add(branchId);
            }
            user.setUserBranches(userBranches);
        }

        // Auto-create Teacher or Student profile if role matches
        for (Role role : user.getUserRoles().stream().map(UserRole::getRole).collect(Collectors.toSet())) {
            String roleCode = role.getCode();
            
            if ("TEACHER".equals(roleCode)) {
                // Auto-create Teacher profile
                if (!teacherRepository.findByUserAccountId(user.getId()).isPresent()) {
                    Teacher teacher = new Teacher();
                    teacher.setUserAccount(user);
                    teacher.setEmployeeCode(generateEmployeeCode(assignedBranchIds));
                    teacher.setHireDate(java.time.LocalDate.now());
                    java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
                    teacher.setCreatedAt(now);
                    teacher.setUpdatedAt(now);
                    teacherRepository.save(teacher);
                    log.info("Auto-created Teacher profile for user: {}", user.getEmail());
                }
            } else if ("STUDENT".equals(roleCode)) {
                // Auto-create Student profile
                if (!studentRepository.findByUserAccountId(user.getId()).isPresent()) {
                    if (assignedBranchIds.isEmpty()) {
                        log.warn("Cannot auto-create Student profile: no branch assigned for user: {}", user.getEmail());
                    } else {
                        Student student = new Student();
                        student.setUserAccount(user);
                        // Use first branch for student code generation
                        Long branchId = assignedBranchIds.iterator().next();
                        student.setStudentCode(generateStudentCode(branchId, user.getFullName(), user.getEmail()));
                        studentRepository.save(student);
                        log.info("Auto-created Student profile for user: {}", user.getEmail());
                    }
                }
            }
        }

        log.info("User created successfully: {}", user.getEmail());

        // Send welcome email
        try {
            String verificationLink = "http://localhost:3000/verify?email=" + user.getEmail() + "&token=" + user.getId();
            emailService.sendWelcomeEmailAsync(user.getEmail(), user.getFullName(), verificationLink);
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
            // Don't fail the user creation if email fails
        }

        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable, String search, String role, String status) {
        log.debug("Fetching all users with pagination and filters - search: {}, role: {}, status: {}", search, role, status);
        
        // Normalize search term - trim and handle null/empty
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String roleCode = (role != null && !role.trim().isEmpty()) ? role.trim() : null;
        UserStatus statusEnum = null;
        
        // Validate and convert status string to enum
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = UserStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status value provided: {}, ignoring status filter", status);
            }
        }
        
        // Use filtered query if any filter is provided, otherwise use simple findAll
        Page<UserAccount> usersPage;
        if (searchTerm != null || roleCode != null || statusEnum != null) {
            usersPage = userAccountRepository.findAllWithFilters(searchTerm, roleCode, statusEnum, pageable);
        } else {
            usersPage = userAccountRepository.findAll(pageable);
        }
        
        return usersPage.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, String status) {
        log.info("Updating status for user ID: {} to {}", id, status);

        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        try {
            UserStatus userStatus = UserStatus.valueOf(status.toUpperCase());
            user.setStatus(userStatus);
            user = userAccountRepository.save(user);
            log.info("User status updated successfully");
            return mapToResponse(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status value: " + status);
        }
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user ID: {}", id);

        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Update basic fields if provided
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getFacebookUrl() != null) {
            user.setFacebookUrl(request.getFacebookUrl());
        }
        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // Update roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            // Remove existing roles
            user.getUserRoles().clear();
            // Add new roles
            for (Long roleId : request.getRoleIds()) {
                org.fyp.tmssep490be.entities.Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found with ID: " + roleId));
                org.fyp.tmssep490be.entities.UserRole userRole = new org.fyp.tmssep490be.entities.UserRole();
                userRole.setUserAccount(user);
                userRole.setRole(role);
                user.getUserRoles().add(userRole);
            }
        }

        // Update branches if provided
        if (request.getBranchIds() != null) {
            // Remove existing branches
            user.getUserBranches().clear();
            // Add new branches
            for (Long branchId : request.getBranchIds()) {
                org.fyp.tmssep490be.entities.Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found with ID: " + branchId));
                org.fyp.tmssep490be.entities.UserBranches userBranch = new org.fyp.tmssep490be.entities.UserBranches();
                org.fyp.tmssep490be.entities.UserBranches.UserBranchesId userBranchId = 
                        new org.fyp.tmssep490be.entities.UserBranches.UserBranchesId();
                userBranchId.setUserId(user.getId());
                userBranchId.setBranchId(branchId);
                userBranch.setId(userBranchId);
                userBranch.setUserAccount(user);
                userBranch.setBranch(branch);
                userBranch.setAssignedAt(java.time.OffsetDateTime.now());
                user.getUserBranches().add(userBranch);
            }
        }

        user = userAccountRepository.save(user);
        log.info("User updated successfully: {}", user.getEmail());
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        UserAccount user = userAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));

        // Soft delete by setting status to INACTIVE
        user.setStatus(UserStatus.INACTIVE);
        userAccountRepository.save(user);

        log.info("User soft deleted successfully: {}", user.getEmail());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userAccountRepository.existsByEmail(email);
    }

    /**
     * Generate employee code for teacher
     */
    private String generateEmployeeCode(Set<Long> branchIds) {
        Long branchId = branchIds.stream().min(Long::compareTo).orElse(0L);
        String code;
        boolean exists;
        do {
            int random = (int) (Math.random() * 1000);
            code = String.format("TC%d%03d", branchId, random);
            final String finalCode = code;
            exists = teacherRepository.findAll().stream().anyMatch(t -> finalCode.equals(t.getEmployeeCode()));
        } while (exists);
        return code;
    }

    /**
     * Generate student code
     */
    private String generateStudentCode(Long branchId, String fullName, String email) {
        String baseName;
        if (fullName != null && !fullName.trim().isEmpty()) {
            baseName = fullName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (baseName.length() > 10) {
                baseName = baseName.substring(0, 10);
            }
        } else if (email != null && email.contains("@")) {
            baseName = email.substring(0, email.indexOf("@"))
                    .replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
            if (baseName.length() > 10) {
                baseName = baseName.substring(0, 10);
            }
        } else {
            baseName = String.valueOf(System.currentTimeMillis()).substring(6);
        }
        String code;
        boolean exists;
        do {
            int randomSuffix = (int) (Math.random() * 1000);
            code = String.format("ST%d%s%03d", branchId, baseName, randomSuffix);
            final String finalCode = code;
            exists = studentRepository.findByStudentCode(finalCode).isPresent();
        } while (exists);
        return code;
    }

    /**
     * Map UserAccount entity to UserResponse DTO
     */
    private UserResponse mapToResponse(UserAccount user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toSet());

        Set<String> branches = user.getUserBranches().stream()
                .map(ub -> ub.getBranch().getName())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .facebookUrl(user.getFacebookUrl())
                .dob(user.getDob())
                .gender(user.getGender())
                .address(user.getAddress())
                .status(user.getStatus())
                .roles(roles)
                .branches(branches)
                .build();
    }
}

