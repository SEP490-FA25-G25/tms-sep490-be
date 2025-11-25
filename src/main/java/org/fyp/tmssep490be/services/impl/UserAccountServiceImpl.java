package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
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
            }
            user.setUserBranches(userBranches);
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

