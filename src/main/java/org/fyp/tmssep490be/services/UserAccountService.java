package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UpdateUserRequest;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.repositories.RoleRepository;
import org.fyp.tmssep490be.repositories.UserRoleRepository;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.entities.Role;
import org.fyp.tmssep490be.entities.UserRole;
import org.fyp.tmssep490be.entities.Branch;
import org.fyp.tmssep490be.entities.UserBranches;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final EmailService emailService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        log.info("Creating new user with email: {}", request.getEmail());

        // Kiểm tra xem email đã tồn tại chưa
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại: " + request.getEmail());
        }

        // Tạo entity UserAccount từ request
        UserAccount user = new UserAccount();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setFacebookUrl(request.getFacebookUrl());
        user.setDob(request.getDob());
        user.setGender(request.getGender());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setStatus(request.getStatus());

        // Mã hóa password
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Lưu vào db
        user = userAccountRepository.save(user);
        log.info("User created with ID: {}", user.getId());

        // Gán role cho user
        for (Long roleId : request.getRoleIds()) {
            Role role = roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleId));
            UserRole userRole = new UserRole();
            userRole.setId(new UserRole.UserRoleId(user.getId(), roleId));
            userRole.setUserAccount(user);
            userRole.setRole(role);
            userRoleRepository.save(userRole);
        }

        // Gán branch cho user
        if (request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
            for (Long branchId : request.getBranchIds()) {
                Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new IllegalArgumentException("Branch không tồn tại: " + branchId));
                UserBranches userBranch = new UserBranches();
                userBranch.setUserAccount(user);
                userBranch.setBranch(branch);
                userBranchesRepository.save(userBranch);
            }
        }

        // Lấy danh sách tên chi nhánh
        String branchNames = "";
        if (request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
            branchNames = request.getBranchIds().stream()
                    .map(branchId -> branchRepository.findById(branchId)
                            .map(Branch::getName)
                            .orElse(""))
                    .filter(name -> !name.isEmpty())
                    .collect(Collectors.joining(", "));
        }

// Gửi email thông tin đăng nhập
        emailService.sendNewUserCredentialsAsync(
                user.getEmail(),
                user.getFullName(),
                user.getEmail(),
                request.getPassword(),
                branchNames
        );

        // Chuyển entity -> response DTO và return
        return mapToResponse(user);

    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user with ID: {}", userId);
        log.info("Request data - roleIds: {}, branchIds: {}", request.getRoleIds(), request.getBranchIds());

        // Tìm user theo userId
        UserAccount user = userAccountRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));

        // Cập nhật thông tin user
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
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        // Lưu vào db
        // userAccountRepository.save(user);

        // Cập nhật role
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            log.info("Updating roles for user {}: {}", user.getId(), request.getRoleIds());

            // Xóa tất cả role cũ (dùng deleteAll)
            if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
                userRoleRepository.deleteAll(user.getUserRoles());
                userRoleRepository.flush();
            }
            
           
            user.getUserRoles().clear();

            // Thêm role mới
            for (Long roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("Role không tồn tại: " + roleId));
                UserRole userRole = new UserRole();
                // Khởi tạo composite key cho UserRole
                userRole.setId(new UserRole.UserRoleId(user.getId(), roleId));
                userRole.setUserAccount(user);
                userRole.setRole(role);
                userRoleRepository.save(userRole);
                
                user.getUserRoles().add(userRole);
                
                log.info("Added role {} for user {}", roleId, user.getId());
            }
        } else {
            log.info("No roleIds provided, keeping existing roles for user {}", user.getId());
        }

        // Cập nhật branch
        if (request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
            log.info("Updating branches for user {}: {}", user.getId(), request.getBranchIds());

            // Xóa tất cả branch cũ (dùng deleteAll)
            if (user.getUserBranches() != null && !user.getUserBranches().isEmpty()) {
                userBranchesRepository.deleteAll(user.getUserBranches());
                userBranchesRepository.flush();
            }
            // userBranchesRepository.deleteByUserAccount(user); // KO DÙNG JPQL DELETE NỮA
            
            // Đồng bộ collection trong memory
            user.getUserBranches().clear();

            // Thêm branch mới
            for (Long branchId : request.getBranchIds()) {
                Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new IllegalArgumentException("Branch không tồn tại: " + branchId));
                UserBranches userBranch = new UserBranches();
                userBranch.setUserAccount(user);
                userBranch.setBranch(branch);
                userBranchesRepository.save(userBranch);
                
                // Add vào memory
                user.getUserBranches().add(userBranch);
                
                log.info("Added branch {} for user {}", branchId, user.getId());
            }
        } else {
            log.info("No branchIds provided, keeping existing branches for user {}", user.getId());
        }

        log.info("User updated successfully with ID: {}", user.getId());

        // Không cần refresh entity nữa vì đã sync memory
        // UserAccount refreshedUser = userAccountRepository.findById(user.getId())
        //         .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + user.getId()));

        return mapToResponse(user);

    }

    @Transactional
    public void deleteUser(Long userId) {

        log.info("Deleting user with ID: {}", userId);

        // tìm user theo userId
        UserAccount user = userAccountRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));

        // xóa user
        user.setStatus(UserStatus.INACTIVE);
        userAccountRepository.save(user);

        log.info("User set to inactive successfully with ID: {}", user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        log.info("Getting user with ID: {}", userId);

        UserAccount user = userAccountRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable, String search, String role, String status, Long branchId) {
        log.info("Getting all users with search: {}, role: {}, status: {}, branchId: {}", search, role, status, branchId);
        UserStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = UserStatus.valueOf(status);
        }

        Page<UserAccount> users = userAccountRepository.findAllWithFilters(search, role, statusEnum, branchId, pageable);
        return users.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("Getting user by email: {}", email);

        UserAccount user = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại với email: " + email));

        return mapToResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, String status) {
        log.info("Updating status for user {}: {}", userId, status);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại: " + userId));

        user.setStatus(UserStatus.valueOf(status));
        userAccountRepository.save(user);

        return mapToResponse(user);
    }

    public boolean checkEmailExists(String email) {
        return userAccountRepository.existsByEmail(email);
    }

    public boolean checkPhoneExists(String phone) {
        return userAccountRepository.existsByPhone(phone);
    }

    private UserResponse mapToResponse(UserAccount user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .facebookUrl(user.getFacebookUrl())
                .dob(user.getDob())
                .gender(user.getGender())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .roles(user.getUserRoles().stream().map(userRole -> userRole.getRole().getCode()).collect(Collectors.toSet()))
                .branches(user.getUserBranches().stream().map(userBranch -> userBranch.getBranch().getName()).collect(Collectors.toSet()))
                .build();
    }


}
