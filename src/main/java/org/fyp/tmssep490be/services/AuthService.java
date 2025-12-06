package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);


    AuthResponse refreshToken(RefreshTokenRequest request);

}
