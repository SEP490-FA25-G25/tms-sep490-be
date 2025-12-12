package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.auth.ResetPasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ResetPasswordResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.InvalidTokenException;

import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthService_ResetPassword_Test {

    @InjectMocks
    private AuthService authService;

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private UserAccountRepository repo;
    @Mock private PasswordEncoder encoder;


    // -----------------------------------------------------------------
    // TC01 — INVALID TOKEN
    // token = "bad" → validatePasswordResetToken throws exception
    // Expected: InvalidTokenException
    // -----------------------------------------------------------------
    @Test
    void TC01_resetPassword_invalidToken() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("bad")
                .newPassword("pass")
                .confirmPassword("pass")
                .build();

        when(tokenProvider.validatePasswordResetToken("bad"))
                .thenThrow(new InvalidTokenException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        assertThrows(
                InvalidTokenException.class,
                () -> authService.resetPassword(req)
        );
    }


    // -----------------------------------------------------------------
    // TC02 — USER INACTIVE
    // Preconditions:
    // token = "valid"
    // userId extracted = 10
    // user.status = INACTIVE
    // Expected: BusinessRuleException("Tài khoản của bạn không hoạt động...")
    // -----------------------------------------------------------------
    @Test
    void TC02_resetPassword_userInactive() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("valid")
                .newPassword("newpass")
                .confirmPassword("newpass")
                .build();

        when(tokenProvider.validatePasswordResetToken("valid"))
                .thenReturn(10L);

        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setStatus(UserStatus.INACTIVE);

        when(repo.findById(10L)).thenReturn(Optional.of(user));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> authService.resetPassword(req)
        );

        assertTrue(ex.getMessage().contains("Tài khoản của bạn không hoạt động"));
    }


    // -----------------------------------------------------------------
    // TC03 — DB ERROR WHEN findById
    // Preconditions:
    // findById throws RuntimeException("DB error")
    // Expected: BusinessRuleException("Không thể đặt lại mật khẩu...")
    // -----------------------------------------------------------------
    @Test
    void TC03_resetPassword_dbErrorOnFind() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("valid")
                .newPassword("newpass")
                .confirmPassword("newpass")
                .build();

        when(tokenProvider.validatePasswordResetToken("valid")).thenReturn(10L);

        when(repo.findById(10L))
                .thenThrow(new RuntimeException("DB error"));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> authService.resetPassword(req)
        );

        assertEquals("Không thể đặt lại mật khẩu. Vui lòng yêu cầu lại liên kết đặt mới.", ex.getMessage());
    }


    // -----------------------------------------------------------------
    // TC04 — repo.save ERROR (updatePassword failed)
    // Expected: BusinessRuleException("Không thể đặt lại mật khẩu...")
    // -----------------------------------------------------------------
    @Test
    void TC04_resetPassword_saveError() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("valid")
                .newPassword("123123")
                .confirmPassword("123123")
                .build();

        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setStatus(UserStatus.ACTIVE);

        when(tokenProvider.validatePasswordResetToken("valid")).thenReturn(10L);
        when(repo.findById(10L)).thenReturn(Optional.of(user));

        when(encoder.encode("123123")).thenReturn("ENCODED");

        doThrow(new RuntimeException("DB update error"))
                .when(repo)
                .save(any(UserAccount.class));

        assertThrows(
                BusinessRuleException.class,
                () -> authService.resetPassword(req)
        );
    }


    // -----------------------------------------------------------------
    // TC05 — SUCCESS CASE
    // Preconditions:
    // token = valid
    // newPassword = pass
    // encodedPassword = ENCODED
    // user ACTIVE
    // Expected: ResetPasswordResponse(success = true)
    // -----------------------------------------------------------------
    @Test
    void TC05_resetPassword_success() {

        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("valid")
                .newPassword("pass")
                .confirmPassword("pass")
                .build();

        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setStatus(UserStatus.ACTIVE);

        when(tokenProvider.validatePasswordResetToken("valid"))
                .thenReturn(10L);

        when(repo.findById(10L))
                .thenReturn(Optional.of(user));

        when(encoder.encode("pass"))
                .thenReturn("ENCODED");

        ResetPasswordResponse res = authService.resetPassword(req);

        assertTrue(res.isSuccess());
        assertTrue(res.getMessage().contains("Mật khẩu đã được đặt lại thành công"));
        verify(repo).save(any(UserAccount.class));
    }

}
