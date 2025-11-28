package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * User account service interface
 */
public interface UserAccountService {

    /**
     * Create new user account (admin only)
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Get user by ID
     */
    UserResponse getUserById(Long id);

    /**
     * Get user by email
     */
    UserResponse getUserByEmail(String email);

    /**
     * Get all users with pagination and filters
     * @param pageable pagination parameters
     * @param search search term for email, fullName, phone
     * @param role filter by role code (e.g., "TEACHER", "STUDENT")
     * @param status filter by status (e.g., "ACTIVE", "INACTIVE", "SUSPENDED")
     */
    Page<UserResponse> getAllUsers(Pageable pageable, String search, String role, String status);

    /**
     * Update user status
     */
    UserResponse updateUserStatus(Long id, String status);

    /**
     * Update user information (admin only)
     * Note: Email and password cannot be changed via this endpoint
     */
    UserResponse updateUser(Long id, org.fyp.tmssep490be.dtos.user.UpdateUserRequest request);

    /**
     * Delete user (soft delete or revoke access)
     */
    void deleteUser(Long id);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
}
