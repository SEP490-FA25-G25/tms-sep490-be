package org.fyp.tmssep490be.services;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final UserAccountRepository userAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // Giới hạn tần suất gửi yêu cầu quên mật khẩu
    private final ConcurrentMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MINUTES = 5;
    private static final long RATE_LIMIT_MILLIS = RATE_LIMIT_MINUTES * 60 * 1000;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    // Gửi email đặt lại mật khẩu
    @Transactional(readOnly = true)
    public ForgotPasswordResponse requestPasswordReset(String email) {
        log.info("Yêu cầu quên mật khẩu cho email: {}", email);

        Long lastTime = lastRequestTime.get(email.toLowerCase());
        long currentTime = System.currentTimeMillis();
        if (lastTime != null && (currentTime - lastTime) < RATE_LIMIT_MILLIS) {
            log.warn("Vượt quá giới hạn tần suất cho email: {}", email);
            return ForgotPasswordResponse.builder()
                    .message("Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi đến đó")
                    .emailSent(true)
                    .maskedEmail(maskEmail(email))
                    .build();
        }
        lastRequestTime.put(email.toLowerCase(), currentTime);

        try {
            UserAccount user = userAccountRepository.findByEmailAndStatus(email, UserStatus.ACTIVE)
                    .orElseThrow(() -> new BusinessRuleException("Email không tồn tại trong hệ thống"));

            String resetToken = jwtTokenProvider.generatePasswordResetToken(user.getId());
            String resetLink = String.format("%s/reset-password?token=%s", frontendUrl, resetToken);

            emailService.sendPasswordResetEmailAsync(
                    user.getEmail(),
                    user.getFullName(),
                    resetLink
            );

            log.info("Đã gửi email đặt lại mật khẩu cho user {}", user.getId());

            return ForgotPasswordResponse.builder()
                    .message("Hướng dẫn đặt lại mật khẩu đã được gửi đến email của bạn")
                    .emailSent(true)
                    .maskedEmail(maskEmail(email))
                    .build();

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi xử lý yêu cầu đặt lại mật khẩu cho email: {}", email, e);
            throw new BusinessRuleException("Không thể xử lý yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.");
        }
    }

    // Đặt lại mật khẩu bằng token
    @Transactional
    public ResetPasswordResponse resetPassword(String token, String newPassword) {
        log.info("Yêu cầu đặt lại mật khẩu bằng token");

        try {
            Long userId = jwtTokenProvider.validatePasswordResetToken(token);
            if (userId == null) {
                throw new InvalidTokenException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
            }

            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new BusinessRuleException("Người dùng không tồn tại"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Tài khoản không hoạt động, không thể đặt lại mật khẩu: {}", userId);
                throw new BusinessRuleException("Tài khoản của bạn không hoạt động. Vui lòng liên hệ quản trị viên.");
            }

            String encodedPassword = passwordEncoder.encode(newPassword);
            int updatedRows = userAccountRepository.updatePasswordById(userId, encodedPassword);
            if (updatedRows == 0) {
                log.error("Cập nhật mật khẩu thất bại cho user: {}", userId);
                throw new BusinessRuleException("Không thể cập nhật mật khẩu. Vui lòng thử lại.");
            }

            log.info("Đặt lại mật khẩu thành công cho user {}", userId);

            return ResetPasswordResponse.builder()
                    .message("Mật khẩu đã được đặt lại thành công. Bạn có thể đăng nhập bằng mật khẩu mới.")
                    .success(true)
                    .build();

        } catch (InvalidTokenException | BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi đặt lại mật khẩu", e);
            throw new BusinessRuleException("Không thể đặt lại mật khẩu. Vui lòng yêu cầu lại liên kết đặt mới.");
        }
    }

    // Che email để tránh lộ thông tin
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

