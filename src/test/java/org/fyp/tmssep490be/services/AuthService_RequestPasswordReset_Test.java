package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.auth.ForgotPasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ForgotPasswordResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthService_RequestPasswordReset_Test {

    @InjectMocks
    private AuthService authService;

    @Mock private UserAccountRepository repo;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private EmailService emailService;


    // ------------------------------------------------------------
    // TC01 — Normal case
    // Preconditions:
    //  - Email exists
    //  - User ACTIVE
    //  - Token generated successfully
    //  - Email sent successfully
    // Confirm:
    //  - No exception, masked email returned
    // ------------------------------------------------------------
    @Test
    void TC01_requestPasswordReset_success() {
        String email = "user@gmail.com";
        ForgotPasswordRequest req = new ForgotPasswordRequest(email);

        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setEmail(email);
        user.setFullName("User");
        user.setStatus(UserStatus.ACTIVE);

        when(repo.findByEmailAndStatus(email, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));

        when(tokenProvider.generatePasswordResetToken(10L))
                .thenReturn("reset-token");

        ForgotPasswordResponse res = authService.requestPasswordReset(req);

        assertTrue(res.isEmailSent());
        assertEquals("user***@gmail.com", res.getMaskedEmail());
        verify(emailService).sendPasswordResetEmailAsync(
                eq(email), eq("User"), contains("reset-token")
        );
    }


    // ------------------------------------------------------------
    // TC02 — User not found (CustomException USER_NOT_FOUND)
    // Preconditions:
    //  - Email not found in DB
    // Confirm:
    //  - Throw BusinessRuleException("Email không tồn tại trong hệ thống")
    // ------------------------------------------------------------
    @Test
    void TC02_requestPasswordReset_userNotFound() {
        String email = "nobody@gmail.com";
        ForgotPasswordRequest req = new ForgotPasswordRequest(email);

        when(repo.findByEmailAndStatus(email, UserStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> authService.requestPasswordReset(req)
        );

        assertEquals("Email không tồn tại trong hệ thống", ex.getMessage());
    }


    // ------------------------------------------------------------
    // TC03 — Account disabled (ACCOUNT_DISABLED)
    // Preconditions:
    //  - Email exists but status != ACTIVE
    // Confirm:
    //  - Throw BusinessRuleException
    // ------------------------------------------------------------
    @Test
    void TC03_requestPasswordReset_accountDisabled() {
        String email = "block@gmail.com";
        ForgotPasswordRequest req = new ForgotPasswordRequest(email);

        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setEmail(email);
        user.setStatus(UserStatus.INACTIVE);   // không ACTIVE → Disabled

        when(repo.findByEmailAndStatus(email, UserStatus.ACTIVE))
                .thenReturn(Optional.empty());  // DB logic: ACTIVE only

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> authService.requestPasswordReset(req)
        );

        assertEquals("Email không tồn tại trong hệ thống", ex.getMessage());
    }


    // ------------------------------------------------------------
    // TC04 — DB Failure (RuntimeException)
    // Preconditions:
    //  - Query throws RuntimeException("DB failed")
    // Confirm:
    //  - Throw BusinessRuleException("Không thể xử lý yêu cầu đặt lại mật khẩu...")
    // ------------------------------------------------------------
    @Test
    void TC04_requestPasswordReset_dbFailure() {
        String email = "user@gmail.com";
        ForgotPasswordRequest req = new ForgotPasswordRequest(email);

        when(repo.findByEmailAndStatus(email, UserStatus.ACTIVE))
                .thenThrow(new RuntimeException("DB failed"));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> authService.requestPasswordReset(req)
        );

        assertEquals("Không thể xử lý yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.", ex.getMessage());
    }


    // ------------------------------------------------------------
    // TC05 — SMTP Email Sending Error (RuntimeException "SMTP Error")
    // Preconditions:
    //  - Email exists & token generated
    //  - Email sending throws RuntimeException("SMTP Error")
    // Confirm:
    //  - Throw BusinessRuleException("Không thể xử lý yêu cầu đặt lại mật khẩu...")
    // ------------------------------------------------------------
    @Test
    void TC05_requestPasswordReset_SMTPError() {
        String email = "user@gmail.com";
        ForgotPasswordRequest req = new ForgotPasswordRequest(email);

        UserAccount user = new UserAccount();
        user.setId(10L);
        user.setEmail(email);
        user.setFullName("User");
        user.setStatus(UserStatus.ACTIVE);

        when(repo.findByEmailAndStatus(email, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));

        when(tokenProvider.generatePasswordResetToken(10L))
                .thenReturn("reset-token");

        doThrow(new RuntimeException("SMTP Error"))
                .when(emailService)
                .sendPasswordResetEmailAsync(any(), any(), any());

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> authService.requestPasswordReset(req)
        );

        assertEquals("Không thể xử lý yêu cầu đặt lại mật khẩu. Vui lòng thử lại sau.", ex.getMessage());
    }
}
