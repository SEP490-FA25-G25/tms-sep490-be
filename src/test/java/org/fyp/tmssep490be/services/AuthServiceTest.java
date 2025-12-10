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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    // Helpers -------------------------------------------------------------

    private UserPrincipal principal(String email, String fullName, List<String> roles) {
        return new UserPrincipal(
                System.nanoTime(),
                email,
                "hash",
                fullName,
                UserStatus.ACTIVE,
                roles.stream().map(SimpleGrantedAuthority::new).toList()
        );
    }

    private Authentication mockAuth(UserPrincipal p) {
        Authentication a = mock(Authentication.class);
        when(a.getPrincipal()).thenReturn(p);
        when(a.getAuthorities()).thenAnswer(i -> p.getAuthorities());
        return a;
    }

    private UserAccount userWith(int branches, int roles) {
        UserAccount u = new UserAccount();
        u.setId(System.nanoTime());
        u.setEmail("test@fpt.edu.vn");
        u.setFullName("Tester");

        Set<UserRole> ur = new HashSet<>();
        for (int i = 0; i < roles; i++) {
            Role r = new Role(); r.setId((long) i+1); r.setCode("ADMIN" + i);
            UserRole x = new UserRole(); x.setRole(r);
            ur.add(x);
        }
        u.setUserRoles(ur);

        Set<UserBranches> br = new HashSet<>();
        for (int i = 0; i < branches; i++) {
            Branch b = new Branch();
            b.setId((long) i+1); b.setName("Branch " + i); b.setCode("B" + i);
            UserBranches ub = new UserBranches(); ub.setBranch(b);
            br.add(ub);
        }
        u.setUserBranches(br);

        return u;
    }

    // ======================================================
    // LOGIN TEST CASES
    // ======================================================

    @Test
    void TC_L1_login_success() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "123456");
        UserPrincipal p = principal("test@fpt.edu.vn", "Tester", List.of("ROLE_ADMIN"));
        Authentication auth = mockAuth(p);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("ACCESS");
        when(jwtTokenProvider.generateRefreshToken(p.getId(), p.getEmail())).thenReturn("REFRESH");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("test@fpt.edu.vn"))
                .thenReturn(Optional.of(userWith(1, 1)));

        AuthResponse res = authService.login(req);

        assertEquals("ACCESS", res.getAccessToken());
        assertEquals("REFRESH", res.getRefreshToken());
        assertTrue(res.getRoles().contains("ADMIN"));
        assertTrue(res.getBranches().size() > 0);
    }

    @Test
    void TC_L2_user_not_found() {
        LoginRequest req = new LoginRequest("ghost@fpt.edu.vn", "123456");
        UserPrincipal p = principal("ghost@fpt.edu.vn", "Ghost", List.of("ROLE_USER"));
        Authentication auth = mockAuth(p);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userAccountRepository.findByEmail("ghost@fpt.edu.vn"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(req));
    }

    @Test
    void TC_L3_wrong_password() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    void TC_L4_single_role_boundary() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "123456");
        UserPrincipal p = principal("test@fpt.edu.vn", "Tester", List.of("ROLE_USER"));
        Authentication auth = mockAuth(p);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("test@fpt.edu.vn"))
                .thenReturn(Optional.of(userWith(1, 1)));

        AuthResponse res = authService.login(req);

        assertEquals(1, res.getRoles().size());
    }

    @Test
    void TC_L5_zero_branches_boundary() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "123456");
        UserPrincipal p = principal("test@fpt.edu.vn", "Tester", List.of("ROLE_ADMIN"));
        Authentication auth = mockAuth(p);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("test@fpt.edu.vn"))
                .thenReturn(Optional.of(userWith(0, 1)));

        AuthResponse res = authService.login(req);

        assertEquals(0, res.getBranches().size());
    }

    @Test
    void TC_L6_multiple_roles() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "123456");
        UserPrincipal p = principal("test@fpt.edu.edu.vn", "Tester",
                List.of("ROLE_ADMIN", "ROLE_STAFF"));
        Authentication auth = mockAuth(p);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(userWith(1, 2)));

        AuthResponse res = authService.login(req);

        assertEquals(2, res.getRoles().size());
    }

    // ======================================================
    // REFRESH TOKEN TEST CASES
    // ======================================================

    @Test
    void TC_R1_refresh_success() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");
        UserAccount user = userWith(1, 1);

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
    void TC_R2_invalid_refresh_token() {
        RefreshTokenRequest req = new RefreshTokenRequest("BAD");

        when(jwtTokenProvider.validateRefreshToken("BAD")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(req));
    }

    @Test
    void TC_R3_refresh_user_not_found() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(999L);
        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.refreshToken(req));
    }

    @Test
    void TC_R4_refresh_no_roles_boundary() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");
        UserAccount user = userWith(1, 0); // role size = 0

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(user.getId());
        when(userAccountRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("NEW_ACCESS");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("NEW_REFRESH");

        AuthResponse res = authService.refreshToken(req);

        assertEquals(0, res.getRoles().size());
    }

    @Test
    void TC_R5_refresh_multiple_roles() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");
        UserAccount user = userWith(1, 3); // ADMIN, STAFF, MANAGER

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(user.getId());
        when(userAccountRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("NEW_ACCESS");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("NEW_REFRESH");

        AuthResponse res = authService.refreshToken(req);

        assertEquals(3, res.getRoles().size());
    }

    @Test
    void TC_R6_refresh_zero_branches() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");
        UserAccount user = userWith(0, 1);

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(user.getId());
        when(userAccountRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("NEW_ACCESS");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("NEW_REFRESH");

        AuthResponse res = authService.refreshToken(req);

        assertEquals(0, res.getBranches().size());
    }
}
