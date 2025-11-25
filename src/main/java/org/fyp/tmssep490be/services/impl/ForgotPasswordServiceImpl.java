package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.auth.ForgotPasswordResponse;
import org.fyp.tmssep490be.dtos.auth.ResetPasswordResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.InvalidTokenException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.fyp.tmssep490be.services.EmailService;
import org.fyp.tmssep490be.services.ForgotPasswordService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service implementation for password reset functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserAccountRepository userAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Simple rate limiting for forgot password requests
    private final ConcurrentMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MINUTES = 5;
    private static final long RATE_LIMIT_MILLIS = RATE_LIMIT_MINUTES * 60 * 1000;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Transactional(readOnly = true)
    public ForgotPasswordResponse requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);

        // Rate limiting check
        Long lastTime = lastRequestTime.get(email.toLowerCase());
        long currentTime = System.currentTimeMillis();
        if (lastTime != null && (currentTime - lastTime) < RATE_LIMIT_MILLIS) {
            log.warn("Rate limit exceeded for password reset request: {}", email);
            // Always return success to prevent email enumeration
            return ForgotPasswordResponse.builder()
                    .message("Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi đến đó")
                    .emailSent(true)
                    .maskedEmail(maskEmail(email))
                    .build();
        }
        lastRequestTime.put(email.toLowerCase(), currentTime);

        try {
            // Find active user by email
            UserAccount user = userAccountRepository.findActiveUserByEmail(email)
                    .orElse(null);

            if (user == null) {
                log.info("No active user found for email: {}", email);
                // Always return success to prevent email enumeration
                return ForgotPasswordResponse.builder()
                        .message("Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi đến đó")
                        .emailSent(true)
                        .maskedEmail(maskEmail(email))
                        .build();
            }

            // Generate password reset token (15-minute validity)
            String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getId());

            // Create reset link
            String resetLink = String.format("%s/reset-password?token=%s", frontendUrl, resetToken);

            // Send password reset email
            emailService.sendPasswordResetEmailAsync(
                    user.getEmail(),
                    user.getFullName(),
                    resetLink
            );

            log.info("Password reset email sent to user: {} (ID: {})", email, user.getId());

            return ForgotPasswordResponse.builder()
                    .message("Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn")
                    .emailSent(true)
                    .maskedEmail(maskEmail(email))
                    .build();

        } catch (Exception e) {
            log.error("Error processing password reset request for email: {}", email, e);
            throw new BusinessRuleException("Không thể xử lý yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.");
        }
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(String token, String newPassword) {
        log.info("Password reset requested with token");

        try {
            // Validate and extract user ID from token
            Long userId = jwtTokenProvider.validatePasswordResetToken(token);
            if (userId == null) {
                log.warn("Invalid password reset token provided");
                throw new InvalidTokenException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
            }

            // Find user by ID
            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found for password reset: {}", userId);
                        return new BusinessRuleException("Người dùng không tồn tại");
                    });

            // Check if user is active
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Password reset attempted for inactive user: {} (status: {})",
                        user.getEmail(), user.getStatus());
                throw new BusinessRuleException("Tài khoản của bạn không hoạt động. Vui lòng liên hệ quản trị viên.");
            }

            // Encode new password
            String encodedPassword = passwordEncoder.encode(newPassword);

            // Update password
            int updatedRows = userAccountRepository.updatePasswordById(userId, encodedPassword);
            if (updatedRows == 0) {
                log.error("Failed to update password for user: {}", userId);
                throw new BusinessRuleException("Không thể cập nhật mật khẩu. Vui lòng thử lại.");
            }

            log.info("Password successfully reset for user: {} (ID: {})", user.getEmail(), user.getId());

            return ResetPasswordResponse.builder()
                    .message("Mật khẩu đã được đặt lại thành công. Bạn có thể đăng nhập bằng mật khẩu mới.")
                    .success(true)
                    .build();

        } catch (InvalidTokenException | BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error resetting password", e);
            throw new BusinessRuleException("Không thể đặt lại mật khẩu. Vui lòng yêu cầu lại liên kết đặt mới.");
        }
    }

    /**
     * Mask email address for privacy
     * Example: nguyenvan@example.com -> nguy****@example.com
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 3) {
            return localPart.charAt(0) + "***" + domain;
        } else {
            return localPart.substring(0, 4) + "***" + domain;
        }
    }
}