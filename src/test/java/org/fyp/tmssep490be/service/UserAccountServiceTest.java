package org.fyp.tmssep490be.service;

import org.fyp.tmssep490be.dtos.user.UpdateUserRequest;
import org.fyp.tmssep490be.entities.Role;
import org.fyp.tmssep490be.services.UserAccountService;
import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private UserBranchesRepository userBranchesRepository;

    @InjectMocks
    private UserAccountService userAccountService;

    private CreateUserRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequest();
        validRequest.setEmail("huyentrang@gmail.com");
        validRequest.setPassword("12345678");
        validRequest.setFullName("Huyen Trang");
        validRequest.setGender(Gender.FEMALE);
        validRequest.setStatus(UserStatus.ACTIVE);
        validRequest.setRoleIds(Set.of(1L));
    }

    @Test
    void createUser_WhenEmailExists_ShouldThrowException() {
        when(userAccountRepository.existsByEmail("huyentrang@gmail.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> userAccountService.createUser(validRequest));
    }

    @Test
    void createUser_WhenValidRequest_ShouldReturnUserResponse() {
        when(userAccountRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        Role mockRole = new Role();
        mockRole.setId(1L);
        mockRole.setCode("ADMIN");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));


        UserResponse response = userAccountService.createUser(validRequest);

        assertNotNull(response);
        assertEquals("huyentrang@gmail.com", response.getEmail());
        verify(userAccountRepository).save(any(UserAccount.class));
    }

}