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

import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock UserAccountRepository userAccountRepository;

    @InjectMocks AuthService authService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private UserPrincipal principal(Long id, String email, String fullName, List<String> roleNames) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roleNames != null) {
            for (String r : roleNames) {
                authorities.add(new SimpleGrantedAuthority(r));
            }
        }
        return new UserPrincipal(
                id,
                email,
                "passwordHash",
                fullName,
                UserStatus.ACTIVE,
                authorities
        );
    }

    private Authentication mockAuth(UserPrincipal principal) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(auth.getAuthorities()).thenAnswer(inv -> principal.getAuthorities());
        return auth;
    }

    private UserAccount userWithRolesBranches(List<String> roles, int branchCount) {
        UserAccount u = new UserAccount();
        u.setId(1L);
        u.setEmail("test@fpt.edu.vn");
        u.setFullName("Tester");
        u.setPasswordHash("passwordHash");
        u.setStatus(UserStatus.ACTIVE);

        Set<UserRole> roleSet = new HashSet<>();
        if (roles != null) {
            for (String rname : roles) {
                Role r = new Role();
                r.setId(new Random().nextLong());
                r.setCode(rname);
                UserRole ur = new UserRole();
                ur.setRole(r);
                roleSet.add(ur);
            }
        }
        u.setUserRoles(roleSet);

        Set<UserBranches> branches = new HashSet<>();
        for (int i = 0; i < branchCount; i++) {
            Branch b = new Branch();
            b.setId((long) (100 + i));
            b.setName("Branch" + i);
            b.setCode("B" + i);
            UserBranches ub = new UserBranches();
            ub.setBranch(b);
            branches.add(ub);
        }
        u.setUserBranches(branches);

        return u;
    }

    // ============================================
    // LOGIN TESTS (8 test cases)
    // ============================================

    @Test // TC-L1
    void login_success_normal() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "123456");

        List<String> roles = List.of("ROLE_ADMIN");
        UserPrincipal principal = principal(1L, "test@fpt.edu.vn", "Tester", roles);

        Authentication auth = mockAuth(principal);
        UserAccount user = userWithRolesBranches(List.of("ADMIN"), 1);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("ACCESS");
        when(jwtTokenProvider.generateRefreshToken(1L, "test@fpt.edu.vn")).thenReturn("REFRESH");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("test@fpt.edu.vn")).thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);
        assertEquals("ACCESS", res.getAccessToken());
    }

    @Test // TC-L2
    void login_singleRole_normal() {
        LoginRequest req = new LoginRequest("one@fpt.edu.vn", "123456");

        UserPrincipal principal = principal(1L, "one@fpt.edu.vn", "One Role", List.of("ROLE_USER"));
        Authentication auth = mockAuth(principal);

        UserAccount user = userWithRolesBranches(List.of("USER"), 1);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(1L, "one@fpt.edu.vn")).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("one@fpt.edu.vn")).thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);
        assertEquals(1, res.getRoles().size());
    }

    @Test // TC-L3
    void login_multiRole_normal() {
        LoginRequest req = new LoginRequest("multi@fpt.edu.vn", "123456");

        UserPrincipal principal = principal(1L, "multi@fpt.edu.vn", "Multi", List.of("ROLE_ADMIN", "ROLE_USER"));
        Authentication auth = mockAuth(principal);

        UserAccount user = userWithRolesBranches(List.of("ADMIN", "USER"), 1);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(1L, "multi@fpt.edu.vn")).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("multi@fpt.edu.vn")).thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);
        assertTrue(res.getRoles().contains("ADMIN"));
        assertTrue(res.getRoles().contains("USER"));
    }

    @Test // TC-L4
    void login_wrongPassword_authFail() {
        LoginRequest req = new LoginRequest("test@fpt.edu.vn", "wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test // TC-L5
    void login_userNotFound() {
        LoginRequest req = new LoginRequest("ghost@fpt.edu.vn", "123456");

        UserPrincipal principal = principal(99L, "ghost@fpt.edu.vn", "Ghost", List.of("ROLE_USER"));
        Authentication auth = mockAuth(principal);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userAccountRepository.findByEmail("ghost@fpt.edu.vn")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(req));
    }

    @Test // TC-L6
    void login_noRoles_boundary() {
        LoginRequest req = new LoginRequest("norole@fpt.edu.vn", "123456");

        UserPrincipal principal = principal(1L, "norole@fpt.edu.vn", "NoRole", List.of());
        Authentication auth = mockAuth(principal);

        UserAccount user = userWithRolesBranches(List.of(), 1);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("norole@fpt.edu.vn")).thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);
        assertTrue(res.getRoles().isEmpty());
    }

    @Test // TC-L7
    void login_noBranches_boundary() {
        LoginRequest req = new LoginRequest("nobranch@fpt.edu.vn", "123456");

        UserPrincipal principal = principal(1L, "nobranch@fpt.edu.vn", "NoBranch", List.of("ROLE_USER"));
        Authentication auth = mockAuth(principal);

        UserAccount user = userWithRolesBranches(List.of("USER"), 0);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("nobranch@fpt.edu.vn")).thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);
        assertEquals(0, res.getBranches().size());
    }

    @Test // TC-L8
    void login_nullAvatar_boundary() {
        LoginRequest req = new LoginRequest("nullavatar@fpt.edu.vn", "123456");

        UserPrincipal principal = principal(1L, "nullavatar@fpt.edu.vn", "NullAvatar", List.of("ROLE_USER"));
        Authentication auth = mockAuth(principal);

        UserAccount user = userWithRolesBranches(List.of("USER"), 1);
        user.setAvatarUrl(null);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("A");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString())).thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);
        when(userAccountRepository.findByEmail("nullavatar@fpt.edu.vn")).thenReturn(Optional.of(user));

        AuthResponse res = authService.login(req);
        assertNull(res.getAvatarUrl());
    }

    // ============================================
    // REFRESH TOKEN TESTS (7 test cases)
    // ============================================

    @Test // TC-R1
    void refresh_success_normal() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        UserAccount user = userWithRolesBranches(List.of("ADMIN"), 1);

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(1L);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(1L, "test@fpt.edu.vn", "ROLE_ADMIN")).thenReturn("NA");
        when(jwtTokenProvider.generateRefreshToken(1L, "test@fpt.edu.vn")).thenReturn("NR");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse res = authService.refreshToken(req);
        assertEquals("NA", res.getAccessToken());
    }

    @Test // TC-R2
    void refresh_success_multiRole_normal() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        UserAccount user = userWithRolesBranches(List.of("ADMIN", "USER"), 1);

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(1L);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("TOKEN");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("REF");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse res = authService.refreshToken(req);
        assertEquals(2, res.getRoles().size());
    }

    @Test // TC-R3
    void refresh_invalidToken_abnormal() {
        RefreshTokenRequest req = new RefreshTokenRequest("BAD");

        when(jwtTokenProvider.validateRefreshToken("BAD")).thenReturn(false);

        assertThrows(InvalidTokenException.class, () -> authService.refreshToken(req));
    }

    @Test // TC-R4
    void refresh_userNotFound_abnormal() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(99L);
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.refreshToken(req));
    }

    @Test // TC-R5
    void refresh_noRoles_boundary() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        UserAccount user = userWithRolesBranches(List.of(), 1);

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(1L);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("NA");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("NR");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse res = authService.refreshToken(req);
        assertTrue(res.getRoles().isEmpty());
    }

    @Test // TC-R6
    void refresh_noBranches_boundary() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        UserAccount user = userWithRolesBranches(List.of("USER"), 0);

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(1L);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("T");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse res = authService.refreshToken(req);
        assertTrue(res.getBranches().isEmpty());
    }

    @Test // TC-R7
    void refresh_suspendedUser_boundary() {
        RefreshTokenRequest req = new RefreshTokenRequest("VALID");

        UserAccount user = userWithRolesBranches(List.of("USER"), 1);
        user.setStatus(UserStatus.SUSPENDED);

        when(jwtTokenProvider.validateRefreshToken("VALID")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("VALID")).thenReturn(1L);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(user));

        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString()))
                .thenReturn("T");
        when(jwtTokenProvider.generateRefreshToken(anyLong(), anyString()))
                .thenReturn("R");
        when(jwtTokenProvider.getAccessTokenExpirationInSeconds()).thenReturn(3600L);

        AuthResponse res = authService.refreshToken(req);
        assertEquals("T", res.getAccessToken()); // vẫn refresh được
    }
}
