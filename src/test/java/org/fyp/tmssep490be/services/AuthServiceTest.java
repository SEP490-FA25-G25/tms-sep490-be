package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.InvalidTokenException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.fyp.tmssep490be.security.UserPrincipal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserAccountRepository userAccountRepository;

    @InjectMocks AuthService authService;

    // ======================================================
    // Helper: Create a test UserPrincipal (clean, no warnings)
    // ======================================================

    private UserPrincipal createPrincipal(String email, String fullName, List<String> roles) {

        long randomId = System.nanoTime(); // ID ko warning "always 1L"

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            for (String roleName : roles) {
                authorities.add(new SimpleGrantedAuthority(roleName));
            }
        }

        return new UserPrincipal(
                randomId,
                email,
                "passwordHash",
                fullName,
                UserStatus.ACTIVE,
                authorities
        );
    }

    // ======================================================
    // Helper: mock Authentication (no raw type, no warnings)
    // ======================================================

    private Authentication mockAuth(UserPrincipal principal) {
        Authentication authentication = mock(Authentication.class);

        when(authentication.getPrincipal()).thenReturn(principal);
        when(authentication.getAuthorities()).thenAnswer(inv -> principal.getAuthorities());

        return authentication;
    }

    // ======================================================
    // Helper: Create UserAccount with roles + 1 branch
    // ======================================================

    private UserAccount createUserAccount(List<String> roleCodes) {

        UserAccount user = new UserAccount();
        user.setId(System.nanoTime());
        user.setEmail("test@fpt.edu.vn");
        user.setFullName("Tester");
        user.setPasswordHash("passwordHash");
        user.setStatus(UserStatus.ACTIVE);

        // Roles
        Set<UserRole> userRoles = new HashSet<>();
        if (roleCodes != null) {
            for (String roleCode : roleCodes) {
                Role role = new Role();
                role.setId(System.nanoTime());
                role.setCode(roleCode);

                UserRole ur = new UserRole();
                ur.setRole(role);

                userRoles.add(ur);
            }
        }
        user.setUserRoles(userRoles);

        // Always 1 branch (clean, no warnings)
        Branch branch = new Branch();
        branch.setId(System.nanoTime());
        branch.setName("Branch");
        branch.setCode("B1");

        UserBranches ub = new UserBranches();
        ub.setBranch(branch);

        user.setUserBranches(Set.of(ub));

        return user;
    }

    // ======================================================
    // LOGIN TESTS
    // ======================================================

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "123456");

        UserPrincipal principal = createPrincipal("test@fpt.edu.vn", "Tester", List.of("ROLE_ADMIN"));
        Authentication auth = mockAuth(principal);

        UserAccount user = createUserAccount(List.of("ADMIN"));

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("ACCESS_TOKEN");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("REFRESH_TOKEN");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("test@fpt.edu.vn"))
                .thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);

        assertEquals("ACCESS_TOKEN", res.getAccessToken());
        assertEquals("REFRESH_TOKEN", res.getRefreshToken());
        assertTrue(res.getRoles().contains("ADMIN"));
    }

    @Test
    void login_userNotFound() {
        LoginRequest req = new LoginRequest("ghost@fpt.edu.vn", "123456");

        UserPrincipal principal = createPrincipal("ghost@fpt.edu.vn", "Ghost", List.of("ROLE_USER"));
        Authentication auth = mockAuth(principal);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userAccountRepository.findByEmail("ghost@fpt.edu.vn"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(req));
    }

    @Test
    void login_wrongPassword() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    // ======================================================
    // REFRESH TOKEN TESTS
    // ======================================================

    @Test
    void refresh_success() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        UserAccount user = createUserAccount(List.of("ADMIN"));

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(user.getId());
        when(userAccountRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("NEW_ACCESS");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("NEW_REFRESH");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse res = authService.refreshToken(req);

        assertEquals("NEW_ACCESS", res.getAccessToken());
        assertEquals("NEW_REFRESH", res.getRefreshToken());
    }

    @Test
    void refresh_invalidToken() {
        RefreshTokenRequest req = new RefreshTokenRequest("BAD");

        when(jwtTokenProvider.validateRefreshToken("BAD"))
                .thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(req));
    }

    @Test
    void refresh_userNotFound() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(999L);
        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.refreshToken(req));
    }
}
