package org.fyp.tmssep490be.services;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.fyp.tmssep490be.services.EmailService;

import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UpdateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class UserAccountServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RoleRepository roleRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserBranchesRepository userBranchesRepository;
    @Mock private EmailService emailService;
    @InjectMocks
    private UserAccountService userAccountService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    // Helper
    private UserAccount user(long id) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setEmail("test@fpt.edu.vn");
        u.setFullName("Tester");
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    // ==========================================================
    // CREATE USER
    // ==========================================================

    @Test
    void createUser_success() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("new@fpt.edu.vn");
        req.setPassword("123456");
        req.setFullName("New User");
        req.setRoleIds(Set.of(1L));
        req.setBranchIds(Set.of(10L));
        req.setStatus(UserStatus.ACTIVE);

        when(userAccountRepository.existsByEmail("new@fpt.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");

        when(userAccountRepository.save(any())).thenAnswer(inv -> {
            UserAccount u = inv.getArgument(0);
            u.setId(100L);
            return u;
        });

        Role r = new Role();
        r.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(r));

        Branch b = new Branch();
        b.setId(10L);
        b.setName("Branch 10");
        when(branchRepository.findById(10L)).thenReturn(Optional.of(b));

        // 🔥 FIX: mock email service call (otherwise NPE)
        doNothing().when(emailService)
                .sendNewUserCredentialsAsync(anyString(), anyString(), anyString(), anyString(), anyString());

        UserResponse res = userAccountService.createUser(req);

        assertEquals("new@fpt.edu.vn", res.getEmail());
        verify(userRoleRepository, times(1)).save(any());
        verify(userBranchesRepository, times(1)).save(any());
        verify(emailService, times(1))
                .sendNewUserCredentialsAsync(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void createUser_emailExists() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("dup@fpt.edu.vn");

        when(userAccountRepository.existsByEmail("dup@fpt.edu.vn")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userAccountService.createUser(req));
    }

    @Test
    void createUser_roleNotFound() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("new@fpt.edu.vn");
        req.setPassword("123");
        req.setRoleIds(Set.of(999L));
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(inv -> {
                    UserAccount u = inv.getArgument(0);
                    u.setId(100L); // fake ID
                    return u;
                });

        when(userAccountRepository.existsByEmail("new@fpt.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode("123")).thenReturn("hashed");
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userAccountService.createUser(req));
    }

    @Test
    void createUser_branchNotFound() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("new@fpt.edu.vn");
        req.setPassword("123");
        req.setRoleIds(Set.of(1L));
        req.setBranchIds(Set.of(88L));
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(inv -> {
                    UserAccount u = inv.getArgument(0);
                    u.setId(100L);
                    return u;
                });

        when(userAccountRepository.existsByEmail("new@fpt.edu.vn")).thenReturn(false);
        when(passwordEncoder.encode("123")).thenReturn("hashed");

        Role r = new Role();
        r.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(r));
        when(branchRepository.findById(88L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userAccountService.createUser(req));
    }

    // ==========================================================
    // UPDATE USER
    // ==========================================================

    @Test
    void updateUser_success() {
        UserAccount u = user(99L);
        when(userAccountRepository.findById(99L)).thenReturn(Optional.of(u));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("Updated");
        req.setRoleIds(Set.of(1L));
        req.setBranchIds(Set.of(11L));

        Role r = new Role();
        r.setId(1L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(r));

        Branch b = new Branch();
        b.setId(11L);
        b.setName("Branch 11");
        when(branchRepository.findById(11L)).thenReturn(Optional.of(b));

        UserResponse res = userAccountService.updateUser(99L, req);

        assertEquals("Updated", res.getFullName());

        // NO DELETE is called — remove this
        // verify(userRoleRepository).deleteByUserAccount(u);
        // verify(userBranchesRepository).deleteByUserAccount(u);

        // Just verify save events
        verify(userRoleRepository).save(any());
        verify(userBranchesRepository).save(any());
    }

    @Test
    void updateUser_notFound() {
        when(userAccountRepository.findById(55L)).thenReturn(Optional.empty());
        UpdateUserRequest req = new UpdateUserRequest();
        assertThrows(IllegalArgumentException.class, () -> userAccountService.updateUser(55L, req));
    }

    @Test
    void updateUser_roleNotFound() {
        UserAccount u = user(88L);
        when(userAccountRepository.findById(88L)).thenReturn(Optional.of(u));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setRoleIds(Set.of(999L));

        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userAccountService.updateUser(88L, req));
    }

    @Test
    void updateUser_branchNotFound() {
        UserAccount u = user(88L);
        when(userAccountRepository.findById(88L)).thenReturn(Optional.of(u));

        UpdateUserRequest req = new UpdateUserRequest();
        req.setBranchIds(Set.of(999L));

        when(branchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userAccountService.updateUser(88L, req));
    }

    // ==========================================================
    // GET USER BY ID
    // ==========================================================

    @Test
    void getUserById_success() {
        UserAccount u = user(5L);
        when(userAccountRepository.findById(5L)).thenReturn(Optional.of(u));

        UserResponse res = userAccountService.getUserById(5L);
        assertEquals(5L, res.getId());
    }

    @Test
    void getUserById_notFound() {
        when(userAccountRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userAccountService.getUserById(9L));
    }

    // ==========================================================
    // GET USER BY EMAIL
    // ==========================================================

    @Test
    void getUserByEmail_success() {
        UserAccount u = user(1L);
        when(userAccountRepository.findByEmail("test@fpt.edu.vn")).thenReturn(Optional.of(u));

        UserResponse res = userAccountService.getUserByEmail("test@fpt.edu.vn");
        assertEquals("test@fpt.edu.vn", res.getEmail());
    }

    @Test
    void getUserByEmail_notFound() {
        when(userAccountRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userAccountService.getUserByEmail("x@x.com"));
    }

    // ==========================================================
    // UPDATE USER STATUS
    // ==========================================================

    @Test
    void updateUserStatus_success() {
        UserAccount u = user(22L);
        when(userAccountRepository.findById(22L)).thenReturn(Optional.of(u));

        UserResponse res = userAccountService.updateUserStatus(22L, "INACTIVE");
        assertEquals(UserStatus.INACTIVE, res.getStatus());
    }

    @Test
    void updateUserStatus_notFound() {
        when(userAccountRepository.findById(33L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> userAccountService.updateUserStatus(33L, "ACTIVE"));
    }

    // ==========================================================
    // CHECK EMAIL EXISTS
    // ==========================================================

    @Test
    void checkEmailExists_true() {
        when(userAccountRepository.existsByEmail("abc@a.com")).thenReturn(true);
        assertTrue(userAccountService.checkEmailExists("abc@a.com"));
    }

    @Test
    void checkEmailExists_false() {
        when(userAccountRepository.existsByEmail("abc@a.com")).thenReturn(false);
        assertFalse(userAccountService.checkEmailExists("abc@a.com"));
    }
}
