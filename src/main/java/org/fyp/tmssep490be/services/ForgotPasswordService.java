package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.auth.ForgotPasswordResponse;
import org.fyp.tmssep490be.dtos.auth.ResetPasswordResponse;

/**
 * Service interface for password reset functionality
 */
public interface ForgotPasswordService {

    /**
     * Request password reset for given email
     * @param email User email address
     * @return ForgotPasswordResponse with status and masked email
     */
    ForgotPasswordResponse requestPasswordReset(String email);

    /**
     * Reset password using token
     * @param token Password reset token
     * @param newPassword New password
     * @return ResetPasswordResponse with status
     */
    ResetPasswordResponse resetPassword(String token, String newPassword);
}