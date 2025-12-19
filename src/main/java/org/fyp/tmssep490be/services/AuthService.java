package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.ChangePasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ChangePasswordResponse;
import org.fyp.tmssep490be.dtos.auth.ForgotPasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ForgotPasswordResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;
import org.fyp.tmssep490be.dtos.auth.ResetPasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ResetPasswordResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.InvalidTokenException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Giới hạn tần suất gửi yêu cầu quên mật khẩu
    private final ConcurrentMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MINUTES = 5;
    private static final long RATE_LIMIT_MILLIS = RATE_LIMIT_MINUTES * 60 * 1000;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(
                userPrincipal.getId(),
                userPrincipal.getEmail());

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.replace("ROLE_", ""))
                .collect(Collectors.toSet());

        UserAccount user = userAccountRepository.findByEmail(userPrincipal.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<AuthResponse.BranchInfo> branches = user.getUserBranches().stream()
                .map(ub -> AuthResponse.BranchInfo.builder()
                        .id(ub.getBranch().getId())
                        .name(ub.getBranch().getName())
                        .code(ub.getBranch().getCode())
                        .build())
                .toList();

        log.info("Login successful for user: {} with {} branches", userPrincipal.getEmail(), branches.size());

        boolean mustChangePassword = user.getLastPasswordChangeAt() == null;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .userId(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .fullName(userPrincipal.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .branches(branches)
                .mustChangePassword(mustChangePassword)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        log.info("Refresh token request received");

        if (!jwtTokenProvider.validateRefreshToken(requestRefreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromJwt(requestRefreshToken);

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String roles = user.getUserRoles().stream()
                .map(ur -> "ROLE_" + ur.getRole().getCode())
                .collect(Collectors.joining(","));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                roles);

        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getEmail());

        Set<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getCode())
                .collect(Collectors.toSet());

        List<AuthResponse.BranchInfo> branches = user.getUserBranches().stream()
                .map(ub -> AuthResponse.BranchInfo.builder()
                        .id(ub.getBranch().getId())
                        .name(ub.getBranch().getName())
                        .code(ub.getBranch().getCode())
                        .build())
                .toList();

        log.info("Token refresh successful for user: {} with {} branches", user.getEmail(), branches.size());

        // Check if password change is required on refresh as well (optional but good for security)
        boolean mustChangePassword = user.getLastPasswordChangeAt() == null;

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationInSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .branches(branches)
                .mustChangePassword(mustChangePassword)
                .build();
    }

    // Đổi mật khẩu cho người dùng đã đăng nhập
    // @return Response cho biết thành công hay thất bại
    @Transactional
    public ChangePasswordResponse changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Yêu cầu đổi mật khẩu cho user: {}", userId);

        // Tìm user
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));

        // Xác minh mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Mật khẩu hiện tại không đúng cho user: {}", userId);
            return ChangePasswordResponse.builder()
                    .success(false)
                    .message("Mật khẩu hiện tại không đúng")
                    .build();
        }

        // Xác minh mật khẩu mới khớp với xác nhận
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ChangePasswordResponse.builder()
                    .success(false)
                    .message("Mật khẩu xác nhận không khớp")
                    .build();
        }

        // Kiểm tra mật khẩu mới phải khác mật khẩu hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            return ChangePasswordResponse.builder()
                    .success(false)
                    .message("Mật khẩu mới phải khác mật khẩu hiện tại")
                    .build();
        }

        // Cập nhật mật khẩu
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setLastPasswordChangeAt(java.time.OffsetDateTime.now());
        userAccountRepository.save(user);

        log.info("Đổi mật khẩu thành công cho user: {}", userId);

        return ChangePasswordResponse.builder()
                .success(true)
                .message("Đổi mật khẩu thành công")
                .build();
    }

    // Gửi email đặt lại mật khẩu
    @Transactional(readOnly = true)
    public ForgotPasswordResponse requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        log.info("Yêu cầu quên mật khẩu cho email: {}", email);

        // Kiểm tra rate limit
        Long lastTime = lastRequestTime.get(email);
        long currentTime = System.currentTimeMillis();
        if (lastTime != null && (currentTime - lastTime) < RATE_LIMIT_MILLIS) {
            log.warn("Vượt quá giới hạn tần suất cho email: {}", email);
            return ForgotPasswordResponse.builder()
                    .message("Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu sẽ được gửi đến đó")
                    .emailSent(true)
                    .maskedEmail(maskEmail(email))
                    .build();
        }
        lastRequestTime.put(email, currentTime);

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
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        log.info("Yêu cầu đặt lại mật khẩu bằng token");

        try {
            // Xác minh mật khẩu mới khớp với xác nhận
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResetPasswordResponse.builder()
                        .success(false)
                        .message("Mật khẩu xác nhận không khớp")
                        .build();
            }

            Long userId = jwtTokenProvider.validatePasswordResetToken(request.getToken());
            if (userId == null) {
                throw new InvalidTokenException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn");
            }

            UserAccount user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new BusinessRuleException("Người dùng không tồn tại"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Tài khoản không hoạt động, không thể đặt lại mật khẩu: {}", userId);
                throw new BusinessRuleException("Tài khoản của bạn không hoạt động. Vui lòng liên hệ quản trị viên.");
            }

            String encodedPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPasswordHash(encodedPassword);
            userAccountRepository.save(user);

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
