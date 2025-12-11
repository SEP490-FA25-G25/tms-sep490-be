package org.fyp.tmssep490be.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.ChangePasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ChangePasswordResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.exceptions.InvalidTokenException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.fyp.tmssep490be.security.UserPrincipal;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

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
        userAccountRepository.save(user);

        log.info("Đổi mật khẩu thành công cho user: {}", userId);

        return ChangePasswordResponse.builder()
                .success(true)
                .message("Đổi mật khẩu thành công")
                .build();
    }
}
