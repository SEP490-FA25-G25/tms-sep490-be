package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.entities.UserAccount;


import org.fyp.tmssep490be.dtos.auth.ChangePasswordRequest;
import org.fyp.tmssep490be.dtos.auth.ChangePasswordResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthService_ChangePassword_Test {

    @InjectMocks
    private AuthService authService;

    @Mock private UserAccountRepository repo;
    @Mock private PasswordEncoder encoder;


    // -------------------------------------------------------------
    // TC01 — SUCCESS
    // Preconditions:
    //  userId = 1
    //  currentPassword = "old123"
    //  newPassword = "new123"
    //  confirmPassword = "new123"
    // Return:
    //  success = true, message = "Đổi mật khẩu thành công"
    // -------------------------------------------------------------
    @Test
    void TC01_changePassword_success() {
        Long userId = 1L;

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPasswordHash("encoded_old");

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("old123")
                .newPassword("new123")
                .confirmPassword("new123")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.matches("old123", "encoded_old")).thenReturn(true);
        when(encoder.matches("new123", "encoded_old")).thenReturn(false);
        when(encoder.encode("new123")).thenReturn("encoded_new");

        ChangePasswordResponse res = authService.changePassword(userId, req);

        assertTrue(res.isSuccess());
        assertEquals("Đổi mật khẩu thành công", res.getMessage());
        verify(repo).save(any(UserAccount.class));
    }


    // -------------------------------------------------------------
    // TC02 — WRONG CURRENT PASSWORD
    // Preconditions:
    //   currentPassword = "wrong"
    // Return:
    //   success = false
    //   message = "Mật khẩu hiện tại không đúng"
    // -------------------------------------------------------------
    @Test
    void TC02_changePassword_wrongCurrentPassword() {
        Long userId = 1L;

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPasswordHash("encoded_old");

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("wrong")
                .newPassword("new123")
                .confirmPassword("new123")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "encoded_old")).thenReturn(false);

        ChangePasswordResponse res = authService.changePassword(userId, req);

        assertFalse(res.isSuccess());
        assertEquals("Mật khẩu hiện tại không đúng", res.getMessage());
    }


    // -------------------------------------------------------------
    // TC03 — NEW PASSWORD CONFIRM MISMATCH
    // Preconditions:
    //   newPassword = "abc"
    //   confirmPassword = "abe"
    // Return:
    //   success = false
    //   message = "Mật khẩu xác nhận không khớp"
    // -------------------------------------------------------------
    @Test
    void TC03_changePassword_confirmMismatch() {
        Long userId = 1L;

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPasswordHash("encoded_old");

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("old123")
                .newPassword("abc")
                .confirmPassword("abe")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.matches("old123", "encoded_old")).thenReturn(true);

        ChangePasswordResponse res = authService.changePassword(userId, req);

        assertFalse(res.isSuccess());
        assertEquals("Mật khẩu xác nhận không khớp", res.getMessage());
    }


    // -------------------------------------------------------------
    // TC04 — NEW PASSWORD SAME AS OLD
    // Preconditions:
    //   newPassword = "old"
    //   confirmPassword = "old"
    // Return:
    //   success = false
    //   message = "Mật khẩu mới phải khác mật khẩu hiện tại"
    // -------------------------------------------------------------
    @Test
    void TC04_changePassword_sameAsOld() {
        Long userId = 1L;

        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setPasswordHash("encoded_old");

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("old123")
                .newPassword("old")
                .confirmPassword("old")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.of(user));
        when(encoder.matches("old123", "encoded_old")).thenReturn(true);
        when(encoder.matches("old", "encoded_old")).thenReturn(true); // new == old

        ChangePasswordResponse res = authService.changePassword(userId, req);

        assertFalse(res.isSuccess());
        assertEquals("Mật khẩu mới phải khác mật khẩu hiện tại", res.getMessage());
    }


    // -------------------------------------------------------------
    // TC05 — USER NOT FOUND
    // Preconditions:
    //   userId = 999
    // Return:
    //   throw UsernameNotFoundException
    // -------------------------------------------------------------
    @Test
    void TC05_changePassword_userNotFound() {
        Long userId = 999L;

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("old123")
                .newPassword("new123")
                .confirmPassword("new123")
                .build();

        when(repo.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> authService.changePassword(userId, req)
        );
    }

}
